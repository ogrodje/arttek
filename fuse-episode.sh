#!/usr/bin/env bash
set -e
code=$1
if [[ -z "$code" ]]; then
    echo "Episode code is missing" 1>&2
    exit 1
fi

sound_source=$(find ~/Dropbox/Ogrodje/final-recordings -name "*$code*" -type f)
sound_source=$(echo "$sound_source" | sed 's/ /\\\ /g')
# sound_source="$(pwd)/video-content/sound.wav"
overlay_file="$(find overlays/ -name "*$code*" -type f)"
video_background="$(pwd)/video-content/backgrond-24.mov"
video_background="$(pwd)/video-content/vibe.mov"

echo "sound_source: $sound_source"
echo "overlay_file: $overlay_file"
echo "video_background: $video_background"
echo "--- encoding ---"

ffmpeg \
    -hwaccel videotoolbox \
    -hide_banner \
    -stream_loop -1 -i ${video_background} \
    -i ${sound_source} \
    -i ${overlay_file} \
    -filter_complex \
    "overlay=((main_w-overlay_w)/2)-10:(main_h-overlay_h)/2" \
    -map 0:v:0 \
    -map 1:a:0 \
    -vcodec libx264 \
    -profile:v high \
    -preset slow \
    -movflags +faststart \
    -bf 2	\
    -crf 18 \
    -g 30	\
    -pix_fmt yuv420p \
    -c:a aac -profile:a aac_low -b:a 384k \
    -shortest \
    -y video-output/$code.mp4

# Resources:
# https://gist.github.com/mikoim/27e4e0dc64e384adbcb91ff10a2d3678
