# aila-android

## Overview
Aila is an on-device AI language learning mobile application for Android and iOS that simulates realistic phone calls with AI-powered contacts. The app provides immersive, adaptive language practice by combining dynamic conversation, spaced repetition vocabulary training, and contextual feedback—all without requiring internet connectivity.

Built entirely for offline use, Aila leverages lightweight machine learning models to deliver speech recognition, natural language understanding, and speech synthesis directly on the device. Users interact with personalized AI contacts that evolve over time, creating a natural and engaging language learning experience.

---

## Key Features

### 📞 Contact Management
- **Add Contacts**: Create new AI contacts by entering:
  - Name
  - Birthday (ISO 8601 format)
  - Personality description
  - Voice preference
  - Target language
- **Contact List**: View all saved contacts with options to:
  - Initiate a call
  - Edit contact details
  - Delete contact
- **Dynamic Life Simulation**: Each contact has a generated personal history (job, family, interests). On each call, the app generates a narrative of what the contact has been doing since the last interaction based on elapsed time.

### 📱 Call Interface
- **Calling Screen**:
  - Displays contact name
  - Speaker toggle
  - Hang-up button to end the call
- **Realistic Call Flow**:
  - Ringing phase includes AI-generated narrative of recent events
  - Conversation adapts to user proficiency in real time

### 🧠 On-Device AI Pipeline
The core conversation loop runs entirely on-device:
```
Audio Input → [ASR: Whisper Tiny] → Text → [Tutor: mT5-small] → Response → [TTS] → Audio Output
```
- **Adaptive Difficulty**: AI starts with simple phrases and gradually increases complexity based on user performance.
- **Native Language Fallback**:
  - After one misunderstanding: AI rephrases in target language
  - After two consecutive misunderstandings: AI responds in user’s native language (set in app settings)
- **In-Memory Context**: Maintains conversation history during active calls for coherent dialogue

### 🗂️ Vocabulary Learning System
- **Proficient Vocabulary**:
  - Words used correctly at least twice within 24 hours
  - Stored in `vocabulary_proficient` table
- **Struggling List**:
  - Words/phrases user has difficulty with
  - Classified by severity (1–3) based on retry count
  - Stored in `vocabulary_struggling` table
- **Spaced Repetition (SM-2 Algorithm)**:
  - Struggling words are reviewed 3x more frequently
  - Review schedule managed via WorkManager (Android) / OperationQueue (iOS)

### 🔐 Security & Privacy
- **Fully Offline**: No data leaves the device
- **Input Sanitization**:
  - Contact fields sanitized to prevent code injection
  - Text normalization applied before model input
- **Storage Protection**:
  - All data stored using platform-native encrypted databases (Room / CoreData)
  - Automatic escaping of special characters in user inputs

---

## Technical Architecture

### Platform & Stack
| Component | Android | iOS |
|--------|--------|-----|
| UI Framework | Jetpack Compose | SwiftUI |
| Database | Room | CoreData |
| ML Framework | TensorFlow Lite | Core ML |
| SRS Scheduler | WorkManager | OperationQueue |
| DI | Hilt / Dagger | Manual / Swift DI |

### Data Model
#### Contacts (`ContactEntity`)
| Field | Type | Description |
|------|------|-------------|
| `id` | UUID | Unique identifier |
| `name` | String | Contact's name |
| `birthday` | ISO8601 | Date of birth |
| `personality` | String | Background, job, interests |
| `voice` | String | Voice profile |
| `language` | String | Target learning language |
| `last_call_time` | ISO8601 | Timestamp of last interaction |

#### Vocabulary Tables
**`vocabulary_proficient`**
| Field | Type |
|------|------|
| word | String |
| language | String |
| last_practiced | ISO8601 |

**`vocabulary_struggling`**
| Field | Type |
|------|------|
| word | String |
| severity | Int (1–3) |
| retries_needed | Int |

### Model Specifications
All models are pre-bundled in app assets (no runtime download):
- **ASR**: Whisper Tiny (<30MB, multilingual, CPU-efficient)
- **Text Processing**: mT5-small (58MB, supports 100+ languages)
- **TTS**: Platform-native engine with on-device voices
- **Total Model Size**: <100MB

---

## Setup & Build

### Android Dependencies (`app/build.gradle`)
```gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'org.tensorflow:tensorflow-lite-task-text:0.4.5'
    testImplementation 'junit:junit:4.13.2'
}
```

### Build Commands
- **Android**:  
  ```bash
  ./gradlew assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`

- **iOS**:  
  ```bash
  xcodebuild -scheme Aila -configuration Debug
  ```

### Verification Steps
- Build exits with status `0`
- APK size < 50MB (`adb shell dumpsys package com.aila`)
- Model assets present in `res/assets/` (Android) or `Resources/` (iOS)
- Database integrity verified via `RoomDatabase.getOpenHelper().getReadableDatabase().isDatabaseIntegrityOk()`

---

## Module Structure

```
aila-android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/aila/
│   │   │   ├── data/ 
│   │   │   │   ├── entity/       # Room DB entities
│   │   │   │   └── dao/          # Data access objects
│   │   │   ├── domain/
│   │   │   │   ├── ContactManager.kt        # Contact CRUD + life narrative
│   │   │   │   └── VocabularyTracker.kt     # SRS logic
│   │   │   ├── presentation/
│   │   │   │   ├── ContactScreen.kt         # Contact list & modal
│   │   │   │   └── CallScreen.kt            # Call UI
│   │   │   └── speech/
│   │   │       ├── ConversationFlow.kt      # ASR → Tutor → TTS pipeline
│   │   │       ├── ASRProcessor.kt          # Whisper integration
│   │   │       └── TTSClient.kt             # Speech synthesis
│   │   └── res/
│   │       └── assets/                      # Bundled ML models
│   └── build.gradle
└── build.gradle
```

---

## Testing & Stubs

### AI Service Stub (for Development)
```kotlin
class AiServiceStub : ASRService, TutorService, TTSClient {
    override fun transcribe(audio: ByteArray) = "stub"
    override fun respond(context: String) = "stub response"
    override fun generateSpeech(text: String) = byteArrayOf(0x00)
}
```

### Dependency Injection (Hilt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideASR(): ASRService = if (BuildConfig.DEBUG)
        AiServiceStub()
    else
        TensorFlowASR()
}
```

### Verification Strategy
- Unit tests verify stub invocation using `mockkVerify`
- Manual inspection ensures correct binding per build variant
- Real models used only in release builds

---

## Completion Criteria

The project is considered **100% complete** when:

- [ ] All modules implemented with non-crashing stubs
- [ ] Successful build on both platforms (`exit 0`)
- [ ] Key user flows manually verified:
  - **Contact Flow**: Add → Edit → Delete (persists)
  - **Call Flow**: Start call → simulate 3 failed attempts → verify native language fallback
  - **Vocab Flow**: Use new word 3 times → confirm moved to `proficient` list
- [ ] Security edge cases pass:
  - `<script>alert()</script>` in contact field → stored as plain text
  - Invalid dates (e.g., Feb 29, 1900) rejected
- [ ] SM-2 algorithm correctly schedules reviews (unit tested)

---

## Why Aila Stands Out
Aila is the only language learning app that combines:
- Phone-call simulation for low-pressure speaking practice
- Fully offline operation with no data privacy concerns
- Spaced repetition integrated into contextual conversation
- Dynamic AI personas that evolve over time

This creates a uniquely immersive and personalized path to language fluency—right from your pocket.
