package com.hypercube.workshop.audioworkshop.files;

import com.hypercube.workshop.audioworkshop.files.png.WaveformConverter;
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
    @ShellMethod(value = "Dumpp all sample values from a WAV File to a CSV file")
    public void wavToCSV(@ShellOption(value = "-i") File file, @ShellOption(value = "-o") File csvFile) {
        RiffReader riffReader = new RiffReader(file, false);
        RiffFileInfo riffFileInfo = riffReader.parse();
        if (riffFileInfo != null) {
            logInfo(riffFileInfo.getAudioInfo());
            WaveformConverter waveformConverter = new WaveformConverter(riffReader, riffFileInfo);
            waveformConverter.saveCsv(csvFile);
        } else {
            log.error("Unable to parse file");
        }
    }

    @ShellMethod(value = "Generate the WAV waveform in PNG")
    public void wavToPng(@ShellOption(value = "-i") File file, @ShellOption(value = "-o") File pngWaveform, @ShellOption(value = "-W") int width, @ShellOption(value = "-H") int height) {
        RiffReader riffReader = new RiffReader(file, false);
        RiffFileInfo riffFileInfo = riffReader.parse();
        if (riffFileInfo != null) {
            logInfo(riffFileInfo.getAudioInfo());
            WaveformConverter waveformConverter = new WaveformConverter(riffReader, riffFileInfo);
            waveformConverter.savePng(pngWaveform, width, height);
        } else {
            log.error("Unable to parse file");
        }
    }

    @ShellMethod(value = "Load a file")
    public void parseWAV(@ShellOption(value = "-i") File file) {
        RiffReader riffReader = new RiffReader(file, false);
        RiffFileInfo riffFileInfo = riffReader.parse();
        if (riffFileInfo != null) {
            if (!riffFileInfo.getFiles()
                    .isEmpty()) {
                riffFileInfo.getFiles()
                        .forEach(WaveCLI::logInfo);
            } else {
                logInfo(riffFileInfo.getAudioInfo());
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
        log.info("samples    : " + info.getNbSamples());
    }
}
