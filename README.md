# Kids Game: Seekho JR — Kotlin Multiplatform

One Kotlin codebase, one shared Compose UI, five targets:

| Target  | Module / entry point | Run |
|---------|---------------------|-----|
| Android | `composeApp` (`androidMain/MainActivity`) | `./gradlew :composeApp:installDebug` |
| iOS     | `iosApp` (SwiftUI shell → `MainViewController`) | `brew install xcodegen && cd iosApp && xcodegen && open iosApp.xcodeproj` (macOS only) |
| Desktop | `composeApp` (`desktopMain/Main.kt`) | `./gradlew :composeApp:run` · package: `:composeApp:packageDmg` / `packageMsi` / `packageDeb` |
| Web     | `composeApp` (Kotlin/Wasm, `wasmJsMain`) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` · prod: `:composeApp:wasmJsBrowserDistribution` |
| Server  | `server` (Ktor/Netty) | `./gradlew :server:run` (port 8080, env `PORT`) |

```
shared/       models + API contract (Android, iOS, JVM, Wasm) — used by the apps AND the server
composeApp/   all UI + logic (commonMain) + thin per-platform code (androidMain, iosMain, desktopMain, wasmJsMain)
server/       Ktor backend: /api/v1/{learning,videos,life-skills}, legacy /123456/get-video, /kidsplay/appversion.php
iosApp/       Xcode shell (generated with XcodeGen)
tools/        extract_legacy_content.py (how the content JSON was derived from the old app), gen_drawable_map.py
```

## What was ported

| Old screen / feature | Now |
|---|---|
| Home dashboard (sections, tiles, "View All", bottom nav, reel FAB, name prompt) | `screens/MainScreen.kt` |
| 22 learning categories × 540 flash cards, swipe pager, speak-on-swipe, Hindi numbers | `LearnScreens.kt`, data in `files/learning.json` (was hard-coded in `SubActivity.java`) |
| Look & Choose, Listen & Guess | `QuizScreen.kt` |
| Learn Writing (1–20 dot-tracing) | `TraceScreens.kt` (digit shapes ported 1:1) |
| Match Pattern (letter → picture drag) | `MatchScreens.kt` |
| About Me & Life Skills (11 levels, stars, unlock order) | `LifeSkillsScreens.kt`, same JSON schema |
| Video songs (12 categories, 396 videos) | `VideoScreens.kt`, data in `files/videos.json` (was `preschool.db`) |
| Kids Reels feed | `ReelsScreen.kt` + `data/ReelApi.kt` (same endpoint) |
| Settings (name, sound, language, privacy, rate) | `SettingsScreen.kt`, same pref keys |
| Retrofit/Gson, SQLite copy-from-assets, SharedPreferences | Ktor client, JSON resources, multiplatform-settings |

All 599 drawables, both fonts and the launcher icons were carried over; the Android app keeps
`applicationId com.kidsplay.kidseducation` (versionCode 9) so it can ship as an update.

## NOT ported yet — decisions for you

- **Ads / Firebase / in-app update**: AdMob, Firebase Analytics/FCM, Play in-app update and the interstitial
  frequency cap are Android-only SDKs. The shared UI has an `AdService` slot (`platform/Platform.kt`, `NoAds`
  by default); implement it in `androidMain` (and iOS if wanted). The server still serves the ad-cap endpoint.
  *Kids apps have COPPA / Families-policy constraints on ads and analytics — review before re-adding.*
- **Embedded YouTube player**: videos open in YouTube instead. Per-platform embedding is a follow-up.
- **`kidsdrawing` module** (flood-fill colouring editor, ~10k lines of Android View/Bitmap code) and
  `indicatorseekbar`: replaced by a simple free-hand drawing pad (`DrawScreen.kt`). A colouring-book mode needs a
  Compose rewrite.
- Life-skills **speech input** (microphone) — typed answers only.
- `GameMenuActivity` / `GameActivity` / `ResultsActivity` ("Maths World") ARE reachable in the old app (home tile "Mathematics"); ported in `screens/MathGameScreens.kt` + `shared/MathGame.kt`.
- Reel inline playback exists on Android (Media3) and iOS (AVPlayer); desktop/web show an "open video" fallback.
- Web: Hindi glyphs need a bundled Devanagari font (Compose/Wasm has no system-font fallback).
- Old per-view animations (daimajia / calligraphy / XML anims) were not recreated.

## Important: this has not been compiled

The project was written and checked by reading, in a sandbox with no access to Maven, Gradle or the Android/Apple
toolchains, so **nobody has built it yet**. Expect a first round of small fixes (imports, API drift).
Versions are pinned in `gradle/libs.versions.toml` (Kotlin 2.1.21, Compose Multiplatform 1.8.2, AGP 8.9.3,
Ktor 3.1.3, Coil 3.2.0) — if Gradle sync complains, bump them together. Needs JDK 17+.
First thing to run: `./gradlew :server:build :composeApp:desktopTest :composeApp:run`, then Android, then Web, then iOS.

## Using your own backend

`AppConfig.API_BASE_URL` points at the legacy server. To use the bundled one, run `./gradlew :server:run` and set it to
`http://localhost:8080/` (Android emulator: `http://10.0.2.2:8080/`). Put reel mp4 URLs in
`server/src/main/resources/reels.json` (`[{"postId":"k1","id":37,"type":"category","language":"kids","image":"https://…mp4","video":true}]`).
To serve the web app from the same server: build the Wasm distribution and start with `WEB_DIST=composeApp/build/dist/wasmJs/productionExecutable`.
