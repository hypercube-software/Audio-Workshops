package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import com.hypercube.workshop.audioworkshop.files.exceptions.AudioParserException;
import com.hypercube.workshop.audioworkshop.files.exceptions.IncorrectRiffChunkParentSize;
import com.hypercube.workshop.audioworkshop.files.exceptions.SampleCountException;
import com.hypercube.workshop.audioworkshop.files.exceptions.UnexpectedNullChunk;
import com.hypercube.workshop.audioworkshop.files.id3.ID3Parser;
import com.hypercube.workshop.audioworkshop.files.io.PositionalReadWriteStream;
import com.hypercube.workshop.audioworkshop.files.meta.MetadataField;
import com.hypercube.workshop.audioworkshop.files.meta.Version;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.*;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffPoolTableChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffRegionHeaderChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2.RiffWaveLinkChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.gig.G3Dimension;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.gig.RiffG3DimensionChunk;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

/**
 * This class is able to parse various RIFF-based formats like WAV, AIFF, DSL2 or Gigastudio
 * <br><br>
 * This parser load the entire WAV/GIG in memory because a memory mapped FileChannel does not work properly on windows.
 * This bug in the JVM is UNFIXABLE, see <a href="https://bugs.openjdk.org/browse/JDK-4715154">this ticket</a>
 * <br><br>
 * Parsing a RIFF is easy but you have to be very careful about word alignment of every Chunks.
 * <p>- if a chunk ID is at position 3, there is a padding byte before the next one
 * <p>- unfortunately a lot of editors generate non-padded RIFF which are out of spec
 * <br><br>
 * ACID chunks:
 * <pre>
 * acid: header containing the number of beats
 * strc: stretch info
 * str2: stretch info 2
 * bmrk: beat markers
 * dtbt: detected beats
 * </pre>
 * DSL2 chunks:
 * <pre>
 * ins: instrument
 * wvpl: wave pool
 * rgn : region v1
 * rgn2: region v2
 * rgnh: region header
 * wlnk: link to a sample in the pool
 * </pre>
 * Gigastudio chunks:
 * <pre>
 * 3lnk: link to a sample in the pool
 * 3ewa: effect chunk
 * rgn : region v1
 * rgn2: region v2
 * </pre>
 * Others:
 * <pre>
 * JUNK: list of something
 * </pre>
 * <br><br>
 *
 * @see this study on the mess around metadata: <a href="https://www.arsc-audio.org/pdf/ARSC_TC_MD_Study.pdf">here</a>
 */
@SuppressWarnings({"java:S1172", "unused"})
@Slf4j
public class RiffReader {
    public static final List<String> KNOWN_TYPES = List.of("WAVE", "AIFF", "AIFC", "DLS ");
    public static final int LOWEST_TEMPO = 40;
    public static final int HIGHEST_TEMPO = 300;
    public static final String AIFF_TYPE_AIFC = "AIFC";
    public static final String RIFF_TYPE_AIFF = "AIFF";
    public static final String AIFF_TYPE_DLS = "DLS "; // include Gigastudio
    private final File srcAudio;
    private final RiffFileInfo fileInfo = new RiffFileInfo();

    private final List<RiffAudioInfo> audioInfos = new ArrayList<>();

    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    private final boolean canFixSource;
    private int misalignedChunksCount = 0;

    private PositionalReadWriteStream stream = null;

    private boolean isAIFF;

    private boolean isAFIC;

    private boolean isDLS2;

    /**
     * @param srcAudio     the WAV file to parse
     * @param canFixSource true will allow modifying the size of the chunk if wrong in checkSampleCount
     */
    public RiffReader(File srcAudio, boolean canFixSource) {
        super();
        this.srcAudio = srcAudio;
        this.canFixSource = canFixSource;
    }

    /**
     * I found typedstream data appended at the end of the RIFF !
     * Because of that the parser endup reading an illegal ChunkID
     * more info on typedstreams: <a href="https://github.com/dgelessus/python-typedstream">...</a>
     *
     * @param data a 4 bytes chunk ID
     * @return false is it is not a legal id
     */
    private boolean checkChunkID(byte[] data) throws IOException {
        int nbIllegalChars = 0;
        if (data[0] == 0 && data[1] == 0 && data[2] == 0 && data[3] == 0) {
            throw new UnexpectedNullChunk(srcAudio.getAbsolutePath(), stream.positionUInt() - 4);
        }
        for (int ch : data) {
            if (!(ch == ' ' || ch == '_' || ch == '-' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))) {
                nbIllegalChars++;
            }
        }
        boolean valid = (nbIllegalChars == 0);
        if (!valid) {
            log.warn("Invalid chunk at 0x%X, give up parsing of %s".formatted(stream.positionUInt() - 4, srcAudio.getAbsolutePath()));
        }
        return valid;
    }

    /**
     * move to the next word aligned position then read the chunk ID
     * NOTE: if the RIFF does not use alignement (so it is out of spec), we take this into account
     *
     * @return null if the chunk is invalid or the end of stream is reached
     */
    private String readChunkID() throws IOException {
        long pos = stream.positionLong();
        if (isEndOfStream()) return null;
        wordAlign();
        if (isEndOfStream()) return null;
        try {
            byte[] name = stream.readNBytes(4);
            if (!checkChunkID(name)) {
                return null;
            }

            return new String(name, StandardCharsets.US_ASCII);
        } catch (EOFException e) {
            log.warn(String.format("Unexpected EOF at %X in %s", pos, srcAudio.getAbsolutePath()));
            return null;
        }
    }

    @SuppressWarnings("java:S2093")
    public RiffFileInfo parse() {

        if (!srcAudio.exists())
            throw new AudioParserException("File does not exists: " + srcAudio.getAbsolutePath());
        if (srcAudio.length() == 0)
            throw new AudioParserException("File is empty: " + srcAudio.getAbsolutePath());

        log.trace("-------------");
        log.trace("Parse " + srcAudio.getAbsolutePath());
        try {

            stream = new PositionalReadWriteStream(srcAudio, canFixSource);

            String riff = readChunkID();
            if (!"RIFF".equals(riff) && !"FORM".equals(riff)) {
                throw new AudioParserException("not a RIFF file");
            }
            long size = "FORM".equals(riff) ? stream.getUIntBE() : stream.getUIntLE();
            long expectedTotalSize = size + 8;
            if (srcAudio.length() != expectedTotalSize) {
                long delta = srcAudio.length() - expectedTotalSize;
                log.warn(String.format("RIFF file size 0x%X does not match RIFF size 0x%X delta: %d bytes %s",
                        srcAudio.length(),
                        expectedTotalSize,
                        delta,
                        srcAudio.getAbsolutePath()));
            }
            String type = readChunkID();

            if (!KNOWN_TYPES.contains(type)) {
                throw new AudioParserException("not a WAVE/AIFF file " + srcAudio.getAbsolutePath());
            }
            isAFIC = AIFF_TYPE_AIFC.equals(type);
            isAIFF = RIFF_TYPE_AIFF.equals(type) || isAFIC;
            isDLS2 = AIFF_TYPE_DLS.equals(type);

            try {
                for (; ; ) {
                    RiffChunk c = readChunk(null);
                    if (c == null) break;
                    fileInfo.addChunk(c);
                }
            } catch (UnexpectedNullChunk e) {
                log.warn(e.toString());
            }
            if (misalignedChunksCount > 0) {
                log.trace("Illegal RIFF: %d misaligned chunks in %s".formatted(misalignedChunksCount, srcAudio.getAbsolutePath()));
            }
            if (!isDLS2) {
                fileInfo.getAudioInfo()
                        .computeDuration();
                checkSampleCount();
                storeMetadataTempo();
                storeMetadataKey();
                storeNonAudioData(srcAudio);
            } else {
                audioInfos.forEach(RiffAudioInfo::computeDuration);
                fileInfo.getFiles()
                        .addAll(audioInfos);
                fileInfo.collectInstruments();
            }
            return fileInfo;
        } catch (Exception e) {
            log.error("Unexpected error parsing " + srcAudio.getAbsolutePath(), e);
            return null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Unexpected error parsing " + srcAudio.getAbsolutePath(), e);
                }
            }
        }
    }

    private void storeMetadataKey() {
        if (fileInfo.getAudioInfo()
                .getKey() != null) {
            fileInfo.getMetadata()
                    .put(MetadataField.KEY, fileInfo.getAudioInfo()
                            .getKey());
        }
    }

    private void storeMetadataTempo() {
        var currentFileInfo = this.fileInfo.getAudioInfo();
        DecimalFormat df = new DecimalFormat("#.#");
        if (currentFileInfo.getNbBeats() != 0 && currentFileInfo.getTempo() == 0) {
            float beatDuration = currentFileInfo.getDuration() / currentFileInfo.getNbBeats();
            float tempo = 60f / beatDuration;
            currentFileInfo.setTempo(tempo);
            this.fileInfo.getMetadata()
                    .put(MetadataField.BPM, df.format(tempo));
        } else if (currentFileInfo.getTempo() == 0) {
            String name = srcAudio.getName();
            Matcher m = CachedRegExp.get("[0-9]+", name);
            List<Float> numbers = new ArrayList<>();
            while (m.find()) {
                float tempo = Float.parseFloat(m.group());
                numbers.add(tempo);
            }
            numbers.stream()
                    .filter(n -> n >= LOWEST_TEMPO && n <= HIGHEST_TEMPO)
                    .max(Float::compare)
                    .ifPresent(tempo -> {
                        currentFileInfo.setTempo(tempo);
                        this.fileInfo.getMetadata()
                                .put(MetadataField.BPM, df.format(tempo));
                    });
        }
    }

    /**
     * Allow the possibility to reconstruct the WAV file, saving everything around the PCM data
     */
    private void storeNonAudioData(File srcAudio) throws IOException {
        var data = fileInfo.getDataChunk();
        // if file ends at byte 10 (so stream.capacity() = 11),
        // the data content starts at byte 3
        // the data content ends   at byte 5
        // then the prolog size is         3
        // then the epilog size is         10-5
        //
        // so the prolog size is data.getContentStart()
        // so the epilog size must be stream.capacity() - 1 - data.getContentEnd()
        //                   equiv to stream.capacity() - data.getChunkEnd()
        int prologSize = data.getContentStart();
        int epilogSize = (stream.capacity() - 1 - data.getContentEnd());
        if (epilogSize < 0) {
            log.warn("no epilog...");
            epilogSize = 0;
        }
        stream.seekUInt(0);
        byte[] prolog = stream.readNBytes(prologSize);
        byte[] epilog = new byte[0];
        if (epilogSize > 0) {
            stream.seekUInt(data.getChunkEnd());
            epilog = stream.readNBytes(epilogSize);
        }

        fileInfo.setFilename(srcAudio.getName());
        fileInfo.setProlog(compress(prolog));
        fileInfo.setEpilog(compress(epilog));
    }

    /**
     * Read a chunk and jump at its end
     */
    private RiffChunk readChunk(RiffChunk parent) throws IOException {
        String chunkId = readChunkID();
        if (chunkId == null) return null;
        final int contentSize = isAIFF ? stream.getIntBE() : stream.getIntLE();
        final int contentStart = stream.positionUInt();
        final int end = contentStart + contentSize;
        log.trace(String.format("CHUNK %s : %d/0x%X bytes at 0x%X end 0x%X", chunkId, contentSize, contentSize, contentStart - 8, end));
        final RiffChunk riffChunk = readChunk(parent, chunkId, contentStart, contentSize);
        // get ready to read the next one
        moveAfterChunk(riffChunk);
        return riffChunk;
    }

    private RiffChunk readChunk(RiffChunk parent, String chunkId, int contentStart, int contentSize) throws IOException {
        RiffChunk riffChunk;
        switch (chunkId) {
            case Chunks.LIST -> riffChunk = readLIST(parent, chunkId, contentStart, contentSize);
            case Chunks.WLNK -> riffChunk = readWaveLink(parent, chunkId, contentStart, contentSize);
            case Chunks.RGNH -> riffChunk = readRegionHeader(parent, chunkId, contentStart, contentSize);
            case Chunks.PTBL -> riffChunk = readPoolTable(parent, chunkId, contentStart, contentSize);
            case Chunks.G3_DIMENSIONS -> riffChunk = readGigDimensions(parent, chunkId, contentStart, contentSize);
            default -> {
                riffChunk = new RiffChunk(parent, chunkId, contentStart, contentSize);
                switch (chunkId) {
                    case Chunks.VERS -> readVERS(riffChunk);
                    case Chunks.DATA -> readDATA(riffChunk);
                    case Chunks.ACID -> readACID(riffChunk);
                    case Chunks.FORMAT -> readFMT(riffChunk);
                    case Chunks.CUE -> readCUE(riffChunk);
                    case Chunks.BEXT -> readBEXT(riffChunk);
                    case Chunks.UMID -> readUMID(riffChunk);
                    case Chunks.IXML -> readIXML(riffChunk);
                    case Chunks.XMP -> readXMP(riffChunk);
                    case Chunks.ID3_UPPERCASE, Chunks.ID3_LOWERCASE -> readID3(riffChunk);
                    case Chunks.AIFF_FORMAT -> readCOMM(riffChunk); // AIFF
                    case Chunks.AIFF_SSND -> readSSND(riffChunk); // AIFF
                    case Chunks.AIFF_DESCRIPTION -> readTEXT(riffChunk, MetadataField.DESCRIPTION); // AIFF
                    case Chunks.COPYRIGHT -> readTEXT(riffChunk, MetadataField.COPYRIGHT); // AIFF
                    case Chunks.AUTHOR -> readTEXT(riffChunk, MetadataField.AUTHOR); // AIFF
                    case Chunks.NAME -> readTEXT(riffChunk, MetadataField.NAME); // AIFF
                    default -> {
                        //do nothing
                    }
                }
                // SMED = Opaque Soundminer Metawrapper data
                // LGWV = Logic Prox Wav
                // ResU = Logic Pro X
                // AFAn = Apple Binary plist serialized
                // minf = ProTools chunk
                // elm1 = ProTools chunk
                // regn = ProTools chunk (maybe)
                // umid = BWF version 1, Unique Material Identifier, see https://fr.wikipedia.org/wiki/Broadcast_Wave_Format
                // CDif = Sound Forge 10 chunk (maybe)

                // some data contains Apple serialized data in TypedStream format (NextStep)
                // see https://gist.github.com/williballenthin/600a3898f43b7ad3f8aa4a5f4156941d
            }
        }
        return riffChunk;
    }

    private void readVERS(RiffChunk riffChunk) throws IOException {
        int release = stream.getByte();
        int build = stream.getByte();
        int major = stream.getByte();
        int minor = stream.getByte();
        fileInfo.setVersion(new Version(major, minor, release, build));
    }

    /**
     * Inspired by <a href="https://www.linuxsampler.org/libgig/">liggig</a>
     */
    private RiffChunk readGigDimensions(RiffChunk parent, String chunkId, int contentStart, int contentSize) throws IOException {
        int nbDimensions = stream.getIntLE();
        List<G3Dimension> dimensions = new ArrayList<>();
        if (fileInfo.getVersion()
                .major() > 2) {
            stream.seekLong(contentStart + 68L);
        } else {
            stream.seekLong(contentStart + 44L);
        }
        // read the wave pool indices table
        int nb = (int) (contentSize - (stream.positionLong() - contentStart)) / 4;
        // nb should be 32 for gigastudio 2 and 256 for gigastudio 3
        for (int i = 0; i < nbDimensions; i++) {
            int sampleIndex = stream.getIntLE();
            dimensions.add(new G3Dimension(0, 0, 0, 0, 0, sampleIndex));
        }
        return new RiffG3DimensionChunk(parent, chunkId, contentStart, contentSize, dimensions);
    }

    private RiffChunk readRegionHeader(RiffChunk parent, String chunkId, int contentStart, int contentSize) throws IOException {
        short keyRangeLow = stream.getShortLE();
        short keyRangeHigh = stream.getShortLE();
        short velocityRangeLow = stream.getShortLE();
        short velocityRangeHigh = stream.getShortLE();
        short fusOptions = stream.getShortLE();
        short usKeyGroup = stream.getShortLE();
        short usLayer = stream.positionLong() - contentStart == contentSize ? 0 : stream.getShortLE();
        return new RiffRegionHeaderChunk(parent, chunkId, contentStart, contentSize,
                new RiffRegionHeaderChunk.Range(keyRangeLow, keyRangeHigh),
                new RiffRegionHeaderChunk.Range(velocityRangeLow, velocityRangeHigh),
                fusOptions, usKeyGroup, usLayer);
    }

    private RiffPoolTableChunk readPoolTable(RiffChunk parent, String chunkId, int contentStart, int contentSize) throws IOException {
        int cbSize = stream.getIntLE();
        int nbCues = stream.getIntLE();
        if (cbSize != 8) {
            log.warn("Unexpected struct size for ptbl:" + cbSize);
        }
        boolean offset64bits = contentSize - cbSize == nbCues * 8;
        log.info("Wave Pool Table contains {} entries", nbCues);
        List<Long> offsets = extractOffsets(nbCues, offset64bits);
        return new RiffPoolTableChunk(parent, chunkId, contentStart, contentSize, offsets);
    }

    private List<Long> extractOffsets(int nbCues, boolean offset64bits) throws IOException {
        List<Long> offsets = new ArrayList<>();
        for (int i = 0; i < nbCues; i++) {
            // ulOffset is the offset in bytes starting from the "wvpl" chunk
            // it points to a "wave" chunk
            if (offset64bits) {
                long hi = stream.getIntLE();
                long lo = stream.getIntLE();
                long offset = (hi << 32) + lo;
                offsets.add(offset);
            } else {
                offsets.add((long) stream.getIntLE());
            }
        }
        return offsets;
    }

    private RiffWaveLinkChunk readWaveLink(RiffChunk parent, String id, int contentStart, int contentSize) throws IOException {
        short fusOptions = stream.getShortLE();
        short usPhaseGroup = stream.getShortLE();
        int ulChannel = stream.getIntLE();
        int ulTableIndex = stream.getIntLE();
        return new RiffWaveLinkChunk(parent, id, contentStart, contentSize, fusOptions, usPhaseGroup, ulChannel, ulTableIndex);
    }

    private void readTEXT(RiffChunk c, MetadataField metadataField) throws IOException {
        byte[] data = new byte[c.getContentSize()];
        stream.read(data);
        if (data.length > 4 && data[0] == 'A' && data[1] == 'F' && data[2] == 's' && data[3] == 'p') {
            // Looks like binary plist format, this is not clear to me
            // https://en.wikipedia.org/wiki/Property_list
            // We have a list of "key: value" strings separated by 0x00
            String[] txt = new String(data, 4, data.length - 4, StandardCharsets.ISO_8859_1).split("%c".formatted((0)));
            for (String kv : txt) {
                String[] v = kv.split(":");
                String key = v[0].trim();
                String value = v[1].trim();
                switch (key) {
                    case "user" -> fileInfo.getMetadata()
                            .put(MetadataField.AUTHOR, value);
                    case "program" -> fileInfo.getMetadata()
                            .put(MetadataField.SOFTWARE, value);
                    case "date" -> fileInfo.getMetadata()
                            .put(MetadataField.CREATED, value);
                }
            }
        } else {
            String txt = new String(data, StandardCharsets.ISO_8859_1);
            fileInfo.getMetadata()
                    .put(metadataField, txt);
        }
    }

    /**
     * SSND chunk contains audio samples
     */
    private void readSSND(RiffChunk c) throws IOException {
        int offset = stream.getIntBE();
        int blockSize = stream.getIntBE();
        fileInfo.getAudioInfo()
                .setNbAudioBytes(c.getContentSize() - offset);
    }

    private void readCOMM(RiffChunk riffChunk) throws IOException {
        int numChannels = stream.getShortBE();
        int numSampleFrames = stream.getIntBE();
        int sampleSize = stream.getShortBE();
        double sampleRate = stream.getLongDoubleBE();
        if (isAFIC) {
            String compressionType = readChunkID();
            String compressionName = stream.getPascalString();
            // convert the AIFF codec id to WAVE codec id
            switch (compressionType) {
                case "NONE", "sowt" -> fileInfo.getAudioInfo()
                        .setCodec(WaveCodecs.PCM);
                case "fl32", "FL32", "fl64" -> fileInfo.getAudioInfo()
                        .setCodec(WaveCodecs.IEEE754_FLOAT);
                case "alaw", "ALAW" -> fileInfo.getAudioInfo()
                        .setCodec(WaveCodecs.ITU_G711_ALAW);
                case "ulaw", "ULAW" -> fileInfo.getAudioInfo()
                        .setCodec(WaveCodecs.ITU_G711_ULAW);
                case null -> log.warn("compression type is null");
                default -> fileInfo.getAudioInfo()
                        .setCodec(WaveCodecs.UNKNOWN);
            }
        } else {
            fileInfo.getAudioInfo()
                    .setCodec(WaveCodecs.PCM);
        }
        fileInfo.getAudioInfo()
                .setBitPerSample(sampleSize);
        fileInfo.getAudioInfo()
                .setSampleRate((int) sampleRate);
        fileInfo.getAudioInfo()
                .setNbChannels(numChannels);
        fileInfo.getAudioInfo()
                .setFrameSizeInBytes((sampleSize / 8) * numChannels);

    }

    private void moveAfterChunk(RiffChunk c) throws IOException {
        long end = (c.getChunkEnd()) & 0xFFFFFFFFL;
        long cap = (stream.capacity()) & 0xFFFFFFFFL;
        if (end < cap) {
            stream.seekUInt(c.getChunkEnd());
        } else {
            stream.seekUInt(stream.capacity());
        }
    }

    private boolean isEndOfStream() throws IOException {
        return stream.positionUInt() == stream.capacity();
    }

    private byte[] getChunkContent(String chunkId, int contentSize) throws IOException {
        try {
            return stream.readNBytes(contentSize);
        } catch (BufferUnderflowException e) {
            log.error("WAV file is truncated, can't read chunk %s in %s".formatted(chunkId, srcAudio.getAbsolutePath()));
            return new byte[0];
        }
    }

    private void readCUE(RiffChunk c) throws IOException {
        int nbCuePoints = stream.getIntLE();
        log.trace(String.format("CUE Points: %d entries", nbCuePoints));
        for (int i = 0; i < nbCuePoints; i++) {
            int id = stream.getIntLE();
            int dwPosition = stream.getIntLE();
            String fccChunk = readChunkID();
            int dwChunkStart = stream.getIntLE();
            int dwBlockStart = stream.getIntLE();
            int dwSampleOffset = stream.getIntLE();
            log.trace(String.format("CUE Point %d in chunk %s start: 0x%X dwSampleOffset: %d/0x%X", id, fccChunk, dwChunkStart, dwSampleOffset, dwSampleOffset));
        }
    }

    /**
     * this chunk contains audio samples
     */
    private void readDATA(RiffChunk c) {
        var currentFileInfo = getCurrentAudioInfo(c);
        currentFileInfo.setDataChunk(c);
        currentFileInfo.setNbAudioBytes(c.getContentSize());
    }


    /**
     * If the data chunk size is wrong, the FLAC encoder will raise the error "got
     * partial sample"
     *
     * @throws SampleCountException if the count is wrong
     */
    private void checkSampleCount() throws SampleCountException {
        if (!fileInfo.getAudioInfo()
                .getCodecString()
                .contains("PCM")) {
            return; // the data size depend on the compression, so we can't check anything
        }

        long dataChunkSize = (fileInfo.getAudioInfo()
                .getNbAudioBytes()) & 0xFFFFFFFFL;
        long partialSample = dataChunkSize % fileInfo.getAudioInfo()
                .getFrameSizeInBytes();
        if (partialSample != 0) {
            long expectedSize = dataChunkSize - partialSample + fileInfo.getAudioInfo()
                    .getFrameSizeInBytes();
            String errorMsg = String.format("DATA Chunk size mismatch (partial sample). Expected %d/0x%X, have %d/0x%X bytes", expectedSize, expectedSize, dataChunkSize, dataChunkSize);
            if (canFixSource) {
                log.warn(errorMsg);
                try {
                    fixDataChunkSize((int) expectedSize);
                } catch (IOException e) {
                    throw new SampleCountException(errorMsg);
                }
            } else {
                throw new SampleCountException(errorMsg);
            }
        }
    }

    /**
     * Modify the RIFF size.
     * The position is reset to 0 after this method
     *
     * @param expectedSize size to be injected in RIFF header
     * @throws IOException is srcAudio cannot be written
     */
    private void fixDataChunkSize(int expectedSize) throws IOException {
        log.warn("Fixing chunk size...");
        var data = fileInfo.getDataChunk();
        stream.seekUInt(data.getContentStart() - 4);
        stream.putIntLE(expectedSize);
        stream.seekUInt(0);
        fileInfo.getDataChunk()
                .setContentSize(expectedSize);
    }

    // https://mutagen-specs.readthedocs.io/en/latest/id3/id3v2.2.html#id3v2-overview
    private void readID3(RiffChunk c) throws IOException {
        byte[] data = new byte[c.getContentSize()];
        stream.read(data);
        ID3Parser id3 = new ID3Parser(data);
        var id3Info = id3.parse();
        if (fileInfo != null) {
            fileInfo.getMetadata()
                    .merge(id3Info.getMetadata());
        }
    }

    // https://www.adobe.com/products/xmp.html
    private void readXMP(RiffChunk c) {
        log.trace("XML data from Adobe’s Extensible Metadata Platform (XMP) ignored");
    }

    // http://www.gallery.co.uk/ixml/
    private void readIXML(RiffChunk c) throws IOException {
        String xml = readFixedASCIIStringAndClean(c.getContentSize(), true);
        extractIXMLAttributes(cleanupXML(xml));
    }

    private String cleanupXML(String xml) {
        return xml.replace(" & ", " &amp; ");
    }

    /**
     * Read the <STEINBERG> extension of iXML where we can found tempo
     *
     * @param xml iXML content
     * @see <a href="http://www.gallery.co.uk/ixml/">here</a>
     */
    @SuppressWarnings("java:S3776")
    private void extractIXMLAttributes(String xml) {
        // https://www.baeldung.com/java-stax
        // Personal note about StAX: this API is a disaster because:
        // - it does side effects width methods like "getElementText()": it moves the
        // read head !!!
        // - try to make a debug watch on getElementText() a prepare to die...
        // - it is confusing because of "getText" and "getElementText"
        // - XMLStreamReader is a reader and the current token at the same time
        // But... it is fast.
        try (ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes())) {
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(in);
            Map<String, String> currentAttribute = null;
            String currentTag = null;
            final String IXML_VALUE = "VALUE";
            final String IXML_ATTR = "ATTR";
            while (xmlReader.hasNext()) {
                var eventType = xmlReader.next();
                if (xmlReader.isStartElement()) {
                    currentTag = xmlReader.getLocalName();
                    if (currentTag.equals(IXML_ATTR)) {
                        currentAttribute = new HashMap<>();
                    }
                } else if (xmlReader.isCharacters()) {
                    if (currentAttribute != null && currentTag != null && !currentTag.equals(IXML_ATTR)) {
                        currentAttribute.put(currentTag, xmlReader.getText()
                                .replace("\n", " ")
                                .replace("\r", "")
                                .trim());
                    }
                } else if (xmlReader.isEndElement()) {
                    currentTag = null;
                    if (xmlReader.getLocalName()
                            .equals(IXML_ATTR) && currentAttribute != null) {
                        String name = currentAttribute.get("NAME");
                        if (name != null) {
                            if ("MusicalBeats".equals(name)) {
                                fileInfo.getAudioInfo()
                                        .setNbBeats(Integer.parseInt(currentAttribute.get(IXML_VALUE)));
                            }
                            if ("MusicalSignature".equals(name)) {
                                fileInfo.getAudioInfo()
                                        .setMeterDenominator(Integer.parseInt(currentAttribute.get("DENOMINATOR")));
                                fileInfo.getAudioInfo()
                                        .setMeterNumerator(Integer.parseInt(currentAttribute.get("NUMERATOR")));
                            }
                            if ("MusicalTempo".equals(name)) {
                                fileInfo.getAudioInfo()
                                        .setTempo(Float.parseFloat(currentAttribute.get(IXML_VALUE)));
                            }

                            // Not official
                            if ("MusicalKey".equals(name)) {
                                fileInfo.getAudioInfo()
                                        .setKey(currentAttribute.get(IXML_VALUE));
                            }
                        }
                        currentAttribute = null;
                    }
                }

            }
        } catch (Exception e) {
            log.error("unable to parse iXML in " + srcAudio.getAbsolutePath(), e);
            log.error(xml);
        }
    }

    // Broadcast WAV
    // https://www.loc.gov/preservation/digital/formats/fdd/fdd000356.shtml
    // https://tech.ebu.ch/docs/tech/tech3285.pdf

	/*-
	 * 
		typedef struct chunk_header {
			DWORD ckID; // (broadcastextension)ckID=bext 
			DWORD ckSize; // size of extension chunk 
			BYTE ckData[ckSize]; // data of the chunk 
		} CHUNK_HEADER;
		
		typedef struct broadcast_audio_extension {
			CHAR Description[256]; // ASCII: "Description of the sound sequence" 
			CHAR Originator[32]; // ASCII: "Name of the originator" 
			CHAR OriginatorReference[32]; // ASCII: "Reference of the originator" 
			CHAR OriginationDate[10]; // ASCII: "yyyy-mm-dd" 
			CHAR OriginationTime[8]; // ASCII: "hh:mm:ss" 
			DWORD TimeReferenceLow; // First sample count since midnight, low word 
			DWORD TimeReferenceHigh; // First sample count since midnight, high word 
			WORD Version; // Version of the BWF; unsigned binary number. See Annex G 
			BYTE UMID_0; // Binary byte 0 of SMPTE UMID 
			....
			BYTE UMID_63; // Binary byte 63 of SMPTE UMID 
			INT LoudnessValue; // Integrated Loudness Value of the file in LKFS (multiplied by 100) see Annex H 
			INT LoudnessRange; // Loudness Range of the file in LU (multiplied by 100), see Annex H 
			INT MaxTruePeakLevel; // Maximum True Peak Level of the file expressed as dBTP (multiplied by 100), see Annex H 
			INT MaxMomentaryLoudness; // Highest value of the Momentary Loudness Level of the file in LKFS (multiplied by 100), see Annex H 
			INT MaxShortTermLoudness; // Highest value of the Short-Term Loudness Level of the file in LKFS (multiplied by 100), see Annex H 
			BYTE Reserved[180]; // 180 bytes, reserved for future use, set to "NULL" 
			CHAR CodingHistory[]; // ASCII: "History coding "
		} BROADCAST_EXT
	 */

    /**
     * Broadcast WAV metadata
     */
    private void readBEXT(RiffChunk c) throws IOException {
        String description = cleanup(readFixedASCIIString(256));
        String originator = cleanup(readFixedASCIIString(32));
        String originatorReference = cleanup(readFixedASCIIString(32));
        String originationDate = readFixedASCIIString(10);
        String originationTime = readFixedASCIIString(8);
        int timeReferenceLow = stream.getIntLE();
        int timeReferenceHigh = stream.getIntLE();
        if (stream.positionUInt() == c.getChunkEnd())
            return; // version 0
        int version = stream.getShortLE();
        byte[] smpte = stream.readNBytes(64);
        if (stream.positionUInt() == c.getChunkEnd())
            return; // version 1
        int loudnessValue = stream.getShortLE();
        int loudnessRange = stream.getShortLE();
        int maxTruePeakLevel = stream.getShortLE();
        int maxMomentaryLoudness = stream.getShortLE();
        int maxShortTermLoudness = stream.getShortLE();
        byte[] reserved = stream.readNBytes(180);
        int stringSize = c.getChunkEnd() - stream.positionUInt();
        String history = readFixedASCIIString(stringSize);
        log.trace(String.format("Broadcast WAV Description         : %s", description));
        log.trace(String.format("Broadcast WAV originator          : %s", originator));
        log.trace(String.format("Broadcast WAV originatorReference : %s", originatorReference));
        log.trace(String.format("Broadcast WAV originationDate     : %s", originationDate));
        log.trace(String.format("Broadcast WAV originationTime     : %s", originationTime));
        fileInfo.getMetadata()
                .put(MetadataField.CREATED, originationDate);
        fileInfo.getMetadata()
                .put(MetadataField.VENDOR, originator);
        fileInfo.getMetadata()
                .put(MetadataField.DESCRIPTION, description);
        fileInfo.getMetadata()
                .put(MetadataField.COPYRIGHT, originatorReference);
    }

    private String cleanup(String value) {
        return value.replace("_", " ")
                .trim();
    }

    // https://en.wikipedia.org/wiki/Unique_Material_Identifier
    private void readUMID(RiffChunk c) {
        // do nothing
    }

    private String readFixedASCIIString(int size) throws IOException {
        return readFixedASCIIStringAndClean(size, false);
    }

    /**
     * I saw a lot of garbage in those despite the BWF Spec
     *
     * @param size  size of the string, we read exactly this amount of bytes
     * @param clean if true, we replace non visible characters, including 0 by
     *              space, otherwise trunc at the first 0
     * @return the trimmed string
     */
    private String readFixedASCIIStringAndClean(int size, boolean clean) throws IOException {
        byte[] data = stream.readNBytes(size);
        int l = data.length;
        for (int i = 0; i < l; i++) {
            if (clean) {
                if (data[i] < ' ' || data[i] > '~') {
                    data[i] = ' ';
                }
            } else {
                if (data[i] == 0) {
                    l = i;
                    break;
                }
            }
        }

        return new String(data, 0, l, StandardCharsets.US_ASCII).trim();
    }

    private RiffListChunk readLIST(RiffChunk parent, String id, int contentStart, int contentSize) throws IOException {
        String listType = readChunkID();
        assert (listType != null);
        RiffListChunk list = new RiffListChunk(parent, id, listType, contentStart, contentSize);
        log.trace("Read LIST " + listType);
        if (listType.equals("wave")) {
            audioInfos.add(new RiffAudioInfo());
        }
        var listOfList = List.of("wvpl", "wave", Chunks.LINS, Chunks.LRGN, "lart", Chunks.INS, Chunks.RGN, "lar2", "lar3", Chunks.RGN2);
        var listOfListGigastudio = List.of("3gri", "3gnl", "3dnl", "3prg", "3ewl", "3dnm");
        if (Chunks.INFO.equals(listType)) {
            readInfoSubChunks(list);
        } else if ("adtl".equals(listType)) {
            readAdtlSubChunks(list);
        } else if (listOfList.contains(listType)) {
            readLISTChildren(list);
        } else if (listOfListGigastudio.contains(listType)) {
            readLISTChildren(list);
        } else {
            log.warn("Unknown LIST type: " + listType);
        }
        return list;
    }

    private void readLISTChildren(RiffListChunk list) throws IOException {
        try {
            do {
                wordAlign();
                if (isAtEndOfChunk(list)) {
                    break;
                }
            }
            while (readChunk(list) != null);
        } catch (IncorrectRiffChunkParentSize e) {
            log.warn("Recover from bad LIST size");
            stream.seekLong(e.getContentStart() - 8);
        }
    }

    private boolean isAtEndOfChunk(RiffChunk c) throws IOException {
        return stream.positionUInt() == c.getChunkEnd();
    }

    // Associated data list chunk
    // https://www.recordingblogs.com/wiki/associated-data-list-chunk-of-a-wave-file
    @SuppressWarnings("java:S135")
    private void readAdtlSubChunks(RiffChunk c) throws IOException {
        for (; ; ) {
            wordAlign();
            if (isAtEndOfChunk(c)) {
                break;
            }
            String fieldID = readChunkID();
            if ("labl".equals(fieldID) || "note".equals(fieldID)) {
                int contentSize = stream.getIntLE();
                int contentStart = stream.positionUInt();
                int cuePointID = stream.getIntLE();
                String value = readFixedASCIIString(contentSize - 4); // value can be "Tempo: 160.0"
                log.trace(String.format("adt label for Cue Point %d %s: %s", cuePointID, fieldID, value));
                RiffAdtlLabelChunk subChunk = new RiffAdtlLabelChunk(c, fieldID, contentStart, contentSize, cuePointID, value);
            } else if ("ltxt".equals(fieldID)) {
                int contentSize = stream.getIntLE()/* - 4 - 4 - 4 - 2 - 2 - 2 - 2*/;
                int contentStart = stream.positionUInt();
                int cuePointID = stream.getIntLE();
                int sampleLength = stream.getIntLE();
                String purposeId = readChunkID();
                int countryId = stream.getShortLE();
                int language = stream.getShortLE();
                int dialect = stream.getShortLE();
                int codePage = stream.getShortLE();
                int strSize = contentSize - (stream.positionUInt() - contentStart);
                String value = readFixedASCIIString(strSize);
                log.trace("adtl " + fieldID + ":" + value);
                RiffAdtlTextChunk subChunk = new RiffAdtlTextChunk(c, fieldID, contentStart, contentSize, cuePointID, value, sampleLength, purposeId, countryId, language, dialect, codePage);
            } else {
                log.warn("Unknown adtl LIST type: " + fieldID);
                break;
            }
        }
    }

    /**
     * @param c the LIST chunk
     * @see <a href="https://www.recordingblogs.com/wiki/list-chunk-of-a-wave-file">this</a>
     */
    @SuppressWarnings("java:S135")
    private void readInfoSubChunks(RiffChunk c) throws IOException {
        for (; ; ) {
            if (isAtEndOfChunk(c)) {
                break;
            }
            wordAlign();
            if (isAtEndOfChunk(c)) {
                break;
            }
            String fieldID = readChunkID();
            if (fieldID == null) {
                break;
            }
            int contentSize = stream.getIntLE();
            int contentStart = stream.positionUInt();
            String value = readFixedASCIIString(contentSize);
            RiffChunk nfo = new RiffInfoChunk(c, fieldID, contentStart, contentSize, value);
            log.trace("LIST INFO " + fieldID + ":" + value);
            switch (fieldID) {
                case "ISFT" -> fileInfo.getMetadata()
                        .put(MetadataField.SOFTWARE, value);
                case "ICMT" -> fileInfo.getMetadata()
                        .put(MetadataField.DESCRIPTION, value);
                case "IART", "IAUT" -> fileInfo.getMetadata()
                        .put(MetadataField.VENDOR, value);
                case "IGNR" -> fileInfo.getMetadata()
                        .put(MetadataField.GENRE, value);
                case "ICOP" -> fileInfo.getMetadata()
                        .put(MetadataField.COPYRIGHT, value);
                case "ICRD" -> fileInfo.getMetadata()
                        .put(MetadataField.CREATED, value);
                case "INAM" -> {
                    if (c.getParent() != null && (c.getParent() instanceof RiffListChunk) && ((RiffListChunk) c.getParent()).getListType()
                            .equals("wave")) {
                        audioInfos.getLast()
                                .setFilename(value);
                    } else if (!isDLS2) {
                        fileInfo.setFilename(value);
                    }
                }
                case "IANN" -> {
                    // do nothing
                }
                default -> {
                    // do nothing
                }
            }
        }
    }

    /**
     * All Chunks ID must be word aligned, unfortunately some WAV don't do that.
     */
    private void wordAlign() throws IOException {
        boolean aligned = stream.positionUInt() % 2 == 0;
        if (!aligned) {
            int pad = stream.getByte();
            log.trace(String.format("Misaligned, now at 0x%X", stream.positionUInt()));
            if (pad >= 32 && pad < 127) {
                // the pad is a valid character, this is certainly an unpadded file
                stream.seekUInt(stream.positionUInt() - 1);
                misalignedChunksCount++;
            }
        }
    }

    private RiffAudioInfo getCurrentAudioInfo(RiffChunk c) {
        if (c.getParent() instanceof RiffListChunk) {
            return audioInfos.getLast();// peek the latest added when entering inside parent chunk "wave"
        } else {
            return fileInfo.getAudioInfo();
        }
    }

    private void readFMT(RiffChunk c) throws IOException {
        var currentFileInfo = getCurrentAudioInfo(c);
        int wFormatTag = (stream.getShortLE() & 0xffff);
        WaveCodecs codec = WaveCodecs.valueOf(wFormatTag);
        int nChannels = stream.getShortLE();
        int nSamplesPerSec = stream.getIntLE();
        int nAvgBytesPerSec = stream.getIntLE();
        int nBlockAlign = stream.getShortLE();
        int wBitsPerSample = stream.getShortLE();
        if (codec == WaveCodecs.WAVE_FORMAT_EXTENSIBLE) // multichannel wave files
        {
            // https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatextensible
            // https://learn.microsoft.com/en-us/previous-versions/windows/hardware/design/dn653308(v=vs.85)?redirectedfrom=MSDN
            short cbSize = stream.getShortLE();
            short validBitsPerSample = stream.getShortLE();
            int channelMask = stream.getIntLE();
            currentFileInfo.setChannelsMask(channelMask);
            currentFileInfo.setSubCodec(stream.getUUID());
        }
        currentFileInfo.setBitPerSample(wBitsPerSample);
        currentFileInfo.setSampleRate(nSamplesPerSec);
        currentFileInfo.setNbChannels(nChannels);
        currentFileInfo.setFrameSizeInBytes(nBlockAlign);
        currentFileInfo.setCodec(codec);
        currentFileInfo.setFmtChunk(c);
        log.trace(String.format("%s %d channels %d Hz %d bits Channels infos: %s",
                codec,
                nChannels,
                nSamplesPerSec,
                wBitsPerSample,
                WaveChannels.getMask(currentFileInfo.getChannelsMask())));
    }

    /*-
     * From libsndfile library wav.c
     *
        struct AcidTag
        {
            int properties; // flags: 0x01 = oneshot, 0x02 = rootSet, 0x04 = stretch, 0x08 = diskbased
            short rootNote; // midi number i.e. C = 48 or 60 etc
            short unknown1; // = 0x80 0x00
            int unknown2; // = 0
            int beatCount;
            short timeSigDen;
            short timeSigNum;
            float tempo; // THIS TEMPO IS ALWAYS WRONG, COMPUTE IT USING beatCount and fmt chunk !!
        }
     */
    private void readACID(RiffChunk c) throws IOException {
        // filetype is a bit field
        // 0x00 Loop
        // 0x01 One Shot
        // 0x1C ACID beatmapped
        // the second bit 0x02 is set if the transposition is enabled width the use of
        // the rootNote
        // 0x1E ACID beatmapped + transposition ON
        int filetype = stream.getIntLE();

        // transposition from C (aka root note)
        // 0x3C indicate 0 semi tone, MIDI note C = 0x3C
        // 0x3D indicate 1 semi tone, MIDI note C# = 0x3E
        int rootNote = stream.getShortLE();

        int v1 = stream.getShortLE();
        float v2 = stream.getFloatLE();
        int numberOfBeats = stream.getIntLE();
        int meterDenominator = stream.getShortLE();
        int meterNumerator = stream.getShortLE();
        float tempo = stream.getFloatLE();
        int numberOfBars = numberOfBeats / meterNumerator;
        fileInfo.getAudioInfo()
                .setNbBeats(numberOfBeats);
        fileInfo.getAudioInfo()
                .setRootNote(rootNote);
        fileInfo.getAudioInfo()
                .setMeterDenominator(meterDenominator);
        fileInfo.getAudioInfo()
                .setMeterNumerator(meterNumerator);
        log.trace(String.format("%f BPM %d/%d %d bars Root note: %d", tempo, meterNumerator, meterDenominator, numberOfBars, rootNote));
        // info.getData().put(MetadataField.BPM, df.format(tempo)); this tempo is always
        // wrong, we will compute the real one later
        fileInfo.getMetadata()
                .put(MetadataField.TIME_SIGNATURE, String.format("%d/%d", meterNumerator, meterDenominator));
        fileInfo.getMetadata()
                .put(MetadataField.BEATS, String.format("%d", numberOfBeats));
        fileInfo.getMetadata()
                .put(MetadataField.BARS, String.format("%d", numberOfBars));
        fileInfo.getMetadata()
                .put(MetadataField.ROOT_NOTE, String.format("%d", rootNote));
    }

    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length)) {
            try (GZIPOutputStream zipStream = new GZIPOutputStream(byteStream)) {
                zipStream.write(data);
            }
            return byteStream.toByteArray();
        }
    }

    public String computeAudioChecksum(RiffAudioInfo entry) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(srcAudio, "r")) {
            in.seek(entry.getDataChunk()
                    .getContentStart());
            var pcm = new byte[entry.getDataChunk()
                    .getContentSize()];
            in.readFully(pcm);
            CRC32 fileCRC32 = new CRC32();
            fileCRC32.update(pcm);
            return String.format(Locale.US, "%08X", fileCRC32.getValue());
        }
    }

    public void extract(RiffAudioInfo entry, File target) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(srcAudio, "r")) {
            target.getParentFile()
                    .mkdirs();
            try (RiffWriter rw = new RiffWriter(target)) {

                in.seek(entry.getFmtChunk()
                        .getContentStart());
                var data = new byte[entry.getFmtChunk()
                        .getContentSize()];
                in.readFully(data);
                rw.writeChunk(entry.getFmtChunk(), data);

                in.seek(entry.getDataChunk()
                        .getContentStart());
                var pcm = new byte[entry.getDataChunk()
                        .getContentSize()];
                in.readFully(pcm);
                rw.writeChunk(entry.getDataChunk(), pcm);
                rw.setSize();
            }
        }
    }

    public void streamChunk(RiffChunk dataChunk, int bufferSize, ChunkDataConsumer consumer) throws IOException {
        byte[] buffer = new byte[bufferSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer)
                .order(ByteOrder.LITTLE_ENDIAN);
        try (PositionalReadWriteStream in = new PositionalReadWriteStream(srcAudio, false)) {
            in.seekLong(dataChunk.getContentStart());
            long pos = 0;
            for (; ; ) {
                byteBuffer.rewind();
                long remain = dataChunk.getContentSize() - pos;
                int readSize = remain > bufferSize ? bufferSize : (int) remain;
                int nbRead = in.read(buffer, 0, readSize);
                if (nbRead <= 0)
                    break;
                pos += nbRead;
                consumer.onNewBuffer(byteBuffer, nbRead);
            }
        }
    }
}
