---
name: pansari-room-db
description: >-
  Extends Pansari Wala Room KMP schema, DAOs, seed data, and ShopRepository.
  Use when adding entities, migrations, inventory/order persistence, or seed products.
---

# Room DB workflow

## Read first
1. `shared/src/nonWebMain/.../data/db/ShopDatabase.kt`
2. Relevant entity + DAO only
3. `RoomShopRepository.kt` mapping methods you will touch
4. `SeedData.kt` only if catalog/users change

## Rules
- Keep `ShopRepository` API stable; prefer additive methods.
- New products: add to `SeedData` and rely on missing-id upsert in `ensureSeeded`.
- Do not put Room deps in js/wasm source sets.
- After entity change: Android assemble + iOS shared compile.

## Verify
`./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`
