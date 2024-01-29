package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.audioworkshop.utils.AudioTestFileDownloader;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Gig files can be found here: https://musical-artifacts.com/artifacts?formats=gig
 * <p>DSL2 files can be found here: https://musical-artifacts.com/artifacts?tags=dls
 */
@Slf4j
class DSL2Test {

    private static File SRC_FOLDER = new File("./sounds/dsl2");

    @BeforeAll
    static void downloadAudioFiles() {
        var d = new AudioTestFileDownloader();
        d.downloadSound("https://musical-artifacts.com/artifacts/787/Nokia_6230i.DLS", SRC_FOLDER);
        d.downloadSound("https://musical-artifacts.com/artifacts/74/TaijiguyGigaTron_switched.gig", SRC_FOLDER);
    }

    @Test
    void parseDSL() {
        File input = new File("%s/%s".formatted(SRC_FOLDER, "Nokia_6230i.DLS"));
        RiffReader r = new RiffReader(input, false);
        var info = r.parse();
        dump("", info.getChunks());
        assertEquals(41, info.getFiles()
                .size());
        String files = info.getFiles()
                .stream()
                .map(f -> f.getFilename())
                .sorted()
                .collect(Collectors.joining(","));
        assertEquals("ACBB1,AGTF3,CLAVC2,CRASH,DSTC5,EGTD3,EPNOC4,FLUC5,FLUC6,HARMC4C,LSTRC4,LSTRG5B,MTCN2c,NUBSFs1,OHD5,OHF3,OPCN2d,ORCHC5D,PNOC3,PNOC4C,PNOC5B,PNOC6E,PSYN,RIDE,ROGC5B,SAXC4,SHAKER,SQUCs4,SQUCs6,STDF4C,TAMB,TOM2,TRC5,TRI1,VIBAs4,VIBRA,VIGC3,VIGC4,nclp,nsd1,pinosine", files);
        info.getFiles()
                .forEach(f -> {
                    log.info(f.getFilename());
                    try {
                        File target = new File("output/nokia/" + f.getFilename() + ".wav");
                        r.extract(f, target);
                    } catch (IOException e) {
                        throw new MidiError(e);
                    }
                });
    }

    @Test
    void parseGig() {
        File input = new File("./sounds/dsl2/TaijiguyGigaTron_switched.gig");
        RiffReader r = new RiffReader(input, false);
        var info = r.parse();
        log.info("Version: " + info.getVersion());
        dump("", info.getChunks());

        info.getInstruments()
                .forEach(instr -> {
                    log.info("Instrument: {}", instr.fullPath());
                    instr.samples()
                            .forEach(sample -> log.info("     {}", sample.getFilename()));
                });
        long nbUsed = info.getFiles()
                .stream()
                .filter(RiffAudioInfo::isUsed)
                .count();
        long nbUnused = info.getFiles()
                .stream()
                .filter(f -> !f.isUsed())
                .count();
        log.info("{} samples used, {} samples not used", nbUsed, nbUnused);
        info.getFiles()
                .stream()
                .filter(f -> !f.isUsed())
                .forEach(f -> log.info("Not used: " + f.getFilename()));

        assertEquals(0, nbUnused);
        assertEquals(455, nbUsed);
        assertEquals("3.0.0.0", info.getVersion()
                .toString());
        log.info("Extract samples...");
        info.getFiles()
                .forEach(f -> {
                    try {
                        File target = new File("output/tron/" + f.getFilename() + ".wav");
                        r.extract(f, target);
                    } catch (IOException e) {
                        throw new MidiError(e);
                    }
                });
        log.info("Done");
    }

    private void dump(String prefix, List<RiffChunk> info) {
        info.forEach(c -> {
            log.info(prefix + c.toString());
            dump(prefix + "   ", c.getChildren());
        });
    }
}
