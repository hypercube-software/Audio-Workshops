package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.config.presets.ConfigMidiPreset;
import com.hypercube.workshop.synthripper.preset.decent.DecentSamplerPresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.DecentSampler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class SynthRipperTest {
    private String toXML(Object object) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DecentSampler.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            try (StringWriter sw = new StringWriter()) {
                jaxbMarshaller.marshal(object, sw);
                return sw.toString();
            }
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void canGenerateBatchAndDecentSamplerModel() {
        // GIVEN
        SynthRipper synthRipper = new SynthRipper(List.of(new DecentSamplerPresetGenerator()));
        SynthRipperConfiguration conf = new SynthRipperConfiguration();
        synthRipper.conf = conf;
        conf.setProjectName("BOSS-DS330");
        var midiSettings = new MidiSettings();
        midiSettings.setCcPerNote(2);
        midiSettings.setLowestPreset("Pad 5 (bowed)");
        midiSettings.setHighestPreset("DrumKit");
        midiSettings.setLowestNote("C2");
        midiSettings.setHighestNote("C3");
        midiSettings.setVelocityPerNote(3);
        midiSettings.setNotesPerOctave(2);
        midiSettings.setPresets(List.of(
                new ConfigMidiPreset("Pad 5 (bowed)", 1, List.of(), List.of(MidiPreset.NO_CC, 1), List.of()),
                new ConfigMidiPreset("DrumKit", 10, List.of(), List.of(MidiPreset.NO_CC), List.of(
                        "64 - BD",
                        "65 - Snare"
                ))
        ));
        conf.setMidi(midiSettings);
        DecentSamplerPresetGenerator decentSamplerPresetGenerator = new DecentSamplerPresetGenerator();

        // WHEN
        var batch = synthRipper.generateBatch();
        var model = decentSamplerPresetGenerator.forgeDecentSamplerPreset(conf, new File("output/preset.dspreset"), batch);
        log.info(toXML(model));

        // THEN
        assertEquals(2, conf.getMidi()
                .getSelectedPresets()
                .size());
        assertEquals(1, model.getMidi()
                .getMidiControlChangeList()
                .size());
        assertEquals(42, batch.size());
        assertEquals(MidiPreset.NO_CC, batch.get(0)
                .getControlChange());
        for (int i = 9; i < 36; i++) {
            assertEquals(1, batch.get(i)
                    .getControlChange());
        }
        assertEquals(127, batch.get(35)
                .getCcValue()
                .value());
    }
}