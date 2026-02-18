package com.hypercube.workshop.audioworkshop.files.flac;

import com.hypercube.workshop.audioworkshop.utils.AudioTestFileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class FlacReaderTest {
    private static final File SRC_FOLDER = new File("./sounds");
    private static final File SRC_FOLDER_SURROUND = new File("%s/surround".formatted(SRC_FOLDER));
    private static final File SRC_FOLDER_STEREO = new File("%s/stereo".formatted(SRC_FOLDER));
    List<FormatAssert> surroundAsserts = List.of(
            new FormatAssert("Assassin's Creed Ezio Family Theme (16b_6ch_48000Hz).flac", 6, 16, 48000, "00:03:57", "ENCODER=fre:ac v1.1.6", List.of())
    );

    ;
    List<FormatAssert> stereoAsserts = List.of(
            new FormatAssert("PianoLoop2.flac", 2, 16, 44100, "00:02:16", "", List.of()),
            new FormatAssert("../forest.flac", 1, 8, 48000, "00:00:09", "TRACKNUMBER=1", List.of(new PictureAssert(512, 341, "image/jpeg", "", 39430)))
    );

    @BeforeAll
    static void downloadAudioFiles() {
        var d = new AudioTestFileDownloader();
        // Internet archive
        d.downloadSound("https://archive.org/download/PianoLoop-Relaxation/PianoLoop2.flac", SRC_FOLDER_STEREO);

        // https://amvizo.com/
        d.downloadSound("https://amvizo.com/downloads/music/flac/5.1/Assassin%27s%20Creed%20Ezio%20Family%20Theme%20%2816b_6ch_48000Hz%29.flac", SRC_FOLDER_SURROUND);

    }

    private void assertFormat(FormatAssert a, File f) {
        if (f.exists()) {
            log.info("Parse {}", f.getAbsolutePath());
            FlacReader r = new FlacReader(f);

            FlacAudioInfo info = r.parse();
            assertNotNull(info);

            log.info("bitDepth    : {}", info.getBitDepth());
            log.info("samplerate  : {}", info.getSampleRate());
            log.info("nbChannels  : {}", info.getNumChannels());
            log.info("duration    : {}", info.getDurationString());
            log.info("description : {}", info.getComment());

            info.getPictures()
                    .forEach(p -> {
                        log.info("Picture");
                        log.info(" width      : {}", p.width());
                        log.info(" height     : {}", p.height());
                        log.info(" mime type  : {}", p.mime());
                        log.info(" description: {}", p.description());
                        log.info(" data size  : {}", p.data().length);
                    });
            assertEquals(a.bitdepth, info.getBitDepth());
            assertEquals(a.samplerate, info.getSampleRate());
            assertEquals(a.nbChannels, info.getNumChannels());
            assertEquals(a.duration, info.getDurationString());
            assertEquals(a.comment, info.getComment());
            assertEquals(a.pictures.size(), info.getPictures()
                    .size());
            IntStream.range(0, a.pictures.size())
                    .forEach(idx -> {
                        var ap = a.pictures.get(idx);
                        var p = info.getPictures()
                                .get(idx);
                        assertEquals(ap.width(), p.width());
                        assertEquals(ap.height(), p.height());
                        assertEquals(ap.mime(), p.mime());
                        assertEquals(ap.description, p.description());
                        assertEquals(ap.size(), p.data().length);
                    });
        }
    }

    private record PictureAssert(int width, int height, String mime, String description, int size) {
    }

    private record FormatAssert(
            String file,
            int nbChannels,
            int bitdepth,
            int samplerate,
            String duration,
            String comment,

            List<PictureAssert> pictures
    ) {
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
}
