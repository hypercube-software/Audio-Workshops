package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Component
public class DecentSamplerPresetGenerator implements PresetGenerator {
    /**
     * Width of the crossfade (seconds) between the ending sustain voice and the
     * non-looping release sample on note-off. The sustain's release envelope and the
     * release sample's attack envelope overlap over this duration so the transition
     * from the loop to the recorded tail blends smoothly instead of clicking.
     */
    private static final float LOOP_RELEASE_CROSSFADE_SEC = 0.1f;
    @Override
    public String getAlias() {
        return "DS";
    }

    @Override
    public void generate(SynthRipperConfiguration conf, List<RecordedSynthNote> sampleBatch) {
        var recordsPerPresets = sampleBatch.stream()
                .collect(groupingBy(RecordedSynthNote::getPreset));
        recordsPerPresets.forEach((preset, recordedSamples) -> {
            String presetId = preset.getShortId();
            File sfzFile = new File("%s/%s %s.dspreset".formatted(conf.getOutputDir(), presetId, preset.getId()
                    .name()));
            DecentSamplerPreset ds = forgeDecentSamplerPreset(sfzFile, recordedSamples);
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(DecentSamplerPreset.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                jaxbMarshaller.marshal(ds, sfzFile);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public DecentSamplerPreset forgeDecentSamplerPreset(File presetFile, List<RecordedSynthNote> recordedSynthNotes) {
        DecentSamplerPreset ds = new DecentSamplerPreset();
        var recordsPerVelocity = recordedSynthNotes.stream()
                .collect(groupingBy(RecordedSynthNote::getVelocity));
        recordsPerVelocity.forEach((velocity, recordedSamplesPerVelocity) -> {
            forgeVelocityGroups(presetFile, velocity, recordedSamplesPerVelocity, ds);
        });
        ds.getGroups()
                .sort(Comparator.comparing(RoundRobinGroup::getTags)
                        .thenComparing(RoundRobinGroup::getName));
        return ds;
    }

    private void forgeVelocityGroups(File presetFile, MidiZone velocity, List<RecordedSynthNote> recordedSamplesPerVelocity, DecentSamplerPreset ds) {
        var recordsPerCC = recordedSamplesPerVelocity.stream()
                .collect(groupingBy(RecordedSynthNote::getControlChange));
        recordsPerCC.forEach((cc, recordedSamplesPerControlChange) -> {
            if (cc == MidiPreset.NO_CC) {
                forgeNoteGroups(presetFile, velocity, cc, null, recordedSamplesPerControlChange, null, ds);
            } else {
                List<Binding> bindings = new ArrayList<>();
                if (!ds.getMidi()
                        .hasControlChange(cc)) {
                    MidiControlChange midiControlChange = new MidiControlChange(cc, bindings);
                    ds.getMidi()
                            .getMidiControlChangeList()
                            .add(midiControlChange);
                }
                var recordsPerCCValue = recordedSamplesPerControlChange.stream()
                        .collect(groupingBy(RecordedSynthNote::getCcValue));
                recordsPerCCValue.forEach((ccValue, recordedSamplesPerControlChangeValue) -> {
                    forgeNoteGroups(presetFile, velocity, cc, ccValue, recordedSamplesPerControlChangeValue, bindings, ds);
                });
            }
        });
    }

    private void forgeNoteGroups(File presetFile, MidiZone velocity, int controlChange, MidiZone controlChangeValue, List<RecordedSynthNote> recordedSamples, List<Binding> bindings, DecentSamplerPreset ds) {
        String groupTag;
        float modVolume;
        if (controlChange != MidiPreset.NO_CC) {
            groupTag = "CC%03d-%03d".formatted(controlChange, controlChangeValue.value());
            modVolume = 0f;
            if (bindings != null && bindings.stream()
                    .filter(b -> b.getTags()
                            .equals(groupTag))
                    .findAny()
                    .isEmpty()) {
                bindings.add(forgeControlChangeBinding(groupTag, controlChange, controlChangeValue));
            }
        } else {
            groupTag = "NoCC";
            modVolume = 1f;
        }
        String velocityTag = "vel%03d".formatted(velocity.high());

        for (RecordedSynthNote recordedSample : recordedSamples) {
            RoundRobinGroup group = new RoundRobinGroup();
            group.setName(SynthRipperConfiguration.noteNameFromPitch(recordedSample.getNote()
                    .value()));
            group.setTags("%s %s".formatted(velocityTag, groupTag));
            group.setModVolume(modVolume);
            group.setLoVel(velocity.low());
            group.setHiVel(velocity.high());
            group.setReleaseTimeInSec(recordedSample.getReleaseTimeInSec());

            String path = presetFile.getParentFile()
                    .toPath()
                    .relativize(recordedSample.getFile()
                            .toPath())
                    .toString()
                    .replace("\\", "/");

            Sample sample = new Sample();
            sample.setTrigger(TriggerMode.ATTACK);
            sample.setPath(path);
            sample.setLowNote(recordedSample.getNote()
                    .low());
            sample.setRootNote(recordedSample.getNote()
                    .value());
            sample.setHiNote(recordedSample.getNote()
                    .high());

            Optional.ofNullable(recordedSample.getLoopSetting())
                    .ifPresent(l -> {
                        sample.setLoopEnabled(true);
                        sample.setLoopStart(l.getSampleStart());
                        sample.setLoopEnd(l.getSampleEnd());
                        // ~5% of the loop length is the recommended crossfade (see DecentSampler dev guide)
                        sample.setLoopCrossfade(Math.max(1L, l.getLoopLength() / 20L));
                        // The sustain sample must not play past its loop end: Decent Sampler keeps
                        // looping after the note is released, so the recorded decay tail is played by
                        // a dedicated non-looping "release" sample (see forgeReleaseSample).
                        sample.setEnd(l.getSampleEnd());
                        // Blend the sustain voice into the release sample: as the sustain
                        // releases over LOOP_RELEASE_CROSSFADE_SEC, the release sample fades in
                        // over the same window (see forgeReleaseSample), avoiding a clicky cut.
                        sample.setAttack(0f);
                        sample.setDecay(0f);
                        sample.setSustain(1f);
                        sample.setRelease(LOOP_RELEASE_CROSSFADE_SEC);
                    });
            group.getSamples()
                    .add(sample);
            forgeReleaseSample(recordedSample, sample, group);
            ds.getGroups()
                    .add(group);
        }
    }

    private Binding forgeControlChangeBinding(String groupName, int cc, MidiZone zone) {
        StringBuilder envelope = new StringBuilder();
        if (zone.low() > 0) {
            envelope.append("0,0;");
        }
        envelope.append("%d,1;".formatted(zone.low()));
        if (zone.high() < 127) {
            envelope.append("%d,1;".formatted(zone.high()));
            envelope.append("128,0");
        } else {
            envelope.append("128,1");
        }
        return new Binding("amp", "group", null, groupName, "AMP_VOLUME", "table", envelope.toString());
    }

    /**
     * Decent Sampler does not leave a loop when the note is released, so the
     * recorded decay tail would otherwise be looping forever. To make the sound
     * play out naturally, add a non-looping {@code trigger=release} sample that
     * spans from the recorded loop end to the end of the file, playing once.
     */
    private void forgeReleaseSample(RecordedSynthNote recordedSample, Sample loopSample, RoundRobinGroup group) {
        Optional.ofNullable(recordedSample.getLoopSetting())
                .ifPresent(l -> {
                    long totalSamples = readTotalSampleCount(recordedSample.getFile());
                    if (totalSamples <= l.getSampleEnd() + 1) {
                        // no audio after the loop end: nothing to release
                        return;
                    }
                    Sample release = new Sample();
                    release.setTrigger(TriggerMode.RELEASE);
                    release.setPath(loopSample.getPath());
                    release.setLowNote(loopSample.getLowNote());
                    release.setRootNote(loopSample.getRootNote());
                    release.setHiNote(loopSample.getHiNote());
                    release.setStart((int) l.getSampleEnd());
                    release.setEnd(totalSamples - 1);
                    release.setLoopEnabled(false);
                    // Fade the release sample in over the same window as the sustain voice
                    // releases (see forgeNoteGroups), so loop and tail crossfade smoothly.
                    release.setAttack(LOOP_RELEASE_CROSSFADE_SEC);
                    release.setDecay(0f);
                    release.setSustain(1f);
                    group.getSamples()
                            .add(release);
                });
    }

    private long readTotalSampleCount(File wavFile) {
        try (RiffReader rf = new RiffReader(wavFile, false)) {
            return rf.parse()
                    .getAudioInfo()
                    .getNbSamples();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read wav length for release sample: " + wavFile, e);
        }
    }
}
