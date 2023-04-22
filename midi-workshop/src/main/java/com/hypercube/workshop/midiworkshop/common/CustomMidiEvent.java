package com.hypercube.workshop.midiworkshop.common;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class CustomMidiEvent extends MidiEvent {
    /**
     * Constructs a new {@code MidiEvent}.
     *
     * @param message the MIDI message contained in the event
     * @param tick    the time-stamp for the event, in MIDI ticks
     */
    public CustomMidiEvent(MidiMessage message, long tick) {
        super(message, tick);
    }

    public String getHexValues() {
        var a = getMessage().getMessage();
        StringBuilder sb = new StringBuilder((a.length + 1) * 2);
        sb.append("0x");
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Override
    public String toString() {
        var message = getMessage();
        if (message instanceof ShortMessage) {
            if (((ShortMessage) message).getCommand() == ShortMessage.NOTE_ON) {
                MidiNote note = MidiNote.fromValue(message.getMessage()[1]);
                return String.format("%s Note ON: %s 0x%02X(%d)", getHexValues(), note.name(), note.value(), note.value());
            } else if (((ShortMessage) message).getCommand() == ShortMessage.NOTE_OFF) {
                MidiNote note = MidiNote.fromValue(message.getMessage()[1]);
                return String.format("%s Note OFF: %s 0x%02X(%d)", getHexValues(), note.name(), note.value(), note.value());
            }
        }
        return getHexValues();
    }
}
