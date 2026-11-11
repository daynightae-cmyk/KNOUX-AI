"""Generate the KNOUX AI splash smile-click as a tiny mono WAV.

The sound is synthesised deterministically (no samples, no dependencies):
a soft noise transient for the tap attack plus three quickly decaying high
partials for a glassy "ting" and a quiet low body so it still reads on
small phone speakers. Re-run to regenerate the checked-in asset:

    python scripts/Generate-SplashSound.py
"""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path

RATE = 44_100
DURATION = 0.155
TARGET_PEAK = 0.42
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw" / "knoux_smile_click.wav"

# (frequency Hz, amplitude, decay seconds)
PARTIALS = (
    (1_975.0, 0.34, 0.046),
    (2_960.0, 0.21, 0.033),
    (4_180.0, 0.10, 0.021),
    (880.0, 0.12, 0.085),
)


def main() -> None:
    rng = random.Random(0x4B4E4F5558)  # "KNOUX"
    frames = int(RATE * DURATION)
    samples: list[float] = []
    for i in range(frames):
        t = i / RATE
        attack = min(1.0, t / 0.0015)
        value = 0.0
        for freq, amp, decay in PARTIALS:
            value += amp * math.sin(2.0 * math.pi * freq * t) * math.exp(-t / decay)
        # Short noise transient gives the perceived "click" edge.
        value += rng.uniform(-1.0, 1.0) * 0.11 * math.exp(-t / 0.0045)
        value *= attack
        # De-click the tail.
        remaining = DURATION - t
        if remaining < 0.008:
            value *= max(0.0, remaining / 0.008)
        samples.append(value)

    peak = max(abs(v) for v in samples) or 1.0
    scale = TARGET_PEAK / peak
    pcm = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, v * scale)) * 32_767)) for v in samples)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(RATE)
        handle.writeframes(pcm)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes, {DURATION * 1000:.0f} ms, {RATE} Hz mono)")


if __name__ == "__main__":
    main()
