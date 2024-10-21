package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.insights.dft.fast.FFTCalculator;
import com.hypercube.workshop.audioworkshop.common.insights.dft.windows.BlackmanHarris;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMMarker;
import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.log.ThreadLogger;
import com.hypercube.workshop.synthripper.model.LoopSetting;
import com.hypercube.workshop.synthripper.model.SynthRipperState;
import com.hypercube.workshop.synthripper.model.SynthRipperStateEnum;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SynthRipper {
    public static final int NOISE_FLOOR_CAPTURE_DURATION_IN_SEC = 4;
    private final SynthRipperConfiguration conf;
    private final PCMBufferFormat format;
    private final PCMBufferFormat wavFormat;
    private MidiOutDevice midiOutDevice;
    private final ThreadLogger threadLogger;
    private final SynthRipperState state = new SynthRipperState();
    private final List<LoopSetting> loopSettings = new ArrayList<>();

    private WavRecorder wavRecorder;

    private File getOutputFile(MidiPreset preset, int note, int velocity) {
        var midiNote = MidiNote.fromValue(note);
        return new File("%s/%01d-%03d %s/Note %s - Velo %03d.wav".formatted(conf.getOutputDir(), preset.bank(), preset.program(), preset.title(), midiNote.name(), Math.min(127, velocity)));
    }

    public SynthRipper(SynthRipperConfiguration config) {
        this.threadLogger = new ThreadLogger();
        this.conf = config;
        this.format = config.getAudio()
                .getAudioFormat();
        this.wavFormat = config.getAudio()
                .getWavFormat();
        initState(config);
    }

    private void initState(SynthRipperConfiguration config) {
        MidiSettings midiSettings = config.getMidi();
        state.presetIndex = 0;
        state.lowestNote = midiSettings.getLowestNoteInt();
        state.highestNote = midiSettings.getHighestNoteInt();
        state.noteIncrement = 12 / midiSettings.getNotesPerOctave();
        state.veloIncrement = (int) Math.ceil(127.f / midiSettings.getVelocityPerNote());
        state.note = state.lowestNote;
        state.velocity = state.veloIncrement;
        state.upperBoundVelocity = state.veloIncrement * (midiSettings.getVelocityPerNote() + 1);
        state.noiseFloorFrequencies = new double[format.getSampleBufferSize() / 2];
        state.signalFrequencies = new double[format.getSampleBufferSize() / 2];
        state.resetNoiseFloorFrequencies();
        updateCurrentPreset();
    }


    public void recordSynth(AudioInputDevice audioInputDevice, AudioOutputDevice audioOutputDevice, MidiOutDevice midiDevice) throws IOException {
        createOutputDir();
        state.nbChannels = format.getNbChannels();
        state.maxNoteReleaseDurationSec = conf.getMidi()
                .getMaxNoteReleaseDurationSec();
        state.maxNoteDurationSec = conf.getMidi()
                .getMaxNoteDurationSec();
        this.midiOutDevice = midiDevice;
        this.midiOutDevice.open();
        this.midiOutDevice.sendAllOff();
        try (FFTCalculator fftCalculator = new FFTCalculator(format, new BlackmanHarris())) {
            try (FFTCalculator shortTermFFTCalculator = new FFTCalculator(format.withDuration(1), new BlackmanHarris())) {
                try (AudioInputLine inputLine = new AudioInputLine(audioInputDevice, format)) {
                    try (AudioOutputLine outputLine = new AudioOutputLine(audioOutputDevice, format)) {
                        threadLogger.start();
                        outputLine.start();
                        inputLine.record((sampleBuffer, pcmBuffer, pcmSize) -> onNewBuffer(fftCalculator, shortTermFFTCalculator, sampleBuffer, pcmBuffer, pcmSize), outputLine);
                    } finally {
                        threadLogger.stop();
                    }
                }
            }
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        } finally {
            try {
                midiDevice.close();
            } catch (IOException e) {
                throw new MidiError(e);
            }
        }
        generateSFZ();
    }

    private void createOutputDir() {
        File f = new File(conf.getOutputDir());
        f.mkdirs();
    }

    /**
     * This method is basically a state machine
     *
     * @param fftCalculator          Used to grossly detect the beginning of the sound
     * @param shortTermFFTCalculator Used to detect precisely the beginning of the sound
     * @param buffer                 Input buffer of samples
     * @param pcmBuffer              Not Used
     * @param pcmSize                Not Used
     * @return false to stop recording
     */
    private boolean onNewBuffer(FFTCalculator fftCalculator, FFTCalculator shortTermFFTCalculator, SampleBuffer buffer, byte[] pcmBuffer, int pcmSize) {
        //
        // Used to truncate the very first block of samples (where precisely start the sound)
        //
        int startPosInsamples = 0;
        //
        // update duration
        //
        state.durationInSec += (float) buffer.nbSamples() / format.getSampleRate();
        state.durationInSamples += buffer.nbSamples();
        //
        // inspect incoming samples and change state accordingly
        //
        try {
            if (state.endOfInit()) {
                threadLogger.info("Capture noise floor...");
                state.resetNoiseFloorFrequencies();
                state.changeState(SynthRipperStateEnum.ACQUIRE_NOISE_FLOOR);
            } else if (state.acquireNoiseFloor()) {
                acquireNoiseFloor(fftCalculator, buffer);
                if (state.endOfAcquireNoiseFloor()) {
                    state.changeState(SynthRipperStateEnum.IDLE);
                    double max = Arrays.stream(state.noiseFloorFrequencies)
                            .average()
                            .orElse(0);
                    threadLogger.info("Noise floor via FFT: %f dB".formatted(max));
                }
            } else {
                updateSignalFrequencies(fftCalculator, buffer);

                if (state.endOfIdle()) {
                    sendNoteOn();
                    state.changeState(SynthRipperStateEnum.NOTE_ON_SEND);
                } else if (state.soundDetected()) {
                    startPosInsamples = preciseLookupSignalStart(shortTermFFTCalculator, buffer);
                    buffer.split(startPosInsamples, shortTermFFTCalculator.getFormat()
                                    .getSampleBufferSize())
                            .fadeIn();
                    threadLogger.info("NOTE_ON_START after " + state.durationInSec + " sec");
                    state.changeState(SynthRipperStateEnum.NOTE_ON_START);
                } else if (state.endOfNoteOn()) {
                    state.noteOffSampleMarker = state.durationInSamples;
                    sendNoteOff();
                    threadLogger.info("NOTE_OFF after " + state.durationInSec + " sec at position " + state.noteOffSampleMarker);
                    state.changeState(SynthRipperStateEnum.NOTE_OFF);
                } else if (state.endOfNoteOff()) {
                    buffer.split(0, buffer.nbSamples())
                            .fadeOut();
                    state.changeState(SynthRipperStateEnum.NOTE_OFF_DONE);
                } else if (state.endOfNoteRecord()) {
                    onNoteRecordTerminated();
                    threadLogger.info("IDLE after " + state.durationInSec + " sec");
                    state.changeState(SynthRipperStateEnum.IDLE);
                }
            }
        } catch (InvalidMidiDataException | IOException e) {
            throw new AudioError(e);
        }
        //
        // record WAV to disk
        //
        if (wavRecorder != null && (state.state == SynthRipperStateEnum.NOTE_ON_START || state.state == SynthRipperStateEnum.NOTE_OFF || state.state == SynthRipperStateEnum.NOTE_OFF_DONE)) {
            wavRecorder.write(buffer, startPosInsamples, conf.getAudio()
                    .getChannelMap());
        }
        return !finished();
    }

    private int preciseLookupSignalStart(FFTCalculator shortTermFFTCalculator, SampleBuffer buffer) {
        int sampleBufferSize = shortTermFFTCalculator.getFormat()
                .getSampleBufferSize();
        int nbBlocks = buffer.nbSamples() / sampleBufferSize;
        int scale = state.noiseFloorFrequencies.length / nbBlocks;
        shortTermFFTCalculator.reset();
        for (int b = 0; b < nbBlocks; b++) {
            SampleBuffer blockBuffer = buffer.split(b * sampleBufferSize, sampleBufferSize);
            shortTermFFTCalculator.onBuffer(blockBuffer);
        }
        var magnitudes = shortTermFFTCalculator.getMagnitudes();
        for (int ch = 0; ch < 1; ch++) {
            var firstMagnitudes = magnitudes[ch].get(0);
            for (int b = 0; b < nbBlocks; b++) {
                var blockMagnitudes = magnitudes[ch].get(b);
                for (int bin = 0; bin < blockMagnitudes.getNbBin(); bin++) {
                    if (blockMagnitudes.getMagnitudes()[bin] > state.noiseFloorFrequencies[bin * scale]) {
                        int startOffset = b * sampleBufferSize;
                        return startOffset;
                    }
                }
            }
        }
        return 0;
    }

    private void updateSignalFrequencies(FFTCalculator fftCalculator, SampleBuffer buffer) {
        fftCalculator.onBuffer(buffer);
        int ch = 0;
        for (int bin = 0; bin < state.noiseFloorFrequencies.length; bin++) {
            state.signalFrequencies[bin] = fftCalculator.getMagnitudes()[ch].getLast()
                    .getMagnitudes()[bin];
        }
    }

    private void onNoteRecordTerminated() throws IOException {
        wavRecorder.endWrite();
        wavRecorder.writeMarkers(List.of(PCMMarker.of("Release", state.noteOffSampleMarker)));
        wavRecorder.close();
        wavRecorder = null;
        state.velocity = state.velocity + state.veloIncrement;
        if (state.velocity >= state.upperBoundVelocity) {
            // all velocities are recorded, jump to the next note
            state.velocity = state.veloIncrement;
            state.note += state.noteIncrement;
            if (state.note > state.highestNote) {
                state.note = state.lowestNote;
                state.presetIndex++;
                updateCurrentPreset();
            }
        }
    }

    private void updateCurrentPreset() {
        List<MidiPreset> selectedPresets = conf.getMidi()
                .getSelectedPresets();
        if (state.presetIndex < selectedPresets.size()) {
            state.preset = selectedPresets.get(state.presetIndex);
        } else {
            state.preset = null;
        }
    }

    private void sendNoteOff() throws InvalidMidiDataException {
        MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, state.note, 0), 0);
        midiOutDevice.send(noteOff);

        if (!state.isSilentBuffer()) {
            threadLogger.info("Looping sound detected");
            LoopSetting currentLoop = new LoopSetting();
            currentLoop.setNote(state.note);
            currentLoop.setPreset(state.preset);
            currentLoop.setSampleStart((long) (1.0f * format.getSampleRate())); // TODO: detect loop start instead of 1sec
            currentLoop.setSampleEnd(state.noteOffSampleMarker);
            loopSettings.add(currentLoop);
        }
    }

    private void sendNoteOn() throws InvalidMidiDataException, IOException {
        if (state.note == state.lowestNote && state.isFirstVelocity()) {
            threadLogger.info("Preset Change %d-%03d".formatted(state.preset.bank(), state.preset.program()));
            midiOutDevice.sendPresetChange(state.preset);
        }
        File outputFile = getOutputFile(state.preset, state.note, state.velocity);
        threadLogger.info("Record %s".formatted(outputFile.getParentFile()
                .getName() + "/" + outputFile.getName()));
        wavRecorder = new WavRecorder(outputFile, wavFormat);
        MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, state.note, Math.min(127, state.velocity)), 0);
        midiOutDevice.send(noteOn);
    }

    private void acquireNoiseFloor(FFTCalculator fftCalculator, SampleBuffer buffer) {

        fftCalculator.onBuffer(buffer);
        int ch = 0;
        for (int bin = 0; bin < state.noiseFloorFrequencies.length; bin++) {
            state.noiseFloorFrequencies[bin] = Math.max(state.noiseFloorFrequencies[bin], fftCalculator.getMagnitudes()[ch].getLast()
                    .getMagnitudes()[bin]);
        }
        state.noiseSamplesRead += buffer.nbSamples();
    }

    private void generateSFZ() {
        conf.getMidi()
                .getSelectedPresets()
                .forEach(p -> {
                    File sfzFile = new File("%s/%01d-%03d %s.sfz".formatted(conf.getOutputDir(), p.bank(), p.program(), p.title()));
                    try (PrintWriter out = new PrintWriter(new FileOutputStream(sfzFile))) {
                        final String SEPARATOR = "----------------------------------------------------------";
                        out.println(SEPARATOR);
                        out.println("%s".formatted(p.title()));
                        out.println(SEPARATOR);
                        out.println("<control>");
                        out.println("default_path=.");
                        out.println("<global>");
                        int prevVelo = -1;
                        int nbVelocityPerNote = conf.getMidi()
                                .getVelocityPerNote();
                        int velocityOffset = (int) Math.ceil(127.f / nbVelocityPerNote);
                        for (int group = 1; group <= nbVelocityPerNote; group++) {
                            int startVelo = prevVelo + 1;
                            int endVelo = Math.min(127, velocityOffset * group);
                            out.println(SEPARATOR);
                            out.println(" Velocity layer %03d".formatted(endVelo));
                            out.println(SEPARATOR);
                            out.println("<group>");
                            out.println("lovel=" + startVelo);
                            out.println("hivel=" + endVelo);
                            prevVelo = endVelo;

                            out.println("");
                            int nbRegion = Math.min(1, state.highestNote + 1 - state.lowestNote / state.noteIncrement);
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
                                        .relativize(getOutputFile(p, note, endVelo).toPath())
                                        .toString()
                                        .replace("\\", "/");

                                getLoop(p, note).ifPresentOrElse(l -> {
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
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private Optional<LoopSetting> getLoop(MidiPreset preset, int note) {
        return loopSettings.stream()
                .filter(l -> l.getPreset()
                        .equals(preset) && l.getNote() == note)
                .findFirst();
    }

    private boolean finished() {
        return state.state == SynthRipperStateEnum.IDLE && state.preset == null;
    }

}
