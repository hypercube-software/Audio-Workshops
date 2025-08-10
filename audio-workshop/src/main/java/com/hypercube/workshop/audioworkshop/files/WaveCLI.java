package com.hypercube.workshop.audioworkshop.files;

import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.insights.EnvelopeFollower;
import com.hypercube.workshop.audioworkshop.files.riff.RiffAudioInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.insights.RiffInspector;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class WaveCLI {
    @ShellMethod(value = "Search loop points in a WAV file")
    public void scanWav(@ShellOption(value = "-i") File file) throws Exception {
        try (RiffReader riffReader = new RiffReader(file, false)) {
            RiffFileInfo riffFileInfo = riffReader.parse();
            if (riffFileInfo != null) {
                logInfo(riffFileInfo.getAudioInfo());
                File debug = new File("target/rms.csv");
                debug.getParentFile()
                        .mkdirs();
                try (PrintWriter out = new PrintWriter(new FileOutputStream(debug))) {
                    out.println("Sep=,");
                    AtomicInteger rowCount = new AtomicInteger(0);
                    PCMFormat pcmFormat = riffFileInfo.getAudioInfo()
                            .toPCMFormat();
                    EnvelopeFollower envelopeFollower = new EnvelopeFollower(pcmFormat, 10, 50, (sample, rms, autoCorrelation, channel) -> {
                        if (rowCount.addAndGet(1) == 1) {
                            out.println("Sample, RMS");
                        }
                        out.println(String.format(Locale.ENGLISH, "%f,%f", sample, rms));
                    });
                    RiffInspector waveformConverter = new RiffInspector(riffReader, riffFileInfo);
                    waveformConverter.inspect(envelopeFollower);
                    log.info("Noise floor  : %f %f dB".formatted(envelopeFollower.getNoiseFloor(), pcmFormat.toDb(envelopeFollower.getNoiseFloor())));
                    log.info("sample start : %010d %05.1f ms".formatted(envelopeFollower.getSampleStart(), pcmFormat.samplesToMilliseconds(envelopeFollower.getSampleStart())));
                    log.info("sample end   : %010d %05.1f ms".formatted(envelopeFollower.getSampleEnd(), pcmFormat.samplesToMilliseconds(envelopeFollower.getSampleEnd())));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.error("Unable to parse file");
            }
        }
    }

    @ShellMethod(value = "Dump all sample values from a WAV File to a CSV file")
    public void wavToCSV(@ShellOption(value = "-i") File file, @ShellOption(value = "-o") File csvFile) throws IOException {
        try (RiffReader riffReader = new RiffReader(file, false)) {
            RiffFileInfo riffFileInfo = riffReader.parse();
            if (riffFileInfo != null) {
                logInfo(riffFileInfo.getAudioInfo());
                RiffInspector waveformConverter = new RiffInspector(riffReader, riffFileInfo);
                waveformConverter.saveCsv(csvFile);
            } else {
                log.error("Unable to parse file");
            }
        }
    }

    @ShellMethod(value = "Generate the WAV waveform in PNG")
    public void wavToPng(@ShellOption(value = "-i") File file, @ShellOption(value = "-o") File pngWaveform, @ShellOption(value = "-W") int width, @ShellOption(value = "-H") int height) throws IOException {
        try (RiffReader riffReader = new RiffReader(file, false)) {
            RiffFileInfo riffFileInfo = riffReader.parse();
            if (riffFileInfo != null) {
                logInfo(riffFileInfo.getAudioInfo());
                RiffInspector waveformConverter = new RiffInspector(riffReader, riffFileInfo);
                waveformConverter.savePng(pngWaveform, width, height);
            } else {
                log.error("Unable to parse file");
            }
        }
    }

    @ShellMethod(value = "Load a file")
    public void parseWAV(@ShellOption(value = "-i") File file) throws IOException {
        try (RiffReader riffReader = new RiffReader(file, false)) {
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
