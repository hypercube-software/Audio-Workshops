package com.hypercube.workshop.audioworkshop.synth.sine;

import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.synth.vca.AdsrVCA;
import com.hypercube.workshop.audioworkshop.synth.vca.SimpleVCA;
import com.hypercube.workshop.audioworkshop.synth.vca.VCA;
import com.hypercube.workshop.audioworkshop.synth.vco.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.stream.Stream;


@Slf4j
@Component
public class AudioSine {
    static final int SAMPLE_RATE = 44100;
    static final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    public static final int BIT_DEPTH = 16;
    public static final int BUFFER_SIZE_REQUESTED_IN_MS = 250;
    public static final int NB_CHANNELS = 1;
    public static final int MIDI_NOTE = 48;
    public static final int NOTE_DURATION_MS = 2000;

    void playSine(AudioOutputDevice audioOutputDevice) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BIT_DEPTH, NB_CHANNELS, true, byteOrder == ByteOrder.BIG_ENDIAN);
            VCA vca = new SimpleVCA(SAMPLE_RATE, -20);
            VCO naiveVco = new NaiveVCO(BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BIT_DEPTH, byteOrder, vca);
            VCO correctVco = new CorrectVCO(BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BIT_DEPTH, byteOrder, vca);
            VCO wavetableVCO = new WavetableVCO(WavetableType.SINE, BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BIT_DEPTH, byteOrder, vca);
            final double freq = VCO.midiNoteToFrequency(MIDI_NOTE);
            byte[] pcmData = correctVco.generateSignal(freq);
            log.info(String.format("Play note at %f Hz", freq));
            audioOutputDevice.play(format, pcmData, NOTE_DURATION_MS);
            log.info("Done");
        } catch (LineUnavailableException e) {
            throw new AudioError(e);
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
            Thread.currentThread()
                    .interrupt();
        }
    }

    void playFile(AudioOutputDevice audioOutputDevice, File file, int loops) {
        try {
            audioOutputDevice.play(file, loops);
            log.info("Done");
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            throw new AudioError(e);
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
            Thread.currentThread()
                    .interrupt();
        }
    }

    void vco(AudioOutputDevice audioOutputDevice) {
        try {
            Thread.currentThread()
                    .setPriority(Thread.MAX_PRIORITY);
            PCMBufferFormat format = new PCMBufferFormat(BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BitDepth.valueOf(BIT_DEPTH), 1, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);

            try (AudioOutputLine line = new AudioOutputLine(audioOutputDevice, format)) {
                line.start();

                // use our broken VCO, then try width CorrectVCO
                VCA vca = new SimpleVCA(SAMPLE_RATE, 10);
                VCO vco = new NaiveVCO(BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BIT_DEPTH, ByteOrder.BIG_ENDIAN, vca);

                // You should ear something wrong especially for the last note
                Stream.of(52, 54, 56, 57, 74)
                        .map(VCO::midiNoteToFrequency)
                        .forEach(freq -> {
                            log.info("Play frequency " + freq + " Hz");
                            long start = System.currentTimeMillis();
                            while (System.currentTimeMillis() - start < 4000) {
                                byte[] data = vco.generateSignal(freq);
                                line.sendBuffer(data, data.length);
                            }
                        });
            }
            log.info("Done");
        } catch (LineUnavailableException e) {
            throw new AudioError(e);
        }
    }

    void generateFile(int midiNote, File file) {
        try (OutputStream out = new FileOutputStream(file)) {
            VCA vca = new AdsrVCA(SAMPLE_RATE, 500, 250, 500);
            VCO vco = new WavetableVCO(WavetableType.SINE, BUFFER_SIZE_REQUESTED_IN_MS, SAMPLE_RATE, BIT_DEPTH, ByteOrder.BIG_ENDIAN, vca);
            double freq = VCO.midiNoteToFrequency(midiNote);
            log.info("Play frequency " + freq + " Hz");
            for (int i = 0; i < 10; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
            vca.onNoteOn(NB_CHANNELS);
            for (int i = 0; i < BUFFER_SIZE_REQUESTED_IN_MS; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
            vca.onNoteOff();
            for (int i = 0; i < NOTE_DURATION_MS; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

}
