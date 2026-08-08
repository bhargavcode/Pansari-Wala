---
name: pansari-voice-stt
description: >-
  Implements or fixes Hindi/Hinglish voice order STT for Pansari Wala (parser,
  fuzzy match, Android SpeechRecognizer, iOS SFSpeechRecognizer, order cart UI).
  Use when editing voice, mic, listening, cart-from-speech, or STT errors.
---

# Voice STT workflow

## Read first (in order)
1. `shared/src/commonMain/.../voice/VoiceIntentParser.kt`
2. `shared/src/commonMain/.../feature/order/OrderEditorViewModel.kt` (speech handlers only)
3. Platform file you are fixing:
   - Android: `.../androidMain/.../voice/SpeechToText.android.kt`
   - iOS: `.../iosMain/.../voice/SpeechToText.ios.kt`

## Invariants
- Cart updates on stable phrase; cancel not required.
- Clear `partialTranscript` after successful add.
- iOS: deferred restart only; swallow 203/209/216-style recoverable errors.
- Android: single `Started` per mic session; commit partial on NO_MATCH; soft-retry busy.
- Parser supports qty before/after product + `adha`/`आधा`.

## Verify
- Host tests if parser changed: `:shared:testAndroidHostTest --tests "*VoiceIntentParserTest*"`
- Else `:androidApp:assembleDebug` and/or `:shared:compileKotlinIosSimulatorArm64`

## Details
See [reference.md](reference.md) only if invariants are insufficient.
