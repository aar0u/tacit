<!--
SPDX-FileCopyrightText: 2026 Tacit contributors

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Privacy policy

Tacit is an open-source Android Pinyin input method. It does not collect, store off the device, or transmit anything you type.

## What the app can access

- **Network:** none. The app does not declare the `INTERNET` permission, so Android blocks all network traffic from it.
- **Storage:** the app's own data directory, for your user dictionary and settings.
- **Microphone:** requested the first time you press the keyboard's voice key. Recognition runs on the device. Audio is not saved, and with no `INTERNET` permission it cannot be sent anywhere. The microphone is only open between pressing the key to start and pressing it again to stop (or leaving the text field).

## Backups

`allowBackup` is off, so Android does not copy the app's data into cloud or device backups.

## Changes

If a release adds a permission, this file and the README list it before the release.

## 隐私政策

Tacit 是开源的安卓拼音输入法，不会收集、上传你输入的任何内容。应用没有声明 `INTERNET` 权限，系统会阻止它的一切网络访问；第一次按语音键时申请麦克风权限，识别在本机完成，音频不会被保存，也因为没有联网权限而无法发送；只有按下语音键开始到再次按下（或离开输入框）之间麦克风才是打开的；`allowBackup` 已关闭。
