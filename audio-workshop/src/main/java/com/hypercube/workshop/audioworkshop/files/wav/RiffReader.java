package com.hypercube.workshop.audioworkshop.files.wav;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import com.hypercube.workshop.audioworkshop.files.exceptions.AudioParserException;
import com.hypercube.workshop.audioworkshop.files.exceptions.SampleCountException;
import com.hypercube.workshop.audioworkshop.files.io.PositionalReadWriteStream;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.GZIPOutputStream;

/**
 * -
 * This parser load the entire WAV in memory because a memory mapped FileChannel does not work properly on windows.
 * This bug in the JVM is UNFIXABLE, see <a href="https://bugs.openjdk.org/browse/JDK-4715154">this ticket</a>
 * <p>
 * Parsing a RIFF is easy but you have to be very careful about word alignment of every Chunks.
 * - if a chunk ID is at position 3, there is a padding byte before the next one
 * - unfortunately a lot of editors generate non-padded RIFF which are out of spec
 * <p>
 * ACID chunks:
 * acid: header containing the number of beats
 * strc: stretch info
 * str2: stretch info 2
 * bmrk: beat markers
 * dtbt: detected beats
 * <p>
 * Others:
 * JUNK: list of something
 * <p>
 * see also this study on the mess around metadata: <a href="https://www.arsc-audio.org/pdf/ARSC_TC_MD_Study.pdf">here</a>
 */
@SuppressWarnings({"java:S1172", "unused"})
@Slf4j
public class RiffReader {
    public static final int LOWEST_TEMPO = 40;
    public static final int HIGHEST_TEMPO = 300;
    private final File srcAudio;
    private final RiffFileInfo info = new RiffFileInfo();

    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    private final boolean canFixSource;
    private int misalignedChunksCount = 0;

    private PositionalReadWriteStream stream = null;

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

            stream = new PositionalReadWriteStream(srcAudio);

            String riff = readChunkID();
            if (!"RIFF".equals(riff)) {
                throw new AudioParserException("not a RIFF file");
            }
            long size = stream.getUIntLE();
            long expectedTotalSize = size + 8;
            if (srcAudio.length() != expectedTotalSize) {
                long delta = srcAudio.length() - expectedTotalSize;
                log.warn(String.format("WAV file size 0x%X does not match RIFF size 0x%X delta: %d bytes %s",
                        srcAudio.length(),
                        expectedTotalSize,
                        delta,
                        srcAudio.getAbsolutePath()));
            }
            String type = readChunkID();
            if (!"WAVE".equals(type)) {
                throw new AudioParserException("not a WAVE file " + srcAudio.getAbsolutePath());
            }

            for (; ; ) {
                RiffChunk c = readChunk();
                if (c == null) break;
                info.addChunk(c);
            }
            if (misalignedChunksCount > 0) {
                log.trace("Illegal WAV: %d misaligned chunks in %s".formatted(misalignedChunksCount, srcAudio.getAbsolutePath()));
            }
            info.getFileInfo()
                    .computeDuration();
            checkSampleCount();
            storeMetadataTempo();
            storeMetadataKey();
            storeNonAudioData(srcAudio);
            return info;
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
        if (info.getFileInfo()
                .getKey() != null) {
            info.getMetadata()
                    .put(MetadataField.KEY, info.getFileInfo()
                            .getKey());
        }
    }

    private void storeMetadataTempo() {
        DecimalFormat df = new DecimalFormat("#.#");
        if (info.getFileInfo()
                .getNbBeats() != 0 && info.getFileInfo()
                .getTempo() == 0) {
            float beatDuration = info.getFileInfo()
                    .getDuration() / info.getFileInfo()
                    .getNbBeats();
            float tempo = 60f / beatDuration;
            info.getFileInfo()
                    .setTempo(tempo);
            info.getMetadata()
                    .put(MetadataField.BPM, df.format(tempo));
        } else if (info.getFileInfo()
                .getTempo() == 0) {
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
                        info.getFileInfo()
                                .setTempo(tempo);
                        info.getMetadata()
                                .put(MetadataField.BPM, df.format(tempo));
                    });
        }
    }

    /**
     * Allow the possibility to reconstruct the WAV file, saving everything around the PCM data
     *
     * @param srcAudio
     * @throws IOException
     */
    private void storeNonAudioData(File srcAudio) throws IOException {
        var data = info.getDataChunk();
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

        info.setFilename(srcAudio.getName());
        info.setProlog(compress(prolog));
        info.setEpilog(compress(epilog));
    }

    private RiffChunk readChunk() throws IOException {
        String chunkId = readChunkID();
        if (chunkId == null) return null;
        final int contentSize = stream.getIntLE();
        final int contentStart = stream.positionUInt();
        int end = contentStart + contentSize;
        log.trace(String.format("CHUNK %s : %d/0x%X bytes at 0x%X end 0x%X", chunkId, contentSize, contentSize, contentStart - 8, end));
        RiffChunk riffChunk = new RiffChunk(chunkId, contentStart, contentSize);
        switch (chunkId) {
            case "data" -> readDATA(riffChunk);
            case "acid" -> readACID(riffChunk);
            case "fmt " -> readFMT(riffChunk);
            case "cue " -> readCUE(riffChunk);
            case "LIST" -> readLIST(riffChunk);
            case "bext" -> readBEXT(riffChunk);
            case "umid" -> readUMID(riffChunk);
            case "iXML" -> readIXML(riffChunk);
            case "_PMX" -> readPMX(riffChunk);
            case "ID3 " -> readID3(riffChunk);
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

        // get ready to read the next one
        moveAfterChunk(riffChunk);
        return riffChunk;
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

    private void readDATA(RiffChunk c) {
        info.getFileInfo()
                .setNbAudioBytes(c.getContentSize());
    }


    /**
     * If the data chunk size is wrong, the FLAC encoder will raise the error "got
     * partial sample"
     *
     * @throws SampleCountException if the count is wrong
     */
    private void checkSampleCount() throws SampleCountException {
        long dataChunkSize = (info.getFileInfo()
                .getNbAudioBytes()) & 0xFFFFFFFFL;
        long partialSample = dataChunkSize % info.getFileInfo()
                .getBytePerSample();
        if (partialSample != 0) {
            long expectedSize = dataChunkSize - partialSample + info.getFileInfo()
                    .getBytePerSample();
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
        var data = info.getDataChunk();
        stream.seekUInt(data.getContentStart() - 4);
        stream.putIntLE(expectedSize);
        stream.seekUInt(0);
        info.getDataChunk()
                .setContentSize(expectedSize);
    }

    // https://mutagen-specs.readthedocs.io/en/latest/id3/id3v2.2.html#id3v2-overview
    private void readID3(RiffChunk c) throws IOException {
        String id = readFixedASCIIStringAndClean(3, true);
        int versionMajor = stream.getByte();
        int versionMinor = stream.getByte();
        int flag = stream.getByte();
        log.trace(String.format("ID3 v%d.%d", versionMajor, versionMinor));
    }

    // https://www.adobe.com/products/xmp.html
    private void readPMX(RiffChunk c) {
        log.trace("XML data from PMX Adobe ignored");
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
                                info.getFileInfo()
                                        .setNbBeats(Integer.parseInt(currentAttribute.get(IXML_VALUE)));
                            }
                            if ("MusicalSignature".equals(name)) {
                                info.getFileInfo()
                                        .setMeterDenominator(Integer.parseInt(currentAttribute.get("DENOMINATOR")));
                                info.getFileInfo()
                                        .setMeterNumerator(Integer.parseInt(currentAttribute.get("NUMERATOR")));
                            }
                            if ("MusicalTempo".equals(name)) {
                                info.getFileInfo()
                                        .setTempo(Float.parseFloat(currentAttribute.get(IXML_VALUE)));
                            }

                            // Not official
                            if ("MusicalKey".equals(name)) {
                                info.getFileInfo()
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
        info.getMetadata()
                .put(MetadataField.CREATED, originationDate);
        info.getMetadata()
                .put(MetadataField.VENDOR, originator);
        info.getMetadata()
                .put(MetadataField.DESCRIPTION, description);
        info.getMetadata()
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

    private void readLIST(RiffChunk c) throws IOException {
        String listType = readChunkID();
        if ("INFO".equals(listType)) {
            readInfoSubChunks(c);
        } else if ("adtl".equals(listType)) {
            readAdtlSubChunks(c);
        } else {
            log.warn("Unknown LIST type: " + listType);
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
                RiffAdtlLabelChunk subChunk = new RiffAdtlLabelChunk(fieldID, contentStart, contentSize, cuePointID, value);
                c.addChild(subChunk);
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
                RiffAdtlTextChunk subChunk = new RiffAdtlTextChunk(fieldID, contentStart, contentSize, cuePointID, value, sampleLength, purposeId, countryId, language, dialect, codePage);
                c.addChild(subChunk);
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
            RiffChunk nfo = new RiffInfoChunk(fieldID, contentStart, contentSize, value);
            c.addChild(nfo);
            log.trace("LIST INFO " + fieldID + ":" + value);
            switch (fieldID) {
                case "ISFT" -> info.getMetadata()
                        .put(MetadataField.SOFTWARE, value);
                case "ICMT" -> info.getMetadata()
                        .put(MetadataField.DESCRIPTION, value);
                case "IART", "IAUT" -> info.getMetadata()
                        .put(MetadataField.VENDOR, value);
                case "IGNR" -> info.getMetadata()
                        .put(MetadataField.GENRE, value);
                case "ICOP" -> info.getMetadata()
                        .put(MetadataField.COPYRIGHT, value);
                case "ICRD" -> info.getMetadata()
                        .put(MetadataField.CREATED, value);
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

    private void readFMT(RiffChunk c) throws IOException {
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
            info.getFileInfo()
                    .setChannelsMask(channelMask);
            // UUID are always in BIG ENDIAN
            long u1 = stream.getLongBE();
            long u2 = stream.getLongBE();
            info.getFileInfo()
                    .setSubCodec(new UUID(u1, u2));
        }
        info.getFileInfo()
                .setBitPerSample(wBitsPerSample);
        info.getFileInfo()
                .setSampleRate(nSamplesPerSec);
        info.getFileInfo()
                .setNbChannels(nChannels);
        info.getFileInfo()
                .setBytePerSample(nBlockAlign);
        info.getFileInfo()
                .setCodec(codec);

        log.trace(String.format("%s %d channels %d Hz %d bits Channels infos: %s",
                codec,
                nChannels,
                nSamplesPerSec,
                wBitsPerSample,
                WaveChannels.getMask(info.getFileInfo()
                        .getChannelsMask())));
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
        info.getFileInfo()
                .setNbBeats(numberOfBeats);
        info.getFileInfo()
                .setRootNote(rootNote);
        info.getFileInfo()
                .setMeterDenominator(meterDenominator);
        info.getFileInfo()
                .setMeterNumerator(meterNumerator);
        log.trace(String.format("%f BPM %d/%d %d bars Root note: %d", tempo, meterNumerator, meterDenominator, numberOfBars, rootNote));
        // info.getData().put(MetadataField.BPM, df.format(tempo)); this tempo is always
        // wrong, we will compute the real one later
        info.getMetadata()
                .put(MetadataField.TIME_SIGNATURE, String.format("%d/%d", meterNumerator, meterDenominator));
        info.getMetadata()
                .put(MetadataField.BEATS, String.format("%d", numberOfBeats));
        info.getMetadata()
                .put(MetadataField.BARS, String.format("%d", numberOfBars));
        info.getMetadata()
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
}
