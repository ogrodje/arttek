#!/usr/bin/env bash
set -e
code=$1
if [[ -z "$code" ]]; then
    echo "Episode code is missing" 1>&2
    exit 1
fi

sound_source=$(find ~/Dropbox/Ogrodje/final-recordings -name "*$code*" -type f)
sound_source=$(echo "$sound_source" | sed 's/ /\\\ /g')
sound_source="$(pwd)/video-content/sound.wav"
overlay_file="$(find overlays/ -name "*$code*" -type f)"
video_background="$(pwd)/video-content/backgrond-24.mov"
video_background="$(pwd)/video-content/vibe.mov"

echo "sound_source: $sound_source"
echo "overlay_file: $overlay_file"
echo "video_background: $video_background"
echo "--- encoding ---"

# -ss 00:00:00 -t 00:00:24 \

# ffmpeg -stream_loop -1 \
#   -i ${video_background} \
#   -t 24 \
#   -c:v copy -c:a copy \
#   -v 0 \
#   -f nut - | \
#   ffmpeg -thread_queue_size 10K \
#     -i - \
#     -i ${sound_source} \
#     -map 0:v \
#     -map 1:a \
#     -y $code.mp4

# ffmpeg \
#     -i ${video_background} \
#     -ss 00:00:00 -t 00:00:24 \
#     -stream_loop -1 \
#     -i ${sound_source} \
#     -shortest \
#     -map 0:v:0 \
#     -map 1:a:0 \
#     -c:v copy output.mp4

# ffmpeg -y -i video.mp4 -i overlay.png -filter_complex [0]overlay=x=0:y=0[out] -map [out] -map 0:a? test.mp4
# -map 0:v \
  #  -map 1:a \
  # -filter_complex "[0:v][1:a] overlay=25:25:enable='between(t,0,20)'" \
      #
      #-codec:a copy output.mp4
      #  -pix_fmt yuv420p \
#-c:a copy \
 #

# ffmpeg -stream_loop -1 -i ${video_background} -i ${overlay_file} -filter_complex \
#  "[1:v]format=argb,geq=r='r(X,Y)':a='1.0*alpha(X,Y)'[zork]; \
#   [0:v][zork]overlay" \
#   -vcodec libx264 \
#   -y $code.mp4

# ffmpeg -stream_loop -1 -i ${video_background} -i ${overlay_file} -filter_complex \
#  "overlay=50:50" \
#   -vcodec libx264 \
#   -y $code.mp4

# this works
# ffmpeg -stream_loop -1 -i ${video_background} \
#     -i ${sound_source} \
#     -i ${overlay_file} \
#     -filter_complex \
#     "overlay=50:50" \
#     -vcodec libx264 \
#     -map 0:v:0 \
#     -map 1:a:0 \
#     -shortest \
#     -y $code.mp4

ffmpeg -stream_loop -1 -i ${video_background} \
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
    -shortest \
    -y $code.mp4

# Resources:
# https://gist.github.com/mikoim/27e4e0dc64e384adbcb91ff10a2d3678
