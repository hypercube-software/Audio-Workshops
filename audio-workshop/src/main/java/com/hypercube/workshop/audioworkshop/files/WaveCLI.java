package com.hypercube.workshop.audioworkshop.files;

import com.hypercube.workshop.audioworkshop.files.wav.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.wav.RiffReader;
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
        log.info("codec      : " + info.getCodecString());
        log.info("nbChannels : " + info.getFileInfo()
                .getNbChannels());
        log.info("bitdepth   : " + info.getFileInfo()
                .getBitPerSample());
        log.info("samplerate : " + info.getFileInfo()
                .getSampleRate());


        log.info("duration   : " + info.getFileInfo()
                .getDurationString());
    }
}
