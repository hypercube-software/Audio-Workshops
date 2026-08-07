package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMMarker;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl.RiffAdtlLabelChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue.RiffCueChunk;
import com.hypercube.workshop.synthripper.model.LoopSetting;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
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
        WavLoopMarkersWriter.copyWavWithLoopMarkers(source, target,
                new PCMMarker("loopStart", loop.getSampleStart()),
                new PCMMarker("loopEnd", loop.getSampleEnd()));
    }

    @ParameterizedTest
    @EnumSource(LoopDetectorType.class)
    void detectLoopOnRealWav(LoopDetectorType type) throws Exception {
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

        LoopSetting loop = type.create(true)
                .detectLoop(context);

        // THEN a loop within the sustain is found and written for observation
        assertNotNull(loop, type + ": a loop should have been detected on the sustained note");
        assertTrue(loop.getSampleStart() >= 0, type + ": loop start must be within the sustain");
        assertTrue(loop.getSampleEnd() <= releasePosition, type + ": loop end must not exceed the release marker");
        assertTrue(loop.getSampleStart() < loop.getSampleEnd(), type + ": loop must be at least one period long");

        log.info("{}: loop start={} end={} ({} samples)", type, loop.getSampleStart(), loop.getSampleEnd(),
                loop.getSampleEnd() - loop.getSampleStart());

        // Save the wav with loop markers so the result can be inspected in a WAV editor.
        // One file per detector so several algorithms can be compared side by side.
        String baseName = wav.getName().replaceAll("(?i)\\.wav$", "");
        File output = new File(OUTPUT_DIR, baseName + "-" + type + ".wav");
        output.getParentFile()
                .mkdirs();
        copyWavWithLoopMarkers(wav, output, loop);
        log.info("Written for inspection: {}", output.getAbsolutePath());
    }
}
