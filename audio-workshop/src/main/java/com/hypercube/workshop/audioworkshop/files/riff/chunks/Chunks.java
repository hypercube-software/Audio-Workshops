package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Chunks {
    public static final String WAVE = "WAVE";
    public static final String AIFF = "AIFF";
    public static final String AIFC = "AIFC";
    public static final String DLS = "DLS ";
    public static final String NUND = "NUND";

    public static final String LIST = "LIST";
    public static final String LIST_TYPE_INFO = "INFO";
    public static final String LIST_TYPE_ADTL = "adtl";

    public static final String INAM = "INAM";
    public static final String RGN = "rgn ";
    public static final String RGN2 = "rgn2";
    public static final String RGNH = "rgnh";
    public static final String WLNK = "wlnk";
    public static final String LRGN = "lrgn";
    public static final String PTBL = "ptbl";
    public static final String LINS = "lins";
    public static final String INS = "ins ";

    // http://www.swamiproject.org/api/libinstpatch/libinstpatch-IpatchGigEffects.html#IpatchGigEffects
    public static final String G3_EFFECT_WAVE = "3ewa"; // GigaSampler effects chunk
    public static final String G3_DIMENSIONS = "3lnk"; // GigaSampler dimensions chunk
    public static final String VERS = "vers"; // version
    public static final String DATA = "data"; // audio samples
    public static final String ACID = "acid"; // ACID metadata  like tempo or key signature
    public static final String FORMAT = "fmt "; // Format of audio samples
    public static final String COPYRIGHT = "(c) ";
    public static final String AUTHOR = "AUTH";
    public static final String NAME = "NAME";
    public static final String IXML = "iXML";
    public static final String BEXT = "bext"; // Broadcast WAV metadata
    public static final String XMP = "_PMX"; // Adobe’s Extensible Metadata Platform (XMP)
    public static final String AIFF_SSND = "SSND"; // Audio samples

    public static final String AIFF_FORMAT = "COMM"; // Format of audio samples
    public static final String AIFF_DESCRIPTION = "ANNO"; // Description
    public static final String ID3_UPPERCASE = "ID3 ";
    public static final String ID3_LOWERCASE = "id3 ";
    public static final String UMID = "umid"; // Unique material identifier

    public static final String CUE = "cue ";
    public static final String PLAYLIST = "plst";
    public static final String ADTL_LONG_TEXT = "ltxt";
    public static final String ADTL_LABEL = "labl";
    public static final String ADTL_NOTE = "note";

    public static final String ROOT = "ROOT";
    public static final String ARCH = "ARCH";
}
