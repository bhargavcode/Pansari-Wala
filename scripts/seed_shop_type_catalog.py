#!/usr/bin/env python3
"""Seed the master catalog with everyday products for shop types that have none yet
(hardware, plumbing, electrical, stationery, medical, ...).

Products live in scripts/shop_catalog/<SHOP_TYPE>.txt (one root category per shop type):

    # type: PLUMBING | Plumbing & Sanitary            shop type id | English name (created if missing)
    # root: mc_plumbing | Plumbing & Sanitary           root category id | name (linked to the shop type)
    # sub:  pipes | Pipes                               sub-category key | name
    pipes | CPVC Pipe 3 m | 260 | v=1/2 inch:260; 3/4 inch:420 | hi=सीपीवीसी पाइप

    Columns: sub | English name | price (INR) | optional key=value fields:
      v=  pack/size variants "label:price; label:price" (the product price is ignored)
      u=  unit KG, GRAM, LITRE, PACKET or PIECE (default PIECE)
      hi= Hindi name (other languages: run translate_products.py apply afterwards)
      b=  brand
      q=  image search text when the name alone finds poor photos

Images are free-licensed photos from Wikimedia Commons (Openverse as fallback), compressed and
uploaded to the platform bucket. The photo credit is kept in the product tags.

Steps (from repo root):
  python3 scripts/seed_shop_type_catalog.py validate
  python3 scripts/seed_shop_type_catalog.py images --types HARDWARE,PLUMBING    # find photos, writes preview HTML
  python3 scripts/seed_shop_type_catalog.py import --types HARDWARE,PLUMBING    # dry run
  python3 scripts/seed_shop_type_catalog.py import --types HARDWARE,PLUMBING --apply
  python3 scripts/translate_products.py apply
Pick a better photo: put "<product id>": "<image url>" in .catalog-cache/shop_type_image_overrides.json.
Admin password: ADMIN_PASSWORD env var or scripts/.catalog-cache/admin_password.
"""
import argparse
import html
import json
import os
import re
import sys
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from seed_master_catalog import CACHE_DIR, Api, compress, download, http_json, with_retry  # noqa: E402

DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "shop_catalog")
IMAGE_CACHE = os.path.join(CACHE_DIR, "shop_type_images.json")
IMAGE_OVERRIDES = os.path.join(CACHE_DIR, "shop_type_image_overrides.json")
PREVIEW_FILE = os.path.join(CACHE_DIR, "shop_type_images.html")
DONE_FILE = os.path.join(CACHE_DIR, "shop_type_imported.txt")
COMMONS_API = "https://commons.wikimedia.org/w/api.php"
OPENVERSE_API = "https://api.openverse.org/v1/images/"
UNITS = {"KG", "GRAM", "LITRE", "PACKET", "PIECE"}
MIN_IMAGE_EDGE = 300
IMAGE_WIDTH = 1000
LOOKUP_DELAY = 0.4
BLOCKED_LICENSES = ("fair use", "non-free")
SIZE_RE = re.compile(r"\b\d+(\.\d+)?\s*(mm|cm|m|ft|inch|in|kg|g|ml|l|w|a|v|hp|mfd|pcs|pages|sq mm)\b|\d+/\d+", re.I)


def slug(text):
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


def parse_file(path):
    meta = {"subs": {}, "items": []}
    for n, raw in enumerate(open(path, encoding="utf-8"), 1):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#"):
            key, _, rest = line[1:].partition(":")
            key = key.strip()
            parts = [p.strip() for p in rest.split("|")]
            if key == "type":
                meta["type"], meta["type_name"] = parts[0], parts[1]
            elif key == "root":
                meta["root"], meta["root_name"] = parts[0], parts[1]
            elif key == "sub":
                meta["subs"][parts[0]] = parts[1]
            continue
        cols = [c.strip() for c in line.split("|")]
        if len(cols) < 3:
            raise ValueError(f"{path}:{n}: need 'sub | name | price'")
        sub, name, price = cols[0], cols[1], float(cols[2])
        if sub not in meta["subs"]:
            raise ValueError(f"{path}:{n}: unknown sub '{sub}'")
        item = {"sub": sub, "name": name, "price": price, "unit": "PIECE", "variants": [], "hi": "", "brand": "", "q": ""}
        for extra in cols[3:]:
            key, _, value = extra.partition("=")
            key, value = key.strip(), value.strip()
            if key == "v":
                for part in value.split(";"):
                    label, _, p = part.rpartition(":")
                    item["variants"].append((label.strip(), float(p)))
            elif key == "u":
                if value not in UNITS:
                    raise ValueError(f"{path}:{n}: unit must be one of {sorted(UNITS)}")
                item["unit"] = value
            elif key in ("hi", "b", "q"):
                item["brand" if key == "b" else key] = value
            else:
                raise ValueError(f"{path}:{n}: unknown field '{key}'")
        item["id"] = f"mp_st_{meta['type'].lower()}_{slug(name)}"
        meta["items"].append(item)
    for key in ("type", "root"):
        if key not in meta:
            raise ValueError(f"{path}: missing '# {key}:' header")
    return meta


def load_catalogs(types=None):
    out = []
    for fname in sorted(os.listdir(DATA_DIR)):
        if not fname.endswith(".txt"):
            continue
        meta = parse_file(os.path.join(DATA_DIR, fname))
        if not types or meta["type"] in types:
            out.append(meta)
    return out


def load_json(path):
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_json(path, data):
    os.makedirs(CACHE_DIR, exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    os.replace(tmp, path)


def strip_html(text):
    return re.sub(r"<[^>]+>", "", html.unescape(text or "")).strip()


def search_query(item, sub_name):
    if item["q"]:
        return item["q"]
    base = SIZE_RE.sub(" ", item["name"])
    base = re.sub(r"\b(pack of|pack|set of|box|per|pair)\b", " ", base, flags=re.I)
    return re.sub(r"\s+", " ", base).strip() or sub_name


def score(title, query):
    """(query words found in the title, minus unrelated title words): favours plain product shots."""
    words = [w for w in re.findall(r"[a-z]+", query.lower()) if len(w) > 2]
    t = re.sub(r"^file:|\.(jpe?g|png)$", "", title.lower())
    hits = sum(1 for w in words if w in t)
    extra = len([w for w in re.findall(r"[a-z]+", t) if len(w) > 2 and not any(q in w for q in words)])
    return hits, -extra


def commons_candidates(query):
    params = {
        "action": "query", "format": "json", "generator": "search", "gsrnamespace": 6,
        "gsrsearch": f"{query} filetype:bitmap", "gsrlimit": 10, "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata", "iiurlwidth": IMAGE_WIDTH,
    }
    data = with_retry(lambda: http_json(COMMONS_API + "?" + urllib.parse.urlencode(params), timeout=30))
    out = []
    for page in (data.get("query") or {}).get("pages", {}).values():
        info = (page.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata") or {}
        lic = strip_html((meta.get("LicenseShortName") or {}).get("value"))
        if info.get("mime") not in ("image/jpeg", "image/png"):
            continue
        if min(info.get("width", 0), info.get("height", 0)) < MIN_IMAGE_EDGE:
            continue
        if not lic or any(b in lic.lower() for b in BLOCKED_LICENSES):
            continue
        out.append({
            "url": info.get("thumburl") or info.get("url"),
            "page": info.get("descriptionurl", ""),
            "title": page.get("title", ""),
            "credit": strip_html((meta.get("Artist") or {}).get("value"))[:80],
            "license": lic,
            "source": "commons",
            "rank": page.get("index", 99),
        })
    return out


def openverse_candidates(query):
    params = {"q": query, "license_type": "commercial", "page_size": 10}
    data = with_retry(lambda: http_json(OPENVERSE_API + "?" + urllib.parse.urlencode(params), timeout=30))
    out = []
    for i, r in enumerate(data.get("results") or []):
        if min(r.get("width") or 0, r.get("height") or 0) < MIN_IMAGE_EDGE and r.get("width"):
            continue
        out.append({
            "url": r.get("url"),
            "page": r.get("foreign_landing_url", ""),
            "title": r.get("title", ""),
            "credit": (r.get("creator") or "")[:80],
            "license": f"{(r.get('license') or '').upper()} {r.get('license_version') or ''}".strip(),
            "source": "openverse",
            "rank": i,
        })
    return out


def find_image(query, fallback=""):
    for q in [query] + ([fallback] if fallback and fallback != query else []):
        for finder in (commons_candidates, openverse_candidates):
            try:
                found = finder(q)
            except Exception as e:  # one source down should not stop the run
                print(f"  {finder.__name__} '{q}': {e}", file=sys.stderr)
                found = []
            found = [c for c in found if c["url"] and score(c["title"], q)[0] > 0]
            if found:
                return max(found, key=lambda c: (score(c["title"], q), -c["rank"]))
            time.sleep(LOOKUP_DELAY)
    return None


def fallback_query(query, sub_name):
    """Last two words plus the sub-category, e.g. 'CPVC Elbow' -> 'CPVC Elbow Pipe Fittings'."""
    return f"{' '.join(query.split()[-2:])} {sub_name.split('&')[0].strip()}"


def cmd_validate(args):
    total = 0
    seen = {}
    for meta in load_catalogs(args.types):
        ids = [i["id"] for i in meta["items"]]
        dupes = sorted({i for i in ids if ids.count(i) > 1})
        for i in ids:
            if i in seen:
                dupes.append(f"{i} (also in {seen[i]})")
            seen[i] = meta["type"]
        total += len(ids)
        flag = "" if len(ids) >= 100 else "  <-- under 100"
        print(f"{meta['type']:18} {len(ids):4} products, {len(meta['subs'])} sub-categories{flag}")
        for d in dupes:
            print(f"   duplicate id: {d}")
    print(f"total {total}")


def cmd_images(args):
    cache = load_json(IMAGE_CACHE)
    overrides = load_json(IMAGE_OVERRIDES)
    catalogs = load_catalogs(args.types)
    todo = [(m, i) for m in catalogs for i in m["items"] if i["id"] not in overrides and (args.refresh or i["id"] not in cache)]
    print(f"looking up {len(todo)} images")

    def lookup(job):
        meta, item = job
        sub_name = meta["subs"][item["sub"]]
        query = search_query(item, sub_name)
        return item["id"], find_image(query, fallback_query(query, sub_name)) or {"missing": True, "query": query}

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for n, fut in enumerate(as_completed([pool.submit(lookup, j) for j in todo]), 1):
            item_id, pick = fut.result()
            cache[item_id] = pick
            if n % 20 == 0:
                save_json(IMAGE_CACHE, cache)
                print(f"  {n}/{len(todo)}", flush=True)
    save_json(IMAGE_CACHE, cache)
    write_preview(catalogs, cache, overrides)


def write_preview(catalogs, cache, overrides):
    rows = []
    for meta in catalogs:
        rows.append(f"<h2>{html.escape(meta['type_name'])}</h2><div class=g>")
        for item in meta["items"]:
            pick = overrides.get(item["id"]) or cache.get(item["id"]) or {}
            url = pick if isinstance(pick, str) else pick.get("url")
            img = f"<img src='{html.escape(url)}' loading=lazy>" if url else "<div class=miss>no image</div>"
            credit = "" if isinstance(pick, str) else html.escape(f"{pick.get('license', '')} {pick.get('credit', '')}")
            rows.append(f"<figure>{img}<figcaption><b>{html.escape(item['name'])}</b><br>"
                        f"<code>{item['id']}</code><br><small>{credit}</small></figcaption></figure>")
        rows.append("</div>")
    page = ("<meta charset=utf-8><style>body{font-family:sans-serif}.g{display:flex;flex-wrap:wrap;gap:8px}"
            "figure{width:180px;margin:0}img,.miss{width:180px;height:140px;object-fit:contain;background:#eee}"
            ".miss{display:flex;align-items:center;justify-content:center;color:#b00}</style>" + "".join(rows))
    with open(PREVIEW_FILE, "w", encoding="utf-8") as f:
        f.write(page)
    missing = sum(1 for m in catalogs for i in m["items"]
                  if not overrides.get(i["id"]) and (cache.get(i["id"]) or {"missing": True}).get("missing"))
    print(f"preview: {PREVIEW_FILE} ({missing} without image)")


def ensure_structure(api, meta):
    types = {t["id"] for t in api.get_json("/master/shop-types")}
    if meta["type"] not in types:
        api.post_json("/admin/master/shop-types", {"id": meta["type"], "name": meta["type_name"], "active": True})
    api.post_json("/admin/master/categories",
                  {"id": meta["root"], "name": meta["root_name"], "shopTypeIds": [meta["type"]]})
    for key, name in meta["subs"].items():
        api.post_json("/admin/master/categories", {"id": f"{meta['root']}__{key}", "name": name, "parentId": meta["root"]})


def upload_image(api, item, pick):
    url = pick if isinstance(pick, str) else pick["url"]
    full, thumb = compress(with_retry(lambda: download(url)))
    return with_retry(lambda: api.upload(item["id"], full, thumb))["url"]


def product_payload(meta, item, image_url, pick):
    variants = [{
        "id": f"v_{item['id']}_{slug(label)}",
        "name": label,
        "sellMode": "PACK",
        "price": price,
        "mrp": price,
        "stockQty": 0.0,
        "barcode": None,
        "sku": "",
        "packSize": 0.0,
        "packUnit": "",
        "bundleCount": 0,
        "baseVariantId": "",
        "imageUrls": [image_url],
        "active": True,
    } for label, price in item["variants"]]
    credit = "" if isinstance(pick, str) else f"photo:{pick.get('source')} {pick.get('license')} {pick.get('credit')}".strip()
    payload = {
        "id": item["id"],
        "name": item["name"],
        "nameHi": item["hi"],
        "categoryId": meta["root"],
        "subcategoryId": f"{meta['root']}__{item['sub']}",
        "unit": item["unit"],
        "barcode": None,
        "salePrice": 0.0 if variants else item["price"],
        "cost": 0.0,
        "active": True,
        "imageUrls": [image_url],
        "imageUrl": image_url,
        "thumbnailUrl": image_url,
        "brandName": item["brand"],
        "companyName": item["brand"],
        "description": item["name"],
        "sku": "",
        "tags": ",".join(x for x in [meta["root_name"], meta["subs"][item["sub"]], item["brand"],
                                     f"shopType:{meta['type']}", "source:curated", credit] if x),
        "variants": variants,
    }
    if item["hi"]:
        payload["translations"] = {"hi": item["hi"]}
    return payload


def cmd_import(args):
    catalogs = load_catalogs(args.types)
    cache = load_json(IMAGE_CACHE)
    overrides = load_json(IMAGE_OVERRIDES)
    done = set()
    if os.path.exists(DONE_FILE):
        done = {line.strip() for line in open(DONE_FILE, encoding="utf-8") if line.strip()}
    def usable(p):
        return isinstance(p, str) or bool(p and not p.get("missing"))

    jobs, no_image, already = [], 0, 0
    for meta in catalogs:
        picks = {i["id"]: overrides.get(i["id"]) or cache.get(i["id"]) for i in meta["items"]}
        for item in meta["items"]:
            pick = picks[item["id"]]
            siblings = [picks[i["id"]] for i in meta["items"]
                        if i["id"] != item["id"] and i["sub"] == item["sub"] and usable(picks[i["id"]])]
            if not siblings:
                siblings = [p for k, p in picks.items() if k != item["id"] and usable(p)]
            if item["id"] in done:
                already += 1
            elif usable(pick) or siblings:
                jobs.append((meta, item, pick if usable(pick) else None, siblings[:3]))
            else:
                no_image += 1
    print(f"{len(jobs)} to import, {no_image} without any image, {already} already done")
    if not args.apply:
        print("dry run - pass --apply to write")
        return
    api = Api(args.api)
    for meta in catalogs:
        ensure_structure(api, meta)

    def run(job):
        meta, item, pick, siblings = job
        shared = False
        for candidate in ([pick] if pick else []) + siblings:
            try:
                image_url = upload_image(api, item, candidate)
                pick, shared = candidate, candidate is not job[2]
                break
            except Exception as e:
                print(f"  image failed {item['id']}: {e}", file=sys.stderr, flush=True)
        else:
            raise RuntimeError("no usable image")
        payload = product_payload(meta, item, image_url, pick)
        if shared:
            payload["tags"] += ",image:shared"
        with_retry(lambda: api.post_json("/admin/master/products", payload))
        return item["id"]

    ok = failed = 0
    with open(DONE_FILE, "a", encoding="utf-8") as done_f, ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(run, j): j[1]["id"] for j in jobs}
        for fut in as_completed(futures):
            try:
                done_f.write(fut.result() + "\n")
                done_f.flush()
                ok += 1
            except Exception as e:
                failed += 1
                print(f"  fail {futures[fut]}: {e}", file=sys.stderr, flush=True)
    print(f"imported {ok}, failed {failed}. Next: python3 scripts/translate_products.py apply")


GROCERY_SHOP_TYPES = ["GROCERY", "SUPERMARKET", "GENERAL_STORE"]


def cmd_assign_categories(args):
    """Unassigned root categories show under every shop type; pin them to grocery-style shops."""
    api = Api(args.api)
    roots = [c for c in api.get_json("/master/categories") if not c.get("parentId") and not c.get("shopTypeIds")]
    print(f"{len(roots)} unassigned root categories -> {GROCERY_SHOP_TYPES}")
    for c in roots:
        print(f"  {c['id']}: {c['name']}")
        if args.apply:
            api.post_json("/admin/master/categories",
                          {"id": c["id"], "name": c["name"], "shopTypeIds": GROCERY_SHOP_TYPES})
    if not args.apply:
        print("dry run - pass --apply to write")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("assign-categories")
    p.add_argument("--api", default=os.environ.get("PANSARI_API", "https://api.pansariwala.shop"))
    p.add_argument("--apply", action="store_true")
    for name in ("validate", "images", "import"):
        p = sub.add_parser(name)
        p.add_argument("--types", default="", help="comma-separated shop type ids (default: all files)")
        if name == "images":
            p.add_argument("--refresh", action="store_true", help="search again for cached products")
            p.add_argument("--workers", type=int, default=8)
        if name == "import":
            p.add_argument("--api", default=os.environ.get("PANSARI_API", "https://api.pansariwala.shop"))
            p.add_argument("--workers", type=int, default=4)
            p.add_argument("--apply", action="store_true", help="write to the server (default: dry run)")
    args = ap.parse_args()
    args.types = {t.strip().upper() for t in getattr(args, "types", "").split(",") if t.strip()}
    {"validate": cmd_validate, "images": cmd_images, "import": cmd_import,
     "assign-categories": cmd_assign_categories}[args.cmd](args)


if __name__ == "__main__":
    main()
