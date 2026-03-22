# AniwoldApp

Ein einfacher Android-WebView-Wrapper für aniworld.to mit integriertem Werbeblocker und Popup-Schutz.

## Features

- WebView-basierter Browser, optimiert für aniworld.to
- Automatische Blockierung von Werbung, Popups und Weiterleitungen
- Vollbildmodus-Unterstützung für Videos
- Minimale Berechtigungen

## Installation

### APK direkt installieren (empfohlen)

1. Die aktuelle APK aus dem [Releases-Bereich](https://github.com/D70notfound/AniwoldApp/releases) herunterladen
2. Auf dem Android-Gerät: Einstellungen → Sicherheit → "Unbekannte Quellen" aktivieren
3. APK installieren

### Aus dem Quellcode bauen

**Voraussetzungen:**
- Android Studio Hedgehog oder neuer
- JDK 17
- Android SDK (API 21+)

```bash
git clone https://github.com/D70notfound/AniwoldApp.git
cd AniwoldApp
./gradlew assembleRelease
```

Die APK liegt anschließend unter `app/build/outputs/apk/release/`.

## Anforderungen

- Android 5.0 (API 21) oder neuer
- Internetverbindung

## Tech-Stack

| Komponente | Version |
|------------|---------|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.0 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 (Android 14) |
| AndroidX WebKit | 1.11.0 |

## Changelog

Siehe [CHANGELOG.md](CHANGELOG.md) für eine vollständige Versionshistorie.

## Lizenz

Dieses Projekt steht unter keiner expliziten Lizenz. Nutzung auf eigene Verantwortung.
