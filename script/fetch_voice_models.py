#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Tacit contributors
#
# SPDX-License-Identifier: GPL-3.0-or-later
"""Download the speech models into voice/src/main/assets/models (not committed to git).

Run once before building the voice app: python3 script/fetch_voice_models.py
Files are checked against pinned SHA-256 hashes; the script needs network, the app does not.
"""
import hashlib
import io
import sys
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "voice" / "src" / "main" / "assets" / "models"

XASR_URL = (
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/"
    "sherpa-onnx-x-asr-zipformer-transducer-zh-en-punct-int8-2026-06-03.tar.bz2"
)
XASR_SHA256 = "5d02c36d7b44e886b7c8f0d8e051f8713acab96c264bb6ef9e718be39a6a2224"
XASR_FILES = {
    "encoder-epoch-99-avg-1.int8.onnx": "encoder.int8.onnx",
    "decoder-epoch-99-avg-1.onnx": "decoder.onnx",
    "joiner-epoch-99-avg-1.int8.onnx": "joiner.int8.onnx",
    "tokens.txt": "tokens.txt",
    "bpe.model": "bpe.model",
}

VAD_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx"
VAD_SHA256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6"


def download(url: str, sha256: str) -> bytes:
    print(f"downloading {url}")
    with urllib.request.urlopen(url, timeout=120) as response:
        data = response.read()
    actual = hashlib.sha256(data).hexdigest()
    if actual != sha256:
        sys.exit(f"SHA-256 mismatch for {url}\n  expected {sha256}\n  actual   {actual}")
    return data


def optimize_encoder(path: Path) -> None:
    """Save the encoder after ONNX Runtime's basic graph optimizations.

    sherpa-onnx re-optimizes the graph on every load, which took 17.7 s on the test phone;
    loading the already-optimized file takes 10.9 s with identical recognition output.
    Basic level only rewrites to standard operators, so it does not depend on the CPU.
    """
    try:
        import onnxruntime as ort
    except ImportError:
        print("onnxruntime is not installed: shipping the unoptimized encoder (slower first load)")
        return
    optimized = path.with_suffix(".opt")
    options = ort.SessionOptions()
    options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
    options.optimized_model_filepath = str(optimized)
    options.intra_op_num_threads = 1
    ort.InferenceSession(str(path), options, providers=["CPUExecutionProvider"])
    ort.InferenceSession(str(optimized), providers=["CPUExecutionProvider"])  # must load again
    optimized.replace(path)
    print(f"optimized {path.name}")


def main() -> None:
    (OUT / "xasr").mkdir(parents=True, exist_ok=True)

    archive = tarfile.open(fileobj=io.BytesIO(download(XASR_URL, XASR_SHA256)), mode="r:bz2")
    wanted = dict(XASR_FILES)
    for member in archive.getmembers():
        name = Path(member.name).name
        if member.isfile() and name in wanted:
            (OUT / "xasr" / wanted.pop(name)).write_bytes(archive.extractfile(member).read())
    if wanted:
        sys.exit(f"missing from the X-ASR archive: {sorted(wanted)}")
    optimize_encoder(OUT / "xasr" / "encoder.int8.onnx")

    (OUT / "silero_vad.onnx").write_bytes(download(VAD_URL, VAD_SHA256))
    print(f"models are in {OUT}")


if __name__ == "__main__":
    main()
