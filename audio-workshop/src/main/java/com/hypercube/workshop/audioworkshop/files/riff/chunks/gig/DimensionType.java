package com.hypercube.workshop.audioworkshop.files.riff.chunks.gig;

/**
 * From Linuxsampler source code gig.h
 */
@SuppressWarnings("java:S115")
public class DimensionType {
    public static final int dimension_none = 0x00; // Dimension not in use.
    public static final int dimension_samplechannel = 0x80; // If used sample has more than one channel (thus is not mono).
    public static final int dimension_layer = 0x81; // For layering of up to 8 instruments (and eventually crossfading of 2 or 4 layers).
    public static final int dimension_velocity = 0x82; // Key Velocity (this is the only dimension in gig2 where the ranges can exactly be defined).
    public static final int dimension_channelaftertouch = 0x83; // Channel Key Pressure 
    public static final int dimension_releasetrigger = 0x84; // Special dimension for triggering samples on releasing a key.
    public static final int dimension_keyboard = 0x85; // Dimension for keyswitching
    public static final int dimension_roundrobin = 0x86; // Different samples triggered each time a note is played; dimension regions selected in sequence
    public static final int dimension_random = 0x87; // Different samples triggered each time a note is played; random order
    public static final int dimension_smartmidi = 0x88; // For MIDI tools like legato and repetition mode
    public static final int dimension_roundrobinkeyboard = 0x89; // Different samples triggered each time a note is played; any key advances the counter 
    public static final int dimension_modwheel = 0x01; // Modulation Wheel (MIDI Controller 1)
    public static final int dimension_breath = 0x02; // Breath Controller (Coarse; MIDI Controller 2)
    public static final int dimension_foot = 0x04; // Foot Pedal (Coarse; MIDI Controller 4)
    public static final int dimension_portamentotime = 0x05; // Portamento Time (Coarse; MIDI Controller 5)
    public static final int dimension_effect1 = 0x0c; // Effect Controller 1 (Coarse; MIDI Controller 12)
    public static final int dimension_effect2 = 0x0d; // Effect Controller 2 (Coarse; MIDI Controller 13)
    public static final int dimension_genpurpose1 = 0x10; // General Purpose Controller 1 (Slider; MIDI Controller 16)
    public static final int dimension_genpurpose2 = 0x11; // General Purpose Controller 2 (Slider; MIDI Controller 17)
    public static final int dimension_genpurpose3 = 0x12; // General Purpose Controller 3 (Slider; MIDI Controller 18)
    public static final int dimension_genpurpose4 = 0x13; // General Purpose Controller 4 (Slider; MIDI Controller 19)
    public static final int dimension_sustainpedal = 0x40; // Sustain Pedal (MIDI Controller 64)
    public static final int dimension_portamento = 0x41; // Portamento (MIDI Controller 65)
    public static final int dimension_sostenutopedal = 0x42; // Sostenuto Pedal (MIDI Controller 66)
    public static final int dimension_softpedal = 0x43; // Soft Pedal (MIDI Controller 67)
    public static final int dimension_genpurpose5 = 0x30; // General Purpose Controller 5 (Button; MIDI Controller 80)
    public static final int dimension_genpurpose6 = 0x31; // General Purpose Controller 6 (Button; MIDI Controller 81)
    public static final int dimension_genpurpose7 = 0x32; // General Purpose Controller 7 (Button; MIDI Controller 82)
    public static final int dimension_genpurpose8 = 0x33; // General Purpose Controller 8 (Button; MIDI Controller 83)
    public static final int dimension_effect1depth = 0x5b; // Effect 1 Depth (MIDI Controller 91)
    public static final int dimension_effect2depth = 0x5c; // Effect 2 Depth (MIDI Controller 92)
    public static final int dimension_effect3depth = 0x5d; // Effect 3 Depth (MIDI Controller 93)
    public static final int dimension_effect4depth = 0x5e; // Effect 4 Depth (MIDI Controller 94)
    public static final int dimension_effect5depth = 0x5f;  // Effect 5 Depth (MIDI Controller 95)
}
