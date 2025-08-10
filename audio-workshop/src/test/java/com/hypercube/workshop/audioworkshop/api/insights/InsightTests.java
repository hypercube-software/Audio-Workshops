package com.hypercube.workshop.audioworkshop.api.insights;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumerChain;
import com.hypercube.workshop.audioworkshop.api.consumer.WindowedSampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.filter.dc.DCOffsetRemover;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.insights.dc.DCOffsetCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.DFTCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.fast.FFTCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.gradient.SpectrogramGenerator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.slow.SimpleDFTCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.dft.windows.BlackmanHarris;
import com.hypercube.workshop.audioworkshop.api.insights.peak.PeakCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.rms.RMSCalculator;
import com.hypercube.workshop.audioworkshop.api.insights.rms.RMSReference;
import com.hypercube.workshop.audioworkshop.api.insights.signal.SignalSegment;
import com.hypercube.workshop.audioworkshop.api.insights.signal.SignalSegmentsDetector;
import com.hypercube.workshop.audioworkshop.api.insights.signal.SignalStartDetector;
import com.hypercube.workshop.audioworkshop.api.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.insights.RiffInspector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class InsightTests {

    public static final File FILE_440HZ = new File("sounds/mono/440Hz.wav");
    public static final File NOISE_FILE = new File("sounds/mono/noise-floor-test.wav");

    @BeforeAll
    static void generate440() throws IOException {
        generateSine(440, 44100, 1000, FILE_440HZ);
        generateNoise(0.1f, 0.5f, 44100, 1000, NOISE_FILE);
    }

    static void generateSine(float frequency, int sampleRate, int durationInMs, File file) throws IOException {
        if (!file.exists() || file.length() == 0) {
            PCMFormat format = new PCMFormat(sampleRate, BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
            try (RiffWriter riffWriter = new RiffWriter(file)) {
                riffWriter.writeFmtChunk(format);
                riffWriter.beginChunk(Chunks.DATA);
                int durationInSamples = format.millisecondsToSamples(durationInMs);
                double periodInSamples = sampleRate / frequency;
                for (int i = 0; i < durationInSamples; i++) {
                    double theta = i / periodInSamples;
                    double sample = Math.sin(theta * 2 * Math.PI) * 0x7FFF;
                    riffWriter.writeShortLE((int) sample);
                }
                riffWriter.endChunk();
            }
        }
    }

    static void generateNoise(float volume, float dcBias, int sampleRate, int durationInMs, File file) throws IOException {
        if (!file.exists() || file.length() == 0) {
            PCMFormat format = new PCMFormat(sampleRate, BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
            try (RiffWriter riffWriter = new RiffWriter(file)) {
                riffWriter.writeFmtChunk(format);
                riffWriter.beginChunk(Chunks.DATA);
                int durationInSamples = format.millisecondsToSamples(durationInMs);
                double scale = volume * 0x7FFF;
                for (int i = 0; i < durationInSamples; i++) {
                    double sample = Math.random() * scale + (dcBias - (volume / 2)) * 0x7FFF;
                    riffWriter.writeShortLE((int) sample);
                }
                riffWriter.endChunk();
            }
        }
    }

    @Test
    void computeInsights() throws IOException {
        try (RiffReader r = new RiffReader(NOISE_FILE, false)) {
            var info = r.parse();
            int nbChannels = info.getAudioInfo()
                    .getNbChannels();
            RiffInspector waveformConverter = new RiffInspector(r, info);
            RMSCalculator rmsCalculator = new RMSCalculator(nbChannels);
            PeakCalculator peakCalculator = new PeakCalculator();
            DCOffsetCalculator dcOffsetCalculator = new DCOffsetCalculator(nbChannels);
            waveformConverter.inspect(new SampleBufferConsumerChain(List.of(rmsCalculator, peakCalculator, dcOffsetCalculator)));
            log.info("samples [%f,%f] DC Offset: %16.16f %s%% Sample peak: %s dBFS Total RMS:  %s dB".formatted(
                    peakCalculator.getSampleMin() * 0x8000,
                    peakCalculator.getSampleMax() * 0x8000,
                    dcOffsetCalculator.getGlobalDcOffset()[0],
                    dcOffsetCalculator.getDCOffsetPercent(2),
                    peakCalculator.getSamplePeakDb(2),
                    rmsCalculator.getRMSDb(RMSReference.SINE_WAVE_AES_17, 2)));

            //DCOffsetCalculator dcOffsetCalculatorRemover = new DCOffsetCalculator(true, nbChannels, new double[]{-0.000579833984375/*-0.000457763671875*/});
            DCOffsetRemover dcOffsetRemover = new DCOffsetRemover(dcOffsetCalculator.getGlobalDcOffset());
            waveformConverter.inspect(new SampleBufferConsumerChain(List.of(dcOffsetRemover, dcOffsetCalculator, rmsCalculator, peakCalculator)));
            log.info("samples [%f,%f] DC Offset REMOVED: %16.16f %s%% Sample peak: %s dBFS Total RMS:  %s dB".formatted(
                    peakCalculator.getSampleMin() * 0x8000,
                    peakCalculator.getSampleMax() * 0x8000,
                    dcOffsetCalculator.getGlobalDcOffset()[0],
                    dcOffsetCalculator.getDCOffsetPercent(2),
                    peakCalculator.getSamplePeakDb(2),
                    rmsCalculator.getRMSDb(RMSReference.SINE_WAVE_AES_17, 2)));
        }
    }

    @Test
    @Disabled
    void windowedInsight() throws IOException {
        File f = new File("synth-ripper/output/BOSS-DS330/048 String Ensemble 1/Note C4 - Velo 127.wav");
        try (RiffReader r = new RiffReader(f, false)) {

            var info = r.parse();
            int nbChannels = info.getAudioInfo()
                    .getNbChannels();
            RiffInspector waveformConverter = new RiffInspector(r, info);
            RMSCalculator rmsCalculator = new RMSCalculator(nbChannels);
            PeakCalculator peakCalculator = new PeakCalculator();

            int windowSizeInSample = (int) (info.getAudioInfo()
                    .getSampleRate() * 0.001F);
            int windowSizeInSample2 = (int) (info.getAudioInfo()
                    .getSampleRate() * 0.250F);

            var signalStartDetector = new SignalStartDetector(info.getAudioInfo()
                    .toPCMFormat());

            var signalEndDetector = new WindowedSampleBufferConsumer(windowSizeInSample2, nbChannels, new SampleBufferConsumer() {
                long positionInSamples = 0;
                long signalEnd = 0;

                @Override
                public void onBuffer(SampleBuffer buffer) {
                    rmsCalculator.reset();
                    rmsCalculator.onBuffer(buffer);
                    peakCalculator.reset();
                    peakCalculator.onBuffer(buffer);
                    long cutPoint = positionInSamples + peakCalculator.getFirstZeroCrossingPosInSample();
                    positionInSamples += buffer.nbSamples();
                    double rmsDb = rmsCalculator.getRMSDb(RMSReference.SINE_WAVE_AES_17, 2);
                    boolean cross = (rmsDb < -42);
                    if (cross) {
                        log.info("OUT  " + getMsg(cutPoint, rmsDb));
                    }
                }

                private String getMsg(long cutPoint, double rmsDb) {
                    long positionInMs = (long) (cutPoint * 1000 / info.getAudioInfo()
                            .getSampleRate());
                    long positionInSeconds = positionInMs / 1000;
                    int hours = (int) (positionInSeconds / 3600);
                    int minutes = (int) ((positionInSeconds % 3600) / 60);
                    int seconds = (int) (positionInSeconds % 60);
                    int ms = (int) (positionInMs % 1000);
                    String msg = String.format("%d %02d:%02d:%02d.%03d %.2f dB", cutPoint, hours, minutes, seconds, ms, rmsDb);
                    return msg;
                }
            });
            waveformConverter.inspect(new SampleBufferConsumerChain(List.of(signalStartDetector, signalEndDetector)));
        }
    }

    @Test
    @Disabled
    void segment() throws IOException {
        File f = new File("../synth-ripper/output/BOSS-DS330/005 Electric Piano 2/Note C4 - Velo 127.wav");
        try (RiffReader r = new RiffReader(f, false)) {

            var info = r.parse();
            int nbChannels = info.getAudioInfo()
                    .getNbChannels();
            RiffInspector waveformConverter = new RiffInspector(r, info);
            SignalSegmentsDetector signalSegmentsDetector = new SignalSegmentsDetector(info.getAudioInfo()
                    .toPCMFormat(), -41.71);
            waveformConverter.inspect(signalSegmentsDetector);
            signalSegmentsDetector.getFirstSegment()
                    .purge();

            List<SignalSegment> segments = signalSegmentsDetector.getSegments();
            segments.forEach(s -> log.info("%d to %d %s".formatted(s.getSampleStart(), s.getSampleEnd(), s.getType())));
            assertEquals(3, segments
                    .size());
        }
    }

    @Test
    void dft() throws IOException {
        try (RiffReader r = new RiffReader(FILE_440HZ, false)) {

            var info = r.parse();
            RiffInspector waveformConverter = new RiffInspector(r, info);
            PCMBufferFormat format = new PCMBufferFormat(80, info.getAudioInfo()
                    .toPCMFormat());
            DFTCalculator dftCalculator = new SimpleDFTCalculator(format, new BlackmanHarris());
            waveformConverter.inspect(new WindowedSampleBufferConsumer(format.getSampleBufferSize(), format.millisecondsToSamples(40), format.getNbChannels(), dftCalculator));
            SpectrogramGenerator spectrogramGenerator = new SpectrogramGenerator();
            spectrogramGenerator.generate(info.getAudioInfo()
                    .getBitPerSample(), dftCalculator.getMagnitudes()[0], new File("target/spectrogram.png"), 1000, 512);
        }
    }

    @Test
    void fft() throws IOException {
        try (RiffReader r = new RiffReader(FILE_440HZ, false)) {

            var info = r.parse();
            RiffInspector waveformConverter = new RiffInspector(r, info);
            PCMBufferFormat format = new PCMBufferFormat(80, info.getAudioInfo()
                    .toPCMFormat());
            try (FFTCalculator dftCalculator = new FFTCalculator(format, new BlackmanHarris())) {
                waveformConverter.inspect(new WindowedSampleBufferConsumer(format.getSampleBufferSize(), format.millisecondsToSamples(5), format.getNbChannels(), dftCalculator));
                SpectrogramGenerator spectrogramGenerator = new SpectrogramGenerator();
                spectrogramGenerator.generate(info.getAudioInfo()
                        .getBitPerSample(), dftCalculator.getMagnitudes()[0], new File("target/spectrogram-fft.png"), 1000, 512);
            }
        }
    }
}
