package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Matcher;

@Setter
@Getter
public class MidiSettings {
    private String lowestNote;
    private String highestNote;
    private int notesPerOctave;

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    private int getNoteNumber(String note) {
        Matcher m = CachedRegExp.get("([A-G#])([0-9])", note);
        if (m.find()) {
            int octave = Integer.parseInt(m.group(2)) + 1;
            String offset = m.group(1);
            int intOffset = offset.charAt(0) - 'C';
            if (offset.length() == 2) {
                intOffset++;// #
            }
            if (intOffset < 0) {
                intOffset += 9;
            }
            return octave * 12 + intOffset;
        }
        throw new MidiError("Unexpected note name:" + note);
    }

}
