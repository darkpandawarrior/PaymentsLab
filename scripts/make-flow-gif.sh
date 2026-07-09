#!/usr/bin/env bash
# Stitch an ordered list of PNG frames into an animated GIF with subtle crossfades and a clean,
# per-GIF-optimised palette (ffmpeg palettegen/paletteuse). All frames must share one resolution.
#
# Usage:   scripts/make-flow-gif.sh <out.gif> <frame1.png> <frame2.png> [frame3.png ...]
# Tunables (env): HOLD (s each frame is fully visible, default 1.5), TRANS (crossfade s, default 0.5),
#                 FPS (default 24), WIDTH (px; default = source width, -1 keeps aspect).
set -euo pipefail

OUT="${1:?usage: make-flow-gif.sh <out.gif> <frame...>}"; shift
[ "$#" -ge 2 ] || { echo "need >= 2 frames" >&2; exit 1; }

HOLD="${HOLD:-1.5}"; TRANS="${TRANS:-0.5}"; FPS="${FPS:-24}"; WIDTH="${WIDTH:-0}"
L=$(awk "BEGIN{print $HOLD + $TRANS}")   # per-input length: visible hold + fade overlap
STEP="$HOLD"                              # xfade offset step = L - TRANS = HOLD

inputs=(); for f in "$@"; do inputs+=(-loop 1 -t "$L" -i "$f"); done

# Chain xfades: [0][1]xfade@off1 -> [x1]; [x1][2]xfade@off2 -> [x2]; ...
n=$#; cur="[0]"; chain=""
for ((i=1; i<n; i++)); do
  off=$(awk "BEGIN{printf \"%.3f\", $i*$STEP}")
  lbl="[x$i]"
  chain+="${cur}[$i]xfade=transition=fade:duration=$TRANS:offset=$off$lbl;"
  cur="$lbl"
done

scale=""; [ "$WIDTH" != "0" ] && scale="scale=$WIDTH:-1:flags=lanczos,"
fc="${chain}${cur}${scale}fps=$FPS,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=3:diff_mode=rectangle"

mkdir -p "$(dirname "$OUT")"
ffmpeg -hide_banner -loglevel error -y "${inputs[@]}" -filter_complex "$fc" -loop 0 "$OUT"
echo "wrote $OUT ($(du -h "$OUT" | cut -f1), ${n} frames, hold=${HOLD}s fade=${TRANS}s)"
