#!/usr/bin/env bash
set -ex
rm -rf tmp/*.html overlays/*.png

for episode in $(seq -w 1 19); do
  code="S01E$episode"
  echo "code $code"
  wget http://localhost:7777/episode-video/$code -O tmp/$code.html
  wkhtmltoimage --transparent --disable-javascript \
    -f png \
    --width 2000 \
    --crop-w 2500 \
    --quality 100 \
    tmp/$code.html overlays/$code.png
done
