package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

/**
 * This class take care of various conversions around Controllers values
 * <ul>
 *     <li>7 bits unsigned CC or NRPN</li>
 *     <li>7 bits signed CC or NRPN</li>
 *     <li>14 bit unsigned CC or NRPN</li>
 *     <li>14 bit signed CC or NRPN (can be found in Alesis Micron or Ion)</li>
 * </ul>
 * <p>Note that in MIDI spec, 7 bit values are send through MSB which is crazy. We don't follow this here</p>
 *
 * @param msb not used for 7 bit values (=0)
 * @param lsb always used
 */
public record MidiControllerValue(int msb, int lsb) {

    public static MidiControllerValue from32BitsSignedValue(int value) {
        int valueMsb = (value >> 7) & 0x7F;
        int valueLsb = value & 0x7F;
        return new MidiControllerValue(valueMsb, valueLsb);
    }

    public int to32BitsUnsignedValue() {
        return (msb << 7) | lsb;
    }

    public int to32bitsSignedValue() {
        int value14bit = (msb << 7) | lsb;
        return (value14bit << 18) >> 18; // expand the bit 14 to 32 bits (since ">>" preserves sign)
    }

    public int to7bitsSignedValue() {
        return lsb;
    }

}
