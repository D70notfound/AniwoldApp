# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt verwendet [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [1.2.0] - 2026-03-23

### Added
- Zwei-Tab-Navigation (Browser / Downloads) am unteren Bildschirmrand
- Download-Button (FAB) erscheint automatisch auf Episode-Seiten
- Einzelne Episode herunterladen: erkennt das aktive Video und startet den Download
- Staffel herunterladen: lädt alle Episoden einer Staffel automatisch sequenziell herunter
- Downloads-Tab: zeigt alle gestarteten und abgeschlossenen Downloads, tippen öffnet den Video-Player
- Video-Erkennung via JavaScript: findet `<video>`-Elemente und speichert die URL

## [1.1.0] - 2025-10-07

### Fixed
- URL-Blocksystem verbessert: zuverlässigere Erkennung und Blockierung von Werbe-URLs
- Vollbild-Probleme behoben: Video-Vollbildmodus funktioniert jetzt korrekt

## [1.0.0] - Initial Release

### Added
- WebView-Wrapper für aniworld.to
- Automatische Blockierung von Werbung und Popups
- Schutz vor unerwünschten Weiterleitungen
- Vollbildmodus-Unterstützung

[Unreleased]: https://github.com/D70notfound/AniwoldApp/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/D70notfound/AniwoldApp/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/D70notfound/AniwoldApp/releases/tag/v1.0.0
