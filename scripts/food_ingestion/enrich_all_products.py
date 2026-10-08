#!/usr/bin/env python3
"""
Albert Heijn High-Speed Product Image & Metadata Enrichment Pipeline
----------------------------------------------------------------------
Enriches all existing products in 'ah_master_ingest.db' with high-resolution
product image URLs (static.ah.nl DAM CDN) and missing price/unit metadata using
the official AH mobile v2 API.

Then re-compiles 'app/src/main/assets/databases/gains_database.db'.
"""

import os
import sys
import time
import json
import re
import sqlite3
import requests
from typing import Dict, List, Optional, Any, Tuple

from build_food_database import (
    get_anonymous_token,
    compile_gains_room_database,
    AH_CATEGORIES,
    HEADERS,
    INGEST_DB_PATH
)

def get_mobile_headers(token: str) -> Dict[str, str]:
    h = dict(HEADERS)
    if token:
        h["Authorization"] = f"Bearer {token}"
    h["X-Application"] = "AHWEBSHOP"
    return h

def extract_best_image_url(images: List[Dict[str, Any]]) -> Optional[str]:
    if not images:
        return None
    # Prefer 400x400 WEBP or 200x200 WEBP
    for img in images:
        if img.get("width") in (400, 200):
            return img.get("url")
    return images[0].get("url")

def run_category_image_sweep(conn: sqlite3.Connection, token: str):
    """Pass 1: Broad sweep across all 100+ Albert Heijn food categories via v2 REST search API."""
    cur = conn.cursor()
    headers = get_mobile_headers(token)

    cur.execute("SELECT count(*) FROM ah_products WHERE imageUrl IS NOT NULL AND imageUrl != ''")
    initial_count = cur.fetchone()[0]

    print(f"\n[PASS 1] Starting Category Image Sweep across {len(AH_CATEGORIES)} categories...", flush=True)
    print(f"         Initial products with image: {initial_count}", flush=True)

    total_categories = len(AH_CATEGORIES)
    for cat_idx, category in enumerate(AH_CATEGORIES, 1):
        page = 0
        max_pages = 15  # Up to 15 pages (450 items) per category

        while page < max_pages:
            url = f"https://api.ah.nl/mobile-services/product/search/v2?query={category}&page={page}&size=30"
            try:
                res = requests.get(url, headers=headers, timeout=8)
                if res.status_code == 429:
                    print("  [WAIT] Rate limited (429). Waiting 5s...", flush=True)
                    time.sleep(5)
                    token = get_anonymous_token()
                    headers = get_mobile_headers(token)
                    continue

                if res.status_code != 200:
                    break

                products = res.json().get("products", [])
                if not products:
                    break

                updates = []
                for p in products:
                    prod_id = str(p.get("webshopId") or p.get("id") or "")
                    img_url = extract_best_image_url(p.get("images") or [])
                    if prod_id and img_url:
                        updates.append((img_url, prod_id))

                if updates:
                    cur.executemany("""
                        UPDATE ah_products 
                        SET imageUrl = ? 
                        WHERE id = ? AND (imageUrl IS NULL OR imageUrl = '')
                    """, updates)
                    conn.commit()

                page += 1
                time.sleep(0.1)
            except Exception as e:
                print(f"  [WARN] Exception on category '{category}' page {page}: {e}", flush=True)
                time.sleep(1)
                break

        if cat_idx % 10 == 0 or cat_idx == total_categories:
            cur.execute("SELECT count(*) FROM ah_products WHERE imageUrl IS NOT NULL AND imageUrl != ''")
            cnt = cur.fetchone()[0]
            print(f"  [{cat_idx}/{total_categories}] Category '{category}' done -> DB items with imageUrl: {cnt}", flush=True)

def run_targeted_title_fallback(conn: sqlite3.Connection, token: str):
    """Pass 2: Targeted title query for remaining products missing imageUrl."""
    cur = conn.cursor()
    headers = get_mobile_headers(token)

    cur.execute("SELECT id, title, brand FROM ah_products WHERE imageUrl IS NULL OR imageUrl = ''")
    missing_rows = cur.fetchall()

    print(f"\n[PASS 2] Starting Targeted Title Fallback for {len(missing_rows)} products...", flush=True)
    if not missing_rows:
        print("         No products missing images! All set.", flush=True)
        return

    success_count = 0
    for idx, (prod_id, title, brand) in enumerate(missing_rows, 1):
        # Clean title for searching: take first 3 words, remove weight specs, pack numbers
        clean = re.sub(r'\b\d+-(pk|pack|stuks|st)\b', '', title, flags=re.IGNORECASE)
        clean = re.sub(r'\b\d+(g|kg|ml|l)\b', '', clean, flags=re.IGNORECASE)
        words = clean.strip().split()
        search_query = " ".join(words[:3]) if len(words) >= 3 else title

        if not search_query:
            continue

        url = f"https://api.ah.nl/mobile-services/product/search/v2?query={search_query}&size=10"
        try:
            res = requests.get(url, headers=headers, timeout=6)
            if res.status_code == 429:
                time.sleep(3)
                token = get_anonymous_token()
                headers = get_mobile_headers(token)
                continue

            if res.status_code == 200:
                prods = res.json().get("products", [])
                match_img = None
                for p in prods:
                    p_id = str(p.get("webshopId") or p.get("id") or "")
                    if p_id == prod_id:
                        match_img = extract_best_image_url(p.get("images") or [])
                        break
                if not match_img and prods:
                    # Fallback to title substring match
                    for p in prods:
                        p_title = (p.get("title") or "").lower()
                        if title.lower() in p_title or p_title in title.lower():
                            match_img = extract_best_image_url(p.get("images") or [])
                            break

                if match_img:
                    cur.execute("UPDATE ah_products SET imageUrl = ? WHERE id = ?", (match_img, prod_id))
                    conn.commit()
                    success_count += 1
        except Exception:
            pass

        if idx % 100 == 0 or idx == len(missing_rows):
            print(f"  [{idx}/{len(missing_rows)}] Targeted title searches complete | Found images for +{success_count} items", flush=True)
        time.sleep(0.15)

def main():
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

    print("=========================================================", flush=True)
    print("  Albert Heijn Product Image Enrichment Pipeline        ", flush=True)
    print("=========================================================", flush=True)

    conn = sqlite3.connect(INGEST_DB_PATH, timeout=30.0)
    conn.execute("PRAGMA journal_mode=WAL")
    
    token = get_anonymous_token()

    # Run Pass 1: Category Sweep
    run_category_image_sweep(conn, token)

    # Run Pass 2: Targeted Title Fallback for remaining items
    run_targeted_title_fallback(conn, token)

    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM ah_products")
    total = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM ah_products WHERE imageUrl IS NOT NULL AND imageUrl != ''")
    with_img = cur.fetchone()[0]

    print(f"\n[RESULT] Final DB State: {with_img} / {total} products ({with_img/total*100:.1f}%) have valid Albert Heijn CDN image URLs.", flush=True)

    # Re-compile Room Database Asset
    print("\n[RE-COMPILE] Writing enriched products into app/src/main/assets/databases/gains_database.db...", flush=True)
    compile_gains_room_database(conn)
    conn.close()

if __name__ == "__main__":
    main()
