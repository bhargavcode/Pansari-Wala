#!/usr/bin/env python3
"""Delete every master product, every shop product and all product images in S3. No backup.

Run on the EC2 API host (reads /opt/pansari/env; S3 via the instance role or env keys):
  python3 -m venv /tmp/purge && /tmp/purge/bin/pip install -q pymongo boto3
  /tmp/purge/bin/python purge_products.py            # dry run: counts only
  /tmp/purge/bin/python purge_products.py --apply    # delete

Removes: master_products, products, price_history, PRODUCT-scope offers,
S3 objects under master/product-images/ and shops/product-images/.
Orders keep their line items (order history is untouched).
"""
import argparse
import os
import urllib.parse

import boto3
from pymongo import MongoClient

ENV_FILE = "/opt/pansari/env"
ATLAS_USER = "pansariwala"
ATLAS_HOST = "pansariwala.nl9gm4j.mongodb.net"
ATLAS_APP = "pansariwala"
S3_PREFIXES = ["master/product-images/", "shops/product-images/"]


def load_env(path):
    env = dict(os.environ)
    if os.path.isfile(path):
        for line in open(path, encoding="utf-8"):
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                env.setdefault(key.strip(), value.strip().strip('"').strip("'"))
    return env


def mongo_uri(env):
    if env.get("MONGODB_URI"):
        return env["MONGODB_URI"]
    password = env.get("MONGODB_PASSWORD") or exit("Set MONGODB_URI or MONGODB_PASSWORD")
    return f"mongodb+srv://{ATLAS_USER}:{urllib.parse.quote(password, safe='')}@{ATLAS_HOST}/?appName={ATLAS_APP}"


def purge_mongo(env, apply):
    db = MongoClient(mongo_uri(env))[env.get("MONGODB_DB", "pansariwala")]
    targets = [
        ("master_products", {}),
        ("products", {}),
        ("price_history", {}),
        ("offers", {"scope": "PRODUCT"}),
    ]
    for name, query in targets:
        count = db[name].count_documents(query)
        if apply:
            count = db[name].delete_many(query).deleted_count
        print(f"mongo {name}: {'deleted' if apply else 'would delete'} {count}")


def purge_s3(env, apply):
    for key in ("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY"):
        if not env.get(key):
            env.pop(key, None)
    s3 = boto3.client(
        "s3",
        region_name=env.get("AWS_REGION", "ap-south-1"),
        aws_access_key_id=env.get("AWS_ACCESS_KEY_ID"),
        aws_secret_access_key=env.get("AWS_SECRET_ACCESS_KEY"),
    )
    bucket = env.get("S3_BUCKET", "pansariwala-assets")
    for prefix in S3_PREFIXES:
        total = 0
        for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket, Prefix=prefix):
            keys = [{"Key": obj["Key"]} for obj in page.get("Contents", [])]
            if not keys:
                continue
            total += len(keys)
            if apply:
                result = s3.delete_objects(Bucket=bucket, Delete={"Objects": keys, "Quiet": True})
                for err in result.get("Errors", [])[:5]:
                    print(f"  s3 error {err.get('Key')}: {err.get('Code')} {err.get('Message')}")
        print(f"s3 s3://{bucket}/{prefix}: {'deleted' if apply else 'would delete'} {total} objects")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apply", action="store_true", help="delete (default: dry run)")
    ap.add_argument("--env-file", default=ENV_FILE)
    args = ap.parse_args()
    env = load_env(args.env_file)
    purge_mongo(env, args.apply)
    purge_s3(env, args.apply)


if __name__ == "__main__":
    main()
