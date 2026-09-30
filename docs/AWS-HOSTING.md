# AWS hosting — pansariwala.shop

Production layout on a **single EC2** instance:

| Host | Purpose |
|---|---|
| `https://pansariwala.shop` | Marketing SPA + `/app` user web + `/master` admin |
| `https://api.pansariwala.shop` | Ktor API (port 8080) + WebSockets |

```text
Internet
   │
   ▼
DNS (Hostinger parking NS) → A records → Elastic IP 52.1.25.27
   │
   ▼
EC2 + nginx (443)
   ├── pansariwala.shop     → /var/www/pansariwala (static JS SPA)
   └── api.pansariwala.shop → proxy → localhost:8080 (pansari-server)
```

## DNS (keep current nameservers)

Nameservers (do **not** move to Route 53 unless you want to):

- `horizon.dns-parking.com`
- `orbit.dns-parking.com`

In the Hostinger (or registrar) DNS panel, create:

| Record | Type | Value |
|---|---|---|
| `@` / `pansariwala.shop` | A | `52.1.25.27` |
| `www` | A | `52.1.25.27` |
| `api` | A | `52.1.25.27` |

Elastic IP **`52.1.25.27`** must stay attached to the EC2 instance.

## EC2 security group

| Port | Source | Use |
|---|---|---|
| 22 | `0.0.0.0/0` (or GitHub Actions IP ranges) | SSH for CI deploy — **your home IP is not enough**; GitHub runners will time out |
| 80 | 0.0.0.0/0 | HTTP (certbot + redirect) |
| 443 | 0.0.0.0/0 | HTTPS |
| 8080 | — | Close from public once nginx is live |

## Bootstrap API

```bash
sudo bash scripts/bootstrap-ec2.sh ec2-user
sudo nano /opt/pansari/env
```

Required in `/opt/pansari/env`:

```bash
ADMIN_USERNAME=bhargav
ADMIN_PASSWORD=<set-on-server-only>
S3_BUCKET=pansariwala-assets
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
MONGODB_PASSWORD=...
JWT_SECRET=...
AUTH_DEV_MODE=false
```

```bash
sudo systemctl start pansari-server
```

On start, the server creates/rotates the master admin from `ADMIN_*` and removes the legacy `admin` user.

## Bootstrap web + nginx

```bash
sudo bash scripts/bootstrap-ec2-web.sh pansariwala.shop ec2-user /var/www/pansariwala
```

CI stages files in `/tmp` (the SSH user cannot write `/var/www`), then `sudo /usr/local/bin/pansari-publish-web` copies them into the nginx root.

If nginx is already installed and you only need CI deploy permission:

```bash
sudo install -m 0755 scripts/pansari-publish-web.sh /usr/local/bin/pansari-publish-web
echo 'ec2-user ALL=(root) NOPASSWD: /usr/local/bin/pansari-publish-web' | sudo tee /etc/sudoers.d/pansari-web
sudo chmod 440 /etc/sudoers.d/pansari-web
sudo mkdir -p /var/www/pansariwala
```

After DNS propagates:

```bash
sudo certbot --nginx -d pansariwala.shop -d www.pansariwala.shop -d api.pansariwala.shop
```

## S3 — `pansariwala-assets` (ap-south-1)

All uploads go to object storage; there is no local-disk fallback. The backend is chosen by `STORAGE_PROVIDER` (currently `s3`). Mongo stores **object keys** (`partners/user-image/<id>.jpg`), and the API turns them into public URLs in every response, so switching backend or adding a CDN only needs a config change plus copying objects.

### One-time setup

```bash
# From a machine with AWS CLI admin access:
export AWS_REGION=ap-south-1
export S3_BUCKET=pansariwala-assets
bash scripts/setup-s3-bucket.sh
```

Creates the bucket, public `GetObject` policy, and CORS.

### API host env (`/opt/pansari/env`)

```bash
STORAGE_PROVIDER=s3
S3_BUCKET=pansariwala-assets
AWS_REGION=ap-south-1
# Credentials: prefer the EC2 instance role (pansariwala-s3-role). Static keys are optional:
# AWS_ACCESS_KEY_ID=AKIA...
# AWS_SECRET_ACCESS_KEY=...
# optional CDN in front of the bucket:
# ASSET_PUBLIC_BASE_URL=https://cdn.pansariwala.shop
```

Then `sudo systemctl restart pansari-server`. Logs should show `Asset storage: s3 bucket=... auth=instance-role`.
The server refuses to start if the backend cannot authenticate.

Public URLs look like:
`https://pansariwala-assets.s3.ap-south-1.amazonaws.com/partners/user-image/<id>.jpg`

On startup the server rewrites any stored asset URL from a known base (current bucket, CDN, or
`LEGACY_ASSET_BASE_URLS`, default `https://api.pansariwala.shop/uploads`) into a plain key. Inbound
URLs sent by clients are normalized the same way before saving.

### Adding another backend

1. Implement `AssetStorage` in `server/.../storage/` (`put`, `publicUrl`, `publicBaseUrls`).
2. Add an entry to `StorageProvider` and a branch in `AssetStorageFactory`.
3. Copy objects to the new backend with the same keys, add the old base to `LEGACY_ASSET_BASE_URLS`, set `STORAGE_PROVIDER`, and restart.

Prefix layout (folders inside one bucket):

```text
master/
  product-images/
  shop-images/
  shop-verification/
users/
  user-image/
  vehicle-image/
  user-ids/
partners/
  user-image/
  vehicle-image/
  user-ids/
shops/
  delivery-packets/
  shop/
  product-images/
```

Constants: `AppConstants.S3Prefix.*`

Upload API:
- Authenticated `POST /uploads?prefix=…` (multipart `file`)
- Guest (pre-login) `POST /uploads/guest?prefix=…` limited to `partners/*` and `users/*`
- Admin also has `POST /admin/uploads`
- Accepts PNG / JPEG / WebP only; server recompresses to JPEG and returns `{ url, thumbnailUrl, key, thumbnailKey }`

IAM (EC2 role `pansariwala-s3-role`): `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on `pansariwala-assets/*`, `s3:ListBucket` on the bucket.  
Public read is via **bucket policy** (not object ACLs).

## CI/CD

| Secret | Example |
|---|---|
| `EC2_HOST` | `52.1.25.27` |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | PEM private key |
| `EC2_DEPLOY_PATH` | `/opt/pansari` |
| `EC2_WEB_ROOT` | `/var/www/pansariwala` |
| `API_BASE_URL` | `https://api.pansariwala.shop` |

Push to `main` deploys server JAR + web dist.

## Web routes

| URL | Screen |
|---|---|
| `/` | Landing — brand, User + Delivery, store CTAs, login/signup |
| `/app` | User web app |
| `/master` | Master admin |

## Branding

- Logo: `shared/.../drawable/pansariwala_logo.png`
- Tagline: **Apka apna market**
- Colors: existing `PansariTheme`
- Store links: `AppConstants.PLAY_STORE_*` / `APP_STORE_*` (App Store IDs are placeholders until listing goes live)

See also [CICD.md](./CICD.md).
