package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.meta.AudioMetadata;
import com.hypercube.workshop.audioworkshop.files.meta.Instrument;
import com.hypercube.workshop.audioworkshop.files.meta.Version;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffInfoChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffListChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffPoolTableChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffRegionHeaderChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffWaveLinkChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.gig.RiffG3DimensionChunk;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Stream;

@Getter
@Slf4j
public class RiffFileInfo {
    @Setter
    private String filename;
    @Setter
    private byte[] prolog;
    @Setter
    private byte[] epilog;
    @Setter
    private Version version = new Version(0, 0, 0, 0);
    private final List<RiffChunk> chunks = new ArrayList<>();
    private final RiffAudioInfo audioInfo = new RiffAudioInfo();
    private final List<RiffAudioInfo> files = new ArrayList<>(); // DLS2 files
    private List<Instrument> instruments = new ArrayList<>(); // DLS2 files
    private final AudioMetadata metadata = new AudioMetadata();


    public boolean isPCM() {
        RiffAudioInfo currentAudioInfo = getAudioInfo();
        UUID subCodec = currentAudioInfo.getSubCodec();
        return currentAudioInfo.getCodec() == WaveCodecs.PCM ||
                (currentAudioInfo.getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE
                        && WaveGUIDCodecs.PCM_CODECS.contains(subCodec)
                );
    }

    public boolean isIEEE754FLOAT() {
        RiffAudioInfo currentAudioInfo = getAudioInfo();
        return currentAudioInfo.getCodec() == WaveCodecs.IEEE754_FLOAT ||
                (currentAudioInfo.getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE
                        && currentAudioInfo.getSubCodec()
                        .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_IEEE754_LE_FLOAT)
                );
    }

    public void addChunk(RiffChunk chunk) {
        chunks.add(chunk);
    }

    public List<RiffChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    public RiffChunk getDataChunk() {
        // WAV files: data
        // AIFF files: SSND
        return getChunk("data").orElse(getChunk("SSND").orElse(null));
    }

    public List<RiffChunk> getListChunks() {
        return getChunks("LIST");
    }

    public Optional<RiffChunk> getChunk(String chunkId) {
        assert (chunkId.length() == 4);
        return chunks.stream()
                .filter(c -> c.getId()
                        .equals(chunkId) || (c instanceof RiffListChunk && ((RiffListChunk) c).getListType()
                        .equals(chunkId)))
                .findFirst();

    }

    public List<RiffChunk> getChunks(String chunkId) {
        assert (chunkId.length() == 4);
        return chunks.stream()
                .filter(c -> c.getId()
                        .equals(chunkId))
                .toList();
    }

    public void collectInstruments() {
        Instrument parent = null;
        List<Instrument> collectedInstruments = new ArrayList<>();
        Map<Long, RiffAudioInfo> samplePool = new HashMap<>();
        files.forEach(f -> {
            long start = f.getFmtChunk()
                    .getParent()
                    .getContentStart() - 12L; // beginning of wvpl chunk
            long offset = start - f.getFmtChunk()
                    .getParent()
                    .getParent()
                    .getContentStart();

            samplePool.put(offset, f);
        });
        for (RiffChunk insChunk : getChunk(Chunks.LINS).map(lins -> lins.getChunks(Chunks.INS))
                .orElse(List.of())) {
            String instrumentName = insChunk.getChunk(Chunks.INFO)
                    .flatMap(info -> info.getChunk(Chunks.INAM)
                            .map(g -> ((RiffInfoChunk) g).getValue()))
                    .orElseThrow();
            var poolOffsets = getSampleOffsets(insChunk)
                    .orElse(List.of());
            log.info(instrumentName + " uses " + poolOffsets.size() + " samples");
            var instrumentSamples = poolOffsets.stream()
                    .sorted()
                    .map(sampleOffset -> {
                        var file = Optional.ofNullable(samplePool.get(sampleOffset));
                        if (file.isEmpty()) {
                            log.error("Illegal offset in ins Chunk %s, sample not found %X".formatted(instrumentName, sampleOffset));
                        }
                        return file;
                    })
                    .flatMap(Optional::stream)
                    .toList();

            instrumentSamples.forEach(s -> s.setUsed(true));
            if (instrumentSamples.isEmpty()) {
                parent = new Instrument(instrumentName, "", instrumentSamples);
            } else {
                String path = parent == null ? "" : parent.name();
                collectedInstruments.add(new Instrument(instrumentName, path, instrumentSamples));
            }
        }
        instruments = collectedInstruments;
    }

    /**
     * Given an "ins" chunk, retrieve the list of pool offsets
     */
    private Optional<List<Long>> getSampleOffsets(RiffChunk instrument) {
        return instrument.getChunk(Chunks.LRGN)
                .map(lrgn -> {
                    var r1 = lrgn.getChunks(Chunks.RGN)
                            .stream()
                            .flatMap(this::getSampleOffset)
                            .flatMap(Optional::stream);
                    var r2 = lrgn.getChunks(Chunks.RGN2)
                            .stream()
                            .flatMap(this::getSampleOffset)
                            .flatMap(Optional::stream);

                    return Stream.concat(r1, r2)
                            .distinct()
                            .sorted()
                            .toList();
                });
    }

    private Stream<Optional<Long>> getSampleOffset(RiffChunk rgn) {
        var header = (RiffRegionHeaderChunk) rgn.getChunk(Chunks.RGNH)
                .orElseThrow();
        var link = (RiffWaveLinkChunk) rgn.getChunk(Chunks.WLNK)
                .orElseThrow();
        var g3links = rgn.getChunk(Chunks.G3_DIMENSIONS)
                .map(dim -> ((RiffG3DimensionChunk) dim).getDimensions())
                .orElse(List.of());
        /*{
            log.info(header.toString());
            log.info("\tdsl sample index: %d %X".formatted(link.getSampleIndex(), getSampleOffset(link.getSampleIndex()).orElse(-1L)));
            g3links.forEach(l -> log.info("\tgig sample index: %d %X".formatted(l.sampleIndex(), getSampleOffset(l.sampleIndex()).orElse(-1L))));
        }*/
        return Stream.concat(Stream.of(getSampleOffset(link.getSampleIndex())), g3links.stream()
                .filter(dim -> dim.sampleIndex() != -1)
                .map(dim -> getSampleOffset(dim.sampleIndex())));
    }

    private Optional<Long> getSampleOffset(int sampleOffsetIndex) {
        if (sampleOffsetIndex == -1)
            return Optional.empty();
        return getChunk(Chunks.PTBL).map(ptbl -> ((RiffPoolTableChunk) ptbl).getSampleOffsets()
                .get(sampleOffsetIndex));
    }
}
