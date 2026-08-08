---
name: pansari-token-budget
description: >-
  Enforces low-token workflows for Pansari Wala (read less, edit less, reply short).
  Use when the user mentions tokens, context size, cheaper runs, or "optimize tokens".
---

# Pansari token budget

## Workflow
1. Restate the task in one line; list max 3 files you will touch.
2. Open only those files (sliced). Skip project exploration.
3. Patch minimally. One concern per change set.
4. Verify with the smallest Gradle target that proves the change.
5. Reply: what changed + how to test (≤6 lines).

## File budget
| Task | Max files to read |
|------|-------------------|
| UI copy / bug in one screen | 2 |
| ViewModel + screen | 3 |
| expect/actual platform fix | 3 (common + 2 actuals) |
| New feature slice | 5, then stop and ask if more needed |

## Never load into context
- `**/build/**`, `.gradle/`, binary fonts, Room schema JSON dumps unless migrating
- Full `SeedData.kt` unless seed aliases/products change
- Entire `OrderEditorScreen.kt` when only ViewModel logic changes

## Response template
```
Done: <one sentence>
Changed: <paths>
Test: <one command or tap path>
```
