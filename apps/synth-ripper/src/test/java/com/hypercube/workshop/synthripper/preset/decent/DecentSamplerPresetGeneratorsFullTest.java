package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetIdentity;
import com.hypercube.workshop.synthripper.AbstractSynthRipperTest;
import com.hypercube.workshop.synthripper.SynthRipper;
import com.hypercube.workshop.synthripper.preset.decent.model.DecentSamplerPreset;
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
class DecentSamplerPresetGeneratorsFullTest extends AbstractSynthRipperTest {
    private String toXML(Object object) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DecentSamplerPreset.class);
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

    private int countOccurrences(String text, String token) {
        return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }


    @Test
    void canGenerateDecentSamplerPresets() throws IOException {
        // GIVEN
        SynthRipper synthRipper = forgeSynthRipper("src/test/resources/config/config-DS-330.yml");
        DecentSamplerPresetGenerator decentSamplerPresetGenerator = new DecentSamplerPresetGenerator();

        // WHEN
        var batch = synthRipper.generateBatch();
        DecentSamplerPreset model = decentSamplerPresetGenerator.forgeDecentSamplerPreset(new File("output/preset.dspreset"), batch);
        log.info(toXML(model));

        // THEN
        List<MidiPreset> selectedPresets = synthRipper.getConf()
                .getSelectedPresets();
        assertEquals(8, selectedPresets.size());
        assertEquals(List.of("Gt.FretNoise", "Breath Noise", "Seashore", "Bird", "Telephone 1", "Helicopter", "Applause", "Gun Shot"), selectedPresets.stream()
                .map(MidiPreset::getId)
                .map(MidiPresetIdentity::name)
                .toList());
        assertEquals(0, model.getMidi()
                .getMidiControlChangeList()
                .size());
        assertEquals(8 * 2 * 2, batch.size());
        assertEquals(MidiPreset.NO_CC, batch.getFirst()
                .getControlChange());
    }
}
