# CI/CD

GitHub Actions workflow: [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml)

## Job toggles

Edit [`.github/cicd.flags`](../.github/cicd.flags) then push (or re-run the workflow):

```
android_enabled = true
ios_enabled = false
web_enabled = true
server_enabled = true
```

`true` / `1` / `yes` runs that job. Anything else skips it (and its deploy). Keep `ios_enabled = false` until iOS CI is implemented.

## Trigger

- Push / merge to **`main`** (also accepts `master`)
- Manual **workflow_dispatch** (optional: deploy server, try iOS placeholder)

## What runs today

| Job | Output |
|---|---|
| **android** | 6 APKs: pos/user/delivery × debug/release → artifact `android-apks` |
| **web** | JS browser dist → artifact `web-js-dist` |
| **server** | Fat JAR `pansari-server-all.jar` → artifact `pansari-server-jar` |
| **deploy-server** | SCP JAR to EC2 + `systemctl restart` (skips if EC2 secrets missing) |
| **deploy-web** | SCP JS dist to EC2 web root + `nginx reload` (skips if EC2 secrets missing) |
| **ios** | Placeholder only — runs only when dispatch `build_ios=true` (exits until implemented) |

## Your environment (Pansari)

| Setting | Value |
|---|---|
| EC2 host | `52.1.25.27` (Elastic IP) |
| EC2 user | `ec2-user` |
| Deploy path | `/opt/pansari` |
| Service | `pansari-server` |
| API (release APKs) | `https://api.pansariwala.shop` |
| Web (marketing + admin) | `https://pansariwala.shop` |

Bootstrap on the instance (Amazon Linux):

```bash
sudo bash scripts/bootstrap-ec2.sh ec2-user
sudo nano /opt/pansari/env
sudo systemctl start pansari-server
```

## One-time GitHub secrets

Repo → **Settings → Secrets and variables → Actions**  
(`https://github.com/bhargavcode/Pansari-Wala/settings/secrets/actions`)

### Android (optional until you need signed release)

| Secret | Value |
|---|---|
| `API_BASE_URL` | `https://api.pansariwala.shop` (**release** BuildConfig) |
| `ANDROID_KEYSTORE_BASE64` | `base64 -i your.keystore \| pbcopy` (macOS) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Without keystore secrets, **release** builds are unsigned locally; CI requires `ANDROID_KEYSTORE_BASE64`.

### Signing behavior

| Build type | Keystore | API URL |
|---|---|---|
| **debug** | Android default debug keystore | Same as release (`API_BASE_URL`) |
| **release** | `pansariwala.jks` (production) | Same as debug (`API_BASE_URL`) |

CI keeps the **latest 5** artifact sets per type (`android-apks-*`, `web-js-dist-*`, `pansari-server-jar-*`); older runs are deleted automatically.

### Download builds after a successful run

1. Open [GitHub Actions](https://github.com/bhargavcode/Pansari-Wala/actions/workflows/ci-cd.yml)
2. Click the green **CI/CD** run for your commit
3. Scroll to **Artifacts** at the bottom of the run page
4. Download:
   - `android-apks-<run_id>` — all 6 APKs (pos/user/delivery × debug/release)
   - `web-js-dist-<run_id>` — web JS bundle
   - `pansari-server-jar-<run_id>` — server fat JAR

Direct path pattern: **Actions → CI/CD run → Artifacts → Download zip**

### EC2 deploy (required for auto-upload)

| Secret | Value |
|---|---|
| `EC2_HOST` | `52.1.25.27` |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | Full private key PEM contents |
| `EC2_DEPLOY_PATH` | `/opt/pansari` |
| `EC2_SERVICE_NAME` | `pansari-server` (optional; this is the default) |
| `EC2_WEB_ROOT` | `/var/www/pansariwala` (optional; web static root) |

## One-time EC2 setup

1. Install **Java 17** (Temurin/Corretto).
2. Copy repo scripts to the instance (or clone), then:

```bash
sudo bash scripts/bootstrap-ec2.sh ec2-user
sudo nano /opt/pansari/env                  # fill JWT/Mongo/Razorpay
```

This installs `scripts/pansari-server.service`, creates `/opt/pansari/env`, and allows passwordless `systemctl restart` for the deploy user.

3. Open security group for **8080** (or terminate TLS on nginx/ALB on 443).
4. After the first CI deploy (or manual `scp` of the JAR): `sudo systemctl start pansari-server`

## Local commands (same as CI)

```bash
# Android
./gradlew :androidApp:assemblePosDebug :androidApp:assemblePosRelease \
  :androidApp:assembleUserDebug :androidApp:assembleUserRelease \
  :androidApp:assembleDeliveryDebug :androidApp:assembleDeliveryRelease

# Web
./gradlew :webApp:jsBrowserDistribution

# Server fat JAR
./gradlew :server:shadowJar
# → server/build/libs/pansari-server-all.jar

# Manual deploy
EC2_HOST=... EC2_USER=... EC2_SSH_KEY_PATH=~/.ssh/key.pem \
  EC2_DEPLOY_PATH=/opt/pansari ./scripts/deploy-server.sh
```

Release API URL locally:

```bash
API_BASE_URL=https://api.example.com ./gradlew :androidApp:assembleUserRelease
```

## iOS (later)

Not auto-built. When ready:

1. Three Xcode schemes with `PansariProduct` = `POS` / `USER` / `DELIVERY`
2. Signing secrets / Fastlane Match
3. Replace the `ios` job placeholder in `ci-cd.yml` with `xcodebuild` archive + IPA upload

## Notes

- Default branch is **`main`** (workflow also listens to `master`).
- Deploy uses atomic upload (`*.jar.tmp` then `mv`) then restart.
- Web dist auto-deploys to EC2 when `EC2_*` secrets are set. See [AWS-HOSTING.md](./AWS-HOSTING.md) for nginx + domain setup.
- Full secret list: see § One-time GitHub secrets above.
