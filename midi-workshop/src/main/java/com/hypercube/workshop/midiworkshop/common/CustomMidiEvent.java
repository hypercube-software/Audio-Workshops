package com.hypercube.workshop.midiworkshop.common;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class CustomMidiEvent extends MidiEvent {

    public static final int NRPN_MSB_PARAM = 0x63;
    public static final int NRPN_LSB_PARAM = 0x62;
    public static final int NRPN_MSB_VALUE = 0x06;
    public static final int NRPN_LSB_VALUE = 0x26;

    /**
     * Constructs a new {@code MidiEvent}.
     *
     * @param message the MIDI message contained in the event
     * @param tick    the time-stamp for the event, in MIDI ticks
     */
    public CustomMidiEvent(MidiMessage message, long tick) {
        super(message, tick);
    }

    /**
     * Constructs a new {@code MidiEvent} with tick -1 ({@link MidiOutDevice#NO_TIME_STAMP}).
     *
     * @param message the MIDI message contained in the event
     */
    public CustomMidiEvent(MidiMessage message) {
        super(message, MidiOutDevice.NO_TIME_STAMP);
    }

    public String getHexValues() {
        var a = getMessage().getMessage();
        StringBuilder sb = new StringBuilder((a.length + 1) * 2);
        sb.append("0x");
        for (byte b : a)
            sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public String getHexValuesSpaced() {
        var a = getMessage().getMessage();
        StringBuilder sb = new StringBuilder((a.length + 1) * 2);
        sb.append("0x");
        for (byte b : a)
            sb.append(String.format(" %02X", b));
        return sb.toString();
    }

    @Override
    public String toString() {
        var message = getMessage();
        switch (message) {
            case ShortMessage shortMessage: {
                byte[] payload = message.getMessage();
                if (shortMessage.getCommand() == ShortMessage.NOTE_ON) {
                    MidiNote note = MidiNote.fromValue(payload[1]);
                    return "%s Note ON: %s 0x%02X(%d)".formatted(getHexValues(), note.name(), note.value(), note.value());
                } else if (shortMessage.getCommand() == ShortMessage.NOTE_OFF) {
                    MidiNote note = MidiNote.fromValue(payload[1]);
                    return "%s Note OFF: %s 0x%02X(%d)".formatted(getHexValues(), note.name(), note.value(), note.value());
                } else if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    int id = payload[1];
                    int value = payload[2];
                    if (id == NRPN_MSB_PARAM) {
                        return "%s NRPN MSB PARAM %d".formatted(getHexValues(), value);
                    } else if (id == NRPN_LSB_PARAM) {
                        return "%s NRPN LSB PARAM %d".formatted(getHexValues(), value);
                    } else if (id == NRPN_MSB_VALUE) {
                        return "%s NRPN MSB VALUE %d".formatted(getHexValues(), value);
                    } else if (id == NRPN_LSB_VALUE) {
                        return "%s NRPN LSB VALUE %d".formatted(getHexValues(), value);
                    } else {
                        return "%s CC $%02X/%d".formatted(getHexValues(), id, id);
                    }

                } else {
                    return getHexValues();
                }
            }
            default:
                return getHexValues();
        }

    }

    public byte[] extractBytes(int start, int size) {
        byte[] originalArray = this.getMessage()
                .getMessage();
        int end = start + size - 1;
        // Index validation
        if (start < 0 || end >= originalArray.length || start > end) {
            throw new IllegalArgumentException("Invalid indices.");
        }

        // Calculate the length of the portion to extract
        int length = end - start + 1;

        // Create the destination array
        byte[] extractedArray = new byte[length];

        // Copy the portion from the original array to the extracted array
        System.arraycopy(originalArray, start, extractedArray, 0, length);

        // Return the extracted array
        return extractedArray;
    }
}
