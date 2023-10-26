# arttek

arttek is a tool for generating (animated) artwork for your podcast.

## Setup

arttek connects to [hygraph] to fetch the episodes information. 
The data is then used to generate HTML/PNG and video/audio files.

```bash
export HYGRAPH_TOKEN="access_token"
export HYGRAPH_ENDPOINT="https://lalaalalal.hygraph.com/v2/xxxx/master"
````

## Usage

```bash
sbt assembly
java -jar target/*/arttek.jar --help
```

```
arttek art builder v0.0.1 -- Generates images and video for your podcast

USAGE

  $ arttek <command>

COMMANDS

  - dev-server [--port text]
  - render-overlay <code> <output-folder>
  - render-youtube-thumbnail <code> <output-folder>
  - render-youtube-thumbnails [--codes text] <output-folder>
  - render-podcast-thumbnail <code> <output-folder>
  - render-podcast-thumbnails [--codes text] <output-folder>
```

## Usage with Docker (experimental)

```bash
sbt assembly docker:publishLocal
```

```bash
docker run -ti -p 4444:4444 --rm \
  -e HYGRAPH_ENDPOINT=${HYGRAPH_ENDPOINT} \
  -e HYGRAPH_TOKEN=${HYGRAPH_TOKEN} \
  -v $(pwd)/outputs-xxx:/tmp/outputs-xxx \
  ghcr.io/ogrodje/arttek render-podcast-thumbnails /tmp/outputs-xxx
```

[hygraph]: https://hygraph.com/

## Dependencies

- The project depends on [ffmpeg](https://ffmpeg.org/) for composing audio/video files.
- The project depends on [sassc]([https://ffmpeg.org/](https://github.com/sass/sassc)) for SASS to CSS rendering.
- [wkhtmltopdf](https://wkhtmltopdf.org/) (`wkhtmltoimage`) is used for the generation of PNGs out of HTML files.
- [pngquant](https://pngquant.org/) is used for (size) optimisation of PNG images.

## Ogrodje specific

This project was designed to work with Ogrodje podcast and its specifics. Some of the things are hard-coded; 
however, with minimum support, many things can be made more generic and reusable. 
A good start is to look into [templates](templates/) and [sass](sass/) folders and start customisation there. 
To change or optimise GraphQL queries look into [OgrodjeClient.scala](src/main/scala/com/pinkstack/arttek/OgrodjeClient.scala)


## Authors

- [Oto Brglez](https://twitter.com/otobrglez)
