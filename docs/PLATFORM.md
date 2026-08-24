# Pansari Wala platform

Four products share one Kotlin codebase and one Ktor backend.

| Product | Who | Android flavor | applicationId | iOS switch |
|---|---|---|---|---|
| PoS | Shop owner / cashier | `pos` | `org.bhargav.pansariwala` | `PansariProduct=POS` |
| User / Market | Customer | `user` | `org.bhargav.pansariwala.user` | `PansariProduct=USER` |
| Delivery partner | E-rickshaw rider | `delivery` | `org.bhargav.pansariwala.delivery` | `PansariProduct=DELIVERY` |
| Server | Platform | `:server` | n/a | n/a |

## Stacks

**Apps (KMP)**
- Kotlin Multiplatform + Compose Multiplatform + Material 3
- Nav3 typed routes (`AppRoute` / `UserRoute` / `DeliveryRoute`)
- Koin DI, DataStore session, Room (PoS local, Android/iOS)
- Ktor client (OkHttp / Darwin / JS)
- Notifications, voice STT (PoS only)

**Server**
- Ktor 3 / Netty / Kotlin serialization
- MongoDB Atlas (`mongodb-driver-kotlin-sync` 5.5) + Kotlinx BSON codecs
- JWT (HMAC256), OTP challenges, Razorpay order + signature verify
- WebSocket `/ws/delivery` for 30s partner ringing

**Payments**
- Razorpay **order is created on the server**
- Client only receives `key_id` + `order_id`
- `key_secret` never leaves the server
- Place-order is rejected unless HMAC-SHA256(`orderId|paymentId`) matches `razorpay_signature`
- Dev: `AUTH_DEV_MODE=true` and empty Razorpay keys allow `pay_dev` / signature `dev`

**Auth**
- PoS: shop username/password (`owner`/`1234`, `cashier`/`1234`) hashed with `PASSWORD_SALT`
- Customer: phone OTP (`AUTH_DEV_MODE` OTP is `123456`) then name + address
- Production phone auth: Firebase ID token → `POST /auth/user/firebase` (audience + issuer checked against `FIREBASE_PROJECT_ID`)
- Partner: register (name, email, address, phone, vehicle reg, two photos) → plate format check → OTP → JWT role `PARTNER`
- Admin: username from `ADMIN_USERNAME` (default `bhargav`) / password from `ADMIN_PASSWORD` env → `/auth/admin/login` (legacy `admin`/`admin123` removed on seed)

## How the marketplace works

1. Customer opens **User** flavor, verifies phone, saves name/address.
2. **Market** lists shops inside the radius (default 20 km), sorted by haversine distance. Search filters by shop name. Radius slider writes the same pref used in Settings.
3. Selecting a shop loads the same catalog the PoS stocks (synced via `/sync/*` and seeded master products).
4. **Checkout** quote:
   - items subtotal
   - shop discount %
   - platform fee **₹10**
   - delivery = **(₹8/km × distance) + 30% of that base**
5. Place order → Razorpay (or dev stub) → server verifies signature → order `RECEIVED` + 4-digit delivery OTP.
6. PoS **Online orders** can Accept, reject items, mark Packing, **Request delivery**.
7. Delivery request rings verified partners within ~8 km for **30 seconds** (WebSocket + poll `/partners/offers/incoming`). First `accept` wins (`ALREADY_TAKEN` otherwise). Late tray tap re-fetches status (`TAKEN_BY_OTHER` vs live offer).
8. Partner **Pickup**: two photos → status `ON_THE_WAY`. Cancel returns order to looking-for-partner.
9. Partner **Deliver**: OTP dialog; match marks `DELIVERED`. Customer sees stepped progress (Placed → Accepted → Packing → On the way → Delivered). Delivered orders can be rated 1–5 with save/update gated on the slider.

## Offline PoS sync

- Local Room remains source of truth for walk-in POS.
- `GET /sync/pull` returns shop catalog, online orders, master SKUs.
- `POST /sync/push` upserts products for that shop JWT.
- Master catalog: `/master/categories`, `/master/products`. Shops add from master; admin can activate/deactivate shops and payments (`/admin/shops/{id}`).

## Security details

| Secret | Where | Notes |
|---|---|---|
| `JWT_SECRET` | server env | HMAC for app JWTs. Change before any real deploy. |
| `PASSWORD_SALT` | server env | Shop/admin password hash = SHA-256(salt + password). Upgrade to bcrypt/argon2 before production. |
| `RAZORPAY_KEY_SECRET` | server env **only** | Used to create orders (Basic auth) and verify checkout signatures. |
| `RAZORPAY_KEY_ID` | server + public `/config/public` | Safe to expose to the User Android SDK. |
| `FIREBASE_PROJECT_ID` | server env | Token `aud`/`iss` must match. |
| `FCM_SERVER_KEY` | server env | For remote push (wire FCM HTTP v1 next). |
| OTP hashes | Mongo `otp_challenges` | SHA-256 of code, 5 min TTL, deleted after use. |
| Delivery OTP | order row | Shown to customer; partner must match. |
| Photos | truncated base64 on partner/order rows | Replace with object storage + signed URLs for production. |
| Cleartext HTTP | emulator `10.0.2.2:8080` | Debug only. Production must be HTTPS. |

**Payment rules**
- Never trust the client “payment success” flag.
- Amount on Razorpay order is created server-side from the quote.
- Idempotent place-order should be added (store `razorpayOrderId` unique) before scale.

**Delivery accept race**
- Synchronized lock per offer id + DB status `RINGING` + not expired.
- Other devices get `TAKEN_BY_OTHER` / HTTP 409.

## Run locally

```bash
# API (MongoDB Atlas cluster pansariwala)
AUTH_DEV_MODE=true MONGODB_PASSWORD='your-db-password' ./gradlew :server:run

# PoS
./gradlew :androidApp:assemblePosDebug

# Customer app
./gradlew :androidApp:assembleUserDebug

# Partner app
./gradlew :androidApp:assembleDeliveryDebug
```

Emulator API base URL is `http://10.0.2.2:8080` (BuildConfig). iOS simulator should use `http://localhost:8080` (set `ApiRuntime.baseUrl` or a scheme-specific plist later).

Dev customer OTP: **123456**. Seed shops: Bhargav Kirana (`shop_1`) near 28.6139, 77.2090.

## Environment

```
PORT=8080
JWT_SECRET=
JWT_ISSUER=pansariwala
MONGODB_PASSWORD=
# or full SRV URI (overrides user/host/password):
# MONGODB_URI=mongodb+srv://pansariwala:<db_password>@pansariwala.nl9gm4j.mongodb.net/?appName=pansariwala
MONGODB_DB=pansariwala
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
FIREBASE_PROJECT_ID=
FCM_SERVER_KEY=
AUTH_DEV_MODE=true
PASSWORD_SALT=
```

## API map (JWT unless noted)

- Public: `GET /health`, `GET /config/public`
- Auth: `/auth/shop/login`, `/auth/admin/login`, `/auth/user/firebase`, `/auth/otp/request|verify`, `/partners/register`
- Customer: `/shops`, `/shops/{id}/catalog|offers`, `/orders/quote`, `/payments/razorpay/order|verify`, `/orders`, `/orders/mine`, `/orders/{id}`, `/orders/{id}/rating`, `/me`, `/me/profile`, `/me/transactions`
- Shop: `/shop/orders`, `/shop/orders/{id}/accept|reject|status|delivery`, `/sync/pull|push`
- Partner: `/partners/dashboard`, `/partners/offers/incoming|{id}/accept|reject`, `/partners/jobs/*`, `WS /ws/delivery`
- Admin: `/admin/shops`, `/admin/shops/{id}`, `/master/categories|products`

## Production follow-ups

- Firebase Auth SDK on Android/iOS User flavor + `google-services.json` / GoogleService-Info.plist
- Razorpay Checkout in `androidApp/src/user` (dependency already on `userImplementation`)
- ML Kit / Vision OCR for number plates (format regex is enforced today)
- FCM data payloads so notification tap opens `OrderDetails` / `IncomingOffer`
- bcrypt, HTTPS, rate limits, signed image URLs, Razorpay webhooks
- Three Xcode schemes copying `PansariProduct` USER / DELIVERY / POS
