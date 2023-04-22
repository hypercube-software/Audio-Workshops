package com.hypercube.workshop.midiworkshop.common;

/**
 * @see <a href="http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html#BM3_1"></a>
 */
public class MidiMetaMessages {
    public static final int META_END_OF_TRACK_TYPE = 0x2F;
    public static final int META_TEMPO_TYPE = 0x51;

    public static final int META_TIME_SIGNATURE_TYPE = 0x58;
    public static final int META_KEY_SIGNATURE_TYPE = 0x59;

    public static final int META_TRACK_NAME_TYPE = 0x03;
    public static final int META_PROPRIETARY = 0x7f;
    public static final int META_TEXT_TYPE = 0x01;
    public static final int META_INSTRUMENT_TYPE = 0x04;
    public static final int META_COPYRIGHT_TYPE = 0x02;
    public static final int META_MIDI_CHANNEL_PREFIX_TYPE = 0x20;
}
