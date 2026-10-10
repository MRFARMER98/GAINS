#!/usr/bin/env python3
"""
Bundle-to-Single Live Resolution Pipeline
-----------------------------------------
For all remaining unresolved bundle/multi-pack products in ah_product_nutrients:
1. Extracts clean base title (e.g. 'Calve Pindakaas crunchy 2-pack' -> 'Calve Pindakaas crunchy')
2. Queries AH search API for the underlying physical single-item product
3. Fetches official nutritional values from detail/v4/fir/{single_id}
4. Updates all matching bundles and recompiles gains_database.db.
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

class TokenManager:
    def __init__(self):
        self.lock = threading.Lock()
        self.token = ""
        self.last_refresh = 0

    def get_token(self) -> str:
        with self.lock:
            now = time.time()
            if not self.token or (now - self.last_refresh) > 300:
                self._refresh()
            return self.token

    def force_refresh(self) -> str:
        with self.lock:
            self._refresh()
            return self.token

    def _refresh(self):
        try:
            res = requests.post(AUTH_URL, json={"clientId": "appie"}, headers={"User-Agent": USER_AGENT}, timeout=10)
            if res.status_code == 200:
                self.token = res.json().get("access_token", "")
                self.last_refresh = time.time()
        except Exception:
            pass

token_mgr = TokenManager()
thread_local = threading.local()

def get_session():
    if not hasattr(thread_local, "session"):
        thread_local.session = requests.Session()
    return thread_local.session

def get_headers():
    return {
        "User-Agent": USER_AGENT,
        "Authorization": f"Bearer {token_mgr.get_token()}",
        "X-Application": "AHWEBSHOP"
    }

def clean_title(title: str) -> str:
    cleaned = re.sub(r'\s*\b\d+\s*[-]?\s*(?:p(?:ac)?k|pck|packb)\b.*$', '', title, flags=re.I)
    cleaned = re.sub(r'\s*\bvoordeel(?:pak|verpakking)\b.*$', '', cleaned, flags=re.I)
    cleaned = re.sub(r'\s*\bmultipack\b.*$', '', cleaned, flags=re.I)
    return cleaned.strip()

def parse_nutritional_detail(dj: Dict[str, Any]) -> Optional[Dict[str, float]]:
    trade = dj.get("tradeItem") or {}
    nut_info = trade.get("nutritionalInformation") or {}
    headers_list = nut_info.get("nutrientHeaders") or []
    if not headers_list: return None
    details = headers_list[0].get("nutrientDetail") or []
    if not details: return None

    parsed = {"kcal": 0.0, "prot": 0.0, "carbs": 0.0, "sug": 0.0, "fat": 0.0, "sat_fat": 0.0, "fib": 0.0, "salt": 0.0}
    found_kcal = False
    kj = 0.0
    for d in details:
        code = d.get("nutrientTypeCode", {}).get("value", "")
        for q in d.get("quantityContained", []):
            u = q.get("measurementUnitCode", {}).get("value", "")
            v = float(q.get("value") or 0.0)
            if code == "ENER-":
                if u == "kcal": parsed["kcal"] = v; found_kcal = True
                elif u == "kJ": kj = v
            elif code == "PRO-": parsed["prot"] = v
            elif code == "FAT": parsed["fat"] = v
            elif code == "FASAT": parsed["sat_fat"] = v
            elif code == "CHOAVL": parsed["carbs"] = v
            elif code == "SUGAR-": parsed["sug"] = v
            elif code == "FIBTS": parsed["fib"] = v
            elif code == "SALTEQ": parsed["salt"] = v
    if not found_kcal and kj > 0:
        parsed["kcal"] = round(kj / 4.184, 1)
    return parsed if parsed["kcal"] > 0 else None

def search_and_fetch_single(clean_query: str) -> Optional[Dict[str, float]]:
    session = get_session()
    search_url = f"https://api.ah.nl/mobile-services/product/search/v2?query={requests.utils.quote(clean_query)}&page=0&size=3"
    try:
        time.sleep(0.05)
        s_res = session.get(search_url, headers=get_headers(), timeout=6)
        if s_res.status_code in (401, 403):
            token_mgr.force_refresh()
            s_res = session.get(search_url, headers=get_headers(), timeout=6)
        if s_res.status_code != 200:
            return None
        products = s_res.json().get("products", [])
        for p in products:
            wid = p.get("webshopId")
            if not wid: continue
            det_url = f"https://api.ah.nl/mobile-services/product/detail/v4/fir/{wid}"
            d_res = session.get(det_url, headers=get_headers(), timeout=6)
            if d_res.status_code == 200:
                nut = parse_nutritional_detail(d_res.json())
                if nut:
                    return nut
    except Exception:
        pass
    return None

def run():
    conn = sqlite3.connect(INGEST_DB_PATH, timeout=60)
    cur = conn.cursor()

    cur.execute("SELECT webshopId, title FROM ah_product_nutrients WHERE caloriesKcal = 0")
    unresolved = cur.fetchall()
    print(f"[START] Unresolved items: {len(unresolved)}")

    # Group unresolved by clean base title
    base_to_wids: Dict[str, List[str]] = {}
    for wid, title in unresolved:
        c = clean_title(title)
        if len(c) > 3:
            base_to_wids.setdefault(c, []).append(wid)

    unique_bases = list(base_to_wids.keys())
    print(f"[GROUPING] Found {len(unique_bases)} unique base titles to resolve via live single-item search.")

    import queue
    write_queue = queue.Queue()
    stop_sentinel = object()

    def db_writer():
        w_conn = sqlite3.connect(INGEST_DB_PATH, timeout=60)
        w_cur = w_conn.cursor()
        while True:
            item = write_queue.get()
            if item is stop_sentinel:
                break
            wids, nut = item
            for wid in wids:
                w_cur.execute("""
                    UPDATE ah_product_nutrients
                    SET caloriesKcal = ?, proteinG = ?, carbsG = ?, sugarsG = ?,
                        fatG = ?, saturatedFatG = ?, fiberG = ?, saltG = ?,
                        status = 'SEARCH_SINGLE', fetched_at = ?
                    WHERE webshopId = ?
                """, (nut["kcal"], nut["prot"], nut["carbs"], nut["sug"], nut["fat"], nut["sat_fat"], nut["fib"], nut["salt"], int(time.time()), wid))
            w_conn.commit()
        w_conn.close()

    writer_t = threading.Thread(target=db_writer, daemon=True)
    writer_t.start()

    processed = 0
    resolved_bases = 0
    total_bundles_resolved = 0
    progress_lock = threading.Lock()

    def worker(base_query):
        nonlocal processed, resolved_bases, total_bundles_resolved
        nut = search_and_fetch_single(base_query)
        wids = base_to_wids[base_query]
        if nut:
            write_queue.put((wids, nut))
        with progress_lock:
            processed += 1
            if nut:
                resolved_bases += 1
                total_bundles_resolved += len(wids)
            if processed % 50 == 0 or processed == len(unique_bases):
                print(f"  [SEARCH PROGRESS] {processed}/{len(unique_bases)} base queries checked (Bases Resolved: {resolved_bases}, Bundles Updated: {total_bundles_resolved})", flush=True)

    print("[PROCESSING] Querying live single items with 8 threads...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        list(executor.map(worker, unique_bases))

    write_queue.put(stop_sentinel)
    writer_t.join()

    # Summary
    cur.execute("SELECT count(*) FROM ah_product_nutrients WHERE caloriesKcal > 0")
    valid_count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM ah_product_nutrients")
    total_count = cur.fetchone()[0]
    print(f"\n[COMPLETE] Total verified products: {valid_count}/{total_count} ({round(valid_count/total_count*100, 1)}%)")

    # Recompile into gains_database.db
    print(f"\n[RECOMPILING] Updating Room asset database: {OUTPUT_ASSET_PATH}")
    asset_db = sqlite3.connect(OUTPUT_ASSET_PATH)
    asset_cur = asset_db.cursor()
    cur.execute("SELECT webshopId, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG FROM ah_product_nutrients WHERE caloriesKcal > 0")
    updates = [(r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], f"ah-{r[0]}") for r in cur.fetchall()]
    asset_cur.executemany("""
        UPDATE food_nutrients 
        SET caloriesKcal = ?, proteinG = ?, carbsG = ?, sugarsG = ?,
            fatG = ?, saturatedFatG = ?, fiberG = ?, saltG = ?
        WHERE foodId = ?
    """, updates)
    asset_db.commit()
    asset_cur.execute("SELECT COUNT(*), COUNT(DISTINCT caloriesKcal) FROM food_nutrients WHERE caloriesKcal > 0")
    tot, dist = asset_cur.fetchone()
    print(f"[ROOM ASSET READY] Total items: {tot}, Distinct calories: {dist}")
    asset_db.close()
    conn.close()

if __name__ == "__main__":
    run()
