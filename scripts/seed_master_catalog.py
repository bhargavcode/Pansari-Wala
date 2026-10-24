#!/usr/bin/env python3
"""Seed the master catalog with real Indian products and a two-level category tree.

Sources (all India-tagged, open data):
  Open Food Facts      groceries, snacks, drinks         (ODbL / CC-BY-SA)
  Open Beauty Facts    personal care, beauty, baby care
  Open Products Facts  household, cleaning, pooja

Steps:
  fetch        Pull products into a local cache, keep only those with >= 3 good photos.
  report       Count cached products per category / sub-category.
  import       Create categories, upload photos (compressed locally) and upsert products.
               Idempotent: ids are `mp_off_<barcode>`; finished codes are tracked locally.
  reimport     Fresh catalog from the cache: India-made (890) only, pack sizes grouped as variants,
               photos on S3, prices from Open Prices / printed "Rs 20" / category estimate.
  recategorize Move remaining old-tree master products into the new tree.
  cleanup      Delete old categories that no longer have products or children.
  packify      Weight-in-name items ("Parle-G 100g", unit KG) -> unit PIECE + one 100 g PACK variant.
  group        Same brand + name, different pack sizes -> one product with a variant per size.
  enrich       Search Open Food Facts India for other pack sizes of each master product and add
               them as variants (barcode, image, price from Open Prices; else estimated from a
               priced sibling; unpriced ones are added switched off).
               packify / group / enrich are dry runs unless --apply is passed.
               Try without login: enrich --sample "Parle:Parle-G Biscuits"

Usage (from repo root):
  python3 scripts/seed_master_catalog.py fetch
  python3 scripts/seed_master_catalog.py import --workers 6
  python3 scripts/seed_master_catalog.py recategorize && python3 scripts/seed_master_catalog.py cleanup
Admin password: ADMIN_PASSWORD env var or scripts/.catalog-cache/admin_password.
"""
import argparse
import datetime
import io
import json
import os
import re
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

from PIL import Image

USER_AGENT = "PansariWala-catalog-seed/2.0 (master catalog import; admin@pansariwala.shop)"
OFF_SEARCH_URL = "https://search.openfoodfacts.org/search"
CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".catalog-cache")
CACHE_FILE = os.path.join(CACHE_DIR, "catalog_v2.json")
RAW_FILE = os.path.join(CACHE_DIR, "raw_v2.json")
DONE_FILE = os.path.join(CACHE_DIR, "imported_v2.txt")
PASSWORD_FILE = os.path.join(CACHE_DIR, "admin_password")
UPLOAD_PREFIX = "master/product-images"
PAGE_SIZE = 100
IMAGES_PER_PRODUCT = 3
MIN_IMAGES = 1
MIN_FRONT_EDGE = 300
MIN_EXTRA_EDGE = 400
MAX_EDGE = 1200
THUMB_EDGE = 320
UPLOAD_MAX_BYTES = 1_450_000
THUMB_MAX_BYTES = 140_000
OFF_FIELDS = ("code,product_name,product_name_en,product_name_hi,generic_name,brands,quantity,"
              "categories_tags,images,unique_scans_n")

SOURCES = {
    "off": ("food", "images.openfoodfacts.org"),
    "obf": ("nonfood", "images.openbeautyfacts.org"),
    "opf": ("nonfood", "images.openproductsfacts.org"),
}
CLASSIC_SEARCH = {
    "obf": "https://world.openbeautyfacts.org/api/v2/search",
    "opf": "https://world.openproductsfacts.org/api/v2/search",
}

PARENTS = [
    ("mc_atta_rice_dal", "Atta, Rice & Dal"),
    ("mc_oil_ghee_masala", "Oil, Ghee & Masala"),
    ("mc_dairy_bread_eggs", "Dairy, Bread & Eggs"),
    ("mc_breakfast", "Breakfast & Spreads"),
    ("mc_snacks", "Snacks & Namkeen"),
    ("mc_biscuits", "Biscuits & Cookies"),
    ("mc_sweets", "Chocolates, Sweets & Desserts"),
    ("mc_instant", "Noodles, Pasta & Instant Food"),
    ("mc_sauces", "Sauces, Pickles & Chutney"),
    ("mc_dry_fruits", "Dry Fruits, Nuts & Seeds"),
    ("mc_tea_coffee", "Tea, Coffee & Health Drinks"),
    ("mc_cold_drinks", "Cold Drinks & Juices"),
    ("mc_frozen_meat", "Frozen, Meat & Fish"),
    ("mc_baby", "Baby Care"),
    ("mc_personal_care", "Personal Care & Beauty"),
    ("mc_home", "Home, Cleaning & Pooja"),
]

# (sub id, parent id, name, scope, name keywords, OFF/OBF tags). Listed in match priority:
# keyword pass walks this list top-down, then the tag pass does the same.
SUBS = [
    # --- non-food (Open Beauty / Open Products Facts) ---
    ("baby_care", "mc_baby", "Baby Bath & Skin Care", "nonfood",
     ["baby", "diaper", "diapers", "nappy", "wipes"], ["en:baby-care"]),
    ("oral_care", "mc_personal_care", "Oral Care", "nonfood",
     ["toothpaste", "toothbrush", "tooth", "mouthwash", "dental", "colgate", "pepsodent", "sensodyne",
      "closeup", "close up", "dant"], ["en:toothpastes", "en:oral-care", "en:mouthwashes"]),
    ("shaving", "mc_personal_care", "Shaving & Men's Grooming", "nonfood",
     ["shaving", "shave", "razor", "beard", "after shave", "aftershave"], ["en:shaving"]),
    ("feminine", "mc_personal_care", "Feminine Hygiene", "nonfood",
     ["sanitary", "pads", "tampon", "panty liner", "intimate", "menstrual"], ["en:feminine-hygiene"]),
    ("fragrance", "mc_personal_care", "Deos & Fragrances", "nonfood",
     ["deo", "deodorant", "perfume", "body spray", "attar", "roll on", "roll-on", "eau de", "fragrance"],
     ["en:deodorants", "en:perfumes"]),
    ("makeup", "mc_personal_care", "Makeup & Cosmetics", "nonfood",
     ["lipstick", "lip color", "kajal", "kohl", "eyeliner", "mascara", "foundation", "compact", "nail",
      "primer", "concealer", "blush", "eye shadow", "eyeshadow", "sindoor", "bindi", "makeup", "lip gloss"],
     ["en:make-up", "en:makeup", "en:nail-polishes", "en:lipsticks"]),
    ("hair_care", "mc_personal_care", "Hair Care", "nonfood",
     ["shampoo", "conditioner", "hair", "henna", "mehndi", "dandruff"],
     ["en:shampoos", "en:hair-care", "en:conditioners", "en:hair-oils"]),
    ("bath_body", "mc_personal_care", "Bath & Body", "nonfood",
     ["soap", "body wash", "shower", "handwash", "hand wash", "sanitizer", "sanitiser", "bathing bar",
      "talc", "talcum", "body lotion", "prickly heat"],
     ["en:soaps", "en:shower-gels", "en:hand-washes", "en:body-care"]),
    ("skin_care", "mc_personal_care", "Skin Care", "nonfood",
     ["cream", "lotion", "face wash", "facewash", "moisturiser", "moisturizer", "sunscreen", "spf",
      "face pack", "serum", "scrub", "aloe vera gel", "petroleum jelly", "lip balm", "face", "skin",
      "cleanser", "toner", "gel"],
     ["en:skin-care", "en:face-care", "en:creams", "en:moisturizers"]),
    ("detergents", "mc_home", "Detergents & Laundry", "nonfood",
     ["detergent", "washing powder", "laundry", "fabric", "surf excel", "ariel", "tide", "rin", "wheel",
      "ghadi", "comfort", "ujala", "stain"], ["en:laundry-detergents", "en:detergents"]),
    ("dishwash", "mc_home", "Dishwash", "nonfood",
     ["dishwash", "dish wash", "vim", "pril", "utensil", "scrub pad", "scrubber"], ["en:dishwashing"]),
    ("cleaners", "mc_home", "Floor & Toilet Cleaners", "nonfood",
     ["cleaner", "toilet", "harpic", "lizol", "colin", "floor", "phenyl", "bleach", "domex",
      "disinfectant", "glass", "surface"], ["en:cleaning-products", "en:household-cleaners"]),
    ("repellents", "mc_home", "Fresheners & Repellents", "nonfood",
     ["freshener", "odonil", "mosquito", "good knight", "goodknight", "all out", "repellent", "insect",
      "cockroach", "naphthalene", "hit"], ["en:air-fresheners", "en:insecticides"]),
    ("pooja", "mc_home", "Pooja Needs", "nonfood",
     ["agarbatti", "incense", "dhoop", "camphor", "kapur", "kapoor", "diya", "pooja", "puja", "wicks",
      "havan", "kumkum", "roli"], ["en:incense"]),
    ("wellness", "mc_personal_care", "Health & Wellness", "nonfood",
     ["chyawanprash", "isabgol", "hajmola", "churan", "immunity", "capsule", "capsules", "tablet", "tablets",
      "vitamin", "multivitamin", "supplement", "creatine", "ashwagandha", "giloy", "volini", "moov", "vicks",
      "iodex", "pain relief", "balm", "eno", "antacid", "odomos"],
     ["en:dietary-supplements", "en:medicine", "en:health-care", "en:bodybuilding-supplements"]),
    ("home_essentials", "mc_home", "Home Essentials", "nonfood",
     ["tissue", "napkin", "foil", "garbage", "cling", "battery", "batteries", "bulb", "candle",
      "matchbox", "paper", "towel"], ["en:household"]),
    # --- food (Open Food Facts) ---
    ("baby_food", "mc_baby", "Baby Food", "food",
     ["cerelac", "baby food", "infant", "nestum", "lactogen", "nan pro", "farex", "baby"],
     ["en:baby-foods", "en:baby-milks", "en:infant-formulas"]),
    ("milk_drinks", "mc_cold_drinks", "Milkshakes, Lassi & Buttermilk", "food",
     ["milkshake", "milk shake", "shake", "flavoured milk", "flavored milk", "lassi", "buttermilk",
      "chaas", "chhach", "cold coffee", "smoothie", "badam milk", "kesar milk"],
     ["en:flavoured-milks", "en:milkshakes", "en:dairy-drinks", "en:buttermilks"]),
    ("ice_cream", "mc_sweets", "Ice Creams & Frozen Desserts", "food",
     ["ice cream", "icecream", "kulfi", "frozen dessert", "cornetto", "sundae", "cone"],
     ["en:ice-creams", "en:frozen-desserts", "en:ice-creams-and-sorbets"]),
    ("peanut_butter", "mc_breakfast", "Peanut Butter", "food",
     ["peanut butter", "almond butter"], ["en:peanut-butters", "en:nut-butters"]),
    ("spreads", "mc_breakfast", "Jams & Spreads", "food",
     ["chocolate spread", "nutella", "jam", "marmalade", "spread", "fruit spread"],
     ["en:jams", "en:spreads", "en:sweet-spreads", "en:marmalades"]),
    ("cream_biscuits", "mc_biscuits", "Cream Biscuits", "food",
     ["cream biscuit", "cream biscuits", "bourbon", "oreo", "jim jam", "dark fantasy", "hide & seek",
      "sandwich biscuit", "creme"], ["en:filled-biscuits", "en:sandwich-biscuits"]),
    ("glucose_marie", "mc_biscuits", "Glucose, Marie & Digestive", "food",
     ["marie", "glucose biscuit", "parle-g", "parle g", "digestive", "tiger", "milk bikis", "nutrichoice"],
     ["en:dry-biscuits", "en:digestive-biscuits"]),
    ("rusk", "mc_biscuits", "Rusk & Khari", "food",
     ["rusk", "khari", "toast", "cake rusk"], ["en:rusks"]),
    ("crackers", "mc_biscuits", "Crackers & Salted Biscuits", "food",
     ["cracker", "crackers", "krackjack", "monaco", "20-20", "salted biscuit", "nimkin", "cream cracker"],
     ["en:crackers", "en:salted-biscuits"]),
    ("cookies", "mc_biscuits", "Cookies & Biscuits", "food",
     ["cookie", "cookies", "biscuit", "biscuits", "biscotti", "nankhatai", "wafer", "wafers"],
     ["en:biscuits", "en:cookies", "en:biscuits-and-cakes", "en:wafers"]),
    ("health_drinks", "mc_tea_coffee", "Health Drinks", "food",
     ["horlicks", "bournvita", "boost", "complan", "protinex", "pediasure", "health drink", "malt",
      "ensure", "protein powder", "whey"], ["en:chocolate-powders", "en:malted-drinks", "en:protein-powders"]),
    ("tea", "mc_tea_coffee", "Tea", "food",
     ["tea", "chai", "green tea", "chaipatti", "tea bags", "masala chai", "kahwa"],
     ["en:teas", "en:tea-bags", "en:green-teas", "en:black-teas"]),
    ("coffee", "mc_tea_coffee", "Coffee", "food",
     ["coffee", "nescafe", "bru", "espresso", "cappuccino", "filter coffee", "chicory"],
     ["en:coffees", "en:instant-coffees"]),
    ("instant_mixes", "mc_instant", "Instant Mixes", "food",
     ["mix", "custard", "jelly crystals", "idli", "dosa", "dhokla", "upma", "khaman", "pancake",
      "baking powder", "cornflour", "corn flour", "cocoa powder", "yeast"],
     ["en:dessert-mixes", "en:baking-aids"]),
    ("cakes", "mc_dairy_bread_eggs", "Cakes & Muffins", "food",
     ["cake", "muffin", "brownie", "swiss roll", "cupcake", "croissant", "doughnut", "donut"],
     ["en:cakes", "en:muffins", "en:pastries"]),
    ("chocolates", "mc_sweets", "Chocolates", "food",
     ["chocolate", "choco", "dairy milk", "kitkat", "kit kat", "5 star", "munch", "perk", "silk", "ferrero",
      "snickers", "bournville", "galaxy", "amul dark", "truffle", "bonbon"],
     ["en:chocolates", "en:chocolate-candies", "en:dark-chocolates", "en:milk-chocolates"]),
    ("candies", "mc_sweets", "Candies, Toffees & Gum", "food",
     ["candy", "toffee", "lollipop", "gum", "mint", "mints", "eclairs", "jelly", "gummy", "gummies",
      "pulse", "lozenge", "chewing", "marshmallow", "fruit drops"],
     ["en:candies", "en:confectioneries", "en:chewing-gum", "en:lollipops", "en:toffees"]),
    ("mithai", "mc_sweets", "Indian Sweets (Mithai)", "food",
     ["laddu", "ladoo", "laddoo", "barfi", "burfi", "halwa", "soan papdi", "son papdi", "rasgulla",
      "gulab jamun", "peda", "kaju katli", "rasmalai", "mysore pak", "chikki", "gajak", "petha", "mithai",
      "sweets", "kheer", "jalebi", "rewari", "patisa"],
     ["en:indian-sweets", "en:sweets"]),
    ("desserts", "mc_sweets", "Puddings & Desserts", "food",
     ["pudding", "dessert", "mousse", "falooda"], ["en:desserts", "en:puddings"]),
    ("noodles", "mc_instant", "Noodles", "food",
     ["noodles", "noodle", "maggi", "ramen", "hakka", "chowmein", "chow mein", "yippee", "top ramen"],
     ["en:instant-noodles", "en:noodles"]),
    ("pasta", "mc_instant", "Pasta & Vermicelli", "food",
     ["pasta", "macaroni", "vermicelli", "seviyan", "sevai", "spaghetti", "penne", "fusilli"],
     ["en:pastas", "en:vermicelli"]),
    ("soups", "mc_instant", "Soups", "food", ["soup"], ["en:soups", "en:instant-soups"]),
    ("ready_meals", "mc_instant", "Ready to Eat & Cook", "food",
     ["ready to eat", "ready-to-eat", "ready to cook", "heat & eat", "heat and eat", "instant meal",
      "khichdi", "biryani", "pulao", "dal makhani", "paneer butter masala", "rajma chawal", "meal"],
     ["en:meals", "en:ready-meals", "en:prepared-meals"]),
    ("nachos", "mc_snacks", "Nachos & Tortilla Chips", "food",
     ["nachos", "nacho", "tortilla", "doritos"], ["en:tortilla-chips"]),
    ("chips", "mc_snacks", "Chips & Crisps", "food",
     ["chips", "crisps", "lays", "lay's", "pringles", "uncle chipps", "bingo", "potato wafer",
      "banana chips"], ["en:chips-and-fries", "en:crisps", "en:potato-crisps"]),
    ("popcorn_puffs", "mc_snacks", "Popcorn & Puffs", "food",
     ["popcorn", "puffs", "puff", "cheese balls", "rings", "corn puffs", "kurkure"], ["en:popcorn"]),
    ("healthy_snacks", "mc_snacks", "Healthy Snacks & Bars", "food",
     ["makhana", "fox nut", "trail mix", "protein bar", "granola bar", "energy bar", "bar", "roasted",
      "baked", "millet", "quinoa puffs"], ["en:cereal-bars", "en:energy-bars", "en:protein-bars"]),
    ("namkeen", "mc_snacks", "Namkeen & Bhujia", "food",
     ["namkeen", "bhujia", "mixture", "sev", "chivda", "chiwda", "chakli", "mathri", "khakhra", "gathiya",
      "chana jor", "navratan", "murukku", "moong dal", "masala peanut", "peanuts masala", "chana dal",
      "matar", "snack", "snacks", "farsan", "papdi", "bhel", "khatta meetha", "kachori", "samosa"],
     ["en:salty-snacks", "en:appetizers", "en:snacks"]),
    ("cereals", "mc_breakfast", "Cereals & Muesli", "food",
     ["cornflakes", "corn flakes", "muesli", "granola", "chocos", "flakes", "cereal", "wheat flakes"],
     ["en:breakfast-cereals", "en:mueslis", "en:cornflakes"]),
    ("oats", "mc_breakfast", "Oats", "food", ["oats", "oat"], ["en:oat-flakes", "en:rolled-oats"]),
    ("honey", "mc_breakfast", "Honey", "food", ["honey", "shahad"], ["en:honeys"]),
    ("curd", "mc_dairy_bread_eggs", "Curd & Yogurt", "food",
     ["curd", "dahi", "yogurt", "yoghurt", "raita", "greek", "shrikhand", "mishti doi"],
     ["en:yogurts", "en:fermented-milk-products"]),
    ("paneer_cheese", "mc_dairy_bread_eggs", "Paneer, Cheese & Tofu", "food",
     ["paneer", "cheese", "tofu", "mozzarella", "cheddar"], ["en:cheeses", "en:paneer", "en:tofu"]),
    ("ghee", "mc_oil_ghee_masala", "Ghee & Vanaspati", "food",
     ["ghee", "vanaspati", "dalda"], ["en:ghee", "en:clarified-butters"]),
    ("butter_cream", "mc_dairy_bread_eggs", "Butter & Cream", "food",
     ["butter", "fresh cream", "malai", "cream", "margarine"], ["en:butters", "en:creams", "en:margarines"]),
    ("milk", "mc_dairy_bread_eggs", "Milk", "food",
     ["milk", "toned", "doodh", "milk powder", "dairy whitener", "condensed"],
     ["en:milks", "en:milk-powders", "en:condensed-milks", "en:dairy-whiteners"]),
    ("bread", "mc_dairy_bread_eggs", "Bread & Buns", "food",
     ["bread", "bun", "buns", "pav", "kulcha", "pizza base", "naan", "paratha", "roti", "tortilla wrap",
      "wrap"], ["en:breads", "en:flatbreads"]),
    ("eggs", "mc_dairy_bread_eggs", "Eggs", "food", ["egg", "eggs"], ["en:eggs", "en:chicken-eggs"]),
    ("energy_drinks", "mc_cold_drinks", "Energy & Sports Drinks", "food",
     ["energy", "red bull", "sting", "glucon-d", "glucon d", "electral", "sports drink", "ors",
      "electrolyte", "gatorade", "monster"], ["en:energy-drinks", "en:sports-drinks"]),
    ("syrups", "mc_cold_drinks", "Syrups & Sharbat", "food",
     ["sharbat", "sherbet", "syrup", "squash", "rooh afza", "concentrate", "rasna", "tang", "jaljeera",
      "aam panna", "kokum", "crush"], ["en:syrups", "en:squashes", "en:fruit-syrups"]),
    ("soft_drinks", "mc_cold_drinks", "Soft Drinks", "food",
     ["cola", "soft drink", "pepsi", "coca", "coke", "sprite", "fanta", "thums up", "limca",
      "mountain dew", "7up", "mirinda", "carbonated", "tonic", "ginger ale", "jeera soda", "campa"],
     ["en:sodas", "en:carbonated-drinks", "en:colas"]),
    ("water", "mc_cold_drinks", "Water & Soda", "food",
     ["water", "soda", "club soda", "mineral water"], ["en:waters", "en:mineral-waters", "en:spring-waters"]),
    ("juices", "mc_cold_drinks", "Juices & Fruit Drinks", "food",
     ["juice", "nectar", "frooti", "maaza", "slice", "tropicana", "paper boat", "fruit drink", "real",
      "coconut water", "drink", "beverage"],
     ["en:fruit-juices", "en:juices-and-nectars", "en:fruit-based-beverages", "en:fruit-nectars",
      "en:beverages", "en:plant-based-beverages"]),
    ("frozen", "mc_frozen_meat", "Frozen Veg & Snacks", "food",
     ["frozen", "french fries", "nuggets", "aloo tikki", "smiles", "momos", "frozen peas"],
     ["en:frozen-foods", "en:frozen-vegetables"]),
    ("meat_fish", "mc_frozen_meat", "Chicken, Meat & Fish", "food",
     ["chicken", "mutton", "fish", "prawn", "prawns", "tuna", "sardine", "sausage", "salami", "ham",
      "keema", "seekh"], ["en:meats", "en:fishes", "en:seafood", "en:poultries"]),
    ("oils", "mc_oil_ghee_masala", "Cooking Oils", "food",
     ["oil", "tel", "sunflower oil", "mustard oil", "groundnut oil", "olive oil", "rice bran"],
     ["en:vegetable-oils", "en:olive-oils", "en:sunflower-oils", "en:mustard-oils", "en:fats"]),
    ("atta_flours", "mc_atta_rice_dal", "Atta & Flours", "food",
     ["atta", "flour", "maida", "besan", "ragi", "bajra", "jowar", "makki", "multigrain"],
     ["en:flours", "en:wheat-flours"]),
    ("rice", "mc_atta_rice_dal", "Rice", "food",
     ["rice", "basmati", "sona masoori", "kolam", "chawal", "brown rice", "idli rice"],
     ["en:rices", "en:basmati-rices"]),
    ("dals", "mc_atta_rice_dal", "Dals & Pulses", "food",
     ["dal", "daal", "dhal", "toor", "tur", "arhar", "moong", "urad", "masoor", "chana", "rajma", "chole",
      "lobia", "kabuli", "chickpea", "lentil", "pulses", "matki"],
     ["en:legumes", "en:pulses", "en:lentils", "en:chickpeas"]),
    ("poha_suji", "mc_atta_rice_dal", "Poha, Suji & Daliya", "food",
     ["poha", "suji", "sooji", "rava", "semolina", "daliya", "dalia", "sabudana", "murmura"],
     ["en:semolinas", "en:flattened-rice"]),
    ("salt_sugar", "mc_oil_ghee_masala", "Salt, Sugar & Jaggery", "food",
     ["salt", "namak", "sugar", "jaggery", "gur", "sendha", "shakkar", "stevia", "sweetener", "mishri"],
     ["en:salts", "en:sugars", "en:jaggery", "en:sweeteners"]),
    ("whole_spices", "mc_oil_ghee_masala", "Whole Spices", "food",
     ["jeera", "cumin", "black pepper", "elaichi", "cardamom", "clove", "laung", "cinnamon", "dalchini",
      "bay leaf", "tej patta", "ajwain", "methi seeds", "saunf", "fennel", "mustard seeds", "rai", "hing",
      "asafoetida", "kalonji", "whole"], ["en:whole-spices", "en:seeds-spices"]),
    ("masala", "mc_oil_ghee_masala", "Masala & Powders", "food",
     ["masala", "powder", "turmeric", "haldi", "chilli", "chili", "mirch", "coriander", "dhania",
      "seasoning", "spice", "spices", "kasuri methi", "garam", "chaat", "sambar", "rasam", "pav bhaji"],
     ["en:spices", "en:spice-mixes", "en:curry-powders"]),
    ("pickles", "mc_sauces", "Pickles (Achar)", "food",
     ["pickle", "achar", "achaar", "aachar"], ["en:pickles"]),
    ("chutney_dips", "mc_sauces", "Chutney & Dips", "food",
     ["chutney", "dip", "salsa", "hummus", "dressing"], ["en:dips", "en:salad-dressings"]),
    ("pastes", "mc_sauces", "Cooking Pastes & Puree", "food",
     ["paste", "tamarind", "imli", "puree", "purée", "vinegar"], ["en:vinegars", "en:tomato-purees"]),
    ("ketchup_sauces", "mc_sauces", "Ketchup & Sauces", "food",
     ["ketchup", "sauce", "mayonnaise", "mayo", "schezwan", "sriracha"],
     ["en:sauces", "en:ketchup", "en:mayonnaises", "en:condiments"]),
    ("nuts", "mc_dry_fruits", "Almonds, Cashews & Nuts", "food",
     ["almond", "almonds", "badam", "cashew", "cashews", "kaju", "walnut", "walnuts", "akhrot", "pista",
      "pistachio", "pistachios", "peanut", "peanuts", "hazelnut", "nuts", "mixed nuts"], ["en:nuts"]),
    ("dried_fruits", "mc_dry_fruits", "Dried Fruits", "food",
     ["raisin", "raisins", "kishmish", "dates", "khajur", "fig", "anjeer", "apricot", "dry fruit",
      "dry fruits", "prune", "cranberry", "cranberries", "blueberries"], ["en:dried-fruits"]),
    ("seeds", "mc_dry_fruits", "Seeds", "food",
     ["chia", "flax", "flaxseed", "pumpkin seed", "sunflower seed", "melon seed", "seeds", "alsi",
      "sesame", "til"], ["en:seeds"]),
]

EXTRA_KEYWORDS = {
    "crackers": ["krack jack", "krackjack"],
    "dals": ["soya chunks", "soya chunk", "soya badi", "nutrela"],
    "poha_suji": ["rawa"],
    "whole_spices": ["ilaichi", "ilachi", "anardana", "anar dana"],
    "atta_flours": ["puttupodi", "puttu"],
    "masala": ["curry"],
    "soups": ["manchow"],
    "chips": ["wefer"],
    "namkeen": ["sticks", "stick", "bhakarwadi", "aloo bhujia", "papad", "papadum", "fryums", "boondi", "aloo",
                "chana", "khakra"],
    "tea": ["chakra gold", "tata agni", "brooke bond"],
    "wellness": ["condom", "condoms", "protein", "casein", "omega", "probiotic", "ors", "churna", "churan",
                 "tulsi", "amla", "giloy", "kadha", "triphala"],
    "home_essentials": ["shoe polish", "dark tan", "cherry blossom", "agarbati"],
    "ketchup_sauces": ["katchup", "catchup", "tomato"],
    "juices": ["guava", "litchi", "pulp", "pulpy", "frut", "nimbu pani"],
    "repellents": ["freshner"],
    "skin_care": ["mask", "aloe"],
    "bath_body": ["wash"],
    "dals": ["soya"],
    "whole_spices": ["methi", "chillies", "peppercorn"],
    "bread": ["parotta", "porotta", "parota"],
    "candies": ["tic tac", "tictac"],
    "chocolates": ["bounty", "twix", "mars"],
    "energy_drinks": ["glucose d", "glucon"],
    "masala": ["ajinomoto", "msg"],
    "frozen": ["patty", "patties"],
    "chips": ["jackfruit chip", "pakkavada", "thatta", "pani puri"],
    "skin_care": ["mask", "aloe", "facial", "safe sun"],
}
# Brand -> sub, used when neither name keywords nor tags decide.
BRAND_FALLBACK = {
    "cadbury": "chocolates", "lindt": "chocolates", "amul dark": "chocolates", "hershey": "chocolates",
    "britannia": "cookies", "parle": "cookies", "sunfeast": "cookies", "unibic": "cookies",
    "mcvities": "cookies", "mc vities": "cookies", "priyagold": "cookies", "anmol": "cookies",
    "haldiram": "namkeen", "bikaji": "namkeen", "bikano": "namkeen", "balaji": "chips",
    "brijwasi": "namkeen", "bhikharam": "namkeen", "prataap": "namkeen",
    "alpenliebe": "candies", "center fruit": "candies", "mentos": "candies", "chupa chups": "candies",
    "knorr": "soups", "ching's": "noodles", "chings": "noodles", "maggi": "noodles", "yippee": "noodles",
    "kissan": "ketchup_sauces", "veeba": "ketchup_sauces", "funfoods": "ketchup_sauces",
    "mdh": "masala", "everest": "masala", "catch": "masala", "badshah": "masala", "eastern": "masala",
    "aashirvaad": "atta_flours", "pillsbury": "atta_flours", "fortune": "oils", "saffola": "oils",
    "amul": "milk", "mother dairy": "milk", "nandini": "milk", "milma": "milk", "aavin": "milk",
    "paper boat": "juices", "real": "juices", "tropicana": "juices", "bisleri": "water",
    "kinley": "water", "milo": "health_drinks", "nescafe": "coffee", "taj mahal": "tea",
    "tata tea": "tea", "red label": "tea", "wagh bakri": "tea", "society": "tea",
    "hajmola": "wellness", "patanjali divya": "wellness", "himalaya wellness": "wellness",
    "lizol": "cleaners", "harpic": "cleaners", "colin": "cleaners", "odonil": "repellents",
    "santoor": "bath_body", "dettol": "bath_body", "lifebuoy": "bath_body", "lux": "bath_body",
    "mysore sandal": "bath_body", "cinthol": "bath_body", "medimix": "bath_body", "pears": "bath_body",
    "nivea": "skin_care", "ponds": "skin_care", "pond's": "skin_care", "vaseline": "skin_care",
    "biotique": "skin_care", "glow & lovely": "skin_care", "fair & lovely": "skin_care",
    "lakme": "makeup", "maybelline": "makeup", "sugar cosmetics": "makeup",
    "set wet": "hair_care", "parachute": "hair_care", "clinic plus": "hair_care", "sunsilk": "hair_care",
    "park avenue": "fragrance", "fogg": "fragrance", "axe": "fragrance", "wild stone": "fragrance",
    "whisper": "feminine", "stayfree": "feminine", "colgate": "oral_care", "closeup": "oral_care",
    "rin": "detergents", "surf excel": "detergents", "ariel": "detergents", "tide": "detergents",
    "vim": "dishwash", "pril": "dishwash", "good knight": "repellents", "all out": "repellents",
    "dabur": "wellness", "patanjali": "wellness", "himalaya": "skin_care", "dove": "bath_body",
    "aachi": "masala", "ashoka": "ready_meals", "apollo": "wellness", "baidyanath": "wellness",
    "zandu": "wellness", "mtr": "ready_meals", "gits": "instant_mixes", "haldirams": "namkeen",
    "britania": "cookies", "nestle": "milk", "godrej": "home_essentials", "tata": "salt_sugar",
}
JUNK_NAME_RE = re.compile(
    r"(www\.|https?:|loading|^exp\b|\b\d{1,2}[/-]\d{2,4}\b|\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\w*\s+\d{4}\b"
    r"|\bbook\b|headphone|mouse|kettle|mixer grinder|charger|cable|\bpen\b|sandals|t-shirt|shirt|iron\b"
    r"|\bbeer\b|whisk(e)?y|vodka|\bwine\b|\brum\b|rakija|budweiser|kingfisher|heineken|alcohol|cigarette)",
    re.I)
COMPANY_RE = re.compile(
    r"\b(company|limited|ltd|pvt|private|industries|inc|corporation|corp|pepsico|unilever|hul|nestl[eé]|itc|"
    r"mondelez|ferrero|procter|p&g|reckitt|marico|emami|dabur india|tata consumer|adani wilmar|hershey india)\b",
    re.I)
NONFOOD_TAGS = {"en:non-food-products", "en:open-beauty-facts", "en:open-products-facts",
                "en:health-beauty", "en:cosmetic-products"}

OLD_CATEGORY_IDS = {
    "cat_off_beverages", "cat_off_dairy_bakery", "cat_off_biscuits_sweets", "cat_off_snacks",
    "cat_off_staples", "cat_grocery", "cat_grocery_oil", "cat_spices", "cat_spices_std", "cat_6eed8101",
}
OLD_FALLBACK_SUB = {
    "cat_off_beverages": "juices",
    "cat_off_dairy_bakery": "milk",
    "cat_off_biscuits_sweets": "cookies",
    "cat_off_snacks": "namkeen",
    "cat_off_staples": "masala",
    "cat_grocery": "masala",
}

SUB_BY_ID = {s[0]: s for s in SUBS}
PARENT_NAME = dict(PARENTS)
KEYWORD_RES = {
    s[0]: re.compile(r"\b(" + "|".join(re.escape(k) for k in s[4] + EXTRA_KEYWORDS.get(s[0], [])) + r")\b", re.I)
    for s in SUBS
}
BRAND_RE = re.compile(r"\b(" + "|".join(re.escape(b) for b in sorted(BRAND_FALLBACK, key=len, reverse=True)) + r")\b",
                      re.I)

UNIT_PATTERNS = [
    (re.compile(r"(\d+(?:\.\d+)?)\s*(kg|kilo)", re.I), "KG", 1.0),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(g|gm|gms|gram|grams)\b", re.I), "KG", 0.001),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(l|ltr|litre|liter)\b", re.I), "LITRE", 1.0),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(ml)\b", re.I), "LITRE", 0.001),
]


def sub_category_id(sub):
    return f"{sub[1]}__{sub[0]}"


def http_json(url, data=None, headers=None, method=None, timeout=90):
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("User-Agent", USER_AGENT)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def with_retry(fn, attempts=5, base_delay=2.0):
    for i in range(attempts):
        try:
            return fn()
        except (OSError, json.JSONDecodeError) as e:
            if isinstance(e, urllib.error.HTTPError) and e.code in (400, 401, 403, 404):
                raise
            if i == attempts - 1:
                raise
            time.sleep(base_delay * (2 ** i))


# ---------------------------------------------------------------- normalisation

def raw_name(p):
    for key in ("product_name_en", "product_name", "generic_name"):
        v = p.get(key)
        if isinstance(v, str) and v.strip():
            return re.sub(r"\s+", " ", v).strip()
    return ""


def first_brand(p):
    b = p.get("brands") or ""
    if isinstance(b, list):
        b = b[0] if b else ""
    b = b.split(",")[0].strip()
    return b.title() if b.isupper() or b.islower() else b


def display_name(p):
    name = raw_name(p)
    if name.isupper() or name.islower():
        name = name.title()
    brand = first_brand(p)
    squash = lambda s: re.sub(r"[^a-z0-9]", "", s.lower())
    if brand and squash(brand) not in squash(name) and not COMPANY_RE.search(brand):
        name = f"{brand} {name}"
    return name


def classify(name, tags, scope, brands=""):
    tags = {t.lower() for t in (tags or [])}
    if scope == "food" and tags & NONFOOD_TAGS:
        scope = "nonfood"
    primary = [s for s in SUBS if s[3] == scope]
    secondary = [s for s in SUBS if s[3] != scope]
    for s in primary:
        if KEYWORD_RES[s[0]].search(name):
            return s[0]
    for s in primary:
        if tags.intersection(s[5]):
            return s[0]
    m = BRAND_RE.search(f"{name} {brands}")
    if m:
        return BRAND_FALLBACK[m.group(1).lower()]
    for s in secondary:
        if KEYWORD_RES[s[0]].search(name):
            return s[0]
    return None


def parse_quantity(q):
    for pattern, unit, factor in UNIT_PATTERNS:
        m = pattern.search(q or "")
        if m:
            return unit, round(float(m.group(1)) * factor, 3)
    return "PCS", 0.0


def barcode_path(code):
    if len(code) <= 8:
        return code
    code = code.zfill(13)
    return f"{code[0:3]}/{code[3:6]}/{code[6:9]}/{code[9:]}"


def pick_images(code, images, host):
    """Front (selected crop, else first raw photo) + other raw photos, full resolution, up to
    IMAGES_PER_PRODUCT; None when fewer than MIN_IMAGES usable photos."""
    if not isinstance(images, dict):
        return None
    base = f"https://{host}/images/products/{barcode_path(code)}"

    def full_size(meta):
        full = ((meta or {}).get("sizes") or {}).get("full") or {}
        return int(full.get("w") or 0), int(full.get("h") or 0)

    label_ids = {str((images[k] or {}).get("imgid") or "") for k in images
                 if k.startswith(("nutrition", "ingredients"))}
    raws = []
    for k, meta in images.items():
        if not k.isdigit():
            continue
        rw, rh = full_size(meta)
        if min(rw, rh) < MIN_EXTRA_EDGE or not (0.33 <= rw / rh <= 3.0):
            continue
        raws.append((k in label_ids, int(k), k, f"{base}/{k}.jpg"))
    raws.sort()

    urls, front_imgid = [], ""
    fronts = sorted((k for k in images if k.startswith("front")), key=lambda k: (k != "front_en", k))
    if fronts:
        fkey = fronts[0]
        fmeta = images[fkey]
        if min(full_size(fmeta)) >= MIN_FRONT_EDGE:
            rev = fmeta.get("rev")
            urls.append(f"{base}/{fkey}.{rev}.full.jpg" if rev and fkey != "front" else f"{base}/front.jpg")
            front_imgid = str(fmeta.get("imgid") or "")
    if not urls and (not raws or raws[0][0]):
        return None
    urls += [u for _, _, k, u in raws if k != front_imgid][: IMAGES_PER_PRODUCT - len(urls)]
    return urls if len(urls) >= MIN_IMAGES else None


def to_record(p, source):
    code = str(p.get("code") or "").strip()
    if not code.isdigit() or len(code) < 8:
        return None
    name = display_name(p)
    brand = first_brand(p)
    rest = name[len(brand):] if brand and name.lower().startswith(brand.lower()) else name
    # One-word names ("Alpenliebe") are fine; classify() below still has to place them in a category.
    if len(name) < 4 or not re.search(r"[A-Za-z]{4}" if len(name.split()) < 2 else r"[A-Za-z]{3}", rest) \
            or JUNK_NAME_RE.search(name):
        return None
    if any("alcoholic" in t for t in (p.get("categories_tags") or [])):
        return None
    scope, host = SOURCES[source]
    brands = p.get("brands") if isinstance(p.get("brands"), str) else ""
    sub = classify(name, p.get("categories_tags"), scope, brands)
    if not sub:
        return None
    imgs = pick_images(code, p.get("images"), host)
    if not imgs:
        return None
    return {
        "code": code,
        "source": source,
        "name": name,
        "nameHi": (p.get("product_name_hi") or "").strip() if isinstance(p.get("product_name_hi"), str) else "",
        "brand": first_brand(p),
        "quantity": (p.get("quantity") or "").strip() if isinstance(p.get("quantity"), str) else "",
        "sub": sub,
        "images": imgs,
        "scans": int(p.get("unique_scans_n") or 0),
    }


# ---------------------------------------------------------------- fetch

def off_ranges():
    edges = [datetime.datetime(2000, 1, 1)] + [datetime.datetime(y, 1, 1) for y in range(2017, 2025)]
    edges += [datetime.datetime(2024, m, 1) for m in (4, 7, 10)] + [datetime.datetime(2030, 1, 1)]
    edges = sorted(set(edges))
    return [(int(a.timestamp()), int(b.timestamp()) - 1) for a, b in zip(edges, edges[1:])]


def fetch_off(delay, base_query='countries_tags:"en:india"'):
    out = []
    for a, b in off_ranges():
        q = f'{base_query} AND created_t:[{a} TO {b}]'
        for page in range(1, 101):
            url = OFF_SEARCH_URL + "?" + urllib.parse.urlencode(
                {"q": q, "page": page, "page_size": PAGE_SIZE, "fields": OFF_FIELDS})
            try:
                hits = with_retry(lambda: http_json(url)).get("hits") or []
            except Exception as e:
                print(f"  off {a}: page {page} stopped ({e})")
                break
            out += hits
            if len(hits) < PAGE_SIZE:
                break
            time.sleep(delay)
        print(f"off range {datetime.date.fromtimestamp(a)}: total raw {len(out)}")
    return out


def fetch_classic(source, delay, sort_by=""):
    out = []
    for page in range(1, 200):
        params = {"countries_tags_en": "india", "page": page, "page_size": PAGE_SIZE, "fields": OFF_FIELDS}
        if sort_by:
            params["sort_by"] = sort_by
        url = CLASSIC_SEARCH[source] + "?" + urllib.parse.urlencode(params)
        try:
            prods = with_retry(lambda: http_json(url)).get("products") or []
        except Exception as e:
            print(f"  {source}: page {page} stopped ({e})")
            break
        out += prods
        print(f"{source} page {page}: raw {len(out)}")
        if len(prods) < PAGE_SIZE:
            break
        time.sleep(delay)
    return out


def cmd_fetch(args):
    os.makedirs(CACHE_DIR, exist_ok=True)
    raw = [(p, "off") for p in fetch_off(args.delay)]
    for src in ("obf", "opf"):
        raw += [(p, src) for p in fetch_classic(src, args.delay)]
    with open(RAW_FILE, "w", encoding="utf-8") as f:
        json.dump(raw, f, ensure_ascii=False)
    build_records(raw)


def cmd_fetch_more(args):
    """Adds India-made (890) products OFF does not tag with India, and re-pulls OBF/OPF, into the raw cache."""
    raw = json.load(open(RAW_FILE, encoding="utf-8")) if os.path.exists(RAW_FILE) else []
    seen = {str(p.get("code") or "") for p, _ in raw}
    before = len(raw)
    fresh = [(p, "off") for p in fetch_off(args.delay, 'code:890* AND NOT countries_tags:"en:india"')]
    # The classic API stops paging at ~1000 hits; opposite sort orders reach both ends of larger sets.
    for src in ("obf", "opf"):
        for sort_by in ("", "created_t", "-created_t", "unique_scans_n"):
            fresh += [(p, src) for p in fetch_classic(src, args.delay, sort_by)]
    for p, src in fresh:
        code = str(p.get("code") or "")
        if code and code not in seen:
            seen.add(code)
            raw.append((p, src))
    print(f"raw cache {before} -> {len(raw)}")
    with open(RAW_FILE, "w", encoding="utf-8") as f:
        json.dump(raw, f, ensure_ascii=False)
    build_records(raw)


def build_records(raw):
    seen_codes, seen_names, records = set(), set(), []
    for p, src in sorted(raw, key=lambda x: -int(x[0].get("unique_scans_n") or 0)):
        r = to_record(p, src)
        if not r or r["code"] in seen_codes:
            continue
        key = (r["name"].lower(), r["quantity"].lower())
        if key in seen_names:
            continue
        seen_codes.add(r["code"])
        seen_names.add(key)
        records.append(r)
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False)
    print(f"raw {len(raw)} -> usable with >= {MIN_IMAGES} photo(s): {len(records)}")
    report(records)


def report(records):
    counts = {}
    for r in records:
        counts[r["sub"]] = counts.get(r["sub"], 0) + 1
    for pid, pname in PARENTS:
        subs = [s for s in SUBS if s[1] == pid]
        print(f"{pname:34} {sum(counts.get(s[0], 0) for s in subs)}")
        for s in subs:
            print(f"    {s[2]:30} {counts.get(s[0], 0)}")
    print(f"{'TOTAL':34} {len(records)}")


def load_cache():
    if not os.path.exists(CACHE_FILE):
        sys.exit("No cache. Run `fetch` first.")
    with open(CACHE_FILE, encoding="utf-8") as f:
        return json.load(f)


# ---------------------------------------------------------------- api

def read_password_file():
    if os.path.exists(PASSWORD_FILE):
        with open(PASSWORD_FILE, encoding="utf-8") as f:
            return f.read().strip()
    return ""


def multipart_body(parts):
    boundary = uuid.uuid4().hex
    body = b""
    for field, filename, data in parts:
        body += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{filename}\"\r\n"
                 f"Content-Type: image/jpeg\r\n\r\n").encode() + data + b"\r\n"
    return boundary, body + f"--{boundary}--\r\n".encode()


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.lock = threading.Lock()
        self.token_at = 0.0
        self.headers = {}
        self.login()

    def login(self):
        username = os.environ.get("ADMIN_USERNAME", "bhargav")
        password = os.environ.get("ADMIN_PASSWORD") or read_password_file() or sys.exit(
            f"Set ADMIN_PASSWORD or write it to {PASSWORD_FILE}")
        token = http_json(self.base + "/auth/admin/login",
                          json.dumps({"username": username, "password": password}).encode(),
                          {"Content-Type": "application/json"}, "POST")["accessToken"]
        self.headers = {"Authorization": f"Bearer {token}"}
        self.token_at = time.time()

    def _call(self, fn):
        try:
            return fn()
        except urllib.error.HTTPError as e:
            if e.code != 401:
                raise
            with self.lock:
                if time.time() - self.token_at > 30:
                    self.login()
            return fn()

    def post_json(self, path, payload):
        data = json.dumps(payload).encode()
        return self._call(lambda: http_json(
            self.base + path, data, {**self.headers, "Content-Type": "application/json"}, "POST"))

    def get_json(self, path):
        return self._call(lambda: http_json(self.base + path, headers=self.headers, timeout=300))

    def delete(self, path):
        req = lambda: urllib.request.urlopen(urllib.request.Request(
            self.base + path, method="DELETE", headers={**self.headers, "User-Agent": USER_AGENT}), timeout=60)
        return self._call(req)

    def upload(self, name, full, thumb):
        boundary, body = multipart_body([("file", f"{name}.jpg", full), ("thumbnail", f"{name}-thumb.jpg", thumb)])
        url = f"{self.base}/admin/uploads?prefix={urllib.parse.quote(UPLOAD_PREFIX)}"
        return self._call(lambda: http_json(
            url, body, {**self.headers, "Content-Type": f"multipart/form-data; boundary={boundary}"}, "POST", 180))


OFF_S3_MIRROR = "https://openfoodfacts-images.s3.eu-west-3.amazonaws.com/data/"
RAW_IMAGE_RE = re.compile(r"^https://images\.openfoodfacts\.org/images/products/(.+/\d+\.jpg)$")


def download(url):
    raw = RAW_IMAGE_RE.match(url)
    if raw:
        # Raw uploads are mirrored on OFF's S3 (seconds vs ~25 s from the throttled image host).
        try:
            return download(OFF_S3_MIRROR + raw.group(1))
        except urllib.error.HTTPError:
            pass
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=90) as resp:
        return resp.read()


def encode_jpeg(img, max_edge, max_bytes, quality):
    img = img.copy()
    img.thumbnail((max_edge, max_edge), Image.LANCZOS)
    while True:
        buf = io.BytesIO()
        img.save(buf, "JPEG", quality=quality, optimize=True, progressive=True)
        if buf.tell() <= max_bytes or quality <= 50:
            return buf.getvalue()
        quality -= 8


def compress(data):
    img = Image.open(io.BytesIO(data))
    img.load()
    if img.mode in ("RGBA", "LA", "P"):
        img = img.convert("RGBA")
        bg = Image.new("RGB", img.size, (255, 255, 255))
        bg.paste(img, mask=img.split()[-1])
        img = bg
    elif img.mode != "RGB":
        img = img.convert("RGB")
    return encode_jpeg(img, MAX_EDGE, UPLOAD_MAX_BYTES, 85), encode_jpeg(img, THUMB_EDGE, THUMB_MAX_BYTES, 78)


PACK_PATTERNS = [
    (re.compile(r"(\d+(?:\.\d+)?)\s*(kg|kilo)s?\b", re.I), "KG", 1.0, "kg"),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(g|gm|gms|gram|grams)\b", re.I), "GRAM", 1.0, "g"),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(l|ltr|litre|liter)s?\b", re.I), "LITRE", 1.0, "ltr"),
    (re.compile(r"(\d+(?:\.\d+)?)\s*(ml)\b", re.I), "LITRE", 0.001, "ml"),
]
BASE_AMOUNT = {"GRAM": 1.0, "KG": 1000.0, "LITRE": 1000.0}


def parse_pack(text):
    """Net content printed on a pack ("Parle-G 100g") -> (size, unit, label, matched span) or None."""
    for pattern, unit, factor, short in PACK_PATTERNS:
        m = pattern.search(text or "")
        if m:
            raw = float(m.group(1))
            return round(raw * factor, 3), unit, f"{raw:g} {short}", m.span()
    return None


def pack_variant(m, pack, with_image=False):
    size, unit, label, _ = pack
    return {
        "id": f"v_{m['id']}",
        "name": label,
        "sellMode": "PACK",
        "price": m.get("salePrice") or 0.0,
        "mrp": 0.0,
        "stockQty": m.get("stockQty") or 0.0,
        "barcode": m.get("barcode"),
        "sku": m.get("sku") or "",
        "packSize": size,
        "packUnit": unit,
        "imageUrls": (m.get("imageUrls") or [])[:1] if with_image else [],
        "active": True,
    }


def product_payload(r, image_urls):
    sub = SUB_BY_ID[r["sub"]]
    unit, weight = parse_quantity(r["quantity"])
    if unit == "PCS":
        unit, weight = parse_quantity(r["name"])
    pack = parse_pack(r["quantity"]) or parse_pack(r["name"])
    payload_id = f"mp_off_{r['code']}"
    variants = [pack_variant({"id": payload_id, "barcode": r["code"], "sku": f"OFF-{r['code']}"}, pack)] if pack else []
    return {
        "id": payload_id,
        "name": r["name"],
        "nameHi": r["nameHi"],
        "categoryId": sub[1],
        "subcategoryId": sub_category_id(sub),
        # Packaged goods: the weight is pack content, not a loose (khula) selling unit.
        "unit": "PIECE" if pack else unit,
        "variants": variants,
        "barcode": r["code"],
        "imageUrls": image_urls,
        "imageUrl": image_urls[0],
        "thumbnailUrl": image_urls[0],
        "brandName": r["brand"],
        "companyName": r["brand"],
        "description": " ".join(x for x in [r["name"], r["quantity"]] if x),
        "sku": f"OFF-{r['code']}",
        "tags": ",".join(x for x in [PARENT_NAME[sub[1]], sub[2], r["brand"], f"source:{r['source']}"] if x),
        "weightKg": weight if unit == "KG" else 0.0,
    }


def import_one(api, r):
    urls = []
    for i, src in enumerate(r["images"]):
        try:
            full, thumb = compress(with_retry(lambda: download(src)))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                continue
            raise
        up = with_retry(lambda: api.upload(f"off_{r['code']}_{i}", full, thumb))
        urls.append(up["url"])
    if not urls:
        raise RuntimeError("no downloadable images")
    with_retry(lambda: api.post_json("/admin/master/products", product_payload(r, urls)))


def ensure_categories(api):
    for pid, pname in PARENTS:
        api.post_json("/admin/master/categories", {"id": pid, "name": pname})
    for s in SUBS:
        api.post_json("/admin/master/categories", {"id": sub_category_id(s), "name": s[2], "parentId": s[1]})


def load_done():
    if not os.path.exists(DONE_FILE):
        return set()
    with open(DONE_FILE, encoding="utf-8") as f:
        return {line.strip() for line in f if line.strip()}


def cmd_import(args):
    api = Api(args.api)
    ensure_categories(api)
    records = load_cache()
    done = load_done()
    todo = [r for r in records if r["code"] not in done][: args.limit or None]
    print(f"{len(records)} cached, {len(done)} already imported, {len(todo)} to import")
    ok = failed = 0
    start = time.time()
    with open(DONE_FILE, "a", encoding="utf-8") as done_f, ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(import_one, api, r): r["code"] for r in todo}
        for fut in as_completed(futures):
            code = futures[fut]
            try:
                fut.result()
                ok += 1
                done_f.write(code + "\n")
                done_f.flush()
            except Exception as e:
                failed += 1
                print(f"  fail {code}: {e}", file=sys.stderr)
            n = ok + failed
            if n % 50 == 0:
                rate = n / max(1.0, time.time() - start)
                print(f"  {n}/{len(todo)} ok {ok} failed {failed} ({rate:.1f}/s)", flush=True)
    print(f"imported {ok}, failed {failed}")


def cmd_recategorize(args):
    api = Api(args.api)
    ensure_categories(api)
    moved = 0
    for m in api.get_json("/master/products"):
        old = m.get("categoryId")
        if old not in OLD_CATEGORY_IDS:
            continue
        sub_id = classify(m.get("name") or "", [], "food") or OLD_FALLBACK_SUB.get(old, "masala")
        sub = SUB_BY_ID[sub_id]
        m["categoryId"] = sub[1]
        m["subcategoryId"] = sub_category_id(sub)
        with_retry(lambda: api.post_json("/admin/master/products", m))
        moved += 1
    print(f"moved {moved} products into the new tree")


def cmd_packify(args):
    """Variant-less weight/volume items whose name carries a pack size become one PACK variant."""
    api = Api(args.api)
    changed = 0
    for m in api.get_json("/master/products"):
        if m.get("variants") or (m.get("unit") or "").upper() not in ("KG", "GRAM", "LITRE", "PCS", "PIECE"):
            continue
        pack = parse_pack(m.get("name"))
        if not pack:
            continue
        changed += 1
        print(f"  {m['id']}: {m['name']!r} {m.get('unit')} -> PIECE + pack {pack[2]}")
        if args.apply:
            m["unit"] = "PIECE"
            m["variants"] = [pack_variant(m, pack)]
            with_retry(lambda: api.post_json("/admin/master/products", m))
    print(f"{'updated' if args.apply else 'would update'} {changed} products")


def base_title(name, span):
    return re.sub(r"\s+", " ", (name[:span[0]] + name[span[1]:])).strip(" -,|/()")


def group_key(m, pack):
    title = re.sub(r"[^a-z0-9]+", " ", base_title(m["name"], pack[3]).lower()).strip()
    return (m.get("brandName") or "").strip().lower(), title


def cmd_group(args):
    """Same brand + same name except pack size -> one master product with a variant per size.
    The first item (most photos) becomes the parent; the others are deactivated and tagged."""
    api = Api(args.api)
    groups = {}
    for m in api.get_json("/master/products"):
        if not m.get("active", True):
            continue
        variants = m.get("variants") or []
        if len(variants) > 1 or any(v.get("sellMode") != "PACK" for v in variants):
            continue
        pack = parse_pack(m.get("name"))
        if pack:
            groups.setdefault(group_key(m, pack), []).append((m, pack))
    merged = 0
    for (brand, _), members in groups.items():
        sizes = {(p[1], p[0]) for _, p in members}
        if not brand or len(members) < 2 or len(sizes) < 2:
            continue
        members.sort(key=lambda mp: (-len(mp[0].get("imageUrls") or []), mp[0]["name"]))
        parent, parent_pack = members[0]
        seen, variants = set(), []
        for m, pack in sorted(members, key=lambda mp: mp[1][0] * BASE_AMOUNT.get(mp[1][1], 1.0)):
            if (pack[1], pack[0]) in seen:
                continue
            seen.add((pack[1], pack[0]))
            existing = (m.get("variants") or [None])[0]
            v = pack_variant(m, pack, with_image=True)
            if existing:
                v.update({"id": existing.get("id") or v["id"], "price": existing.get("price") or v["price"],
                          "mrp": existing.get("mrp") or 0.0, "stockQty": existing.get("stockQty") or v["stockQty"]})
            variants.append(v)
        merged += 1
        title = base_title(parent["name"], parent_pack[3])
        print(f"  {title!r} ({brand}): {', '.join(v['name'] for v in variants)} <- {[m['id'] for m, _ in members]}")
        if not args.apply:
            continue
        parent.update({"name": title, "unit": "PIECE", "variants": variants})
        with_retry(lambda: api.post_json("/admin/master/products", parent))
        for m, _ in members[1:]:
            m["active"] = False
            m["tags"] = ",".join(x for x in [m.get("tags") or "", f"merged_into:{parent['id']}"] if x)
            with_retry(lambda: api.post_json("/admin/master/products", m))
    print(f"{'merged' if args.apply else 'would merge'} {merged} groups")


OPEN_PRICES_URL = "https://prices.openfoodfacts.org/api/v1/prices"
OFF_IMAGE_HOST = "images.openfoodfacts.org"
ENRICH_CACHE = os.path.join(CACHE_DIR, "enrich_cache.json")
ENRICH_REPORT = os.path.join(CACHE_DIR, "enrich_report.json")
ENRICH_FIELDS = "code,product_name,product_name_en,brands,quantity,images,unique_scans_n"
TITLE_STOPWORDS = {"pack", "of", "the", "and", "with", "new", "pouch", "box", "jar", "bottle", "packet", "combo"}
# Descriptive words OFF contributors add or drop freely; ignored when comparing names.
GENERIC_WORDS = {"biscuit", "biscuits", "cookie", "cookies", "original", "classic", "gluco", "glucose", "refill",
                 "family", "fresh", "premium", "pasteurized", "pasteurised", "refined", "iodised", "iodized",
                 "india", "indian", "pure", "natural", "regular", "value", "saver", "mrp", "rs"}
MASS = {"GRAM", "KG"}


def title_tokens(name, brand):
    pack = parse_pack(name)
    title = base_title(name, pack[3]) if pack else name
    words = set(re.findall(r"[a-z0-9]+", title.lower())) - TITLE_STOPWORDS - GENERIC_WORDS
    words -= {w for w in words if w.isdigit()}
    core = words - set(re.findall(r"[a-z0-9]+", (brand or "").lower()))
    return core or words


def same_item(master_tokens, cand_tokens):
    if not master_tokens or not cand_tokens:
        return False
    return master_tokens == cand_tokens or (
        len(master_tokens) >= 3 and len(master_tokens & cand_tokens) / len(master_tokens | cand_tokens) >= 0.8)


def hit_brands(hit):
    b = hit.get("brands") or []
    names = b if isinstance(b, list) else b.split(",")
    return {re.sub(r"[^a-z0-9]", "", n.lower()) for n in names if n.strip()}


def brand_matches(hit, brand):
    want = re.sub(r"[^a-z0-9]", "", brand.lower())
    return any(h == want or h.startswith(want) or want.startswith(h) for h in hit_brands(hit) if len(h) >= 3)


def base_amount(size, unit):
    return size * BASE_AMOUNT.get(unit, 1.0)


def comparable(u1, u2):
    return u1 == u2 or (u1 in MASS and u2 in MASS)


class OffLookup:
    """Open Food Facts search + Open Prices, cached on disk so reruns are offline and resumable."""

    def __init__(self, delay):
        self.delay = delay
        self.cache = json.load(open(ENRICH_CACHE, encoding="utf-8")) if os.path.exists(ENRICH_CACHE) else {}

    def save(self):
        os.makedirs(CACHE_DIR, exist_ok=True)
        with open(ENRICH_CACHE, "w", encoding="utf-8") as f:
            json.dump(self.cache, f)

    def _get(self, key, url, pick):
        if key not in self.cache:
            try:
                self.cache[key] = pick(with_retry(lambda: http_json(url, timeout=60)))
            except Exception as e:
                print(f"    lookup failed {key}: {e}", file=sys.stderr)
                return None
            time.sleep(self.delay)
        return self.cache[key]

    def search(self, brand, title):
        q = f'brands:"{brand}" AND countries_tags:"en:india" {title}'
        url = OFF_SEARCH_URL + "?" + urllib.parse.urlencode({"q": q, "page_size": 50, "fields": ENRICH_FIELDS})
        return self._get(f"s:{brand}|{title}".lower(), url, lambda r: r.get("hits") or []) or []

    def price(self, code):
        url = OPEN_PRICES_URL + "?" + urllib.parse.urlencode(
            {"product_code": code, "currency": "INR", "order_by": "-date", "size": 1})
        items = self._get(f"p:{code}", url, lambda r: r.get("items") or []) or []
        return float(items[0].get("price") or 0.0) if items else 0.0


def estimate_price(size, unit, priced):
    """Proportional to the nearest priced sibling of the same dimension; 0 when none."""
    same = [(abs(base_amount(s, u) - base_amount(size, unit)), p, base_amount(s, u))
            for s, u, p in priced if p > 0 and comparable(u, unit)]
    if not same:
        return 0.0
    _, price, amount = min(same)
    return float(round(price * base_amount(size, unit) / amount))


def find_new_variants(m, off, known_barcodes):
    """OFF siblings of master product [m] (same brand, same title, other pack size)."""
    brand = (m.get("brandName") or "").strip()
    name = m.get("name") or ""
    if not brand:
        return []
    tokens = title_tokens(name, brand)
    variants = m.get("variants") or []
    have = {(v.get("packUnit"), float(v.get("packSize") or 0)) for v in variants}
    have_codes = {v.get("barcode") for v in variants if v.get("barcode")} | {m.get("barcode")}
    priced = [(float(v.get("packSize") or 0), v.get("packUnit"), float(v.get("price") or 0)) for v in variants]
    pack = parse_pack(name)
    if pack and not variants:
        have.add((pack[1], pack[0]))
        priced.append((pack[0], pack[1], float(m.get("salePrice") or 0)))
    title = base_title(name, pack[3]) if pack else name
    found = []
    hits = sorted(off.search(brand, title), key=lambda h: -(h.get("unique_scans_n") or 0))
    for hit in hits:
        code = str(hit.get("code") or "").strip()
        if not code or code in have_codes or (code in known_barcodes and known_barcodes[code] != m["id"]):
            continue
        if not brand_matches(hit, brand):
            continue
        cand_name = display_name(hit)
        if not same_item(tokens, title_tokens(cand_name, brand)):
            continue
        cpack = parse_pack(hit.get("quantity") or "") or parse_pack(cand_name)
        if not cpack or (cpack[1], cpack[0]) in have:
            continue
        size, unit, label, _ = cpack
        have.add((unit, size))
        have_codes.add(code)
        price, source = off.price(code), "open_prices"
        if price <= 0:
            price, source = estimate_price(size, unit, priced), "estimated"
        found.append({
            "id": f"v_off_{code}",
            "name": label,
            "sellMode": "PACK",
            "price": price,
            "mrp": price if source == "open_prices" else 0.0,
            "barcode": code,
            "sku": f"OFF-{code}",
            "packSize": size,
            "packUnit": unit,
            "imageUrls": (pick_images(code, hit.get("images"), OFF_IMAGE_HOST) or [])[:1],
            # Never sell at Rs 0: unpriced variants are added switched off for the admin to price.
            "active": price > 0,
            "attributes": {"source": "openfoodfacts", "priceSource": source if price > 0 else "none",
                           "offName": cand_name[:120]},
        })
    return found


def upload_variant_image(api, v):
    urls = v.get("imageUrls") or []
    if not urls or not api:
        return
    try:
        full, thumb = compress(with_retry(lambda: download(urls[0])))
        v["imageUrls"] = [with_retry(lambda: api.upload(f"off_{v['barcode']}_v", full, thumb))["url"]]
    except Exception:
        v["imageUrls"] = []


def cmd_enrich(args):
    """Add missing pack-size variants (with barcode, image, price) to every master product."""
    off = OffLookup(args.delay)
    if args.sample:
        brand, _, name = args.sample.partition(":")
        m = {"id": "sample", "name": name.strip(), "brandName": brand.strip(), "variants": []}
        for v in find_new_variants(m, off, {}):
            print(f"  {v['name']:>10}  Rs {v['price']:>7}  {v['attributes']['priceSource']:<12} {v['barcode']}  "
                  f"{v['attributes']['offName']}")
        off.save()
        return
    api = Api(args.api)
    masters = [m for m in api.get_json("/master/products") if m.get("active", True)]
    known = {}
    for m in masters:
        for code in [m.get("barcode")] + [v.get("barcode") for v in m.get("variants") or []]:
            if code:
                known.setdefault(code, m["id"])
    todo = masters[: args.limit or None]
    report_rows, added, products = [], 0, 0
    for i, m in enumerate(todo, 1):
        new = find_new_variants(m, off, known)
        if i % 25 == 0:
            off.save()
            print(f"  {i}/{len(todo)} checked, {added} variants found", flush=True)
        if not new:
            continue
        products += 1
        added += len(new)
        print(f"  {m['id']} {m['name']!r}: + " + ", ".join(
            f"{v['name']} Rs{v['price']:g}{'' if v['active'] else ' (off)'}" for v in new))
        report_rows.append({"id": m["id"], "name": m["name"], "variants": new})
        if not args.apply:
            continue
        variants = m.get("variants") or []
        pack = parse_pack(m.get("name"))
        if not variants and pack:
            variants = [pack_variant(m, pack)]
            m["name"] = base_title(m["name"], pack[3]) or m["name"]
        for v in new:
            upload_variant_image(api, v)
            known[v["barcode"]] = m["id"]
        m["unit"] = "PIECE"
        m["variants"] = variants + new
        with_retry(lambda: api.post_json("/admin/master/products", m))
    off.save()
    with open(ENRICH_REPORT, "w", encoding="utf-8") as f:
        json.dump(report_rows, f, ensure_ascii=False, indent=1)
    unpriced = sum(1 for r in report_rows for v in r["variants"] if not v["active"])
    print(f"{'added' if args.apply else 'would add'} {added} variants to {products} products "
          f"({unpriced} without a price, added switched off). Report: {ENRICH_REPORT}")


def cmd_cleanup(args):
    api = Api(args.api)
    cats = api.get_json("/master/categories")
    old = [c for c in cats if c["id"] in OLD_CATEGORY_IDS]
    for c in sorted(old, key=lambda c: c.get("parentId") is None):
        try:
            api.delete(f"/admin/master/categories/{urllib.parse.quote(c['id'])}")
            print(f"deleted {c['id']} ({c['name']})")
        except urllib.error.HTTPError as e:
            print(f"kept {c['id']}: HTTP {e.code} {e.read().decode(errors='ignore')[:120]}")


# ---------------------------------------------------------------- reimport

REIMPORT_DONE_FILE = os.path.join(CACHE_DIR, "imported_v3.txt")
INDIA_PREFIX = "890"
# Typical Indian shelf price (Rs) per kg / litre, per sub-category; small packs snap to MRP points.
RATE_PER_BASE = {
    "atta_flours": 55, "rice": 90, "dals": 160, "poha_suji": 70, "oils": 180, "ghee": 650,
    "salt_sugar": 45, "whole_spices": 900, "masala": 600, "milk": 66, "curd": 120, "paneer_cheese": 500,
    "butter_cream": 550, "bread": 90, "cakes": 400, "cereals": 450, "oats": 220, "honey": 500,
    "peanut_butter": 450, "spreads": 400, "chips": 450, "namkeen": 300, "nachos": 500, "popcorn_puffs": 500,
    "healthy_snacks": 700, "cookies": 250, "cream_biscuits": 300, "glucose_marie": 150, "crackers": 250,
    "rusk": 200, "chocolates": 900, "candies": 500, "mithai": 500, "ice_cream": 250, "desserts": 400,
    "noodles": 220, "pasta": 200, "soups": 1000, "instant_mixes": 500, "ready_meals": 450,
    "ketchup_sauces": 200, "pickles": 300, "chutney_dips": 400, "pastes": 300, "nuts": 1000,
    "dried_fruits": 700, "seeds": 500, "tea": 500, "coffee": 1500, "health_drinks": 550, "soft_drinks": 80,
    "juices": 120, "water": 20, "energy_drinks": 400, "syrups": 250, "milk_drinks": 150, "frozen": 300,
    "meat_fish": 500, "baby_food": 600, "baby_care": 600, "oral_care": 600, "bath_body": 400,
    "skin_care": 1500, "hair_care": 700, "shaving": 600, "fragrance": 1200, "makeup": 2000, "feminine": 400,
    "wellness": 800, "detergents": 120, "dishwash": 150, "cleaners": 200, "repellents": 600, "pooja": 400,
    "home_essentials": 300,
}
PIECE_PRICE = {"eggs": 7, "candies": 5, "chips": 20, "namkeen": 20, "cookies": 20, "chocolates": 20,
               "feminine": 40, "home_essentials": 50, "pooja": 30, "makeup": 150, "fragrance": 199}
DEFAULT_RATE = 400
DEFAULT_PIECE_PRICE = 50
MRP_POINTS = [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 99]
PRINTED_PRICE_RE = re.compile(r"(?:₹|\brs\.?|\binr|\bmrp)\s*(\d{1,4})\b|\b(\d{1,4})\s*(?:rs\b|₹|/-)", re.I)


def snap_mrp(value):
    if value <= 5:
        return 5.0
    if value < 100:
        return float(min(MRP_POINTS, key=lambda p: abs(p - value)))
    step = 5 if value < 500 else 10
    return float(round(value / step) * step)


def printed_price(text):
    m = PRINTED_PRICE_RE.search(text or "")
    if not m:
        return 0.0
    value = float(m.group(1) or m.group(2))
    return value if 1 <= value <= 5000 else 0.0


def estimated_price(sub, pack):
    if not pack:
        return float(PIECE_PRICE.get(sub, DEFAULT_PIECE_PRICE))
    size, unit = pack[0], pack[1]
    amount = size if unit in ("KG", "LITRE") else size / 1000.0
    return snap_mrp(RATE_PER_BASE.get(sub, DEFAULT_RATE) * amount)


def load_open_prices():
    """All INR prices in Open Prices, latest per barcode."""
    prices, page = {}, 1
    while True:
        url = OPEN_PRICES_URL + "?" + urllib.parse.urlencode(
            {"currency": "INR", "order_by": "date", "size": 100, "page": page})
        try:
            data = with_retry(lambda: http_json(url, timeout=60))
        except Exception as e:
            print(f"  open prices page {page} stopped ({e})")
            break
        for item in data.get("items") or []:
            code = str(item.get("product_code") or "")
            if code and float(item.get("price") or 0) > 0:
                prices[code] = float(item["price"])
        if page >= int(data.get("pages") or 1):
            break
        page += 1
    return prices


def reimport_groups(records):
    """Records grouped by brand + title without pack size; India-made (890) first, then most-scanned.
    Every non-890 record in the cache came from an India-tagged search (sold in India)."""
    groups = {}
    for r in records:
        name_pack = parse_pack(r["name"])
        pack = name_pack or parse_pack(r["quantity"])
        title = base_title(r["name"], name_pack[3]) if name_pack else r["name"]
        key = ((r["brand"] or "").strip().lower(), re.sub(r"[^a-z0-9]+", " ", title.lower()).strip()) \
            if pack and r["brand"] else ("", r["code"])
        groups.setdefault(key, {"title": title, "members": []})["members"].append((r, pack))
    out = []
    for g in groups.values():
        seen, members = set(), []
        for r, pack in sorted(g["members"], key=lambda m: -m[0]["scans"]):
            size_key = (pack[1], pack[0]) if pack else None
            if size_key in seen:
                continue
            seen.add(size_key)
            members.append((r, pack))
        members.sort(key=lambda m: m[1][0] * BASE_AMOUNT.get(m[1][1], 1.0) if m[1] else 0)
        out.append({"title": g["title"], "members": members, "scans": max(r["scans"] for r, _ in members)})
    out.sort(key=lambda g: (not g["members"][0][0]["code"].startswith(INDIA_PREFIX), -g["scans"]))
    return out


def upload_images(api, code, urls):
    out = []
    for i, src in enumerate(urls):
        try:
            full, thumb = compress(with_retry(lambda: download(src)))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                continue
            raise
        out.append(with_retry(lambda: api.upload(f"off_{code}_{i}", full, thumb))["url"])
    return out


def reimport_one(api, group, open_prices):
    members = group["members"]
    parent = max(members, key=lambda m: (len(m[0]["images"]), m[0]["scans"]))[0]
    sub = SUB_BY_ID[parent["sub"]]
    product_images = upload_images(api, parent["code"], parent["images"])
    if not product_images:
        raise RuntimeError("no downloadable images")
    variants = []
    for r, pack in members:
        images = product_images[:1] if r is parent else upload_images(api, r["code"], r["images"][:1])
        price = open_prices.get(r["code"]) or printed_price(f"{r['name']} {r['quantity']}") \
            or estimated_price(r["sub"], pack)
        variants.append({
            "id": f"v_{r['code']}",
            "name": pack[2] if pack else "1 pc",
            "sellMode": "PACK",
            "price": price,
            "mrp": price,
            "stockQty": 0.0,
            "barcode": r["code"],
            "sku": f"OFF-{r['code']}",
            "packSize": pack[0] if pack else 0.0,
            "packUnit": pack[1] if pack else "",
            "bundleCount": 0,
            "baseVariantId": "",
            "imageUrls": images or product_images[:1],
            "active": True,
        })
    name = group["title"] if len(members) > 1 else parent["name"]
    payload = {
        "id": f"mp_off_{parent['code']}",
        "name": name,
        "nameHi": next((r["nameHi"] for r, _ in members if r["nameHi"]), ""),
        "categoryId": sub[1],
        "subcategoryId": sub_category_id(sub),
        "brandName": parent["brand"],
        "companyName": parent["brand"],
        "unit": "PIECE",
        "barcode": None,
        "salePrice": 0.0,
        "cost": 0.0,
        "active": True,
        "imageUrls": product_images,
        "imageUrl": product_images[0],
        "thumbnailUrl": product_images[0],
        "description": " ".join(x for x in [parent["name"], parent["quantity"]] if x),
        "sku": f"OFF-{parent['code']}",
        "tags": ",".join(x for x in [
            PARENT_NAME[sub[1]], sub[2], parent["brand"], f"source:{parent['source']}",
            "origin:india_made" if parent["code"].startswith(INDIA_PREFIX) else "origin:sold_in_india",
        ] if x),
        "variants": variants,
    }
    with_retry(lambda: api.post_json("/admin/master/products", payload))
    return payload["id"]


def cmd_reimport(args):
    """Fresh master catalog: India-made (890) products, pack sizes as variants, images on S3, priced."""
    api = Api(args.api)
    ensure_categories(api)
    groups = reimport_groups(load_cache())
    done = set()
    if os.path.exists(REIMPORT_DONE_FILE):
        with open(REIMPORT_DONE_FILE, encoding="utf-8") as f:
            done = {line.strip() for line in f if line.strip()}
    existing, page = set(), 0
    while True:
        data = api.get_json(f"/master/products/page?page={page}&pageSize=100")
        for m in data.get("items") or []:
            existing.update(v.get("barcode") for v in m.get("variants") or [] if v.get("barcode"))
        if not data.get("hasMore"):
            break
        page += 1
    todo = [g for g in groups if g["members"][0][0]["code"] not in done
            and not any(r["code"] in existing for r, _ in g["members"])][: args.limit or None]
    open_prices = load_open_prices()
    print(f"{len(groups)} products ({sum(len(g['members']) for g in groups)} variants), "
          f"{len(done)} done, {len(todo)} to import; {len(open_prices)} Open Prices barcodes", flush=True)
    ok = failed = 0
    start = time.time()
    with open(REIMPORT_DONE_FILE, "a", encoding="utf-8") as done_f, ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(reimport_one, api, g, open_prices): g["members"][0][0]["code"] for g in todo}
        for fut in as_completed(futures):
            key = futures[fut]
            try:
                fut.result()
                ok += 1
                done_f.write(key + "\n")
                done_f.flush()
            except Exception as e:
                failed += 1
                print(f"  fail {key}: {e}", file=sys.stderr, flush=True)
            n = ok + failed
            if n % 100 == 0:
                rate = n / max(1.0, time.time() - start)
                print(f"  {n}/{len(todo)} ok {ok} failed {failed} ({rate:.1f}/s)", flush=True)
    print(f"reimported {ok}, failed {failed}", flush=True)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    f = sub.add_parser("fetch")
    f.add_argument("--delay", type=float, default=0.3)
    fm = sub.add_parser("fetch-more", help="add India-made products OFF does not tag with India")
    fm.add_argument("--delay", type=float, default=0.3)
    sub.add_parser("report")
    sub.add_parser("rebuild", help="re-classify the cached raw fetch without network")
    for name in ("import", "reimport", "recategorize", "cleanup", "packify", "group", "enrich"):
        p = sub.add_parser(name)
        p.add_argument("--api", default=os.environ.get("PANSARI_API", "https://api.pansariwala.shop"))
        if name in ("import", "reimport", "enrich"):
            p.add_argument("--limit", type=int, default=0, help="max products this run (0 = all)")
        if name in ("import", "reimport"):
            p.add_argument("--workers", type=int, default=6)
        if name in ("packify", "group", "enrich"):
            p.add_argument("--apply", action="store_true", help="write changes (default: dry run)")
        if name == "enrich":
            p.add_argument("--delay", type=float, default=0.5, help="seconds between internet lookups")
            p.add_argument("--sample", default="", help='"Brand:Product name" - search only, no admin login')
    args = ap.parse_args()
    {"fetch": cmd_fetch, "fetch-more": cmd_fetch_more, "report": lambda a: report(load_cache()), "import": cmd_import, "reimport": cmd_reimport,
     "rebuild": lambda a: build_records(json.load(open(RAW_FILE, encoding="utf-8"))),
     "recategorize": cmd_recategorize, "cleanup": cmd_cleanup,
     "packify": cmd_packify, "group": cmd_group, "enrich": cmd_enrich}[args.cmd](args)


if __name__ == "__main__":
    main()
