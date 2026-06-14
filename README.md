# Bible Analysis — Android Study Portal

A comprehensive, state-of-the-art Android application built with modern Kotlin and Jetpack Compose. This app is designed for deep theological study, featuring semantic search, cross-references, historical commentary integrations, offline reading modes, personal journal logging, tiered memberships, customizable text-to-speech voice cover, and multi-themes.

---

## 🎨 Design Concept: Cosmic Study & Serenity
The app leverages Material Design 3 (M3) with tailored visual schemes that prioritize night reading and eye safety:
*   **Cosmic Midnight Theme**: A deep space canvas paired with celestial blue accents for zero-strain reading.
*   **Warm Parchment Theme**: A vintage, high-comfort textured tone resembling ancient codices for daytime studying.
*   **Aesthetic Details**: Fluid screen transitions, crisp display headings, and dynamic color highlights mapped to individual historical source commentaries.

---

## 🛠 Features & Capabilities

### 1. Advanced Bible Reader Screen
*   **Offline Access**: Core biblical verses are preloaded and fully accessible without an active internet connection.
*   **Personalized Study Themes**: Cycle between **Cosmic Midnight**, **Warm Parchment**, and **Light Day** themes. Customize font sizes dynamically with a responsive slider (12sp to 28sp).
*   **Voice Cover (TTS)**: Features high-quality Text-to-Speech reading of scriptures with adjustable playback speed control (0.5x to 2.0x) and quick volume muting.
*   **Interactive Verses**: Tap any verse to select, toggle bookmarks, add cross-references, or jot down notes.

### 2. Semantic Search & AI Commentary
*   **Multi-Engine Search**: Switch between fast local database pattern-queries and deep semantic concept searches powered by **Gemini AI**.
*   **Concept Discovery**: Search for abstract mood strings, feelings, or concepts (e.g., *"strength in difficulty"*, *"creation of universe"*).
*   **AI Historical Commentaries**: Request real-time context-rich commentary of any selected verse by querying a simulated or live secure Gemini API endpoint.

### 3. Study Journal & Bookmarking
*   **Saved Passages**: Save favored passages for quick recall, complete with visual state badges.
*   **Active Journaling**: Log study notes alongside verses. Each notebook entry lists the target scripture reference.
*   **Database Cloud-Sync Simulation**: Mimics a robust mobile-to-web schema, showing synchronized green badges (`SYNCED`) or amber upload state changes (`PENDING_SYNC`).

### 4. Reading Plans & Trackers
*   **Themed Paths**: Subscribe to organized reading disciplines, such as "Gospels in 30 Days" or "Wisdom Literature".
*   **Progress Indicators**: View daily progression status, completion checkmarks, and percentage bars representing current study pathways.

### 5. Tiered Membership (Stripe Checkout)
*   **Premium Resources**: Secure paywalls guard advanced academic resources and commentary volumes.
*   **Stripe billing Integration**: Simulates standard subscription checkout steps (Free Standard tier vs. Gold Academic Membership) displaying animated checkout success screens.

---

## 🏗 Architectural Blueprint (MVVM Block Diagram)

```
                       [ Jetpack Compose UI Screens ]
                       (BibleApp.kt - Activity Content)
                                     ▼
                        [ BibleViewModel (State) ]
                                     ▼
                [ BibleRepository (Data Orchestration) ]
                    /                               \
                   ▼                                 ▼
   [ BibleDatabase (Room DB) ]          [ GeminiService (Retrofit REST) ]
  (Verses, Notes, Sync, Plans)                 (AI Semantic Search)
```

The codebase is built entirely on the industry-standard **Model-View-ViewModel (MVVM)** pattern, utilizing Kotlin Coroutines, reactive StateFlow triggers, and constructor repository injections:

### Core File Structure
*   `MainActivity.kt`: The main entry point, loading the Compose layout with injected `BibleViewModel`.
*   `ui/BibleApp.kt`: The master Compose file hosting layout scaffolding, bottom screens navigation, state renders, and dialogue prompts.
*   `viewmodel/BibleViewModel.kt`: Central state coordinator tracking current active verses, search lists, TTS playback levels, Stripe tier, and dialogue boxes.
*   `data/BibleDatabase.kt`: Formulates the Room database layer, mapping entity models for Verse, Bookmarks, Notes, CrossReference, and ReadingPlans under individual clean DAOs.
*   `data/BibleRepository.kt`: Brokering proxy between background Database calls, TTS engine triggers, and REST API network payloads. Includes automated database seeding of books on first startup.
*   `service/GeminiService.kt`: Retrofit communications client formatting Moshi schemas to converse with the Gemini REST API for semantic searches.

---

## 💾 Schema Definitions (Room Database Models)

```
        =================        =================        =================
        │    Verses     │        │   Bookmarks   │        │     Notes     │
        =================        =================        =================
        │ id: Long [PK] │        │ id: Long [PK] │        │ id: Long [PK] │
        │ book: String  │        │ book: String  │        │ content: Str  │
        │ chapter: Int  │        │ chapter: Int  │        │ bookSymbol:Str│
        │ verseNum: Int │        │ verseNum: Int │        │ chapterNum:Int│
        │ text: String  │        │ syncStatus:Str│        │ verseNum: Int │
        │ isPremium:Bool│        =================        │ syncStatus:Str│
        =================                                 =================
```

---

## 🛠 Setup & Run Guide

### Prerequisite: Set Gemini API Key
The application reads live search responses from Gemini AI. Ensure you set your API Key securely in AI Studio's **Secrets Panel**:
1.  Open the Secrets panel in Google AI Studio.
2.  Add a secret named `GEMINI_API_KEY`.
3.  Its value will be automatically injected into your compilation environment.

Alternatively, review `/app/build.gradle.kts` which utilizes the **Secrets Gradle Plugin** to read variables from your local environment or custom `.env` during builds.

### ⚙️ Compilation Steps
To build the Android application package (APK):
```bash
# 1. Run Kotlin Code Compilation
gradle compileDebugKotlin

# 2. Run Local Unit and Robolectric JVM Checks
gradle :app:testDebugUnitTest

# 3. Assemble Debug APK
gradle assembleDebug
```

---

## 🧪 Automated Testing
Verify application stability utilizing the pre-integrated JVM tests:
*   **Robolectric Tests** (`ExampleRobolectricTest.kt`): Validates application properties, context setups, database seeding, and Resource translations safely in memory.
*   **Roborazzi Visual Regression** (`GreetingScreenshotTest.kt`): Runs screenshot UI-assertions on custom text containers, caching images inside `src/test/screenshots/` to confirm layout integrity across modifications.

To verify visual screenshots:
```bash
gradle :app:verifyRoborazziDebug
```

To record new reference screenshots after editing typography and margin layouts:
```bash
gradle :app:recordRoborazziDebug
```

---

## 🛸 Contributing
We welcome contributions to expand translations, enrich commentaries, and streamline reading plans!
1.  **Fork** this repository.
2.  Create your feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your edits with clear logs (`git commit -m 'Add support for multi-language TTS speed levels'`).
4.  Run local verification checks (`gradle :app:testDebugUnitTest`).
5.  Submit a **Pull Request**.

Enjoy your theological journey! 🕊
