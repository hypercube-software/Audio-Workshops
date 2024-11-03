package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.DecentSampler;
import com.hypercube.workshop.synthripper.preset.decent.model.Group;
import com.hypercube.workshop.synthripper.preset.decent.model.Sample;
import com.hypercube.workshop.synthripper.preset.decent.model.TriggerMode;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Component;

import java.io.File;
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
            File sfzFile = new File("%s/%s %s.dspreset".formatted(conf.getOutputDir(), preset.getId(), preset.title()));
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

    private DecentSampler forgeDecentSamplerPreset(SynthRipperConfiguration conf, File sfzFile, List<RecordedSynthNote> recordedSynthNotes) {
        DecentSampler ds = new DecentSampler();

        ds.setGroups(recordedSynthNotes.stream()
                .map(recordedSample -> {
                    Group dsGroup = new Group();
                    dsGroup.setLoVel(recordedSample.getLowVelocity());
                    dsGroup.setHiVel(recordedSample.getHighVelocity());
                    dsGroup.setReleaseTimeInSec(recordedSample.getReleaseTimeInSec());
                    String path = sfzFile.getParentFile()
                            .toPath()
                            .relativize(recordedSample.getFile()
                                    .toPath())
                            .toString()
                            .replace("\\", "/");

                    Sample sample = new Sample();
                    sample.setTrigger(TriggerMode.ATTACK);
                    sample.setPath(path);
                    sample.setLowNote(recordedSample.getLowNote());
                    sample.setRootNote(recordedSample.getNote());
                    sample.setHiNote(recordedSample.getHighNote());

                    Optional.ofNullable(recordedSample.getLoopSetting())
                            .ifPresent(l -> {
                                sample.setLoopEnabled(true);
                                sample.setLoopStart(l.getSampleStart());
                                sample.setLoopEnd(l.getSampleEnd());
                                sample.setLoopCrossfade(8000L);
                            });
                    dsGroup.getSamples()
                            .add(sample);
                    return dsGroup;
                })
                .toList());

        return ds;
    }
}
