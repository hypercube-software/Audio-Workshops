package com.hypercube.workshop.audioworkshop.files;

import com.hypercube.workshop.audioworkshop.files.riff.RiffAudioInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class WaveCLI {
    @ShellMethod(value = "Load a file")
    public void parseWAV(@ShellOption(value = "-f") File file) {
        RiffReader r = new RiffReader(file, false);
        RiffFileInfo info = r.parse();
        if (info != null) {
            if (!info.getFiles()
                    .isEmpty()) {
                info.getFiles()
                        .forEach(WaveCLI::logInfo);
            } else {
                logInfo(info.getAudioInfo());
            }
        } else {
            log.error("Unable to parse file");
        }
    }

    private static void logInfo(RiffAudioInfo info) {
        if (info.getFilename() != null) {
            log.info("-----------------------");
            log.info(info.getFilename());
        }
        log.info("codec      : " + info.getCodecString());
        log.info("nbChannels : " + info.getNbChannels());
        log.info("bitdepth   : " + info.getBitPerSample());
        log.info("samplerate : " + info.getSampleRate());
        log.info("duration   : " + info.getDurationString());
    }
}
