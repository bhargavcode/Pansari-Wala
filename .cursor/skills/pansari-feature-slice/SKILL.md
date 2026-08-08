---
name: pansari-feature-slice
description: >-
  Adds a Pansari Wala feature screen end-to-end with minimal context (route,
  ViewModel, screen, Koin). Use when adding a new dashboard card, screen, or Nav3 route.
---

# Feature slice (minimal)

## Checklist
1. Domain model / repo method (only if data missing)
2. ViewModel in `feature/<name>/`
3. Screen composable
4. `AppRoute` + serializer in `AppNavGraph`
5. `viewModelOf` in `AppModule`
6. Wire navigation from existing screen

## Token rules
- Copy patterns from the closest existing feature (dashboard/order/inventory).
- Do not invent new DI/nav frameworks.
- Skip analytics/crash wiring unless the screen already uses them nearby.
- Stop after compile-green; no drive-by refactors.

## Verify
`./gradlew :androidApp:assembleDebug`
