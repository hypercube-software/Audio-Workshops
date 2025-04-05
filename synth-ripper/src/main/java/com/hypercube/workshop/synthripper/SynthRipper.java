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
import com.hypercube.workshop.midiworkshop.common.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.log.ThreadLogger;
import com.hypercube.workshop.synthripper.model.*;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
public class SynthRipper {
    public static final int NOISE_FLOOR_CAPTURE_DURATION_IN_SEC = 4;
    SynthRipperConfiguration conf;
    private PCMBufferFormat format;
    private PCMBufferFormat wavFormat;
    private MidiOutDevice midiOutDevice;
    private final ThreadLogger threadLogger;
    private final SynthRipperState state = new SynthRipperState();
    private final List<PresetGenerator> presetGenerators;
    private WavRecorder wavRecorder;


    public SynthRipper(List<PresetGenerator> presetGenerators) {
        this.threadLogger = new ThreadLogger();
        this.presetGenerators = presetGenerators;
    }

    public void init(SynthRipperConfiguration config) {
        this.conf = config;
        this.format = config.getAudio()
                .getAudioFormat();
        this.wavFormat = config.getAudio()
                .getWavFormat();
        initState();
    }

    private void initState() {
        MidiSettings midiSettings = conf.getMidi();
        state.noiseFloorFrequencies = new double[format.getSampleBufferSize() / 2];
        state.signalFrequencies = new double[format.getSampleBufferSize() / 2];
        state.resetNoiseFloorFrequencies();
        state.prev = null;
        state.currentBatchEntry = 0;
        state.sampleBatch = generateBatch();
    }

    private int boundValue(int value) {
        return Math.max(Math.min(127, value), 0);
    }

    List<RecordedSynthNote> generateBatch() {
        MidiSettings midiSettings = conf.getMidi();
        int veloIncrement = (int) Math.ceil(128f / midiSettings
                .getVelocityPerNote());
        int upperBoundVelocity = veloIncrement * (midiSettings.getVelocityPerNote() + 1);

        int defaultLowestNote = midiSettings.getLowestNoteInt();
        int defaultHighestNote = midiSettings.getHighestNoteInt();
        int defaultNoteIncrement = 12 / midiSettings.getNotesPerOctave();
        int ccIncrement = (int) Math.ceil(128f / midiSettings
                .getCcPerNote());
        int upperBoundCC = ccIncrement * (midiSettings.getCcPerNote() + 1);
        return conf.getSelectedPresets()
                .stream()
                .flatMap(preset -> {
                    log.info("=========== {} ===========", preset.getTitle());
                    List<RecordedSynthNote> samples = new ArrayList<>();
                    int prevCcValue = 0;
                    for (int cc : preset.getControlChanges()) {
                        if (cc == MidiPreset.NO_CC) {
                            log.info("Without CC:");
                        } else {
                            log.info("With CC " + cc + ":");
                        }
                        for (int ccValue = 1; ccValue < upperBoundCC; ccValue += ccIncrement) {
                            int lowestNote = getLowestNote(defaultLowestNote, preset);
                            int highestNote = getHighestNote(defaultHighestNote, preset);
                            int noteIncrement = getNoteIncrement(defaultNoteIncrement, preset);
                            for (int note = lowestNote; note <= highestNote; note += noteIncrement) {
                                for (int velocity = veloIncrement; velocity < upperBoundVelocity; velocity += veloIncrement) {
                                    RecordedSynthNote rs = new RecordedSynthNote();
                                    rs.setChannel(preset.getChannel() - 1);
                                    rs.setNote(computeNoteMidiZone(lowestNote, highestNote, noteIncrement, note));

                                    rs.setVelocity(computeVelocityMidiZone(velocity, veloIncrement));

                                    if (cc == MidiPreset.NO_CC) {
                                        // normal sample without any CC
                                        rs.setControlChange(MidiPreset.NO_CC);
                                        rs.setCcValue(null);
                                        prevCcValue = 0;
                                    } else {
                                        // apply a CC to the sound
                                        rs.setControlChange(cc);
                                        rs.setCcValue(computeControlChangeMidiZone(prevCcValue, ccValue));
                                    }
                                    rs.setName(getNoteName(preset, note));
                                    rs.setPreset(preset);
                                    rs.setFile(getOutputFile(rs));
                                    samples.add(rs);

                                    log.info("%s %s = %s".formatted(rs.getNote(), rs.getVelocity(), rs.getFile()
                                            .getName()));
                                }
                            }
                            if (cc == MidiPreset.NO_CC) {
                                break;
                            } else {
                                prevCcValue = ccValue + 1;
                            }
                        }
                    }
                    return samples.stream();
                })
                .toList();
    }

    private String getNoteName(MidiPreset preset, int note) {
        String noteName = MidiNote.fromValue(note)
                .name();
        return preset.getDrumKitNotes()
                .stream()
                .filter(n -> n.note() == note)
                .map(dn -> "%03d %s %s".formatted(note, noteName, dn.title()))
                .findFirst()
                .orElse("%03d %s".formatted(note, noteName));
    }

    private int getNoteIncrement(int defaultNoteIncrement, MidiPreset preset) {
        return preset.getDrumKitNotes()
                .isEmpty() ? defaultNoteIncrement : 1;
    }

    private int getHighestNote(int defaultHighestNote, MidiPreset preset) {
        return preset.getDrumKitNotes()
                .stream()
                .map(DrumKitNote::note)
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElse(defaultHighestNote);

    }

    private int getLowestNote(int defaultLowestNote, MidiPreset preset) {
        return preset.getDrumKitNotes()
                .stream()
                .map(DrumKitNote::note)
                .sorted()
                .findFirst()
                .orElse(defaultLowestNote);
    }

    private MidiZone computeControlChangeMidiZone(int prevCC, int ccValue) {
        return new MidiZone(prevCC, boundValue(ccValue), boundValue(ccValue));
    }

    private MidiZone computeVelocityMidiZone(int velocity, int veloIncrement) {
        return new MidiZone(boundValue(velocity - veloIncrement + 1), boundValue(velocity), boundValue(velocity));
    }

    private MidiZone computeNoteMidiZone(int lowestNote, int highestNote, int noteIncrement, int note) {
        Supplier<Integer> low = () -> {
            if (note == lowestNote) {
                return 0;
            } else {
                return boundValue(Math.max(0, note - noteIncrement + 1));
            }
        };
        Supplier<Integer> high = () -> {
            if (note == highestNote) {
                return 127;
            } else {
                return boundValue(note);
            }
        };
        return new MidiZone(low.get(), high.get(), boundValue(note));
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
        try (FFTCalculator fftCalculator = new FFTCalculator(format, new BlackmanHarris())) {
            try (FFTCalculator shortTermFFTCalculator = new FFTCalculator(format.withDuration(1), new BlackmanHarris())) {
                try (AudioInputLine inputLine = new AudioInputLine(audioInputDevice, format)) {
                    try (AudioOutputLine outputLine = new AudioOutputLine(audioOutputDevice, format)) {
                        midiDevice.sendAllOff();
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
                midiDevice.sendAllOff();
                midiDevice.close();
            } catch (IOException e) {
                throw new MidiError(e);
            }
        }
        savePresets();
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
        state.durationInSec += format.samplesToMilliseconds(buffer.nbSamples()) / 1000;
        state.durationInSamples += buffer.nbSamples();
        //
        // inspect incoming samples and change state accordingly
        //
        try {
            if (state.endOfInit()) {
                threadLogger.log("======================================================");
                threadLogger.log("Capture noise floor...");
                state.resetNoiseFloorFrequencies();
                state.changeState(SynthRipperStateEnum.ACQUIRE_NOISE_FLOOR);
            } else if (state.acquireNoiseFloor()) {
                acquireNoiseFloor(fftCalculator, buffer);
                if (state.endOfAcquireNoiseFloor()) {
                    state.changeState(SynthRipperStateEnum.IDLE);
                    double max = 0;
                    for (int ch = 0; ch < buffer.nbChannels(); ch++) {
                        for (int s = 0; s < buffer.nbSamples(); s++) {
                            max = Math.max(max, buffer.sample(ch, s));
                        }
                    }
                    double noiseFloor = Arrays.stream(state.noiseFloorFrequencies)
                            .average()
                            .orElse(0);
                    threadLogger.log("Noise floor via FFT: avg %f dB, max %f dB".formatted(noiseFloor, max));
                    if (max == 0) {
                        throw new AudioError("No sound recorded, check your sound card settings");
                    }
                }
            } else {
                updateSignalFrequencies(fftCalculator, buffer);

                if (state.endOfIdle()) {
                    sendNoteOn();
                    state.changeState(SynthRipperStateEnum.NOTE_ON_SEND);
                } else if (state.hearNothing()) {
                    threadLogger.log("**** ERROR **** NOTE_ON_START timeout after " + state.durationInSec + " sec");
                    state.noteOffSampleMarker = state.durationInSamples;
                    sendNoteOff();
                    threadLogger.log("NOTE_OFF after " + state.durationInSec + " sec at position " + state.noteOffSampleMarker);
                    state.changeState(SynthRipperStateEnum.NOTE_OFF);
                } else if (state.soundDetected()) {
                    startPosInsamples = preciseLookupSignalStart(shortTermFFTCalculator, buffer);
                    buffer.split(startPosInsamples, shortTermFFTCalculator.getFormat()
                                    .getSampleBufferSize())
                            .fadeIn();
                    threadLogger.log("NOTE_ON_START after " + state.durationInSec + " sec");
                    state.changeState(SynthRipperStateEnum.NOTE_ON_START);
                } else if (state.endOfNoteOn()) {
                    state.noteOffSampleMarker = state.durationInSamples;
                    sendNoteOff();
                    threadLogger.log("NOTE_OFF after " + state.durationInSec + " sec at position " + state.noteOffSampleMarker);
                    state.changeState(SynthRipperStateEnum.NOTE_OFF);
                } else if (state.endOfNoteOff()) {
                    buffer.split(0, buffer.nbSamples())
                            .fadeOut();
                    threadLogger.log("Release time is " + state.durationInSec + " sec");
                    state.getCurrentRecordedSynthNote()
                            .setReleaseTimeInSec(state.durationInSec);
                    midiOutDevice.sendAllOff();
                    state.changeState(SynthRipperStateEnum.NOTE_OFF_DONE);
                } else if (state.endOfNoteRecord()) {
                    onNoteRecordTerminated();
                    threadLogger.log("IDLE after " + state.durationInSec + " sec");
                    state.changeState(SynthRipperStateEnum.IDLE);
                }
            }
        } catch (InvalidMidiDataException | IOException e) {
            throw new AudioError(e);
        }
        //
        // record WAV to disk
        //
        if (wavRecorder != null &&
                (state.state == SynthRipperStateEnum.NOTE_ON_START ||
                        state.state == SynthRipperStateEnum.NOTE_OFF ||
                        state.state == SynthRipperStateEnum.NOTE_OFF_DONE)) {
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
        state.nextRecordedSynthNote();
    }

    private void sendNoteOff() throws InvalidMidiDataException {
        RecordedSynthNote currentRecordedSynthNote = state.getCurrentRecordedSynthNote();
        MidiEvent noteOff = new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, currentRecordedSynthNote.getChannel(), currentRecordedSynthNote
                .getNote()
                .value(), 0), 0);
        midiOutDevice.send(noteOff);

        if (!state.isSilentBuffer()) {
            threadLogger.log("Looping sound detected");
            LoopSetting loopSetting = new LoopSetting();
            // TODO: detect loop start instead of nsec before end
            loopSetting.setSampleStart(state.noteOffSampleMarker - (long) (3.0f * format.getSampleRate()));
            loopSetting.setSampleEnd(state.noteOffSampleMarker);
            currentRecordedSynthNote
                    .setLoopSetting(loopSetting);
        }
    }

    private File getOutputFile(RecordedSynthNote recordedSynthNote) {
        MidiPreset preset = recordedSynthNote.getPreset();
        int note = recordedSynthNote.getNote()
                .value();
        int velocity = recordedSynthNote.getVelocity()
                .value();
        var midiNote = MidiNote.fromValue(note);

        StringBuffer sb = new StringBuffer();
        sb.append("%s/%s %s/".formatted(conf.getOutputDir(), preset.getId(), preset.getTitle()));
        sb.append("%s - Velo %03d".formatted(recordedSynthNote.getName(), Math.min(127, velocity)));
        if (recordedSynthNote.getControlChange() != MidiPreset.NO_CC) {
            sb.append(" CC%d[%d]".formatted(recordedSynthNote.getControlChange(), recordedSynthNote.getCcValue()
                    .value()));
        }
        sb.append(".wav");
        return new File(sb.toString());
    }

    private void sendNoteOn() throws InvalidMidiDataException, IOException {
        RecordedSynthNote currentRecordedSynthNote = state.getCurrentRecordedSynthNote();
        RecordedSynthNote previousRecordedSynthNote = state.getPreviousRecordedSynthNote();
        if (previousRecordedSynthNote == null || !currentRecordedSynthNote.getPreset()
                .getTitle()
                .equals(previousRecordedSynthNote.getPreset()
                        .getTitle())) {
            threadLogger.log("======================================================");
            threadLogger.log("Preset Change \"%s\"".formatted(currentRecordedSynthNote.getPreset()
                    .getTitle()));
            midiOutDevice.sendPresetChange(currentRecordedSynthNote.getPreset());
        }
        if (previousRecordedSynthNote != null && currentRecordedSynthNote.getControlChange() != previousRecordedSynthNote.getControlChange()) {
            threadLogger.log("--------------------");
            threadLogger.log("Control Change " + currentRecordedSynthNote.getControlChange());
        }
        File outputFile = getOutputFile(currentRecordedSynthNote);
        threadLogger.log("Record %s".formatted(outputFile.getParentFile()
                .getName() + "/" + outputFile.getName()));
        wavRecorder = new WavRecorder(outputFile, wavFormat, threadLogger);
        if (currentRecordedSynthNote.getControlChange() != MidiPreset.NO_CC) {
            MidiEvent controlChangeEvent = new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, currentRecordedSynthNote.getChannel(), currentRecordedSynthNote.getControlChange(), currentRecordedSynthNote.getCcValue()
                    .value()), 0);
            threadLogger.log("Send Control Change " + currentRecordedSynthNote.getControlChange() + " with value " + currentRecordedSynthNote.getCcValue()
                    .value() + " on channel " + currentRecordedSynthNote.getChannel());
            midiOutDevice.send(controlChangeEvent);
        } else {
            threadLogger.log("Reset all controllers on channel " + currentRecordedSynthNote.getChannel());
            midiOutDevice.sendAllcontrollersOff(currentRecordedSynthNote.getChannel());
        }
        MidiEvent noteOn = new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, currentRecordedSynthNote.getChannel(), currentRecordedSynthNote.getNote()
                .value(), currentRecordedSynthNote.getVelocity()
                .value()), 0);
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

    private void savePresets() {
        String outputFormat = conf.getMidi()
                .getOutputFormat();
        presetGenerators.stream()
                .filter(pg -> pg.getAlias()
                        .equals(outputFormat))
                .findFirst()
                .ifPresentOrElse(pg ->
                {
                    pg.generate(conf, state.sampleBatch);
                }, () -> log.error("Unknown output format: " + outputFormat));
    }

    private boolean finished() {
        return state.state == SynthRipperStateEnum.IDLE && state.getCurrentRecordedSynthNote() == null;
    }

}
