package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMMarker;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl.RiffAdtlLabelChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue.RiffCueChunk;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RiffWriterTest {
    @Test
    void writeMarkers() throws IOException {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        File file = new File("target/markers.wav");
        PCMMarker expectedLabel1 = new PCMMarker("label1", format.millisecondsToSamples(1));
        PCMMarker expectedLabel2 = new PCMMarker("label2", format.millisecondsToSamples(25));
        try (RiffWriter riffWriter = new RiffWriter(file)) {
            riffWriter.writeFmtChunk(format);
            riffWriter.beginChunk(Chunks.DATA);
            for (int i = 0; i < format.millisecondsToSamples(50); i++) {
                riffWriter.writeShortLE(0);
            }
            riffWriter.endChunk();
            riffWriter.writeMarkers(List.of(expectedLabel1, expectedLabel2));
        }
        try (RiffReader riffReader = new RiffReader(file, false)) {
            var info = riffReader.parse();
            List<RiffAdtlLabelChunk> labels = info.collectChunks(Chunks.ADTL_LABEL);
            List<RiffCueChunk> cue = info.collectChunks(Chunks.CUE);
            assert (labels.size() == 2);
            assert (cue.size() == 1);
            assertEquals(expectedLabel1.label(), labels.get(0)
                    .getCuePointLabel()
                    .label());
            assertEquals(expectedLabel2.label(), labels.get(1)
                    .getCuePointLabel()
                    .label());
            assertEquals(expectedLabel1.samplePosition(), cue.getFirst()
                    .getCuePoints()
                    .get(0)
                    .sampleOffset());
            assertEquals(expectedLabel2.samplePosition(), cue.getFirst()
                    .getCuePoints()
                    .get(1)
                    .sampleOffset());
        }
    }
}
