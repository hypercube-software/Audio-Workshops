package com.hypercube.workshop.audioworkshop.common.vco;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.vca.VCA;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@AllArgsConstructor
public abstract class VCO {
    public static final double SIN_PERIOD_2PI = 2.0 * Math.PI;
    protected final int bufferSizeRequestedInMs;
    protected final double sampleRate;
    protected final int bitDepth;
    protected final ByteOrder byteOrder;
    protected final VCA vca;

    /**
     * See <a href="https://www.inspiredacoustics.com/en/MIDI_note_numbers_and_center_frequencies">MIDI_note_numbers_and_center_frequencies</a>
     */
    public static double midiNoteToFrequency(int keyNumber) {
        return 440 * Math.pow(2, (keyNumber - 69) / 12f);
    }

    public abstract byte[] generateSignal(double freq);

    public static void saveBuffer(String filename, byte[] data) {
        try {
            Files.write(Path.of(filename), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    public int getBytesPerSamples() {
        return bitDepth / 8;
    }
}
