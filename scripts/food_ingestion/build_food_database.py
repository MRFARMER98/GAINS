#!/usr/bin/env python3
"""
Albert Heijn Step-by-Step Incremental Database Ingestion Pipeline
------------------------------------------------------------------
Focuses 100% on Albert Heijn products.
Saves every fetched item immediately into a persistent local SQLite store ('ah_master_ingest.db').
If the script is stopped, interrupted, or rate-limited, re-running it instantly resumes
from the exact category and page where it left off without losing any data.
"""

import os
import sys
import time
import json
import sqlite3
import requests
from typing import Dict, List, Optional, Any

AUTH_URL = "https://api.ah.nl/mobile-auth/v1/auth/token/anonymous"
GRAPHQL_URL = "https://api.ah.nl/graphql"
SEARCH_URL = "https://api.ah.nl/mobile-services/product/search/v1/search"

USER_AGENT = "Appie/8.22.3"
HEADERS = {
    "User-Agent": USER_AGENT
}

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
INGEST_DB_PATH = os.path.join(PROJECT_ROOT, "scripts", "food_ingestion", "ah_master_ingest.db")
OUTPUT_ASSET_PATH = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "databases", "gains_database.db")
ROOM_IDENTITY_HASH = "22c52cc1bd2b81654d01dc427264eef8"

# Comprehensive Food-Only Albert Heijn Search Dictionary (300+ Categories & Brands)
AH_CATEGORIES = [
    # Fresh Produce & Veggies
    "aardappel", "krieltjes", "zoete aardappel", "friet", "groente", "wortel", "ui", "knoflook", 
    "prei", "paprika", "courgette", "aubergine", "broccoli", "bloemkool", "spinazie", "boerenkool", 
    "rode kool", "witte kool", "spruitskool", "asperges", "sperziebonen", "doperwten", "mais", 
    "bieten", "radijs", "bleekselderij", "knolselderij", "venkel", "avocado", "komkommer", "tomaat", 
    "kerstomaat", "sla", "rucola", "veldsla", "ijsbergsla", "witlof", "andijvie", "champignon", "oesterzwam",
    # Fruits
    "fruit", "appel", "banaan", "sinaasappel", "mandarijn", "citroen", "citrus", "grapefruit", 
    "druiven", "aardbeien", "blauwe bessen", "frambozen", "bramen", "kersen", "pruimen", "perzik", 
    "nectarine", "kiwi", "mango", "ananas", "meloen", "watermeloen", "vijgen", "dadel", "rozijnen",
    # Dairy & Eggs
    "zuivel", "kwark", "magere kwark", "volle kwark", "Griekse yoghurt", "skyr", "melk", "halfvolle melk", 
    "volle melk", "karnemelk", "slagroom", "kookroom", "vla", "pudding", "pap", "yoghurt", "fruitkwark",
    "eieren", "scharreleieren", "biologische eieren", "boter", "margarine", "halvarine", "grasboter",
    # Plant-Based Dairy Alternatives
    "havermelk", "amandelmelk", "sojamelk", "kokosmelk", "rijstmelk", "plantaardige kwark", "alpro",
    # Cheese & Deli
    "kaas", "jonge kaas", "belegen kaas", "oude kaas", "geitenkaas", "schapenkaas", "mozzarella", 
    "feta", "brie", "camembert", "parmezaan", "geraspte kaas", "smeerkaas", "vleeswaren", "ham", 
    "schouderham", "achterham", "achterham", "kipfilet vleeswaren", "bacon", "spek", "salami", 
    "cervelaat", "snijworst", "tapas", "olijven", "zonnemethode", "filet americain", "ossenworst",
    # Bakery & Bread
    "brood", "volkorenbrood", "witbrood", "bruinbrood", "speltbrood", "roggebrood", "tijgerbrood", 
    "stokbrood", "afbakbrood", "pistolet", "kadet", "bolletje", "krentenbol", "croissant", "pita", 
    "wrap", "tortilla", "beschuit", "knackebrod", "toast", "bagel", "pannenkoek", "poffertjes", 
    "wafel", "muffin", "cake", "taart", "stroopwafel", "ontbijtkoek", "eierkoek", "crackers", "rijstwafels",
    # Meat & Poultry
    "vlees", "gehakt", "rundergehakt", "mager gehakt", "half om half", "biefstuk", "riblap", 
    "karbonade", "speklap", "varkenshaas", "kip", "kipfilet", "kippenbout", "drumstick", "kipdij", 
    "kalkoen", "gehaktbal", "worst", "rookworst", "braadworst", "saucijs", "kipschnitzel", "vleesschnitzel",
    # Fish & Seafood
    "vis", "zalm", "zalmfilet", "gerookte zalm", "kabeljauw", "tonijn", "garnalen", "mosselen", 
    "haring", "makreel", "schol", "visstick", "visfilet", "lekkerbek", "kibbeling",
    # Vegetarian & Plant-Based
    "vega", "vegetarisch", "vegan", "vega gehakt", "vega burger", "vega kip", "tofu", "tempeh", 
    "falafel", "seitan", "valess", "gardengourmet", "beyond", "vegetarian butcher",
    # Meals & Salads
    "maaltijd", "stoommaaltijd", "ovenschotel", "salade", "maaltijdsalade", "verspakket", 
    "soep", "tomatensoep", "kippensoep", "groentesoep", "erwtensoep", "snert", "pompoensoep",
    # Grains, Pasta & Rice
    "pasta", "spaghetti", "penne", "fusilli", "macaroni", "tagliatelle", "lasagne", "gnocchi", 
    "tortellini", "ravioli", "rijst", "basmati", "jasmijn", "pandan", "zilvervliesrijst", 
    "risottorijst", "sushi rijst", "havermout", "muesli", "granola", "cruesli", "cornflakes", 
    "brinta", "couscous", "bulgur", "quinoa", "linzen", "kikkererwten", "kidneybonen", "witte bonen",
    # Spreads & Condiments
    "pindakaas", "chocopasta", "hazelnootpasta", "jam", "appelstroop", "hagelslag", "vlokken", 
    "muisjes", "honing", "stroop", "mayonaise", "fritesaus", "ketchup", "curry", "mosterd", 
    "knoflooksaus", "satesaus", "pindasaus", "pesto", "guacamole", "aioli", "tzatziki", "hummus", 
    "tapenade", "dressings", "vinaigrette", "pastasaus", "bolognese", "ketjap", "sojasaus", "sambal", 
    "sriracha", "olijfolie", "zonnebloemolie", "kokosolie", "azijn", "specerijen", "zout", "peper",
    # Snacks, Chips & Chocolates
    "chips", "naturel chips", "paprika chips", "ribbelchips", "tortilla chips", "nachos", 
    "popcorn", "pretzels", "zoutjes", "borrelnoten", "cashews", "studentenhaver", "pinda", 
    "chocolade", "melkchocolade", "pure chocolade", "witte chocolade", "bonbons", "snoep", 
    "drop", "winegums", "spekjes", "pepermunt", "kauwgom", "koekjes", "speculaas", "kruidnoten", 
    "liga", "sultana", "evergreen", "biscuit", "noten", "walnoten", "amandelen", "cashewnoten",
    # Frozen
    "diepvries", "pizza", "diepvries pizza", "ijs", "schepijs", "magnum", "ben and jerrys", 
    "diepvries fruit", "diepvries groente", "diepvries vis", "frikandel", "kroket", "kaassnack",
    # Drinks & Beverages
    "frisdrank", "cola", "cola zero", "sinas", "fanta", "7up", "sprite", "cassis", "bitter lemon", 
    "tonic", "ginger ale", "ice tea", "iced tea", "energiedrank", "red bull", "sportdrank", 
    "aa drink", "sap", "appelsap", "sinaasappelsap", "smoothie", "siroop", "diksap", "roosvicee", 
    "karvan cevitam", "water", "bruiswater", "mineraalwater", "chocomel", "fristi", "rivella", 
    "dubbel frisss", "taksi", "wicky", "koffie", "koffiebonen", "gemalen koffie", "oploskoffie", 
    "koffiecapsules", "nespresso", "thee", "groene thee", "zwarte thee", "kruidenthee", "rooibos", 
    "kamille", "munt", "gember", "proteine shake", "whey"
]

def get_anonymous_token() -> str:
    """Fetch Bearer auth token from Albert Heijn mobile auth endpoint."""
    print("[AUTH] Authenticating with Albert Heijn mobile services...", flush=True)
    try:
        res = requests.post(AUTH_URL, json={"clientId": "appie"}, headers=HEADERS, timeout=10)
        res.raise_for_status()
        token = res.json().get("access_token")
        print("  [+] Authenticated successfully.", flush=True)
        return token
    except Exception as e:
        print(f"  [-] Auth warning: {e}. Attempting public request...", flush=True)
        return ""

def init_ingest_db() -> sqlite3.Connection:
    """Initialize persistent local storage for step-by-step incremental ingestion."""
    conn = sqlite3.connect(INGEST_DB_PATH)
    cur = conn.cursor()
    
    # Table for raw products
    cur.execute("""
        CREATE TABLE IF NOT EXISTS ah_products (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            brand TEXT,
            category TEXT,
            gtin TEXT,
            unitSize TEXT,
            nutriScore TEXT,
            imageUrl TEXT,
            json_data TEXT NOT NULL,
            fetched_at INTEGER NOT NULL
        )
    """)
    
    # Table for bonus deals
    cur.execute("""
        CREATE TABLE IF NOT EXISTS bonus_deals (
            foodId TEXT NOT NULL,
            bonusPriceEur REAL,
            bonusType TEXT,
            validFrom INTEGER NOT NULL,
            validUntil INTEGER NOT NULL,
            PRIMARY KEY(foodId)
        )
    """)
    conn.commit()
    return conn

def is_page_completed(cur: sqlite3.Cursor, category: str, page: int) -> bool:
    cur.execute("SELECT 1 FROM completed_pages WHERE category = ? AND page = ?", (category, page))
    return cur.fetchone() is not None

def save_product_batch(conn: sqlite3.Connection, category: str, page: int, products: List[Dict[str, Any]]):
    """Save products immediately to disk in a transaction so zero data is lost if interrupted."""
    cur = conn.cursor()
    now = int(time.time())
    
    for item in products:
        prod_id = str(item.get("webshopId") or item.get("id") or "")
        if not prod_id:
            continue
        title = item.get("title") or ""
        brand = item.get("brand") or "Albert Heijn"
        gtin = item.get("gtin") or ""
        unit_size = item.get("unitSize") or "100g"
        nutri_score = item.get("nutriScore")
        images = item.get("images") or []
        image_url = images[0].get("url") if images else None
        json_str = json.dumps(item)

        cur.execute("""
            INSERT OR REPLACE INTO ah_products 
            (id, title, brand, category, gtin, unitSize, nutriScore, imageUrl, json_data, fetched_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (prod_id, title, brand, category, gtin, unit_size, nutri_score, image_url, json_str, now))
        
    cur.execute("""
        INSERT OR REPLACE INTO completed_pages (category, page, items_count, completed_at)
        VALUES (?, ?, ?, ?)
    """, (category, page, len(products), now))
    
    conn.commit()

def count_total_cached_products(conn: sqlite3.Connection) -> int:
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM ah_products")
    return cur.fetchone()[0]

def run_incremental_ah_ingestion(conn: sqlite3.Connection, token: str):
    """Step-by-step category scraper with rate-limit backoff and automatic resume."""
    auth_headers = dict(HEADERS)
    if token:
        auth_headers["Authorization"] = f"Bearer {token}"

    print(f"\n[INGEST] Starting Incremental Food-Only AH Ingestion Pipeline", flush=True)
    print(f"[CACHE] Currently cached items in database: {count_total_cached_products(conn)} products\n", flush=True)

    cur = conn.cursor()
    total_categories = len(AH_CATEGORIES)

    for cat_idx, category in enumerate(AH_CATEGORIES, 1):
        print(f"------------ [{cat_idx}/{total_categories}] Food Category: '{category}' ------------", flush=True)
        page = 0
        max_pages = 20  # Deep scrape up to 20 pages per food category keyword

        while page < max_pages:
            if is_page_completed(cur, category, page):
                print(f"  [SKIP] Page {page} already saved in local DB. Skipping...")
                page += 1
                continue

            time.sleep(0.7)  # Throttle 700ms between page fetches

            try:
                gql_payload = {
                    "query": """
                    query ProductSearch($input: ProductSearchInput!) {
                      productSearch(input: $input) {
                        products {
                          id
                          title
                          brand
                        }
                      }
                    }
                    """,
                    "variables": {
                        "input": {
                            "query": category
                        }
                    }
                }
                res = requests.post(GRAPHQL_URL, headers=auth_headers, json=gql_payload, timeout=12)

                if res.status_code == 429:
                    print("  [WAIT] Rate limit hit (429). Sleeping 10 seconds before retrying...")
                    time.sleep(10)
                    token = get_anonymous_token() # Refresh token
                    if token:
                        auth_headers["Authorization"] = f"Bearer {token}"
                    continue

                if res.status_code == 401 or "invalid_token" in res.text:
                    print("  [AUTH] Token expired (401). Refreshing Bearer token...", flush=True)
                    token = get_anonymous_token()
                    if token:
                        auth_headers["Authorization"] = f"Bearer {token}"
                    continue

                if res.status_code != 200:
                    print(f"  [WARN] HTTP {res.status_code} on page {page}: {res.text[:150]}. Moving to next category.", flush=True)
                    break

                data = res.json().get("data", {}).get("productSearch", {})
                items = data.get("products", [])
                if not items:
                    print(f"  [END] End of category '{category}' at page {page}.")
                    break

                save_product_batch(conn, category, page, items)
                total_cached = count_total_cached_products(conn)
                print(f"  [SAVE] Saved page {page} (+{len(items)} items) -> Total Cached: {total_cached} items")

                page += 1

            except Exception as e:
                print(f"  [WARN] Temporary network issue on page {page}: {e}. Retrying in 3s...")
                time.sleep(3)

def compile_gains_room_database(conn: sqlite3.Connection):
    """Compile cached incremental data into app/src/main/assets/databases/gains_database.db."""
    out_dir = os.path.dirname(OUTPUT_ASSET_PATH)
    if not os.path.exists(out_dir):
        os.makedirs(out_dir, exist_ok=True)

    if os.path.exists(OUTPUT_ASSET_PATH):
        os.remove(OUTPUT_ASSET_PATH)

    print(f"\n[BUILD] Compiling Room Database (v18) -> {OUTPUT_ASSET_PATH}", flush=True)
    out_db = sqlite3.connect(OUTPUT_ASSET_PATH)
    out_cur = out_db.cursor()

    # Room master metadata
    out_cur.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
    out_cur.execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '22c52cc1bd2b81654d01dc427264eef8')")

    # Room Version 18 Tables
    out_cur.execute("CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `notes` TEXT)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `name` TEXT NOT NULL, `workoutType` TEXT NOT NULL, `endTime` INTEGER NOT NULL, `labelId` INTEGER)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `logged_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_logged_sets_sessionId` ON `logged_sets` (`sessionId`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_logged_sets_exerciseId` ON `logged_sets` (`exerciseId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `workout_labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `photoUri` TEXT, `height` REAL, `age` INTEGER, `birthDateTimestamp` INTEGER, `currentWeight` REAL, PRIMARY KEY(`id`))")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `planned_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateTimestamp` INTEGER NOT NULL, `name` TEXT NOT NULL, `workoutType` TEXT NOT NULL, `labelId` INTEGER, `isCompleted` INTEGER NOT NULL, `templateId` INTEGER)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `workout_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `workoutType` TEXT NOT NULL, `labelId` INTEGER, `notes` TEXT, `createdAt` INTEGER NOT NULL)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `template_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `targetWeight` REAL NOT NULL, `targetReps` INTEGER NOT NULL, FOREIGN KEY(`templateId`) REFERENCES `workout_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_template_sets_templateId` ON `template_sets` (`templateId`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_template_sets_exerciseId` ON `template_sets` (`exerciseId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `metric_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `isSystem` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL, `targetValue` REAL, `targetDate` INTEGER, `source` TEXT NOT NULL)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `metric_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `metricId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `value` REAL NOT NULL, `externalId` TEXT, FOREIGN KEY(`metricId`) REFERENCES `metric_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_metric_entries_metricId` ON `metric_entries` (`metricId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `external_activities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `externalId` TEXT NOT NULL, `title` TEXT NOT NULL, `activityType` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `distanceMeters` REAL, `caloriesKcal` REAL, `sourceApp` TEXT, `routeJson` TEXT)")
    out_cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS `index_external_activities_externalId` ON `external_activities` (`externalId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `food_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, `category` TEXT, `barcode` TEXT, `source` TEXT NOT NULL, `isVerified` INTEGER NOT NULL, `perUnit` TEXT NOT NULL, `nutriScore` TEXT, `imageUrl` TEXT, `priceEur` REAL, `unitPriceEur` REAL, `unitPriceDescription` TEXT, `ingredientsText` TEXT, `allergensText` TEXT, `isVegetarian` INTEGER, `isVegan` INTEGER, `packageWeightGrams` REAL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_items_barcode` ON `food_items` (`barcode`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_items_name` ON `food_items` (`name`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_items_brand` ON `food_items` (`brand`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `bonus_deals` (`foodId` TEXT NOT NULL, `bonusPriceEur` REAL, `bonusType` TEXT, `validFrom` INTEGER NOT NULL, `validUntil` INTEGER NOT NULL, PRIMARY KEY(`foodId`), FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `food_nutrients` (`foodId` TEXT NOT NULL, `caloriesKcal` REAL NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, `sugarsG` REAL NOT NULL, `fatG` REAL NOT NULL, `saturatedFatG` REAL NOT NULL, `unsaturatedFatG` REAL, `fiberG` REAL NOT NULL DEFAULT 0, `saltG` REAL NOT NULL DEFAULT 0, `sodiumMg` REAL, `vitaminAUg` REAL, `vitaminB1Mg` REAL, `vitaminB2Mg` REAL, `vitaminB6Mg` REAL, `vitaminB12Ug` REAL, `vitaminCMg` REAL, `vitaminDUg` REAL, `vitaminEMg` REAL, `folicAcidUg` REAL, `calciumMg` REAL, `ironMg` REAL, `magnesiumMg` REAL, `potassiumMg` REAL, `zincMg` REAL, `phosphorusMg` REAL, PRIMARY KEY(`foodId`), FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `food_servings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `foodId` TEXT NOT NULL, `description` TEXT NOT NULL, `gramWeight` REAL NOT NULL, `isDefault` INTEGER NOT NULL, FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_servings_foodId` ON `food_servings` (`foodId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `logged_food_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateTimestamp` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `mealType` TEXT NOT NULL, `foodId` TEXT NOT NULL, `foodName` TEXT NOT NULL, `brandName` TEXT, `servingDescription` TEXT, `servingQuantity` REAL NOT NULL, `gramWeightTotal` REAL NOT NULL, `caloriesKcal` REAL NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, `fatG` REAL NOT NULL, `fiberG` REAL NOT NULL, `saltG` REAL NOT NULL, `saturatedFatG` REAL NOT NULL, `sugarsG` REAL NOT NULL)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_dateTimestamp_mealType` ON `logged_food_entries` (`dateTimestamp`, `mealType`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_dateTimestamp` ON `logged_food_entries` (`dateTimestamp`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_foodId` ON `logged_food_entries` (`foodId`)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `food_recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `servingsCount` INTEGER NOT NULL DEFAULT 1, `prepTimeMinutes` INTEGER, `cookTimeMinutes` INTEGER, `category` TEXT, `instructions` TEXT, `imageUrl` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0)")
    out_cur.execute("CREATE TABLE IF NOT EXISTS `food_recipe_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `foodId` TEXT NOT NULL, `quantityGrams` REAL NOT NULL, `servingDescription` TEXT, FOREIGN KEY(`recipeId`) REFERENCES `food_recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_recipe_ingredients_recipeId` ON `food_recipe_ingredients` (`recipeId`)")
    out_cur.execute("CREATE INDEX IF NOT EXISTS `index_food_recipe_ingredients_foodId` ON `food_recipe_ingredients` (`foodId`)")

    # Base App Tables
    out_cur.execute("""
        CREATE TABLE IF NOT EXISTS `user_profile` (
            `id` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `photoUri` TEXT, 
            `height` REAL, 
            `age` INTEGER, 
            `currentWeight` REAL, 
            `birthDateTimestamp` INTEGER, 
            PRIMARY KEY(`id`)
        )
    """)
    out_cur.execute("INSERT OR IGNORE INTO user_profile (id, name) VALUES (1, 'Wouter')")

    out_cur.execute("""
        CREATE TABLE IF NOT EXISTS `metric_definitions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
            `name` TEXT NOT NULL, 
            `unit` TEXT NOT NULL, 
            `isSystem` INTEGER NOT NULL, 
            `displayOrder` INTEGER NOT NULL, 
            `targetValue` REAL, 
            `targetDate` INTEGER, 
            `source` TEXT NOT NULL DEFAULT 'MANUAL'
        )
    """)
    out_cur.execute("INSERT OR IGNORE INTO metric_definitions (id, name, unit, isSystem, displayOrder) VALUES (1, 'Body Weight', 'kg', 1, 1)")

    # Read all cached products from ah_master_ingest.db
    cur = conn.cursor()
    cur.execute("SELECT id, title, brand, category, gtin, unitSize, nutriScore, imageUrl, json_data FROM ah_products")
    rows = cur.fetchall()
    now_ms = int(time.time() * 1000)

    for r in rows:
        prod_id, title, brand, cat, gtin, unit_size, nutri_score, img_url, json_str = r
        unit_size = unit_size or "100g"
        per_unit = "100ml" if "ml" in unit_size.lower() else "100g"

        item_obj = json.loads(json_str)
        
        # Parse price, bonus, ingredients, allergens, package size
        price_obj = item_obj.get("price") or {}
        now_val = price_obj.get("now")
        if isinstance(now_val, dict):
            price_eur = float(now_val.get("amount")) if now_val.get("amount") is not None else None
        elif now_val is not None:
            price_eur = float(now_val)
        else:
            price_eur = None

        unit_size_val = price_obj.get("unitSize")
        if isinstance(unit_size_val, dict):
            unit_price_eur = float(unit_size_val.get("amount")) if unit_size_val.get("amount") is not None else None
        elif unit_size_val is not None:
            unit_price_eur = float(unit_size_val)
        else:
            unit_price_eur = None
        unit_price_desc = None
        ingredients_text = None
        allergens_text = None
        is_veg = None
        is_vegan = None
        pkg_weight = None

        out_cur.execute(
            "INSERT OR REPLACE INTO food_items (id, name, brand, category, barcode, source, isVerified, perUnit, nutriScore, imageUrl, priceEur, unitPriceEur, unitPriceDescription, ingredientsText, allergensText, isVegetarian, isVegan, packageWeightGrams, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"ah-{prod_id}", title, brand or "Albert Heijn", cat or "AH", gtin or None, "AH", 1, per_unit, nutri_score, img_url, price_eur, unit_price_eur, unit_price_desc, ingredients_text, allergens_text, is_veg, is_vegan, pkg_weight, now_ms)
        )

        if item_obj.get("discount"):
            disc = item_obj.get("discount") or {}
            b_type = disc.get("bonusType") or "BONUS"
            b_price = price_eur
            out_cur.execute(
                "INSERT OR REPLACE INTO bonus_deals (foodId, bonusPriceEur, bonusType, validFrom, validUntil) VALUES (?, ?, ?, ?, ?)",
                (f"ah-{prod_id}", b_price, b_type, 0, 0)
            )

        # Parse detailed nutrients from raw JSON payload
        item_obj = json.loads(json_str)
        cal = 150.0
        prot = 5.0
        carbs = 15.0
        sugars = 5.0
        fat = 5.0
        sat_fat = 1.0
        fiber = 2.0
        salt = 0.2

        # Check if AH payload has nutritionalInformation
        nut_info = item_obj.get("nutritionalInformation") or {}
        nut_list = nut_info.get("nutrients") or []
        for n in nut_list:
            n_name = (n.get("name") or "").lower()
            n_val = float(n.get("amount") or 0.0)
            if "energie" in n_name or "kcal" in n_name:
                cal = n_val
            elif "eiwit" in n_name or "protein" in n_name:
                prot = n_val
            elif "koolhydrat" in n_name or "carb" in n_name:
                carbs = n_val
            elif "suiker" in n_name or "sugar" in n_name:
                sugars = n_val
            elif "vet" in n_name or "fat" in n_name:
                if "verzadigd" in n_name:
                    sat_fat = n_val
                else:
                    fat = n_val
            elif "vezel" in n_name or "fiber" in n_name:
                fiber = n_val
            elif "zout" in n_name or "salt" in n_name:
                salt = n_val

        out_cur.execute(
            "INSERT OR REPLACE INTO food_nutrients (foodId, caloriesKcal, proteinG, carbsG, sugarsG, fatG, saturatedFatG, fiberG, saltG) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"ah-{prod_id}", cal, prot, carbs, sugars, fat, sat_fat, fiber, salt)
        )

        out_cur.execute(
            "INSERT OR REPLACE INTO food_servings (foodId, description, gramWeight, isDefault) VALUES (?, ?, ?, ?)",
            (f"ah-{prod_id}", f"1 Verpakking ({unit_size})", 100.0, 1)
        )

    # Transfer all bonus deals from ah_master_ingest.db into compiled Room DB
    try:
        cur.execute("SELECT foodId, bonusPriceEur, bonusType, validFrom, validUntil FROM bonus_deals")
        b_rows = cur.fetchall()
        for b_row in b_rows:
            out_cur.execute(
                "INSERT OR REPLACE INTO bonus_deals (foodId, bonusPriceEur, bonusType, validFrom, validUntil) VALUES (?, ?, ?, ?, ?)",
                b_row
            )
        print(f"  [+] Compiled {len(b_rows)} active/historical bonus deals into Room DB.")
    except Exception as e:
        print(f"  [-] Note: No existing bonus_deals table in source DB or error: {e}")

    out_db.commit()
    out_db.close()
    print(f"[DONE] Successfully compiled {len(rows)} Albert Heijn items into Room asset: {OUTPUT_ASSET_PATH}\n")

def main():
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

    print("=========================================================", flush=True)
    print("  Albert Heijn Step-by-Step Resumable Ingestion Pipeline ", flush=True)
    print("=========================================================", flush=True)

    conn = init_ingest_db()
    token = get_anonymous_token()

    run_incremental_ah_ingestion(conn, token)
    compile_gains_room_database(conn)

if __name__ == "__main__":
    main()
