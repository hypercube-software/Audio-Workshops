package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

@Component
public class DecentSamplerPresetGenerator implements PresetGenerator {
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
            DecentSampler ds = forgeDecentSamplerPreset(conf, sfzFile, recordedSamples);
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(DecentSampler.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                jaxbMarshaller.marshal(ds, sfzFile);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public DecentSampler forgeDecentSamplerPreset(SynthRipperConfiguration conf, File presetFile, List<RecordedSynthNote> recordedSynthNotes) {
        DecentSampler ds = new DecentSampler();
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

    private void forgeVelocityGroups(File presetFile, MidiZone velocity, List<RecordedSynthNote> recordedSamplesPerVelocity, DecentSampler ds) {
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

    private void forgeNoteGroups(File presetFile, MidiZone velocity, int controlChange, MidiZone controlChangeValue, List<RecordedSynthNote> recordedSamples, List<Binding> bindings, DecentSampler ds) {
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
                        sample.setLoopCrossfade(8000L);
                    });
            group.getSamples()
                    .add(sample);
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
}
