package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
            File sfzFile = new File("%s/%s %s.dspreset".formatted(conf.getOutputDir(), preset.getFirstProgram(), preset.getId()
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
            forgeVelocityGroup(presetFile, velocity, recordedSamplesPerVelocity, ds);
        });
        return ds;
    }

    private void forgeVelocityGroup(File presetFile, MidiZone velocity, List<RecordedSynthNote> recordedSamplesPerVelocity, DecentSampler ds) {
        Groups groups = new Groups();
        groups.setTags("vel" + velocity
                .high());
        ds.getGroups()
                .add(groups);
        var recordsPerCC = recordedSamplesPerVelocity.stream()
                .collect(groupingBy(RecordedSynthNote::getControlChange));
        recordsPerCC.forEach((cc, recordedSamplesPerControlChange) -> {
            if (cc == MidiPreset.NO_CC) {
                groups.getRoundRobinGroups()
                        .addAll(forgeRoundRobinGroup(presetFile, cc, null, recordedSamplesPerControlChange, null));
            } else {
                List<Binding> bindings = new ArrayList<>();
                // each velocity group produces the same CC bindings, so we save only the first one we produce
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
                    groups.getRoundRobinGroups()
                            .addAll(forgeRoundRobinGroup(presetFile, cc, ccValue, recordedSamplesPerControlChangeValue, bindings));
                });
            }
        });
        groups.getRoundRobinGroups()
                .sort(Comparator.comparing(RoundRobinGroup::getTags));
    }

    private List<RoundRobinGroup> forgeRoundRobinGroup(File presetFile, int controlChange, MidiZone controlChangeValue, List<RecordedSynthNote> recordedSamples, List<Binding> bindings) {
        return recordedSamples.stream()
                .map(recordedSample -> {
                    RoundRobinGroup group = new RoundRobinGroup();
                    group.setName(SynthRipperConfiguration.noteNameFromPitch(recordedSample.getNote()
                            .value()));
                    if (controlChange != MidiPreset.NO_CC) {
                        group.setModVolume(0f);
                        String groupTag = "CC%03d-%03d".formatted(controlChange, controlChangeValue.value());
                        group.setTags(groupTag);
                        if (bindings.stream()
                                .filter(b -> b.getTags()
                                        .equals(groupTag))
                                .findAny()
                                .isEmpty()) {
                            bindings.add(forgeControlChangeBinding(groupTag, controlChange, controlChangeValue));
                        }
                    } else {
                        group.setModVolume(1f);
                        String groupTag = "NoCC";
                        group.setTags(groupTag);
                    }
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
                    sample.setLoVel(recordedSample.getVelocity()
                            .low());
                    sample.setHiVel(recordedSample.getVelocity()
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
                    return group;
                })
                .toList();
    }

    private Binding forgeControlChangeBinding(String groupName, int cc, MidiZone zone) {
        StringBuilder enveloppe = new StringBuilder();
        if (zone.low() > 0) {
            enveloppe.append("0,0;");
        }
        enveloppe.append("%d,1;".formatted(zone.low()));
        if (zone.high() < 127) {
            enveloppe.append("%d,1;".formatted(zone.high()));
            enveloppe.append("128,0");
        } else {
            enveloppe.append("128,1");
        }
        int overlap = zone.width() / 2;
        int begin = zone.low() - overlap;
        int middle = zone.value();
        int end = zone.high() + overlap;

        return new Binding("amp", "group", null, groupName, "AMP_VOLUME", "table", enveloppe.toString());
    }
}
