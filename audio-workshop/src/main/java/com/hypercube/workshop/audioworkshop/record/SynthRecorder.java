package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.RecordListener;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMconverter;
import com.hypercube.workshop.audioworkshop.common.wav.WavRecordListener;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class SynthRecorder extends WavRecordListener {
    private enum STATE {
        IDLE,
        NOTE_OFF,
        NOTE_ON,
    }

    public SynthRecorder(File output, AudioFormat format) throws IOException {
        super(output, format);
    }

    public void recordSynth(AudioInputDevice audioDevice, MidiOutDevice midiDevice) {
        midiDevice.open();
        try (AudioInputLine line = new AudioInputLine(audioDevice, format, 250)) {
            try (FileOutputStream out = new FileOutputStream(new File("output.pcm"))) {
                line.record(new RecordListener() {
                    double durationInSec = 0;
                    long lastTime = 0;
                    BitDepth outputBitDepth = BitDepth.BIT_DEPTH_16;
                    byte[] pcmBufferOut = new byte[1000 * line.getSampleRate() * outputBitDepth.getBytes()];
                    STATE state = STATE.IDLE;
                    int note = 48;
                    int nbChannels = line.getNbChannels();

                    @Override
                    public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
                        durationInSec += (double) nbSamples / line.getSampleRate();
                        try {
                            if (state == STATE.IDLE && durationInSec > 1) {
                                MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, note, 127), 0);
                                midiDevice.send(noteOn);
                                state = STATE.NOTE_ON;
                                durationInSec = 0;
                                log.info("Note on {}", note);
                            } else if (state == STATE.NOTE_ON && durationInSec > 2) {
                                MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, note, 0), 0);
                                midiDevice.send(noteOff);
                                state = STATE.NOTE_OFF;
                                log.info("Note off {}", note);
                                durationInSec = 0;
                                note += 6;
                            } else if (state == STATE.NOTE_OFF && durationInSec > 2) {
                                state = STATE.IDLE;
                                durationInSec = 0;
                                log.info("IDLE");
                            }
                        } catch (InvalidMidiDataException e) {
                            throw new RuntimeException(e);
                        }
                        if (state == STATE.NOTE_ON || state == STATE.NOTE_OFF) {
                            PCMconverter.convert(sampleBuffer, pcmBufferOut, nbSamples, nbChannels, outputBitDepth, true, true);
                            try {
                                out.write(pcmBufferOut, 0, nbSamples * outputBitDepth.getBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            float[] total = new float[nbChannels];
                            for (int c = 0; c < nbChannels; c++) {
                                for (int s = 0; s < nbSamples; s++) {
                                    float sample = Math.abs(sampleBuffer[c][s]);
                                    total[c] += sample;
                                }
                            }
                            for (int c = 0; c < nbChannels; c++) {
                                total[c] = total[c] / nbSamples;
                            }
                            long now = System.currentTimeMillis();
                            if (now - lastTime > 1000) {
                                lastTime = now;
                                double dB = 20 * Math.log10(total[0]); // look first channel for now
                                log.info("Volume: {} dB", dB);
                            }
                        }
                        return !(note >= 72 && state == STATE.IDLE);
                    }
                });
            }
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        } finally {
            try {
                midiDevice.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
