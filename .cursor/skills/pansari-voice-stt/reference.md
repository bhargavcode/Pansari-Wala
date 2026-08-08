# Voice STT reference

## Key types
- `SpeechToText` / `SpeechEvent` — common bridge
- `VoiceIntentParser` — qty/unit/query extraction
- `ProductFuzzyMatcher` — catalog match + unit conversion
- `RequestMicrophonePermission` — expect/actual Compose permission gate

## Android pitfalls
- `ERROR_NO_MATCH` after good partials → commit `lastPartial`
- `ERROR_RECOGNIZER_BUSY` → destroy/recreate recognizer, delayed restart
- Do not emit `Started` every restart cycle

## iOS pitfalls
- Sync `startEngine()` inside recognition callback → `kAFAssistantErrorDomain` 216
- Cancel sets intentional stop before `task.cancel()` so error is not shown
- Prefer on-device recognition when available
- Invalid audio format (0 channels) → delay restart

## UI
- Mic trailing icon on order search field
- Listening pulse + Cancel listening while active
