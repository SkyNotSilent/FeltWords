# FeltWords

**FeltWords** is an AI-powered English learning app for children aged 3-8. It turns photos of everyday objects into vocabulary cards, felt-style illustrations, and narrated four-page storybooks.

> The real world can be a child's first English textbook.

## Platform Status

| Platform | Status |
| --- | --- |
| Android | Primary runnable version. Unit tests, Debug APK build, emulator installation, and core flows have been verified. |
| iOS | Source code included in the separate `ios/` directory. Simulator build passes, but the current version has not completed full runtime validation. |

## Core Flow

```text
Take a photo → Recognize an English word → Save it
→ Generate a felt-style storybook → Listen and review
```

## Highlights

- Multimodal photo-to-vocabulary recognition
- AI-generated felt-style illustrations
- Four-page child-friendly English stories
- Narration, autoplay, pause, replay, and page progression
- Local wordbook, recognition history, and story library
- Native Android and iOS implementations
- Child-friendly visual language and interaction design

## Repository

```text
android/   Kotlin + Jetpack Compose, primary validated version
ios/       Swift + SwiftUI, source and buildable project
docs/      Product, design, architecture, status, and screenshots
```

See the full [Chinese README](README.md), [architecture](docs/ARCHITECTURE.md), and [project status](docs/PROJECT_STATUS.md).

## Keywords

AI English learning, kids education, photo vocabulary, AI storybook, felt art, multimodal AI, Jetpack Compose, SwiftUI, CameraX, children's language learning.

## License

No open-source license has been declared yet. All rights are reserved unless explicitly granted.
