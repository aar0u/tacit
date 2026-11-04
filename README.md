<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Tacit

A private Pinyin input method for Android, forked from [Trime](https://github.com/osfans/trime) and built on the [RIME](https://rime.im) engine.

English | [简体中文](README_sc.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Goals

- Pinyin only. No Wubi or other shape-based schemes.
- No network access. The app declares no `INTERNET` permission, so Android blocks all network traffic from it, including from native libraries.
- No telemetry, no cloud features, no backup of app data (`allowBackup=false`).
- Accurate offline voice input.

## Status

| Item | State |
|---|---|
| Rebrand (`com.aar0u.tacit`, app name Tacit) | Done |
| Unused permission removed (`RECEIVE_BOOT_COMPLETED`) | Done |
| CI build of a debug APK | Done |
| [rime-ice](https://github.com/iDvel/rime-ice) dictionary and grammar model | Planned |
| Offline voice input (sherpa-onnx, whole-sentence recognition with VAD) | Planned |

## Download

Debug builds are published by CI as the [`ci-latest` pre-release](../../releases/tag/ci-latest). They are arm64-v8a only and signed with a debug key that changes between CI runs, so Android will not update one build over another. Uninstall the old build first.

## Build

Requirements: JDK 25, Android SDK with CMake 4.1.2 and the NDK version used by the build, Python 3.

```sh
git clone --recursive https://github.com/aar0u/tacit.git
cd tacit
make debug
```

Release signing reads `keystore.properties` (`storePassword`, `keyPassword`, `keyAlias`, `storeFile`); run `make release`.

## Staying in sync with Trime

```sh
script/sync-upstream.sh v3.3.14
```

The script fetches `upstream` (osfans/trime), enables the `ours` merge driver, and merges the given tag. `README*.md` and `.github/workflows/*` always keep this fork's version. GitHub's "Sync fork" button ignores merge drivers, so use the script instead of the button.

Expect small manual conflicts in `app/build.gradle.kts` (`applicationId`, version) and the app name strings.

## Credits and license

Tacit is a fork of [Trime](https://github.com/osfans/trime) by osfans and contributors, and is licensed under GPL-3.0-or-later like the original. It uses [librime](https://github.com/rime/librime) and other libraries listed in the upstream README.
