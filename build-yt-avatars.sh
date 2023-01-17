#!/usr/bin/env bash
set -ex
rm -rf tmp/*.html yt-avatars/*.png

for episode in $(seq -w 1 19); do
  code="S01E$episode"
  echo "code $code"
  wget http://localhost:7777/episode-yt-avatar/$code -O tmp/$code.html
  wkhtmltoimage --transparent \
    -f png \
    --width 1280 \
    --crop-w 1280 \
    --height 720 \
    --crop-h 720 \
    --quality 100 \
    --images \
    --log-level info \
    tmp/$code.html yt-avatars/$code.png
done

cd yt-avatars/ && pngquant --ext -opt.png ./*.png
