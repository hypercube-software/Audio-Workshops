package com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

import java.util.List;

@Getter
public class RiffRegionHeaderChunk extends RiffChunk {

    private static final List<String> notes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");

    public static String noteNameFromPitch(int input) {
        int octave = (input - 12) / 12;
        int offset = input % 12;
        return notes.get(offset) + octave;
    }

    public record Range(short usLow, short usHigh) {
        public String toString(boolean toNote) {
            if (toNote) {
                return "[%s/%d,%s/%d]".formatted(noteNameFromPitch(usLow), usLow, noteNameFromPitch(usHigh), usHigh);
            } else {
                return "[%d,%d]".formatted(usLow, usHigh);
            }
        }
    }

    public RiffRegionHeaderChunk(RiffChunk parent, String id, int contentStart, int contentSize, Range keyRange, Range velovityRange, int fusOptions, int usKeyGroups, int usLayer) {
        super(parent, id, contentStart, contentSize);
        this.keyRange = keyRange;
        this.velovityRange = velovityRange;
        this.fusOptions = fusOptions;
        this.usKeyGroups = usKeyGroups;
        this.usLayer = usLayer;
    }

    final Range keyRange;
    final Range velovityRange;
    final int fusOptions;
    final int usKeyGroups;
    final int usLayer;

    @Override
    public String toString() {
        return "RiffRegionHeaderChunk{" +
                "keyRange=" + keyRange.toString(true) +
                ", velovityRange=" + velovityRange.toString(false) +
                '}';
    }
}
