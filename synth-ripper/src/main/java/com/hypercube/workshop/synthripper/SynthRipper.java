package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.record.RecordListener;
import com.hypercube.workshop.audioworkshop.common.record.WavRecordListener;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.synthripper.config.MidiSettings;
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
    public static final int BUFFER_DURATION_MS = 60;
    private final AudioFormat format;
    private MidiOutDevice midiOutDevice;
    private AudioInputLine inputLine;

    private final SynthRipperState state = new SynthRipperState();
    private WavRecordListener wavRecordListener;

    private File getOutputFile(int cc, int note) {
        return new File("output/%03d %s - Note %03d.wav".formatted(cc, conf.getMidi()
                .getFilename(cc), note));
    }

    public SynthRipper(SynthRipperConfiguration config, AudioFormat format) throws IOException {
        this.conf = config;
        this.format = format;
        MidiSettings midiSettings = config.getMidi();
        state.lowestCC = midiSettings.getLowestCC();
        state.highestCC = midiSettings.getHighestCC();
        state.lowestNote = midiSettings.getLowestNoteInt();
        state.highestNote = midiSettings.getHighestNoteInt();
        state.noteIncrement = 12 / midiSettings.getNotesPerOctave();
        state.cc = state.lowestCC;
        state.note = state.lowestNote;
    }

    public void recordSynth(AudioInputDevice audioInputDevice, AudioOutputDevice audioOutputDevice, MidiOutDevice midiDevice) throws IOException {
        state.nbChannels = format.getChannels();
        state.loudnessPerChannel = new float[state.nbChannels];
        state.maxNoteReleaseDurationSec = conf.getMidi()
                .getMaxNoteReleaseDurationSec();
        state.maxNoteDurationSec = conf.getMidi()
                .getMaxNoteDurationSec();
        generateSFZ();
        RecordListener listener = new RecordListener() {
            @Override
            public boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
                computeLoudness(sampleBuffer, nbSamples);
                boolean isSilentBuffer = state.loudness <= state.noiseFloor;
                state.durationInSec += (float) nbSamples / inputLine.getSampleRate();
                try {
                    if (state.state == SynthRipperStateEnum.GET_NOISE_FLOOR) {
                        if (state.noiseFloor == -1) {
                            log.info("Capture noise floor...");
                        }
                        state.noiseFloor = (float) (Math.max(state.noiseFloor, state.loudness));
                        if (state.durationInSec > 4) {
                            log.info("Noise floor: %d dB".formatted(state.getNoiseFloorDb()));
                            state.state = SynthRipperStateEnum.IDLE;
                        }
                    }
                    if (state.state == SynthRipperStateEnum.IDLE && state.durationInSec > 1) {
                        if (state.note == state.lowestNote) {
                            log.info("Program Change %03d".formatted(state.cc));
                            MidiEvent midiCC = new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, state.cc, 0), 0);
                            midiOutDevice.send(midiCC);
                        }
                        File outputFile = getOutputFile(state.cc, state.note);
                        log.info("Record %s".formatted(outputFile.getName()));
                        wavRecordListener = new WavRecordListener(outputFile, format);
                        MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, state.note, 127), 0);
                        midiOutDevice.send(noteOn);
                        state.state = SynthRipperStateEnum.NOTE_ON;
                        state.durationInSec = 0;
                    } else if (state.state == SynthRipperStateEnum.NOTE_ON && (state.durationInSec > state.maxNoteDurationSec)) {
                        MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, state.note, 0), 0);
                        midiOutDevice.send(noteOff);
                        state.state = SynthRipperStateEnum.NOTE_OFF;
                        state.durationInSec = 0;
                    } else if (state.state == SynthRipperStateEnum.NOTE_OFF && (state.durationInSec > state.maxNoteReleaseDurationSec)) {
                        state.state = SynthRipperStateEnum.IDLE;
                        state.durationInSec = 0;
                        wavRecordListener.close();
                        wavRecordListener = null;
                        state.note += state.noteIncrement;
                        if (state.note > state.highestNote) {
                            state.note = state.lowestNote;
                            state.cc++;
                        }
                    }
                } catch (InvalidMidiDataException | IOException e) {
                    throw new AudioError(e);
                }
                if (wavRecordListener != null && (state.state == SynthRipperStateEnum.NOTE_ON || state.state == SynthRipperStateEnum.NOTE_OFF)) {
                    if (!isSilentBuffer) {
                        wavRecordListener.onNewBuffer(sampleBuffer, nbSamples, pcmBuffer, pcmSize);
                    }
                }
                return !finished();
            }
        };
        this.midiOutDevice = midiDevice;
        this.midiOutDevice.open();
        try (AudioInputLine inputLine = new AudioInputLine(audioInputDevice, format, BUFFER_DURATION_MS)) {
            this.inputLine = inputLine;
            try (AudioOutputLine outputLine = new AudioOutputLine(audioOutputDevice, format, BUFFER_DURATION_MS)) {
                outputLine.start();
                inputLine.record(listener, outputLine);
            }
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

    private void computeLoudness(float[][] sampleBuffer, int nbSamples) {
        for (int c = 0; c < state.nbChannels; c++) {
            for (int s = 0; s < nbSamples; s++) {
                float sample = sampleBuffer[c][s] * sampleBuffer[c][s];
                state.loudnessPerChannel[c] += sample;
            }
        }
        state.loudness = 0;
        for (int c = 0; c < state.nbChannels; c++) {
            state.loudnessPerChannel[c] = (float) Math.sqrt(state.loudnessPerChannel[c] / nbSamples);
            state.loudness += state.loudnessPerChannel[c];
        }
        state.loudness = state.loudness / state.nbChannels;
    }

    private void generateSFZ() throws FileNotFoundException {
        for (int cc = conf.getMidi()
                .getLowestCC(); cc <= conf.getMidi()
                .getHighestCC(); cc++) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(new File("output/%03d %s.sfz".formatted(cc, conf.getMidi()
                    .getFilename(cc)))))) {
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
                    out.println("sample=" + getOutputFile(cc, note).getName());
                    out.println("lokey=" + begin);
                    out.println("pitch_keycenter=" + note);
                    out.println("hikey=" + end);
                }
            }
        }
    }

    private boolean finished() {
        return state.state == SynthRipperStateEnum.IDLE && state.cc > state.highestCC;
    }

}
