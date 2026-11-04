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
| No `INTERNET` permission in the built APK; `RECORD_AUDIO` is the only sensitive permission, used by voice input | Verified on device |
| CI build of a debug APK | Done |
| One theme, Tacit (rounded keys, tinted function keys, simplified labels) with six CyIME palettes: lavender, blue, black, mint, sakura, cream; each has a dark variant (quick menu: Colors, Dark mode) | Done |
| [rime-ice](https://github.com/iDvel/rime-ice) Pinyin dictionary (no grammar model yet) | Done |
| Offline voice input in the keyboard (sherpa-onnx, X-ASR, Silero VAD) | Done |
| Key click vibration on by default | Done |
| Cursor and selection keyboard, clipboard, one-hand mode (long press the comma or question mark key on the symbols keyboard) | Done |
| A notice in the toolbar while Rime deploys the dictionary (a few minutes after the first install) | Done |

## Voice input

Voice runs inside the keyboard process, so the engine stays loaded so recording starts at once. Two ways to use it: hold the space bar and speak, and the text is typed after you let go and pause; or tap the microphone key (bottom row, lit while recording) to start and tap again to stop. The first start after the process is created loads the model, which takes a few seconds. The keyboard asks for the microphone permission the first time you press the key. It still has no `INTERNET` permission, so audio cannot leave the device. The engine is released when Android asks for memory back (about half a gigabyte while loaded).

Speech is cut into sentences by a VAD and each sentence is recognized in one pass. In a desktop comparison on 753 sentences of spoken Mandarin, X-ASR had a character error rate of 5.8%, 3.9% and 8.9% on three talks, against 6.9%, 4.9% and 11.1% for the streaming Zipformer model. On the test phone the whole pipeline ran at 0.19 times real time, and loading the model took about 11 seconds after pre-optimizing the encoder. The cost is that text appears after you pause, not while you speak.

The models (about 175 MB) are not in git. `script/fetch_voice_models.py` downloads them, checks SHA-256 hashes and pre-optimizes the encoder; CI runs it before building.

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
