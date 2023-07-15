package com.hypercube.workshop.audioworkshop.files.wav;

import com.hypercube.workshop.audioworkshop.utils.AudioTestFileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class RiffReaderTest {
    private static File SRC_FOLDER = new File("./sounds");
    private static File SRC_FOLDER_SURROUND = new File("%s/surround".formatted(SRC_FOLDER));
    private static File SRC_FOLDER_STEREO = new File("%s/stereo".formatted(SRC_FOLDER));

    private record FormatAssert(
            String file,
            String format,
            UUID subformat,
            String chunks,
            int nbChannels,
            int bitdepth,
            int samplerate,
            String duration) {
    }

    @BeforeAll
    static void downloadAudioFiles() {
        var d = new AudioTestFileDownloader();
        // https://www.jensign.com/bdp95/7dot1voiced/index.html
        d.downloadSound("https://www.jensign.com/bdp95/7dot1voiced/7dot1voiced.zip", SRC_FOLDER_SURROUND);
        // fraunhofer test sounds
        d.downloadSound("https://www2.iis.fraunhofer.de/AAC/ChID-BLITS-EBU-Narration441-16b.wav", SRC_FOLDER_SURROUND);
        d.downloadSound("https://www2.iis.fraunhofer.de/AAC/SBR_LFETest5_1-441-16b.wav", SRC_FOLDER_SURROUND);
        d.downloadSound("https://www2.iis.fraunhofer.de/AAC/7.1auditionOutLeader%20v2.wav", SRC_FOLDER_SURROUND);
        // https://www.mmsp.ece.mcgill.ca/
        d.downloadSound("https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples/Microsoft/6_Channel_ID.wav", SRC_FOLDER_SURROUND);
        d.downloadSound("https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples/Microsoft/8_Channel_ID.wav", SRC_FOLDER_SURROUND);
        d.downloadSound("https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples/AFsp/M1F1-Alaw-AFsp.wav", SRC_FOLDER_STEREO);
        d.downloadSound("https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples/CCRMA/voxware.wav", SRC_FOLDER_STEREO);
        // https://rhythm-lab.com/
        d.downloadSound("https://rhythm-lab.com/sstorage/53/2023/01/Al%20Jarreau%20-%20Kissing%20My%20Love%20(CD).wav", SRC_FOLDER_STEREO);
        // music radar
        // https://www.musicradar.com/news/sampleradar-starting-point-samples-1
        // https://www.musicradar.com/news/tech/free-music-samples-royalty-free-loops-hits-and-multis-to-download
        //d.downloadSound("https://cdn.mos.musicradar.com/audio/samples/musicradar-starting-point-samples.zip", SRC_FOLDER_STEREO);
        // https://www.audiocheck.net/
        //
    }

    List<FormatAssert> surroundAsserts = List.of(
            new FormatAssert("SBR_LFETest5_1-441-16b.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,data,LIST,bext,_PMX", 6, 16, 44100, "00:00:45"),
            new FormatAssert("ChID-BLITS-EBU-Narration441-16b.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,data,LIST,bext,_PMX", 6, 16, 44100, "00:00:46"),
            new FormatAssert("7.1auditionOutLeader v2.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,data,afsp,LIST,_PMX", 8, 16, 48000, "00:00:31"),
            new FormatAssert("Nums_7dot1_24_48000.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,data", 8, 24, 48000, "00:00:09"),
            new FormatAssert("Nums_5dot1_24_48000.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,data", 6, 24, 48000, "00:00:09"),
            new FormatAssert("6_Channel_ID.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,cue,data", 6, 16, 44100, "00:00:05"),
            new FormatAssert("8_Channel_ID.wav", null, WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE, "fmt,cue,data", 8, 24, 48000, "00:00:08"));
    List<FormatAssert> stereoAsserts = List.of(
            new FormatAssert("M1F1-Alaw-AFsp.wav", "ITU_G711_ALAW", null, "fmt,fact,data,afsp,LIST", 2, 8, 8000, "00:00:02"),
            new FormatAssert("voxware.wav", "UNKNOWN", null, "fmt,fact,data", 1, 0, 8000, "00:00:00"),
            new FormatAssert("Al Jarreau - Kissing My Love (CD).wav", "PCM", null, "fmt,data", 2, 16, 44100, "00:00:10")
    );

    List<FormatAssert> appleLoopsAsserts = List.of(
            new FormatAssert("AlchemyLoopsBeatBoxBreaks-Vox Breaks 01a.wav", "PCM", null, "fmt,smpl,inst,FLLR,data", 1, 16, 44100, "00:00:04")
    );

    private <T> String listToString(List<T> list, Function<T, String> map) {
        return list.stream()
                .reduce(new StringBuilder(""), (a, b) -> {
                    if (a.length() > 0) {
                        a.append(",");
                    }
                    a.append(map.apply(b)
                            .trim());
                    return a;
                }, (a, b) -> a)
                .toString();
    }

    @Test
    void parseStereoFiles() {
        stereoAsserts.forEach(a -> {
            File f = new File("%s/%s".formatted(SRC_FOLDER_STEREO, a.file));
            assertFormat(a, f);
        });
    }

    @Test
    void parseMultichannelFiles() {
        surroundAsserts.forEach(a -> {
            File f = new File("%s/%s".formatted(SRC_FOLDER_SURROUND, a.file));
            assertFormat(a, f);
        });
    }

    private void assertFormat(FormatAssert a, File f) {
        if (f.exists()) {
            log.info("Parse " + f.getAbsolutePath());
            RiffReader r = new RiffReader(f, false);
            RiffFileInfo info = r.parse();
            String chunks = listToString(info.getChunks(), RiffChunk::getId);

            log.info("codec      : " + info.getCodecString());
            log.info("chunks     : " + chunks);
            log.info("nbChannels : " + info.getFileInfo()
                    .getNbChannels());
            log.info("bitdepth   : " + info.getFileInfo()
                    .getBitPerSample());
            log.info("samplerate : " + info.getFileInfo()
                    .getSampleRate());
            log.info("tempo      : " + info.getFileInfo()
                    .getTempo());

            log.info("duration   : " + info.getFileInfo()
                    .getDurationString());
            log.info("Metadata   : %d entries".formatted(info.getMetadata()
                    .getAll()
                    .size()));
            info.getMetadata()
                    .getAll()
                    .forEach((k, v) -> {
                        log.info("       %s   : %s".formatted(k, v));
                    });

            if (a.format != null) {
                assertEquals(a.format, info.getCodecString());
            }
            assertEquals(a.nbChannels, info.getFileInfo()
                    .getNbChannels());
            assertEquals(a.bitdepth, info.getFileInfo()
                    .getBitPerSample());
            assertEquals(a.samplerate, info.getFileInfo()
                    .getSampleRate());
            assertEquals(a.duration, info.getFileInfo()
                    .getDurationString());
            assertEquals(a.chunks, chunks);
            assertEquals(a.subformat, info.getFileInfo()
                    .getSubCodec());
        }
    }


}
