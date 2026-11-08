# Architecture Decision Records

## ADR-001: Standalone product
**Status:** Accepted

PolyaTV is developed in its own repository and does not use CleanMyTV2 as its application base. CleanMyTV2 may be inspected for reusable technical knowledge/components only after license, compatibility and security review.

## ADR-002: Android TV UI
**Status:** Proposed

Use Kotlin and Jetpack Compose for TV. Keep source adapters and data access outside composable UI.

## ADR-003: Preserve standard TorrServe chooser
**Status:** Accepted

Selecting movie/series content must open the standard TorrServe MatriX chooser. Do not auto-start playback or interpose a custom details screen.

## ADR-004: Independent source adapters
**Status:** Accepted

TorrServe, Lampa and NUM are independent integrations. Unified content is normalized above the adapters.

## ADR-005: Windows Companion is separate
**Status:** Proposed

Windows Companion has its own lifecycle and release process. It may reuse/refactor independently verified CleanMyTV desktop tooling, but must be localized and adapted to PolyaTV contracts.
