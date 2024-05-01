package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.record.RecordListener;
import com.hypercube.workshop.audioworkshop.common.record.WavRecordListener;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.*;

@Slf4j
public class SynthRipper {
    private final SynthRipperConfiguration conf;
    public static final int BUFFER_DURATION_MS = 62;
    private final AudioFormat format;
    private MidiOutDevice midiOutDevice;
    private AudioInputLine inputLine;

    private final SynthRipperState state = new SynthRipperState();
    private WavRecordListener wavRecordListener;

    private File getOutputFile(int note) {
        return new File("output/20 - String %s.wav".formatted(note));
    }

    public SynthRipper(SynthRipperConfiguration config, AudioFormat format) throws IOException {
        this.conf = config;
        this.format = format;
        state.lowestNote = config.getMidi()
                .getLowestNoteInt();
        state.highestNote = config.getMidi()
                .getHighestNoteInt();
        state.noteIncrement = 12 / config.getMidi()
                .getNotesPerOctave();
        state.note = state.lowestNote;
    }

    public void recordSynth(AudioInputDevice audioInputDevice, AudioOutputDevice audioOutputDevice, MidiOutDevice midiDevice) throws IOException {
        this.wavRecordListener = new WavRecordListener(getOutputFile(state.note), format);
        RecordListener listener = new RecordListener() {
            @Override
            public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
                state.durationInSec += (float) nbSamples / inputLine.getSampleRate();
                try {
                    if (state.state == SynthRipperStateEnum.IDLE && state.durationInSec > 1) {
                        MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, state.note, 127), 0);
                        midiOutDevice.send(noteOn);
                        state.state = SynthRipperStateEnum.NOTE_ON;
                        state.durationInSec = 0;
                    } else if (state.state == SynthRipperStateEnum.NOTE_ON && state.durationInSec > 2) {
                        MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, state.note, 0), 0);
                        midiOutDevice.send(noteOff);
                        state.state = SynthRipperStateEnum.NOTE_OFF;
                        state.durationInSec = 0;
                    } else if (state.state == SynthRipperStateEnum.NOTE_OFF && state.durationInSec > 2) {
                        state.state = SynthRipperStateEnum.IDLE;
                        state.durationInSec = 0;
                        wavRecordListener.close();
                        state.note += state.noteIncrement;
                        wavRecordListener = isLastNotePlayed() ? null : new WavRecordListener(getOutputFile(state.note), format);
                    }
                } catch (InvalidMidiDataException | IOException e) {
                    throw new AudioError(e);
                }
                if (state.state == SynthRipperStateEnum.NOTE_ON || state.state == SynthRipperStateEnum.NOTE_OFF) {
                    wavRecordListener.onNewBuffer(sampleBuffer, nbSamples, pcmBuffer, pcmSize);
                }
                return !isLastNotePlayed();
            }
        };
        this.midiOutDevice = midiDevice;
        this.midiOutDevice.open();
        try (AudioInputLine inputLine = new AudioInputLine(audioInputDevice, format, BUFFER_DURATION_MS)) {
            this.inputLine = inputLine;
            try (AudioOutputLine outputLine = new AudioOutputLine(audioOutputDevice, format, BUFFER_DURATION_MS)) {
                outputLine.start();
                inputLine.record(listener, outputLine);
/*
                RecordListener rl = new RecordListener() {
                    double durationInSec = 0;
                    long lastTime = 0;
                    BitDepth outputBitDepth = BitDepth.BIT_DEPTH_16;
                    byte[] pcmBufferOut = new byte[1000 * inputLine.getSampleRate() * outputBitDepth.getBytes()];
                    SynthRipperStateEnum state = SynthRipperStateEnum.IDLE;
                    int note = 48;
                    int nbChannels = inputLine.getNbChannels();

                    @Override
                    public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {

                        if (state == SynthRipperStateEnum.NOTE_ON || state == SynthRipperStateEnum.NOTE_OFF) {
                            PCMconverter.convert(sampleBuffer, pcmBufferOut, nbSamples, nbChannels, outputBitDepth, true, true);

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
                        return !(note >= 72 && state == SynthRipperStateEnum.IDLE);
                    }
                };*/
            }
            generateSFZ();
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        } finally {
            if (wavRecordListener != null) {
                wavRecordListener.close();
            }
            try {
                midiDevice.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void generateSFZ() throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(new File("output/test.sfz")))) {
            out.println("<control>");
            out.println("default_path=.");
            out.println("<global>");
            out.println("<group>");
            int nbRegion = (state.highestNote - state.lowestNote) / state.noteIncrement;
            int regionSize = state.noteIncrement;
            for (int r = 0; r < nbRegion; r++) {
                out.println("<region>");
                int note = state.lowestNote + r * state.noteIncrement;
                int begin = note;
                int end = note + state.noteIncrement - 1;
                if (r == 0) {
                    begin = 0;
                }
                if (r == nbRegion - 1) {
                    end = 127;
                }
                out.println("sample=" + getOutputFile(note).getName());
                out.println("lokey=" + begin);
                out.println("pitch_keycenter=" + note);
                out.println("hikey=" + end);
            }
        }
    }

    private boolean isLastNotePlayed() {
        return state.note >= state.highestNote && state.state == SynthRipperStateEnum.IDLE;
    }

}
