# Audio Formats

# WAV

The WAV format is based on RIFF and have many possible chunks. Documentation can be found here:

- RIFF [wiki](https://en.wikipedia.org/wiki/Resource_Interchange_File_Format) with the official [pdf](https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/riffmci.pdf).
- WAV [wiki](https://en.wikipedia.org/wiki/WAV)

##  Chunks

| Chunk ID | Sub Chunk ID | Description                                                  | Documentation                                                |
| -------- | ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ` data`  |              | Where the audio is                                           | RIFF spec                                                    |
| ` fmt `  |              | Describe the audio format                                    | RIFF spec                                                    |
| ` BEXT`  |              | Broadcast WAV data, can contain various fields like ` originator`  or `description` | [Broadcast WAV](https://www.loc.gov/preservation/digital/formats/fdd/fdd000356.shtml), [EBU-TECH 3285 Spec](https://tech.ebu.ch/docs/tech/tech3285.pdf) |
| `ACID`   |              | Contains musical informations created by the software [Avid Acid Pro](https://www.magix.com/us/music-editing/acid/) | [libsndfile](https://github.com/libsndfile/libsndfile)       |
| `cue  `  |              | Contains markers in the audio file                           |                                                              |
| `umid`   |              | Unique Material identifier (part of [SMPTE](https://en.wikipedia.org/wiki/Society_of_Motion_Picture_and_Television_Engineers) spec) | [wiki](https://en.wikipedia.org/wiki/Unique_Material_Identifier) |
| `iXML`   |              | Metadata. Can contain [Steinberg](https://steinberg.help/cubase_pro_artist/v9/en/cubase_nuendo/topics/export_audio_mixdown/export_audio_mixdown_file_format_wave_files_r.html) extension like `MusicalBeats`, `MusicalSignature`, `MusicalTempo` | [iXML spec](http://www.gallery.co.uk/ixml/)                  |
| `_PMX`   |              | Metadata from Adobe’s Extensible Metadata Platform (XMP)     | [official page](https://www.adobe.com/products/xmp.html)     |
| `ID3 `   |              | Metadata in ID3 format                                       | [offical Mutagen ID3 spec](https://mutagen-specs.readthedocs.io/en/latest/) |
| `SMED`   |              | Opaque Soundminer Metawrapper data                           |                                                              |
| `LGWV`   |              | Logic Pro X data                                             |                                                              |
| `ResU`   |              | Logic Pro X data                                             |                                                              |
| `AFAn`   |              | Apple Binary plist serialized                                | [Apple TypedStream serialization format](https://gist.github.com/williballenthin/600a3898f43b7ad3f8aa4a5f4156941d) |
| `minf`   |              | ProTools data                                                |                                                              |
| `elm1`   |              | ProTools data                                                |                                                              |
| `regn`   |              | ProTools data (may be)                                       |                                                              |
| `CDif`   |              | Sound Forge 10 data (maybe)                                  |                                                              |
| `LIST`   |              |                                                              |                                                              |
|          | `INFO`       | Contains many metadata like Software, Description, Genre, Author... | [recordingblogs.com](https://www.recordingblogs.com/wiki/list-chunk-of-a-wave-file) |
|          | `adtl`       | Associated data list data. Contains subtitles, text for cue points | [recordingblogs.com](https://www.recordingblogs.com/wiki/associated-data-list-chunk-of-a-wave-file) |

## Source code

You will find a robust parser called `RiffParser` in  `com/hypercube/workshop/audioworkshop/files/wav` folder

**NOTE:** Optionally, this parser can fix incorrect RIFF size.

Metadata are extracted and normalized with this enum:

```java
public enum MetadataField {
    DESCRIPTION,
    BPM,
    GENRE,
    TIME_SIGNATURE,
    BARS,
    ROOT_NOTE,
    BEATS,
    VENDOR,
    COPYRIGHT,
    SOFTWARE,
    CREATED,
    KEY
}
```

See the unit test `audio-workshop/src/test/java/com/hypercube/workshop/audioworkshop/files/wav/RiffReaderTest.java` to learn how to use the `RiffParser`.

# FLAC

FLAC is a popular lossless format for audio files. Documentation can be found here:

- [Official FLAC format](https://xiph.org/flac/documentation.html)
- In-depth explanation of the compression can be found on github [here](https://uforobot.github.io/2018/04/01/flac-format/).

## Source code

Our parser `FlacReader` extracts metadata from the Flac.

See the unit test `audio-workshop/src/test/java/com/hypercube/workshop/audioworkshop/files/flac/FlacReaderTest.java` to learn how to use it.