<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Tacit

注重隐私的安卓拼音输入法，从 [Trime（同文）](https://github.com/osfans/trime) 分叉而来，基于 [RIME](https://rime.im) 引擎。

[English](README.md) | 简体中文

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## 目标

- 只做拼音，不含五笔等形码方案。
- 不联网。应用不声明 `INTERNET` 权限，由 Android 系统阻止它（包括原生库）的一切网络访问。
- 无统计上报，无云功能，不备份应用数据（`allowBackup=false`）。
- 准确的离线语音输入。

## 进度

| 项目 | 状态 |
|---|---|
| 改名（`com.aar0u.tacit`，应用名 Tacit） | 已完成 |
| 删除未使用的权限（`RECEIVE_BOOT_COMPLETED`） | 已完成 |
| CI 构建调试版 APK | 已完成 |
| 接入 [雾凇拼音](https://github.com/iDvel/rime-ice) 词库和语法模型 | 计划中 |
| 离线语音输入（sherpa-onnx，VAD 加整句识别） | 计划中 |

## 下载

调试版由 CI 发布在 [`ci-latest` 预发布](../../releases/tag/ci-latest)，只有 arm64-v8a。调试签名每次 CI 都不同，所以不能覆盖安装，请先卸载旧版。

## 构建

需要 JDK 25、带 CMake 4.1.2 和对应 NDK 的 Android SDK、Python 3。

```sh
git clone --recursive https://github.com/aar0u/tacit.git
cd tacit
make debug
```

正式版签名读取 `keystore.properties`（`storePassword`、`keyPassword`、`keyAlias`、`storeFile`），执行 `make release`。

## 与 Trime 上游同步

```sh
script/sync-upstream.sh v3.3.14
```

脚本会拉取 `upstream`（osfans/trime）、启用 `ours` 合并驱动，再合并指定的 tag。`README*.md` 和 `.github/workflows/*` 始终保留本项目的版本。GitHub 网页上的 "Sync fork" 按钮不支持合并驱动，请用脚本，不要用按钮。

`app/build.gradle.kts`（`applicationId`、版本号）和应用名字符串可能需要手动解决小冲突。

## 致谢与许可

Tacit 是 osfans 及贡献者的 [Trime](https://github.com/osfans/trime) 的分叉，与原项目一样使用 GPL-3.0-or-later 许可。使用 [librime](https://github.com/rime/librime) 等库，完整列表见上游 README。
