#!/usr/bin/env python3
"""
Albert Heijn Allerhande Recipe Ingestion Pipeline for GAINS
-----------------------------------------------------------
Pulls hundreds of curated recipes with high-res food photography,
step-by-step cooking instructions, servings, prep times, and ingredients
from the official Albert Heijn GraphQL API.
Maps ingredients into the GAINS Room database (`gains_database.db`)
satisfying all SQLite foreign keys and indexes.
"""

import os
import re
import sys
import time
import json
import hashlib
import sqlite3
import argparse
from typing import Dict, List, Optional, Tuple, Any
import requests

AUTH_URL = "https://api.ah.nl/mobile-auth/v1/auth/token/anonymous"
GRAPHQL_URL = "https://api.ah.nl/graphql"
USER_AGENT = "Appie/8.22.3"

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROOM_DB_PATH = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "databases", "gains_database.db")
INGEST_DB_PATH = os.path.join(PROJECT_ROOT, "scripts", "food_ingestion", "ah_master_ingest.db")

# Standard weight conversions for Dutch recipe units
UNIT_CONVERSIONS = {
    'kg': 1000.0,
    'kilo': 1000.0,
    'g': 1.0,
    'gram': 1.0,
    'l': 1000.0,
    'liter': 1000.0,
    'dl': 100.0,
    'cl': 10.0,
    'ml': 1.0,
    'el': 15.0,  # eetlepel
    'eetlepel': 15.0,
    'eetlepels': 15.0,
    'tl': 5.0,   # theelepel
    'theelepel': 5.0,
    'theelepels': 5.0,
    'mespunt': 1.0,
    'snuf': 1.0,
    'snufje': 1.0,
    'teentje': 5.0,
    'teen': 5.0,
    'tenen': 5.0,
    'stronk': 150.0,
    'stronken': 150.0,
    'stuks': 100.0,
    'stuk': 100.0,
    'stengel': 50.0,
    'stengels': 50.0,
    'takje': 3.0,
    'takjes': 3.0,
    'blaadjes': 2.0,
    'plakjes': 25.0,
    'blik': 400.0,
    'blikje': 70.0,
    'zak': 400.0,
    'zakje': 30.0,
    'pot': 350.0,
    'potje': 100.0,
    'bakje': 250.0,
    'kuipje': 150.0,
    'fles': 500.0,
    'pak': 500.0,
}

# Generic nutrition estimates (per 100g) for common base ingredient categories
BASE_NUTRITION_ESTIMATES = {
    'groente': {'cal': 30.0, 'prot': 2.0, 'carbs': 5.0, 'fat': 0.5, 'fib': 2.5},
    'fruit': {'cal': 55.0, 'prot': 1.0, 'carbs': 13.0, 'fat': 0.2, 'fib': 2.0},
    'vlees': {'cal': 200.0, 'prot': 22.0, 'carbs': 0.0, 'fat': 12.0, 'fib': 0.0},
    'vis': {'cal': 140.0, 'prot': 20.0, 'carbs': 0.0, 'fat': 6.0, 'fib': 0.0},
    'zuivel': {'cal': 80.0, 'prot': 6.0, 'carbs': 5.0, 'fat': 4.0, 'fib': 0.0},
    'kaas': {'cal': 350.0, 'prot': 25.0, 'carbs': 1.0, 'fat': 28.0, 'fib': 0.0},
    'olie': {'cal': 820.0, 'prot': 0.0, 'carbs': 0.0, 'fat': 91.0, 'fib': 0.0},
    'kruiden': {'cal': 20.0, 'prot': 1.0, 'carbs': 2.0, 'fat': 0.2, 'fib': 1.0},
    'noten': {'cal': 600.0, 'prot': 18.0, 'carbs': 12.0, 'fat': 52.0, 'fib': 6.0},
    'pasta': {'cal': 350.0, 'prot': 12.0, 'carbs': 70.0, 'fat': 2.0, 'fib': 3.0},
    'rijst': {'cal': 350.0, 'prot': 7.0, 'carbs': 78.0, 'fat': 1.0, 'fib': 1.5},
    'default': {'cal': 100.0, 'prot': 4.0, 'carbs': 12.0, 'fat': 3.0, 'fib': 1.5}
}

STOP_WORDS = {
    'verse', 'vers', 'fijngehakt', 'fijngehakte', 'grofgehakt', 'grofgehakte',
    'gesneden', 'gaspeld', 'geraspt', 'geraspte', 'gemalen', 'gedroogd', 'gedroogde',
    'rijpe', 'rijp', 'middelgrote', 'grote', 'kleine', 'stukjes', 'plakjes',
    'biologisch', 'biologische', 'ongezouten', 'extra', 'vierge', 'traditionele',
    'kruimige', 'vastkokende', 'milde', 'scharrel', 'scharrelei', 'scharreleieren',
    'diepvries', 'in', 'op', 'van', 'met', 'zonder', 'voor'
}


def get_anonymous_token() -> str:
    res = requests.post(
        AUTH_URL,
        json={"clientId": "appie"},
        headers={"User-Agent": USER_AGENT},
        timeout=10
    )
    res.raise_for_status()
    token = res.json().get("access_token")
    if not token:
        raise ValueError("Failed to obtain anonymous AH token")
    return token


import html

FRACTIONS = {'½': 0.5, '¼': 0.25, '¾': 0.75, '⅓': 0.33, '⅔': 0.67, '⅛': 0.125}

def clean_quantity_str(text: str) -> str:
    if not text:
        return ""
    text = html.unescape(text)
    for frac_char, frac_val in FRACTIONS.items():
        text = re.sub(rf'(\d+)\s*{frac_char}', lambda m: str(float(m.group(1)) + frac_val), text)
        text = text.replace(frac_char, str(frac_val))
    return text

def parse_quantity_grams(text: str, raw_quantity: Any) -> float:
    """Estimates the gram weight from Dutch ingredient text and quantity."""
    if not text:
        return 100.0

    lower = clean_quantity_str(text).lower().strip()
    
    # Check for direct number + unit (e.g., "600 g", "1.5 kg", "200 ml", "2 el")
    match = re.search(r'([\d.]+)\s*([a-zA-Z]+)', lower)
    if match:
        num_str = match.group(1)
        unit_str = match.group(2).lower()
        try:
            num = float(num_str)
            if unit_str in UNIT_CONVERSIONS:
                return max(1.0, num * UNIT_CONVERSIONS[unit_str])
        except ValueError:
            pass

    # If raw quantity is provided as a number
    if raw_quantity:
        try:
            q_num = float(raw_quantity)
            for unit_key, factor in UNIT_CONVERSIONS.items():
                if f" {unit_key}" in lower or f"{unit_key} " in lower:
                    return max(1.0, q_num * factor)
            # Default piece count (e.g. 2 eggs or 1 apple)
            return max(1.0, q_num * 100.0)
        except (ValueError, TypeError):
            pass

    return 100.0


class AllerhandeImporter:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.conn = sqlite3.connect(db_path)
        self.cursor = self.conn.cursor()
        self.token = get_anonymous_token()
        self.headers = {
            "User-Agent": USER_AGENT,
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
        self._ensure_tables()
        self._load_existing_foods()

    def _ensure_tables(self):
        # Ensure food_recipes has all columns
        self.cursor.execute("""
            CREATE TABLE IF NOT EXISTS food_recipes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                servingsCount INTEGER NOT NULL DEFAULT 1,
                prepTimeMinutes INTEGER,
                cookTimeMinutes INTEGER,
                category TEXT,
                instructions TEXT,
                imageUrl TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        self.cursor.execute("""
            CREATE TABLE IF NOT EXISTS food_recipe_ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recipeId INTEGER NOT NULL,
                foodId TEXT NOT NULL,
                quantityGrams REAL NOT NULL,
                servingDescription TEXT,
                FOREIGN KEY(recipeId) REFERENCES food_recipes(id) ON DELETE CASCADE,
                FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE CASCADE
            )
        """)
        self.cursor.execute("CREATE INDEX IF NOT EXISTS index_food_recipe_ingredients_recipeId ON food_recipe_ingredients(recipeId)")
        self.cursor.execute("CREATE INDEX IF NOT EXISTS index_food_recipe_ingredients_foodId ON food_recipe_ingredients(foodId)")
        self.conn.commit()

    def _load_existing_foods(self):
        self.cursor.execute("SELECT id, LOWER(name) FROM food_items")
        self.existing_foods = self.cursor.fetchall()
        print(f"[INIT] Loaded {len(self.existing_foods)} existing food products for recipe ingredient matching.")

    def _find_best_match(self, singular_name: str) -> Optional[str]:
        if not singular_name:
            return None

        clean = singular_name.lower().strip()
        words = [w for w in re.findall(r'[a-zA-Z\u00C0-\u017F]+', clean) if w not in STOP_WORDS and len(w) > 2]
        
        # 1. Exact match
        for fid, fname in self.existing_foods:
            if fname == clean:
                return fid

        # 2. Strict startswith or containment with Albert Heijn brand preference
        if words:
            core_keyword = words[-1] # Usually the main noun in Dutch
            if len(core_keyword) > 3:
                candidates = []
                for fid, fname in self.existing_foods:
                    # e.g., 'AH Uien' contains 'ui' or 'aardappelen' contains 'aardappel'
                    if core_keyword in fname:
                        candidates.append((fid, fname))
                if candidates:
                    # Prefer AH brand if available
                    for cid, cname in candidates:
                        if cname.startswith("ah ") or "albert heijn" in cname:
                            return cid
                    return candidates[0][0]

        return None

    def _create_generic_food_item(self, ingredient_name: str) -> str:
        """Creates a clean generic food item to ensure FK validity and macro calculation."""
        slug = re.sub(r'[^a-zA-Z0-9]+', '-', ingredient_name.lower()).strip('-')
        food_id = f"ah-rec-{hashlib.md5(ingredient_name.lower().encode('utf-8')).hexdigest()[:10]}"

        # Check if already created
        self.cursor.execute("SELECT id FROM food_items WHERE id = ?", (food_id,))
        if self.cursor.fetchone():
            return food_id

        # Estimate nutrition category
        name_lower = ingredient_name.lower()
        cat_key = 'default'
        for key in BASE_NUTRITION_ESTIMATES.keys():
            if key in name_lower:
                cat_key = key
                break
        
        macros = BASE_NUTRITION_ESTIMATES[cat_key]
        now_ms = int(time.time() * 1000)

        # Insert food item
        self.cursor.execute("""
            INSERT OR IGNORE INTO food_items (
                id, name, brand, category, barcode, source, isVerified,
                perUnit, nutriScore, imageUrl, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            food_id,
            ingredient_name.capitalize(),
            "Allerhande",
            "Recept Ingrediënt",
            None,
            "AH",
            1,
            "100g",
            "A",
            None,
            now_ms
        ))

        # Insert nutrients
        self.cursor.execute("""
            INSERT OR IGNORE INTO food_nutrients (
                foodId, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            food_id,
            macros['cal'],
            macros['prot'],
            macros['carbs'],
            macros['carbs'] * 0.3,
            macros['fat'],
            macros['fat'] * 0.2,
            macros['fib'],
            0.1
        ))

        # Insert default serving
        self.cursor.execute("""
            INSERT OR IGNORE INTO food_servings (
                foodId, description, gramWeight, isDefault
            ) VALUES (?, ?, ?, ?)
        """, (
            food_id,
            "1 Portie (100g)",
            100.0,
            1
        ))

        # Update cache
        self.existing_foods.append((food_id, ingredient_name.lower()))
        return food_id

    def fetch_recipe_summaries(self, start: int, size: int = 50) -> List[Dict[str, Any]]:
        query = f"""
        query {{
            recipeSearch(query: {{ start: {start}, size: {size} }}) {{
                result {{
                    id
                    title
                    slug
                }}
            }}
        }}
        """
        resp = requests.post(GRAPHQL_URL, headers=self.headers, json={"query": query}, timeout=15)
        if resp.status_code == 401 or resp.status_code == 403:
            # Refresh token
            self.token = get_anonymous_token()
            self.headers["Authorization"] = f"Bearer {self.token}"
            resp = requests.post(GRAPHQL_URL, headers=self.headers, json={"query": query}, timeout=15)

        resp.raise_for_status()
        data = resp.json()
        return data.get("data", {}).get("recipeSearch", {}).get("result", [])

    def fetch_recipe_detail(self, recipe_id: int) -> Optional[Dict[str, Any]]:
        query = f"""
        query {{
            recipe(id: {recipe_id}) {{
                id
                title
                description
                cookTime
                waitTime
                courses
                cuisines
                images {{
                    url
                    width
                    height
                }}
                servings {{
                    number
                    type
                }}
                tags {{
                    key
                    value
                }}
                preparation {{
                    steps
                }}
                ingredients {{
                    text
                    quantity
                    name {{
                        singular
                        plural
                    }}
                }}
            }}
        }}
        """
        try:
            resp = requests.post(GRAPHQL_URL, headers=self.headers, json={"query": query}, timeout=15)
            if resp.status_code in (401, 403):
                self.token = get_anonymous_token()
                self.headers["Authorization"] = f"Bearer {self.token}"
                resp = requests.post(GRAPHQL_URL, headers=self.headers, json={"query": query}, timeout=15)

            data = resp.json()
            return data.get("data", {}).get("recipe")
        except Exception as e:
            print(f"  [!] Error fetching recipe {recipe_id}: {e}")
            return None

    def import_recipes(self, target_count: int = 300, batch_size: int = 50):
        print(f"\n[START] Beginning ingestion of {target_count} Allerhande recipes...")
        imported_count = 0
        current_start = 0

        while imported_count < target_count:
            summaries = self.fetch_recipe_summaries(start=current_start, size=batch_size)
            if not summaries:
                print("[INFO] No more recipes returned by API.")
                break

            for summary in summaries:
                if imported_count >= target_count:
                    break

                rid = summary.get("id")
                detail = self.fetch_recipe_detail(rid)
                if not detail:
                    continue

                self._save_recipe_to_db(detail)
                imported_count += 1
                if imported_count % 25 == 0 or imported_count == target_count:
                    self.conn.commit()
                    print(f"  [+] Ingested {imported_count}/{target_count} recipes... (Latest: '{detail.get('title')}')")

                # Gentle pacing
                time.sleep(0.08)

            current_start += len(summaries)

        self.conn.commit()
        print(f"[DONE] Successfully ingested {imported_count} recipes into {self.db_path}\n")

    def _save_recipe_to_db(self, r: Dict[str, Any]):
        title = html.unescape(r.get("title") or "Onbekend recept")
        servings = (r.get("servings") or {}).get("number") or 4
        cook_time = r.get("cookTime") or 25
        desc = html.unescape(r.get("description") or "")

        # Category logic: courses or tags
        courses = r.get("courses") or []
        tags = r.get("tags") or []
        category = "Hoofdgerecht"
        if courses:
            category = courses[0].capitalize()
        else:
            for t in tags:
                if t.get("key") == "soort-gerecht" or t.get("key") == "menugang":
                    category = t.get("value", "").capitalize()
                    break

        # Pick best resolution image URL (612x450 or 890x594 preferred for mobile cards)
        images = r.get("images") or []
        image_url = None
        for img in images:
            w = img.get("width") or 0
            if 500 <= w <= 900:
                image_url = img.get("url")
                break
        if not image_url and images:
            image_url = images[-1].get("url")

        # Instructions formatted as numbered steps
        prep = r.get("preparation") or {}
        raw_steps = prep.get("steps") or []
        formatted_instructions = ""
        if raw_steps:
            formatted_instructions = "\n\n".join(f"{i+1}. {html.unescape(step.strip())}" for i, step in enumerate(raw_steps))

        now_ms = int(time.time() * 1000)

        # Insert / Update Recipe
        # Use AH recipe ID as primary key or auto-increment
        self.cursor.execute("""
            INSERT OR REPLACE INTO food_recipes (
                id, name, servingsCount, prepTimeMinutes, cookTimeMinutes,
                category, instructions, imageUrl, notes, createdAt, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            r.get("id"),
            title,
            servings,
            None,
            cook_time,
            category,
            formatted_instructions,
            image_url,
            desc,
            now_ms,
            now_ms
        ))
        recipe_id = r.get("id")

        # Delete existing ingredients for this recipe (if re-ingesting)
        self.cursor.execute("DELETE FROM food_recipe_ingredients WHERE recipeId = ?", (recipe_id,))

        # Process Ingredients
        ingredients = r.get("ingredients") or []
        for ing in ingredients:
            raw_text = ing.get("text") or ""
            name_obj = ing.get("name") or {}
            singular_name = name_obj.get("singular") or name_obj.get("plural") or raw_text
            quantity = ing.get("quantity")

            if not singular_name.strip():
                continue

            # Skip water/kraanwater as an ingredient with food ID
            if singular_name.lower().strip() in ('water', 'kraanwater', 'koud water', 'warm water'):
                continue

            # Find or create food item
            food_id = self._find_best_match(singular_name)
            if not food_id:
                food_id = self._create_generic_food_item(singular_name)

            grams = parse_quantity_grams(raw_text, quantity)

            self.cursor.execute("""
                INSERT INTO food_recipe_ingredients (
                    recipeId, foodId, quantityGrams, servingDescription
                ) VALUES (?, ?, ?, ?)
            """, (
                recipe_id,
                food_id,
                grams,
                raw_text
            ))


def main():
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

    parser = argparse.ArgumentParser(description="Ingest Allerhande recipes into GAINS")
    parser.add_argument("--count", type=int, default=300, help="Number of recipes to ingest (default: 300)")
    parser.add_argument("--test", action="store_true", help="Run a quick 5-recipe test")
    args = parser.parse_args()

    count = 5 if args.test else args.count

    print("=============================================================", flush=True)
    print("   GAINS Food Database: Allerhande Recipe Ingestion Pipeline ", flush=True)
    print("=============================================================", flush=True)

    importer = AllerhandeImporter(ROOM_DB_PATH)
    importer.import_recipes(target_count=count)

    # Foreign key check
    importer.cursor.execute("PRAGMA foreign_key_check")
    fk_errors = importer.cursor.fetchall()
    if fk_errors:
        print(f"[WARNING] Foreign key check reported {len(fk_errors)} issues: {fk_errors[:5]}")
    else:
        print("[SUCCESS] SQLite foreign key integrity verified (0 errors).")

    importer.cursor.execute("SELECT COUNT(*) FROM food_recipes")
    total_recipes = importer.cursor.fetchone()[0]
    importer.cursor.execute("SELECT COUNT(*) FROM food_recipe_ingredients")
    total_ing = importer.cursor.fetchone()[0]
    importer.cursor.execute("SELECT COUNT(*) FROM food_recipes WHERE imageUrl IS NOT NULL")
    total_img = importer.cursor.fetchone()[0]

    print(f"\n[DATABASE SUMMARY]")
    print(f"  - Total Recipes: {total_recipes}")
    print(f"  - Recipes with Images: {total_img} ({total_img/max(1, total_recipes)*100:.1f}%)")
    print(f"  - Total Ingredients linked: {total_ing}")
    print("=============================================================\n")


if __name__ == "__main__":
    main()
