# Changelog

All notable changes to this library are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Releases are published via [JitPack](https://jitpack.io) from the git tags.

## [Unreleased]

## [1.1.1] - 2026-11-12

### Fixed
- Links with `force_typeid` for an external system (e.g. sags-doch.de) were
  always rejected as "not valid at this location", because the category was
  requested from the default server instead of the external system.

## [1.1.0] - 2026-11-03

### Added
- External categories now support `mailto:` links in addition to `http://` and
  `https://` URLs. Selecting such a category opens the user's mail app. Links
  with parameters such as `?subject=…` are supported, and unencoded characters
  in them are encoded automatically.
- The prompt for external categories shows a mail-specific text with the
  recipient address for `mailto:` links.

### Fixed
- Opening an external category no longer crashes when no app can handle the
  link. An error message with the address or URL is shown instead, and the app
  stays open.
- External category links are recognized case-insensitively and with
  surrounding whitespace.
- Outdated unit tests `CategoryTest.attributeIdsForUpdate` and
  `MessageDetailTest.parse` now match the current behavior and pass again.

## [1.0.3] - 2026-09-01

### Fixed
- The host app's custom font is now applied to Showcase tutorial overlays.
  The fix in 1.0.2 ran before the overlay's views existed and had no effect.

## [1.0.2] - 2026-09-01

### Fixed
- Apply the host app's custom font to tutorial showcase overlays, which run in
  their own third-party activity outside ViewPump/Calligraphy.

## [1.0.1] - 2026-08-27

### Fixed
- Apply the host app's custom font to buttons that are direct children of a
  `Toolbar` used as a plain container.

## [1.0.0] - 2026-06-24

Initial public release.

### Changed
- The library builds without a configured Google Maps API key, so it can be
  built on JitPack.

[Unreleased]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.0.3...v1.1.0
[1.0.3]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/wer-denkt-was/maengelmelder-android/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/wer-denkt-was/maengelmelder-android/releases/tag/v1.0.0
