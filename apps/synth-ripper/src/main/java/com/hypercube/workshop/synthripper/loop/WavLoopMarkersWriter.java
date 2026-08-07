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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a WAV file combining the samples of a source wav with a set of markers.
 * Used after loop detection to embed the {@code loopStart}/{@code loopEnd} markers
 * (plus any pre-existing markers) into the final recorded sample.
 */
@Slf4j
public final class WavLoopMarkersWriter {

    private WavLoopMarkersWriter() {
    }

    /**
     * Rewrite {@code source} to {@code target}, preserving all existing markers and
     * adding {@code loopStart} / {@code loopEnd} markers for the given loop.
     *
     * <p>{@code source} and {@code target} may point to the same file: in that case
     * the rewrite goes through a temporary file which is atomically moved over
     * {@code target} once complete, so the original samples are never truncated
     * before they have been read.
     */
    public static void copyWavWithLoopMarkers(File source, File target, PCMMarker... extraMarkers) throws IOException {
        boolean inPlace = source.equals(target);
        File targetFile = inPlace
                ? File.createTempFile(target.getName() + ".", ".tmp", target.getParentFile())
                : target;
        try {
            writeWav(source, targetFile, extraMarkers);
            if (inPlace) {
                Files.move(targetFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (inPlace && targetFile.exists()) {
                targetFile.delete();
            }
        }
    }

    private static void writeWav(File source, File target, PCMMarker... extraMarkers) throws IOException {
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
                for (PCMMarker marker : extraMarkers) {
                    markers.add(marker);
                }
                writer.writeMarkers(markers);
            }
        }
    }

    /**
     * Reconstruct all markers (CUE + ADTL labels) of a source wav so they are
     * preserved when rewriting the samples.
     */
    private static List<PCMMarker> readAllMarkers(RiffFileInfo info) {
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
}