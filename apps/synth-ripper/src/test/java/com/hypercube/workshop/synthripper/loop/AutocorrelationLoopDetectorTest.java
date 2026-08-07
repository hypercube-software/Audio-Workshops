package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMMarker;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.RiffWriter;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl.RiffAdtlLabelChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue.RiffCueChunk;
import com.hypercube.workshop.audioworkshop.files.riff.insights.RiffInspector;
import com.hypercube.workshop.synthripper.model.LoopSetting;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AutocorrelationLoopDetectorTest {

    private static final String TEST_WAV = "src/test/resources/loop/071 B3 Stream - Velo 064.wav";
    private static final String OUTPUT_DIR = "target/output";

    /**
     * Find the sample position of the marker with the given label in the wav.
     * The "Release" marker designates the end of the sustain (start of the release),
     * it is used as the loop end.
     */
    private Optional<Long> findMarkerPosition(RiffFileInfo info, String label) {
        List<RiffAdtlLabelChunk> labels = info.collectChunks(Chunks.ADTL_LABEL);
        List<RiffCueChunk> cueChunks = info.collectChunks(Chunks.CUE);
        for (RiffAdtlLabelChunk labelChunk : labels) {
            if (label.equals(labelChunk.getCuePointLabel()
                    .label())) {
                int identifier = labelChunk.getCuePointLabel()
                        .dwIdentifier();
                return cueChunks.stream()
                        .flatMap(cue -> cue.getCuePoints()
                                .stream())
                        .filter(cuePoint -> cuePoint.identifier() == identifier)
                        .map(cuePoint -> (long) cuePoint.sampleOffset())
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private void copyWavWithLoopMarkers(File source, File target, LoopSetting loop) throws Exception {
        PCMFormat format;
        try (RiffReader riffReader = new RiffReader(source, false)) {
            RiffFileInfo info = riffReader.parse();
            format = info.getAudioInfo()
                    .toPCMFormat();
            try (RiffWriter writer = new RiffWriter(target)) {
                writer.writeFmtChunk(format);
                writer.beginChunk(Chunks.DATA);
                RiffInspector inspector = new RiffInspector(riffReader, info);
                inspector.inspect(buffer -> {
                    try {
                        for (int s = 0; s < buffer.nbSamples(); s++) {
                            for (int ch = 0; ch < buffer.nbChannels(); ch++) {
                                short sample = (short) Math.clamp(buffer.sample(ch, s) * Short.MAX_VALUE, -32768, 32767);
                                writer.writeShortLE(sample);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                writer.endChunk();
                List<PCMMarker> markers = new ArrayList<>(readAllMarkers(info));
                markers.add(new PCMMarker("loopStart", loop.getSampleStart()));
                markers.add(new PCMMarker("loopEnd", loop.getSampleEnd()));
                writer.writeMarkers(markers);
            }
        }
    }

    /**
     * Reconstruct all markers (CUE + ADTL labels) of the source wav so they are
     * preserved when copying the samples.
     */
    private List<PCMMarker> readAllMarkers(RiffFileInfo info) {
        List<RiffCueChunk> cueChunks = info.collectChunks(Chunks.CUE);
        List<RiffAdtlLabelChunk> labels = info.collectChunks(Chunks.ADTL_LABEL);
        List<PCMMarker> markers = new ArrayList<>();
        for (RiffAdtlLabelChunk labelChunk : labels) {
            int identifier = labelChunk.getCuePointLabel()
                    .dwIdentifier();
            String label = labelChunk.getCuePointLabel()
                    .label();
            cueChunks.stream()
                    .flatMap(cue -> cue.getCuePoints()
                            .stream())
                    .filter(cuePoint -> cuePoint.identifier() == identifier)
                    .map(cuePoint -> new PCMMarker(label, cuePoint.sampleOffset()))
                    .findFirst()
                    .ifPresent(markers::add);
        }
        return markers;
    }

    @Test
    void detectLoopOnRealWav() throws Exception {
        // GIVEN the recorded note wav with a "Release" marker
        File wav = new File(TEST_WAV);
        assertTrue(wav.exists(), "Missing test wav: " + TEST_WAV);

        long releasePosition;
        PCMFormat format;
        try (RiffReader riffReader = new RiffReader(wav, false)) {
            RiffFileInfo info = riffReader.parse();
            format = info.getAudioInfo()
                    .toPCMFormat();
            // The "Release" marker is the end of the sustain. If it is absent,
            // fall back to the very end of the file.
            releasePosition = findMarkerPosition(info, "Release")
                    .orElseGet(() -> {
                        log.warn("No 'Release' marker found, using end of file as sustain end");
                        return (long) info.getAudioInfo()
                                .getNbSamples();
                    });
        }
        log.info("Release marker at sample {}", releasePosition);

        // WHEN the loop is detected on the sustain (which ends at the release marker)
        LoopDetectionContext context = LoopDetectionContext.builder()
                .sampleRate(format.getSampleRate())
                .nbChannels(format.getNbChannels())
                .wavFile(wav)
                .noteOffSampleMarker(releasePosition)
                .build();
        LoopSetting loop = new AutocorrelationLoopDetector().detectLoop(context);

        // THEN a loop within the sustain is found and written for observation
        assertNotNull(loop, "A loop should have been detected on the sustained note");
        assertTrue(loop.getSampleStart() >= 0, "Loop start must be within the sustain");
        assertTrue(loop.getSampleEnd() <= releasePosition, "Loop end must not exceed the release marker");
        assertTrue(loop.getSampleStart() < loop.getSampleEnd(), "Loop must be at least one period long");

        // WARN: the loop must normally be found before the release. If it ends at the
        // release marker, the detection probably failed to find the period.
        if (loop.getSampleEnd() == releasePosition) {
            log.warn("Loop end equals the release marker: loop likely not found before the release");
        }

        // Save the wav with loop markers so the result can be inspected in a WAV editor
        File output = new File(OUTPUT_DIR, "loop-detected.wav");
        output.getParentFile()
                .mkdirs();
        copyWavWithLoopMarkers(wav, output, loop);
        log.info("Loop detected: start={} end={} ({} samples)", loop.getSampleStart(), loop.getSampleEnd(), loop.getSampleEnd() - loop.getSampleStart());
        log.info("Written for inspection: {}", output.getAbsolutePath());
    }
}
