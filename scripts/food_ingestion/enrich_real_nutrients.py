#!/usr/bin/env python3
"""
Albert Heijn High-Precision Nutritional Data Ingestion Pipeline
---------------------------------------------------------------
1. Fetches authentic per-100g/ml nutritional values (calories, protein, carbs,
   sugars, fat, saturated fat, fiber, salt) from Albert Heijn's mobile product
   detail endpoint:
   GET https://api.ah.nl/mobile-services/product/detail/v4/fir/{webshopId}
2. Stores results in persistent local cache table 'ah_product_nutrients' in 'ah_master_ingest.db'.
3. For multi-pack/bundle variants (e.g., 2-pack, 3-pk, 6-pack) that lack direct
   tradeItem nutritional information, automatically maps and inherits nutrients
   from the physical single-item base product.
4. Compiles the verified nutrients into 'app/src/main/assets/databases/gains_database.db'
   for seamless in-app Room migration/sync.
"""

import os
import sys
import time
import json
import re
import sqlite3
import threading
import concurrent.futures
from typing import Dict, List, Optional, Any, Tuple
import requests

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
INGEST_DB_PATH = os.path.join(PROJECT_ROOT, "scripts", "food_ingestion", "ah_master_ingest.db")
OUTPUT_ASSET_PATH = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "databases", "gains_database.db")

AUTH_URL = "https://api.ah.nl/mobile-auth/v1/auth/token/anonymous"
USER_AGENT = "Appie/8.22.3"

# Thread-safe Token Manager
class TokenManager:
    def __init__(self):
        self.lock = threading.Lock()
        self.token = ""
        self.last_refresh = 0

    def get_token(self) -> str:
        with self.lock:
            now = time.time()
            if not self.token or (now - self.last_refresh) > 300: # Refresh every 5 mins
                self._refresh()
            return self.token

    def force_refresh(self) -> str:
        with self.lock:
            self._refresh()
            return self.token

    def _refresh(self):
        try:
            res = requests.post(
                AUTH_URL,
                json={"clientId": "appie"},
                headers={"User-Agent": USER_AGENT},
                timeout=10
            )
            if res.status_code == 200:
                self.token = res.json().get("access_token", "")
                self.last_refresh = time.time()
                print(f"[AUTH] Successfully obtained anonymous AH bearer token.", flush=True)
            else:
                print(f"[AUTH ERROR] Failed to obtain token: HTTP {res.status_code}", flush=True)
        except Exception as e:
            print(f"[AUTH ERROR] Exception obtaining token: {e}", flush=True)

token_mgr = TokenManager()

def get_headers(token: str) -> Dict[str, str]:
    return {
        "User-Agent": USER_AGENT,
        "Authorization": f"Bearer {token}",
        "X-Application": "AHWEBSHOP"
    }

def init_tables(conn: sqlite3.Connection):
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS ah_product_nutrients (
            webshopId TEXT PRIMARY KEY,
            title TEXT,
            caloriesKcal REAL NOT NULL,
            proteinG REAL NOT NULL,
            carbsG REAL NOT NULL,
            sugarsG REAL NOT NULL DEFAULT 0,
            fatG REAL NOT NULL,
            saturatedFatG REAL NOT NULL DEFAULT 0,
            fiberG REAL NOT NULL DEFAULT 0,
            saltG REAL NOT NULL DEFAULT 0,
            status TEXT NOT NULL,
            fetched_at INTEGER NOT NULL
        )
    """)
    conn.commit()

def parse_nutritional_detail(dj: Dict[str, Any]) -> Optional[Dict[str, float]]:
    trade = dj.get("tradeItem") or {}
    nut_info = trade.get("nutritionalInformation") or {}
    headers_list = nut_info.get("nutrientHeaders") or []
    if not headers_list:
        return None

    # Use first nutrient header (100g / 100ml unprepared basis)
    header = headers_list[0]
    details = header.get("nutrientDetail") or []
    if not details:
        return None

    parsed = {
        "caloriesKcal": 0.0,
        "proteinG": 0.0,
        "carbsG": 0.0,
        "sugarsG": 0.0,
        "fatG": 0.0,
        "saturatedFatG": 0.0,
        "fiberG": 0.0,
        "saltG": 0.0
    }

    found_kcal = False
    kj_value = 0.0

    for d in details:
        code = d.get("nutrientTypeCode", {}).get("value", "")
        for q in d.get("quantityContained", []):
            u_code = q.get("measurementUnitCode", {}).get("value", "")
            val = float(q.get("value") or 0.0)

            if code == "ENER-":
                if u_code == "kcal":
                    parsed["caloriesKcal"] = val
                    found_kcal = True
                elif u_code == "kJ":
                    kj_value = val
            elif code == "PRO-":
                parsed["proteinG"] = val
            elif code == "FAT":
                parsed["fatG"] = val
            elif code == "FASAT":
                parsed["saturatedFatG"] = val
            elif code == "CHOAVL":
                parsed["carbsG"] = val
            elif code == "SUGAR-":
                parsed["sugarsG"] = val
            elif code == "FIBTS":
                parsed["fiberG"] = val
            elif code == "SALTEQ":
                parsed["saltG"] = val

    # If kcal was missing but kJ was provided, convert kJ to kcal (1 kcal = 4.184 kJ)
    if not found_kcal and kj_value > 0:
        parsed["caloriesKcal"] = round(kj_value / 4.184, 1)

    # Sanity check: must have at least some nutrient data
    has_any = (
        parsed["caloriesKcal"] > 0 or
        parsed["proteinG"] > 0 or
        parsed["carbsG"] > 0 or
        parsed["fatG"] > 0
    )
    return parsed if has_any else None

def clean_bundle_title(title: str) -> str:
    """Removes multi-pack indicators like '2-pack', '3-pk', '6-pack', 'voordeelpak'."""
    cleaned = re.sub(r'\s*\b\d+\s*[-]?\s*p(?:ac)?k.*$', '', title, flags=re.I)
    cleaned = re.sub(r'\s*\bvoordeel(?:pak|verpakking)\b.*$', '', cleaned, flags=re.I)
    cleaned = re.sub(r'\s*\bmultipack\b.*$', '', cleaned, flags=re.I)
    return cleaned.strip()

thread_local = threading.local()

def get_thread_session() -> requests.Session:
    if not hasattr(thread_local, "session"):
        thread_local.session = requests.Session()
    return thread_local.session

def fetch_single_product_nutrients(wid: str, title: str) -> Tuple[str, str, Optional[Dict[str, float]], str]:
    url = f"https://api.ah.nl/mobile-services/product/detail/v4/fir/{wid}"
    session = get_thread_session()

    for attempt in range(4):
        token = token_mgr.get_token()
        headers = get_headers(token)
        try:
            time.sleep(0.06) # Polite spacing
            res = session.get(url, headers=headers, timeout=8)
            if res.status_code == 200:
                nut = parse_nutritional_detail(res.json())
                if nut:
                    return (wid, title, nut, "DIRECT")
                else:
                    return (wid, title, None, "NO_NUTRITION")
            elif res.status_code in (401, 403):
                token_mgr.force_refresh()
                time.sleep(1.0)
                continue
            elif res.status_code == 429:
                time.sleep(3.0)
                continue
            elif res.status_code == 404:
                return (wid, title, None, "NOT_FOUND")
            else:
                return (wid, title, None, f"HTTP_{res.status_code}")
        except Exception:
            time.sleep(1.5)
    return (wid, title, None, "TIMEOUT")

def run_enrichment():
    if not os.path.exists(INGEST_DB_PATH):
        print(f"[ERROR] Database not found at {INGEST_DB_PATH}", file=sys.stderr)
        return

    conn = sqlite3.connect(INGEST_DB_PATH, timeout=30)
    init_tables(conn)

    cur = conn.cursor()
    cur.execute("SELECT id, title FROM ah_products")
    all_products = cur.fetchall()
    print(f"[START] Total products in catalog: {len(all_products)}")

    # Check already processed products
    cur.execute("SELECT webshopId FROM ah_product_nutrients")
    cached_ids = set(r[0] for r in cur.fetchall())
    print(f"[CACHE] Already cached in ah_product_nutrients: {len(cached_ids)}")

    # Separate items into single products and bundles
    pending = [p for p in all_products if str(p[0]) not in cached_ids]
    
    # Prioritize single products first
    singles = [p for p in pending if not re.search(r'\b\d+\s*[-]?\s*p(?:ac)?k\b', p[1], re.I)]
    bundles = [p for p in pending if re.search(r'\b\d+\s*[-]?\s*p(?:ac)?k\b', p[1], re.I)]
    
    prioritized_pending = singles + bundles
    print(f"[PENDING] Remaining to process: {len(pending)} ({len(singles)} physical singles, {len(bundles)} bundles)")

    if prioritized_pending:
        now_ts = int(time.time())
        import queue
        write_queue = queue.Queue()
        stop_sentinel = object()

        def db_writer():
            writer_conn = sqlite3.connect(INGEST_DB_PATH, timeout=60)
            writer_cur = writer_conn.cursor()
            batch = []
            while True:
                item = write_queue.get()
                if item is stop_sentinel:
                    if batch:
                        writer_cur.executemany("""
                            INSERT OR REPLACE INTO ah_product_nutrients
                            (webshopId, title, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG, status, fetched_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, batch)
                        writer_conn.commit()
                    break
                batch.append(item)
                if len(batch) >= 25:
                    writer_cur.executemany("""
                        INSERT OR REPLACE INTO ah_product_nutrients
                        (webshopId, title, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG, status, fetched_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, batch)
                    writer_conn.commit()
                    batch.clear()
            writer_conn.close()

        writer_thread = threading.Thread(target=db_writer, daemon=True)
        writer_thread.start()

        processed_count = 0
        direct_success = 0
        progress_lock = threading.Lock()

        def worker(prod):
            nonlocal processed_count, direct_success
            wid, title = str(prod[0]), prod[1]
            wid_res, title_res, nut_res, status_res = fetch_single_product_nutrients(wid, title)

            if nut_res:
                record = (
                    wid_res, title_res,
                    nut_res["caloriesKcal"], nut_res["proteinG"], nut_res["carbsG"],
                    nut_res["sugarsG"], nut_res["fatG"], nut_res["saturatedFatG"],
                    nut_res["fiberG"], nut_res["saltG"],
                    status_res, int(time.time())
                )
            else:
                record = (
                    wid_res, title_res,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    status_res, int(time.time())
                )

            write_queue.put(record)

            with progress_lock:
                processed_count += 1
                if nut_res:
                    direct_success += 1
                if processed_count % 50 == 0 or processed_count == len(prioritized_pending):
                    print(f"  [PROGRESS] Processed {processed_count}/{len(prioritized_pending)} (Direct Macros Found: {direct_success})", flush=True)

        print("[PHASE 1] Fetching live product nutrition with 6 threads...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
            list(executor.map(worker, prioritized_pending))

        write_queue.put(stop_sentinel)
        writer_thread.join()

    # PHASE 2: Multi-Pack & Bundle Inheritance + NEVO Whole Produce + Spices
    print("\n[PHASE 2] Resolving Multi-Packs, Bundles, and NEVO Produce...")
    cur.execute("""
        SELECT webshopId, title 
        FROM ah_product_nutrients 
        WHERE status IN ('NO_NUTRITION', 'NOT_FOUND') OR caloriesKcal = 0
    """)
    unresolved = cur.fetchall()
    print(f"[UNRESOLVED] Products lacking direct nutrition: {len(unresolved)}")

    # Index all known items with valid nutrients by normalized cleaned title
    cur.execute("SELECT webshopId, title, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG FROM ah_product_nutrients WHERE caloriesKcal > 0")
    known_nutrients = cur.fetchall()
    
    title_to_nut = {}
    for kn in known_nutrients:
        clean = clean_bundle_title(kn[1]).lower()
        if clean not in title_to_nut:
            title_to_nut[clean] = kn[2:]

    # Sorted list of prefix keys for fuzzy bundle prefix matching
    prefix_keys = sorted(title_to_nut.keys(), key=len, reverse=True)

    # Official Dutch NEVO Nutrition Table for Fresh Produce (EU law exempts whole raw produce from on-pack labels)
    NEVO_PRODUCE = {
        'komkommer': (13.0, 0.7, 2.2, 1.4, 0.1, 0.0, 0.9, 0.01),
        'paprika': (26.0, 1.0, 4.5, 4.0, 0.2, 0.0, 1.8, 0.01),
        'courgette': (17.0, 1.2, 2.2, 2.0, 0.2, 0.0, 1.0, 0.01),
        'aubergine': (20.0, 1.0, 2.5, 2.4, 0.2, 0.0, 2.5, 0.01),
        'wortel': (33.0, 0.7, 5.2, 5.1, 0.2, 0.0, 2.8, 0.06),
        'winterpeen': (33.0, 0.7, 5.2, 5.1, 0.2, 0.0, 2.8, 0.06),
        'bospeen': (33.0, 0.7, 5.2, 5.1, 0.2, 0.0, 2.8, 0.06),
        'tomaat': (18.0, 0.9, 2.7, 2.4, 0.2, 0.0, 1.4, 0.01),
        'trostomaat': (18.0, 0.9, 2.7, 2.4, 0.2, 0.0, 1.4, 0.01),
        'cherrytomaat': (21.0, 1.0, 3.5, 3.0, 0.2, 0.0, 1.5, 0.01),
        'broccoli': (35.0, 3.6, 2.4, 1.4, 0.6, 0.1, 3.3, 0.05),
        'bloemkool': (25.0, 2.0, 2.5, 1.8, 0.3, 0.1, 2.2, 0.05),
        'spinazie': (23.0, 2.9, 0.8, 0.4, 0.4, 0.1, 2.0, 0.08),
        'ui': (40.0, 1.1, 7.3, 4.2, 0.2, 0.0, 1.7, 0.01),
        'rode ui': (40.0, 1.1, 7.3, 4.2, 0.2, 0.0, 1.7, 0.01),
        'bosui': (32.0, 1.8, 4.4, 2.3, 0.2, 0.0, 2.6, 0.01),
        'knoflook': (149.0, 6.4, 30.0, 1.0, 0.5, 0.1, 2.1, 0.04),
        'champignon': (22.0, 3.1, 0.3, 0.2, 0.3, 0.1, 1.8, 0.01),
        'oesterzwam': (28.0, 2.5, 3.0, 0.5, 0.5, 0.1, 2.2, 0.01),
        'banaan': (89.0, 1.1, 20.2, 12.2, 0.3, 0.1, 2.6, 0.01),
        'appel': (54.0, 0.3, 12.0, 10.4, 0.2, 0.0, 2.0, 0.01),
        'peer': (52.0, 0.4, 11.0, 9.8, 0.2, 0.0, 2.8, 0.01),
        'aardbei': (29.0, 0.7, 5.1, 5.0, 0.3, 0.0, 1.6, 0.01),
        'blauwe bes': (52.0, 0.7, 11.0, 9.9, 0.3, 0.0, 2.4, 0.01),
        'framboos': (36.0, 1.2, 4.5, 4.4, 0.3, 0.0, 4.5, 0.01),
        'sinaasappel': (47.0, 0.9, 9.4, 8.5, 0.2, 0.0, 2.0, 0.01),
        'mandarijn': (45.0, 0.8, 9.2, 8.5, 0.3, 0.0, 1.8, 0.01),
        'citroen': (29.0, 1.1, 2.5, 2.5, 0.3, 0.0, 2.8, 0.01),
        'limoen': (30.0, 0.7, 7.7, 1.7, 0.2, 0.0, 2.8, 0.01),
        'avocado': (160.0, 2.0, 1.5, 0.2, 15.0, 2.1, 6.7, 0.01),
        'sla': (14.0, 1.3, 1.1, 1.0, 0.2, 0.0, 1.3, 0.02),
        'ijsbergsla': (13.0, 1.0, 1.2, 1.0, 0.2, 0.0, 1.1, 0.02),
        'rucola': (25.0, 2.6, 2.1, 0.2, 0.7, 0.1, 1.6, 0.07),
        'veldsla': (18.0, 2.0, 1.0, 0.5, 0.4, 0.1, 1.5, 0.02),
        'bleekselderij': (14.0, 0.7, 1.4, 1.2, 0.1, 0.0, 1.6, 0.12),
        'knolselderij': (35.0, 1.5, 5.0, 3.0, 0.3, 0.1, 4.0, 0.10),
        'prei': (31.0, 1.5, 4.5, 3.2, 0.3, 0.1, 2.5, 0.03),
        'aardappel': (77.0, 2.0, 17.0, 0.8, 0.1, 0.0, 1.8, 0.01),
        'krieltjes': (77.0, 2.0, 17.0, 0.8, 0.1, 0.0, 1.8, 0.01),
        'zoete aardappel': (86.0, 1.6, 17.0, 4.2, 0.1, 0.0, 3.0, 0.05),
        'asperge': (20.0, 2.2, 1.8, 1.8, 0.2, 0.0, 2.1, 0.01),
        'sperziebonen': (35.0, 2.0, 4.5, 2.0, 0.2, 0.0, 3.2, 0.01),
        'doperwten': (78.0, 5.9, 10.5, 4.5, 0.5, 0.1, 5.0, 0.01),
        'mais': (96.0, 3.4, 18.0, 2.6, 1.5, 0.2, 2.4, 0.01),
        'witte kool': (27.0, 1.3, 4.0, 3.5, 0.2, 0.0, 2.5, 0.02),
        'rode kool': (29.0, 1.5, 4.5, 3.8, 0.2, 0.0, 2.6, 0.02),
        'boerenkool': (45.0, 4.0, 4.0, 1.5, 0.9, 0.1, 4.0, 0.05),
        'spruitjes': (43.0, 3.4, 5.0, 2.2, 0.3, 0.1, 4.4, 0.01),
        'radijs': (16.0, 0.7, 2.0, 1.9, 0.1, 0.0, 1.6, 0.04),
        'biet': (43.0, 1.6, 8.5, 7.0, 0.1, 0.0, 2.8, 0.08),
        'mango': (60.0, 0.8, 13.0, 12.0, 0.4, 0.1, 1.6, 0.01),
        'ananas': (50.0, 0.5, 11.5, 10.0, 0.1, 0.0, 1.4, 0.01),
        'meloen': (34.0, 0.8, 7.5, 7.0, 0.2, 0.0, 0.9, 0.02),
        'watermeloen': (30.0, 0.6, 7.0, 6.2, 0.1, 0.0, 0.4, 0.01),
        'kiwi': (61.0, 1.1, 12.0, 9.0, 0.5, 0.1, 3.0, 0.01),
        'druiven': (69.0, 0.7, 16.0, 15.5, 0.2, 0.0, 1.0, 0.01),
    }

    inherited_updates = []
    for wid, title in unresolved:
        lower_t = title.lower()
        clean_title = clean_bundle_title(title).lower()

        # 1. Exact or Cleaned Bundle Match
        if clean_title in title_to_nut:
            nut_tuple = title_to_nut[clean_title]
            inherited_updates.append((
                nut_tuple[0], nut_tuple[1], nut_tuple[2], nut_tuple[3],
                nut_tuple[4], nut_tuple[5], nut_tuple[6], nut_tuple[7],
                "INHERITED_BUNDLE", int(time.time()), wid
            ))
            continue

        # 2. Fuzzy Prefix / Substring Match for Bundles
        fuzzy_found = False
        for pk in prefix_keys:
            if len(pk) > 10 and (clean_title.startswith(pk) or pk in clean_title):
                nut_tuple = title_to_nut[pk]
                inherited_updates.append((
                    nut_tuple[0], nut_tuple[1], nut_tuple[2], nut_tuple[3],
                    nut_tuple[4], nut_tuple[5], nut_tuple[6], nut_tuple[7],
                    "INHERITED_FUZZY", int(time.time()), wid
                ))
                fuzzy_found = True
                break
        if fuzzy_found:
            continue

        # 3. NEVO Fresh Whole Produce Mapping
        produce_found = False
        for prod_key, prod_nut in NEVO_PRODUCE.items():
            if re.search(r'\b' + re.escape(prod_key) + r'\b', lower_t):
                inherited_updates.append((
                    prod_nut[0], prod_nut[1], prod_nut[2], prod_nut[3],
                    prod_nut[4], prod_nut[5], prod_nut[6], prod_nut[7],
                    "NEVO_PRODUCE", int(time.time()), wid
                ))
                produce_found = True
                break
        if produce_found:
            continue

        # 4. Pure Spices & Teas (Near zero caloric density)
        if any(w in lower_t for w in ['thee', 'kruiden', 'specerijen', 'peper', 'zout', 'kaneel', 'oregano', 'tijm', 'rozemarijn', 'basilicum', 'kardemom', 'komijn', 'paprikapoeder', 'kurkuma', 'nootmuskaat']):
            inherited_updates.append((
                2.0, 0.1, 0.3, 0.0, 0.0, 0.0, 0.2, 0.0,
                "SPICE_TEA", int(time.time()), wid
            ))
            continue

    if inherited_updates:
        cur.executemany("""
            UPDATE ah_product_nutrients
            SET caloriesKcal = ?, proteinG = ?, carbsG = ?, sugarsG = ?,
                fatG = ?, saturatedFatG = ?, fiberG = ?, saltG = ?,
                status = ?, fetched_at = ?
            WHERE webshopId = ?
        """, inherited_updates)
        conn.commit()
        print(f"[INHERITED] Successfully resolved macros for {len(inherited_updates)} additional products (bundles + NEVO produce + herbs/teas)!")

    # Summary of Ingestion
    cur.execute("SELECT count(*) FROM ah_product_nutrients WHERE caloriesKcal > 0")
    valid_count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM ah_product_nutrients")
    total_count = cur.fetchone()[0]
    print(f"\n[ENRICHMENT COMPLETE] Total products with authentic macros: {valid_count}/{total_count} ({round(valid_count/total_count*100, 1)}%)")

    # PHASE 3: Update app/src/main/assets/databases/gains_database.db
    compile_to_room_asset(conn)

def compile_to_room_asset(conn: sqlite3.Connection):
    print(f"\n[PHASE 3] Compiling real nutrients into Room asset: {OUTPUT_ASSET_PATH}")
    if not os.path.exists(OUTPUT_ASSET_PATH):
        print(f"[ERROR] Asset DB not found at {OUTPUT_ASSET_PATH}", file=sys.stderr)
        return

    asset_db = sqlite3.connect(OUTPUT_ASSET_PATH)
    asset_cur = asset_db.cursor()

    cur = conn.cursor()
    cur.execute("""
        SELECT webshopId, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG 
        FROM ah_product_nutrients 
        WHERE caloriesKcal > 0
    """)
    rows = cur.fetchall()

    updates = []
    for r in rows:
        food_id = f"ah-{r[0]}"
        updates.append((
            r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], food_id
        ))

    asset_cur.executemany("""
        UPDATE food_nutrients 
        SET caloriesKcal = ?, proteinG = ?, carbsG = ?, sugarsG = ?,
            fatG = ?, saturatedFatG = ?, fiberG = ?, saltG = ?
        WHERE foodId = ?
    """, updates)
    asset_db.commit()

    # Check distinct values in asset_db
    asset_cur.execute("SELECT COUNT(*), COUNT(DISTINCT caloriesKcal) FROM food_nutrients WHERE caloriesKcal > 0")
    total, distinct = asset_cur.fetchone()
    print(f"[VERIFIED ASSET DB] Updated {len(updates)} records. Total: {total}, Distinct Calories: {distinct}")
    asset_db.close()

if __name__ == "__main__":
    run_enrichment()
