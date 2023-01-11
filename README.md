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

[hygraph]: https://hygraph.com/

## Dependencies

- The project depends on [ffmpeg](https://ffmpeg.org/) for for composing audio/video files.
- [wkhtmltopdf](https://wkhtmltopdf.org/) is used for generating PNG out of HTML files.


## Ogrodje specific

This project was designed to work with Ogrodje podcast and its specifics. Some of the things are hard-coded; 
however with minimum support a lot of the things can be made more generic and reusable. 
Good start is to look into [templates](templates/) folder and start customisation there. 
To change or optimise GraphQL queries look into [OgrodjeClient.scala](src/main/scala/com/pinkstack/arttek/OgrodjeClient.scala)


## Authors

- [Oto Brglez](https://twitter.com/otobrglez)
