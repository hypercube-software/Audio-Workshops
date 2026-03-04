package com.hypercube.workshop.audioworkshop.files.id3;

import com.hypercube.workshop.audioworkshop.files.meta.MetadataField;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * The ID3 spec is known to be awful, so hang-on!
 * <p>
 * References:
 * <a href="https://gigamonkeys.com/book/practical-an-id3-parser.html">practical-an-id3-parser.html</a>
 * <a href="https://mutagen-specs.readthedocs.io/en/latest/">Mutagen Spec Collection</a>
 */
@Slf4j
@SuppressWarnings({"java:S1481", "java:S1854"})
public class ID3Parser {

    public static final String TALB_ALBUM = "TALB";
    public static final String TPE1_ARTIST = "TPE1";
    public static final String TPE2_BAND = "TPE2";
    public static final String TPE3_CONDUCTOR = "TPE3";
    public static final String TCOM_COMPOSER = "TCOM";
    public static final String TCOP_COPYRIGHT = "TCOP";
    public static final String TDAT_DATE = "TDAT";
    public static final String TIT2_TITLE = "TIT2";
    public static final String TYER_YEAR = "TYER";
    private final byte[] data;

    private final ByteBuffer b;

    public ID3Parser(byte[] data) {
        this.data = data;
        this.b = ByteBuffer.wrap(data);
    }

    private int getByte() {
        return b.get();
    }

    private char getChar() {
        return (char) b.get();
    }

    private String getId() {
        byte[] id = new byte[3];
        b.get(id);
        return new String(id, StandardCharsets.US_ASCII);
    }

    private String getFrameId() {
        byte[] id = new byte[4];
        b.get(id);
        if (id[0] == 0 || id[1] == 0 || id[2] == 0 || id[3] == 0)
            return null;
        return new String(id, StandardCharsets.US_ASCII);
    }

    private boolean bit(int mask, int pos) {
        return (mask & (1 << pos)) != 0;
    }

    /**
     * Taken from the doc:
     *
     * <div>
     * The ID3v2 tag size is encoded with four bytes where the most significant bit (bit 7) is set to zero
     * in every byte, making a total of 28 bits. The zeroed bits are ignored,
     * so a 257 bytes long tag is represented as $00 00 02 01.
     * </div>
     */
    private int getSyncSafeInteger() {
        int v0 = getByte();
        int v1 = getByte();
        int v2 = getByte();
        int v3 = getByte();
        // xaaaaaaa xbbbbbbb xccccccc xddddddd
        // aaaaaaabbbbbbbcccccccddddddd
        //        |      |      |
        //        21     14     7
        return v0 << 21 | v1 << 14 | v2 << 7 | v3;
    }

    private int getFrameSize() {
        int v0 = getByte();
        int v1 = getByte();
        int v2 = getByte();
        return v0 << 16 | v1 << 8 | v2;
    }

    public ID3Info parse() {
        String id = getId();
        if (!id.equals("ID3"))
            return null;
        int versionMajor = getByte();
        int versionMinor = getByte();
        String version = "2.%d.%d".formatted(versionMajor, versionMinor);
        if (!version.equals("2.3.0") && !version.equals("2.4.0"))
            return null;
        int flag = getByte();
        boolean unsynchronisation = bit(flag, 7);
        boolean extendedHeader = bit(flag, 6);
        boolean experimental = bit(flag, 5);
        boolean hasFooter = bit(flag, 4);
        int size = getSyncSafeInteger();
        if (extendedHeader) {
            int ehSize = getSyncSafeInteger();
            int nbFlags = b.get();
            int[] flags = new int[nbFlags];
            for (int i = 0; i < nbFlags; i++) {
                flags[i] = getByte();
            }
        }
        var info = new ID3Info(version);
        while (b.position() != data.length) {
            String frameID = getFrameId();
            if (frameID == null)
                break;
            // SyncSafeInteger seems only used in 2.4 spec for frameSize
            // see: https://hydrogenaud.io/index.php/topic,67145.0.html
            int frameSize = version.equals("2.3.0") ? b.getInt() : getSyncSafeInteger();
            int flag1 = getByte();
            int flag2 = getByte();
            byte[] frameData = new byte[frameSize];
            b.get(frameData);
            String strContent = readTextFrame(frameID, frameData);
            log.trace("{} of size {} bytes {}", frameID, frameSize, strContent);
            info.getFrames()
                    .put(frameID, new ID3Frame(frameID, flag1, flag2, frameData, strContent));
        }
        extractMetadata(info);
        return info;
    }

    /**
     * Read Text frame (their id begin with 'T')
     *
     * @param frameID Something like 'Txxx'
     * @param data    Frame content
     * @return empty string if there is no text content
     */
    private static String readTextFrame(String frameID, byte[] data) {
        String strContent = "";
        if (frameID.charAt(0) == 'T') {
            int encodingFlag = data[0];
            int encodingFlagSize = 1;
            if (encodingFlag == 1) {
                int bomSize = 2; // 0xFFFE (little endian) or 0xFEFF (big endian)
                int bom = (data[1] << 8 | data[2]) & 0xFFFF;
                var charset = bom == 0xFEFF ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
                strContent = new String(data, encodingFlagSize + bomSize, data.length - encodingFlagSize - bomSize, charset);
            } else {
                strContent = new String(data, encodingFlagSize, data.length - encodingFlagSize, StandardCharsets.ISO_8859_1);
            }
        }
        return strContent;
    }

    private void extractMetadata(ID3Info info) {
        // Multiple input tags lead to a a single MetadataField
        extractMetadataField(info, TALB_ALBUM, MetadataField.ALBUM);
        extractMetadataField(info, TPE1_ARTIST, MetadataField.AUTHOR);
        extractMetadataField(info, TPE2_BAND, MetadataField.AUTHOR);
        extractMetadataField(info, TPE3_CONDUCTOR, MetadataField.AUTHOR);
        extractMetadataField(info, TCOM_COMPOSER, MetadataField.AUTHOR);
        extractMetadataField(info, TCOP_COPYRIGHT, MetadataField.COPYRIGHT);
        extractMetadataField(info, TDAT_DATE, MetadataField.CREATED);
        extractMetadataField(info, TYER_YEAR, MetadataField.YEAR);
        extractMetadataField(info, TIT2_TITLE, MetadataField.DESCRIPTION);
    }

    private void extractMetadataField(ID3Info info, String frameID, MetadataField field) {
        if (!info.getMetadata()
                .contains(field)) {
            var frame = info.getFrames()
                    .get(frameID);
            if (frame != null) {
                info.getMetadata()
                        .put(field, frame.strContent());
            }
        }
    }
}

