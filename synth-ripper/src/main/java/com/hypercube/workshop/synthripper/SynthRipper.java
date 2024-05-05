package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SynthRipper {
    private final SynthRipperConfiguration conf;
    private final AudioLineFormat format;
    private final AudioLineFormat wavFormat;
    private MidiOutDevice midiOutDevice;
    private AudioInputLine inputLine;

    private final SynthRipperState state = new SynthRipperState();
    private final List<LoopSetting> loopSettings = new ArrayList<>();

    private WavRecorder wavRecorder;

    private File getOutputFile(int cc, int note, int velocity) {
        var midiNote = MidiNote.fromValue(note);
        return new File("%s/%03d %s/Note %s - Velo %03d.wav".formatted(conf.getOutputDir(), cc, conf.getMidi()
                .getFilename(cc), midiNote.name(), (int) Math.min(127, velocity)));
    }

    public SynthRipper(SynthRipperConfiguration config) throws IOException {
        this.conf = config;
        this.format = config.getAudio()
                .getAudioFormat();
        this.wavFormat = config.getAudio()
                .getWavFormat();
        initState(config);
    }

    private void initState(SynthRipperConfiguration config) {
        MidiSettings midiSettings = config.getMidi();
        state.lowestCC = midiSettings.getLowestCC();
        state.highestCC = midiSettings.getHighestCC();
        state.lowestNote = midiSettings.getLowestNoteInt();
        state.highestNote = midiSettings.getHighestNoteInt();
        state.noteIncrement = 12 / midiSettings.getNotesPerOctave();
        state.veloIncrement = (int) Math.ceil(127.f / midiSettings.getVelocityPerNote());
        state.cc = state.lowestCC;
        state.note = state.lowestNote;
        state.velocity = state.veloIncrement;
        state.upperBoundVelocity = state.veloIncrement * (midiSettings.getVelocityPerNote() + 1);
    }

    public void recordSynth(AudioInputDevice audioInputDevice, AudioOutputDevice audioOutputDevice, MidiOutDevice midiDevice) throws IOException {
        createOutputDir();
        state.nbChannels = format.getNbChannels();
        state.loudnessPerChannel = new float[state.nbChannels];
        state.maxNoteReleaseDurationSec = conf.getMidi()
                .getMaxNoteReleaseDurationSec();
        state.maxNoteDurationSec = conf.getMidi()
                .getMaxNoteDurationSec();
        this.midiOutDevice = midiDevice;
        this.midiOutDevice.open();
        this.midiOutDevice.sendAllOff();
        try (AudioInputLine inputLine = new AudioInputLine(audioInputDevice, format)) {
            this.inputLine = inputLine;
            try (AudioOutputLine outputLine = new AudioOutputLine(audioOutputDevice, format)) {
                outputLine.start();
                inputLine.record(this::onNewBuffer, outputLine);
            }
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        } finally {
            if (wavRecorder != null) {
                wavRecorder.close();
            }
            try {
                midiDevice.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        generateSFZ();
    }

    private void createOutputDir() {
        File f = new File(conf.getOutputDir());
        f.mkdirs();
    }

    private boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize) {
        computeLoudness(sampleBuffer, nbSamples);

        state.durationInSec += (float) nbSamples / inputLine.getFormat()
                .getSampleRate();
        try {
            if (state.state == SynthRipperStateEnum.GET_NOISE_FLOOR) {
                onGetNoiseFloorState();
            } else if (state.state == SynthRipperStateEnum.IDLE && state.durationInSec > 1) {
                onIdleStateTerminated();
            } else if (state.state == SynthRipperStateEnum.NOTE_ON && (state.durationInSec > state.maxNoteDurationSec /*|| state.isSilentBuffer()*/)) {
                onNoteOnTerminated();
            } else if (state.state == SynthRipperStateEnum.NOTE_OFF && state.durationInSec > state.maxNoteReleaseDurationSec && state.isSilentBuffer()) {
                onNoteOffTerminated();
            }
        } catch (InvalidMidiDataException | IOException e) {
            throw new AudioError(e);
        }
        if (wavRecorder != null && (state.state == SynthRipperStateEnum.NOTE_ON || state.state == SynthRipperStateEnum.NOTE_OFF)) {
            if (!state.isSilentBuffer()) {
                wavRecorder.write(sampleBuffer, nbSamples, conf.getAudio()
                        .getChannelMap());
            }
        }
        return !finished();
    }

    private void onNoteOffTerminated() throws IOException {
        state.state = SynthRipperStateEnum.IDLE;
        state.durationInSec = 0;
        wavRecorder.close();
        wavRecorder = null;
        state.velocity = state.velocity + state.veloIncrement;
        if (state.velocity >= state.upperBoundVelocity) {
            // all velocities are recorded, jump to the next note
            state.velocity = state.veloIncrement;
            state.note += state.noteIncrement;
            if (state.note > state.highestNote) {
                state.note = state.lowestNote;
                state.cc++;
            }
        }
    }

    private void onNoteOnTerminated() throws InvalidMidiDataException {
        MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, state.note, 0), 0);
        midiOutDevice.send(noteOff);
        state.state = SynthRipperStateEnum.NOTE_OFF;

        if (!state.isSilentBuffer()) {
            LoopSetting currentLoop = new LoopSetting();
            currentLoop.setNote(state.note);
            currentLoop.setCc(state.cc);
            currentLoop.setSampleStart((long) (1.0f * format.getSampleRate()));
            currentLoop.setSampleEnd((long) (state.durationInSec * format.getSampleRate()));
            loopSettings.add(currentLoop);
        }

        state.durationInSec = 0;
    }

    private void onIdleStateTerminated() throws InvalidMidiDataException, IOException {
        if (state.note == state.lowestNote && state.isFirstVelocity()) {
            log.info("Program Change %03d".formatted(state.cc));
            MidiEvent midiCC = new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, state.cc, 0), 0);
            midiOutDevice.send(midiCC);
        }
        File outputFile = getOutputFile(state.cc, state.note, state.velocity);
        outputFile.getParentFile()
                .mkdirs();
        log.info("Record %s".formatted(outputFile.getParentFile()
                .getName() + "/" + outputFile.getName()));
        wavRecorder = new WavRecorder(outputFile, wavFormat);
        MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, state.note, Math.min(127, state.velocity)), 0);
        midiOutDevice.send(noteOn);
        state.state = SynthRipperStateEnum.NOTE_ON;
        state.durationInSec = 0;
    }

    private void onGetNoiseFloorState() {
        if (state.durationInSec > 1) {
            if (state.noiseFloor == -1) {
                log.info("Capture noise floor...");
            }
            state.noiseFloor = (float) (Math.max(state.noiseFloor, state.loudness));
        }

        if (state.durationInSec > 1 + 10) {
            log.info("Noise floor: %d dB".formatted(state.getNoiseFloorDb()));
            state.state = SynthRipperStateEnum.IDLE;
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
            String filename = conf.getMidi()
                    .getFilename(cc);
            File sfzFile = new File("%s/%03d %s.sfz".formatted(conf.getOutputDir(), cc, filename));
            try (PrintWriter out = new PrintWriter(new FileOutputStream(sfzFile))) {
                out.println("----------------------------------------------------------");
                out.println("%s".formatted(filename));
                out.println("----------------------------------------------------------");
                out.println("<control>");
                out.println("default_path=.");
                out.println("<global>");
                int prevVelo = -1;
                int nbVelocityPerNote = conf.getMidi()
                        .getVelocityPerNote();
                int velocityOffset = (int) Math.ceil(127.f / nbVelocityPerNote);
                for (int group = 1; group <= nbVelocityPerNote; group++) {
                    int startVelo = prevVelo + 1;
                    int endVelo = (int) Math.min(127, velocityOffset * group);
                    out.println("----------------------------------------------------------");
                    out.println(" Velocity layer %03d".formatted(endVelo));
                    out.println("----------------------------------------------------------");
                    out.println("<group>");
                    out.println("lovel=" + startVelo);
                    out.println("hivel=" + endVelo);
                    prevVelo = endVelo;

                    out.println("");
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
                        String path = sfzFile.getParentFile()
                                .toPath()
                                .relativize(getOutputFile(cc, note, endVelo).toPath())
                                .toString()
                                .replace("\\", "/");

                        getLoop(cc, note).ifPresentOrElse(l -> {
                            out.println("loop_mode=loop_sustain");
                            out.println("loop_start=" + l.getSampleStart());
                            out.println("loop_end=" + l.getSampleEnd());
                        }, () -> out.println("loop_mode=no_loop"));

                        out.println("sample=" + path);
                        out.println("lokey=" + begin);
                        out.println("pitch_keycenter=" + note);
                        out.println("hikey=" + end);
                        out.println("");
                    }
                }
            }
        }
    }

    private Optional<LoopSetting> getLoop(int cc, int note) {
        return loopSettings.stream()
                .filter(l -> l.getCc() == cc && l.getNote() == note)
                .findFirst();
    }

    private boolean finished() {
        return state.state == SynthRipperStateEnum.IDLE && state.cc > state.highestCC;
    }

}
