package com.hypercube.workshop.midiworkshop.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public record MidiControl(int id, String name) {
    private static final Map<Integer, String> namesMap = new HashMap<>();
    @SuppressWarnings("java:S1192")
    private static final List<Object> names = List.of(
            1, "Modulation Wheel",
            2, "Breath controller",
            4, "Foot Pedal (MSB)",
            5, "Portamento Time (MSB)",
            6, "Data Entry (MSB)",
            7, "Volume (MSB)",
            8, "Balance (MSB",
            10, "Pan position (MSB)",
            11, "Expression (MSB)",
            12, "Effect Control 1 (MSB)",
            13, "Effect Control 2 (MSB)",
            16, "General Purpose",
            17, "General Purpose",
            18, "General Purpose",
            19, "General Purpose",
            64, "Hold Pedal (on/off)",
            65, "Portamento (on/off)",
            66, "Sostenuto Pedal (on/off)",
            67, "Soft Pedal (on/off)",
            68, "Legato Pedal (on/off)",
            69, "Hold 2 Pedal (on/off)",
            70, "Sound Variation",
            71, "Resonance (Timbre)",
            72, "Sound Release Time",
            73, "Sound Attack Time",
            74, "Frequency Cutoff (Brightness)",
            75, "Sound Control 6",
            76, "Sound Control 7",
            77, "Sound Control 8",
            78, "Sound Control 9",
            79, "Sound Control 10",
            80, "General Purpose Button 1",
            81, "General Purpose Button 2",
            82, "General Purpose Button 3",
            83, "General Purpose Button 4",
            84, "Portamento Amount",
            91, "Reverb Level",
            92, "Tremolo Level",
            93, "Chorus Level",
            94, "Detune Level",
            95, "Phaser Level",
            96, "Data Button increment",
            97, "Data Button decrement",
            98, "Non-registered Parameter (LSB)",
            99, "Non-registered Parameter (MSB)",
            100, "Registered Parameter (LSB)",
            101, "Registered Parameter (MSB)",
            120, "All Sound Off",
            121, "All Controllers Off",
            122, "Local Keyboard (on/off)",
            123, "All Notes Off",
            124, "Omni Mode Off",
            125, "Omni Mode On",
            126, "Mono Operation",
            127, "Poly Mode"
    );

    public static MidiControl fromId(int id) {
        if (namesMap.isEmpty()) {
            for (int i = 0; i < names.size(); i += 2) {
                namesMap.put((Integer) names.get(i), (String) names.get(i + 1));
            }
        }
        if (namesMap.containsKey(id)) {
            return new MidiControl(id, namesMap.get(id));
        }
        return new MidiControl(id, String.format("%d undefined", id));
    }
}
