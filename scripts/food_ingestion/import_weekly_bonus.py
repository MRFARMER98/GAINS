#!/usr/bin/env python3
"""
Albert Heijn Weekly Bonus Importer
-----------------------------------
Fetches current weekly bonus/discount deals from Albert Heijn GraphQL API
and updates the master ingestion database ('ah_master_ingest.db') and 
compiles the Room Database asset ('gains_database.db').

Preserves historical bonus entries by using validFrom (Monday 00:00:00) 
and validUntil (Sunday 23:59:59) millisecond timestamps.
"""

import os
import sys
import time
import json
import sqlite3
import requests
from datetime import datetime, time as dtime, timedelta
from typing import Dict, List, Optional, Any

from build_food_database import (
    AH_CATEGORIES,
    get_anonymous_token,
    init_ingest_db,
    compile_gains_room_database,
    GRAPHQL_URL,
    HEADERS,
    INGEST_DB_PATH
)

def get_current_week_timestamps():
    """
    Returns (validFrom_ms, validUntil_ms) for the current week.
    Monday 00:00:00 to Sunday 23:59:59.999 local time.
    """
    now = datetime.now()
    monday = now - timedelta(days=now.weekday())
    monday_start = datetime.combine(monday.date(), dtime.min)
    sunday_end = datetime.combine(monday_start.date() + timedelta(days=6), dtime.max)

    valid_from_ms = int(monday_start.timestamp() * 1000)
    valid_until_ms = int(sunday_end.timestamp() * 1000)
    return valid_from_ms, valid_until_ms

def import_weekly_bonus_deals(conn: sqlite3.Connection, auth_token: str):
    """
    Scans Albert Heijn GraphQL product catalog across categories for active bonus/discount deals.
    Inserts active deals into `bonus_deals` table using date validity ranges.
    """
    valid_from_ms, valid_until_ms = get_current_week_timestamps()
    print(f"\n=========================================================", flush=True)
    print(f"  Albert Heijn Weekly Bonus Importer                      ", flush=True)
    print(f"  Week Bounds: {datetime.fromtimestamp(valid_from_ms/1000)} -> {datetime.fromtimestamp(valid_until_ms/1000)}", flush=True)
    print(f"=========================================================\n", flush=True)

    auth_headers = dict(HEADERS)
    if auth_token:
        auth_headers["Authorization"] = f"Bearer {auth_token}"
    auth_headers["X-Application-Name"] = "AHMobileMenu"

    cur = conn.cursor()
    # Ensure bonus_deals table exists
    cur.execute("""
        CREATE TABLE IF NOT EXISTS bonus_deals (
            foodId TEXT NOT NULL,
            bonusPriceEur REAL,
            bonusType TEXT,
            validFrom INTEGER NOT NULL,
            validUntil INTEGER NOT NULL,
            PRIMARY KEY(foodId, validFrom)
        )
    """)
    conn.commit()

    total_bonus_found = 0
    total_categories = len(AH_CATEGORIES)

    gql_query = """
    query ProductSearch($input: ProductSearchInput!) {
      productSearch(input: $input) {
        products {
          id
          title
          brand
          category
          salesUnitSize
          price {
            now {
              amount
            }
            was {
              amount
            }
          }
        }
      }
    }
    """

    for cat_idx, category in enumerate(AH_CATEGORIES, 1):
        page = 0
        max_pages = 5  # Check top pages for bonus items per category
        category_bonus_count = 0

        while page < max_pages:
            time.sleep(0.3)  # Throttle requests

            try:
                payload = {
                    "query": gql_query,
                    "variables": {
                        "input": {
                            "query": category,
                            "page": page,
                            "size": 30
                        }
                    }
                }

                res = requests.post(GRAPHQL_URL, headers=auth_headers, json=payload, timeout=10)

                if res.status_code == 429:
                    print("  [WAIT] Rate limit hit (429). Retrying in 5s...", flush=True)
                    time.sleep(5)
                    token = get_anonymous_token()
                    if token:
                        auth_headers["Authorization"] = f"Bearer {token}"
                    continue

                if res.status_code == 401 or "invalid_token" in res.text:
                    token = get_anonymous_token()
                    if token:
                        auth_headers["Authorization"] = f"Bearer {token}"
                    continue

                if res.status_code != 200:
                    break

                data = res.json().get("data", {}).get("productSearch", {})
                products = data.get("products", [])
                if not products:
                    break

                for prod in products:
                    p_id = prod.get("id")
                    title = prod.get("title")
                    price_obj = prod.get("price") or {}
                    now_price = price_obj.get("now", {}).get("amount") if price_obj.get("now") else None
                    was_price = price_obj.get("was", {}).get("amount") if price_obj.get("was") else None

                    # Check if item is on bonus / discount
                    if was_price is not None and now_price is not None and was_price != now_price:
                        food_id = f"ah-{p_id}"
                        bonus_price = float(now_price)
                        bonus_type = "BONUS"

                        # Ensure raw product entry exists in ah_products
                        cur.execute("SELECT id FROM ah_products WHERE id = ?", (str(p_id),))
                        if not cur.fetchone():
                            cur.execute(
                                "INSERT OR IGNORE INTO ah_products (id, title, brand, category, gtin, unitSize, nutriScore, imageUrl, json_data, fetched_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                (str(p_id), title, prod.get("brand") or "Albert Heijn", prod.get("category") or "AH", None, prod.get("salesUnitSize") or "100g", None, None, json.dumps(prod), int(time.time() * 1000))
                            )

                        # Insert/Replace active bonus deal with date-range validity
                        cur.execute(
                            "INSERT OR REPLACE INTO bonus_deals (foodId, bonusPriceEur, bonusType, validFrom, validUntil) VALUES (?, ?, ?, ?, ?)",
                            (food_id, bonus_price, bonus_type, valid_from_ms, valid_until_ms)
                        )
                        category_bonus_count += 1
                        total_bonus_found += 1

                page += 1

            except Exception as e:
                print(f"  [WARN] Exception during category '{category}' page {page}: {e}")
                break

        if category_bonus_count > 0:
            print(f"[{cat_idx}/{total_categories}] Category '{category}': Found {category_bonus_count} active bonus deals.", flush=True)

    conn.commit()
    print(f"\n[+] Total Active Bonus Deals Ingested: {total_bonus_found}", flush=True)
    return total_bonus_found

def main():
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

    conn = sqlite3.connect(INGEST_DB_PATH, timeout=30.0)
    conn.execute("PRAGMA journal_mode=WAL")
    token = get_anonymous_token()

    total_deals = import_weekly_bonus_deals(conn, token)
    
    # Re-compile Room Database Asset
    compile_gains_room_database(conn)
    conn.close()

if __name__ == "__main__":
    main()
