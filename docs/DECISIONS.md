# Architecture Decision Records

## ADR-001: Standalone product
**Status:** Accepted

PolyaTV is developed in its own repository. CleanMyTV2 is not the application base. Any reuse requires separate license, compatibility and security review.

## ADR-002: Arc Launcher as application base
**Status:** Accepted; applied in `work/android-foundation`

Use [Arc Launcher](https://github.com/meddouribadis/arclauncher/) as the Flutter/Dart base, preserving its upstream history and GPL-3.0 license. Its LTvLauncher and FLauncher lineage is recorded in README and the upstream license/header files remain intact. Keep new integrations in separate modules to ease upstream updates. This replaces the earlier Kotlin/Compose-from-scratch direction.

## ADR-003: Do not auto-start playback from PolyaTV
**Status:** Accepted

Selecting a movie or series must not silently start playback or show a PolyaTV details screen. The target is the native TorrServe MatriX file/episode selection UI. If MatriX cannot expose that flow safely for a selected item, do not label `action=play` as a chooser: source code shows that a single playable file can start immediately. This requirement remains open for implementation pending a verified external contract or an explicit product-level alternative.

## ADR-004: Independent source adapters
**Status:** Accepted

TorrServe, Lampa and NUM are separate sources. Normalize data above their adapters. A source-specific ID or UI route is not assumed to be a TorrServe hash or launch reference.

## ADR-005: Windows Companion is separate
**Status:** Proposed

Windows Companion has its own lifecycle and release process. It starts only after the stable storefront milestones and explicit agreement.

## ADR-006: Evidence before integration claims
**Status:** Accepted

Mark integration facts as verified only when backed by primary documentation/source or a reproducible request/app test. Label untested device behavior as a hypothesis. Do not infer a NUM API from Android TV capabilities or a TorrServe chooser from an Activity being exported. When the needed data or launch contract is unavailable, document the limitation and an honest fallback before implementation.

## ADR-007: Stage 0 completion is based on source audit, not device validation
**Status:** Accepted

The Stage 0 research gate is satisfied by documenting, for TorrServe, Lampa, and NUM, either a substantiated external data/launch path or an explicit unverified/unavailable result with a practical alternative. This does not mean the installed apps, server endpoint, history, or launch behavior were tested on Philips. Those device-specific checks remain prerequisites for claiming an integration works. Do not begin a storefront integration on the assumption that an unverified MatriX chooser or NUM interface exists.

## ADR-008: Arc Watch Next is the first PolyaTV storefront surface
**Status:** Accepted

Build on Arc's existing Watch Next presentation and D-pad focus model. Treat it as PolyaTV's primary first storefront row while keeping its current Android TV `TvProvider` ingestion as one source. Add TorrServe, Lampa and NUM through isolated adapters only after validating each source's listing data and launch behavior on the target device. Exclude YouTube TV and Kinopoisk from the row on first run because the product owner explicitly does not want them; keep them available through the existing app filter. This does not alter what those apps publish to Android TV's system-managed Watch Next channel.
