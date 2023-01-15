#!/usr/bin/env bash
set -ex
rm -rf tmp/*.html overlays/*.png

for episode in $(seq -w 1 19); do
  code="S01E$episode"
  echo "code $code"
  wget http://localhost:7777/episode-video-2/$code -O tmp/$code.html
  wkhtmltoimage --transparent \
    -f png \
    --width 1220 \
    --crop-w 1220 \
    --quality 100 \
    --images \
    --log-level info \
    tmp/$code.html overlays/$code.png
done
