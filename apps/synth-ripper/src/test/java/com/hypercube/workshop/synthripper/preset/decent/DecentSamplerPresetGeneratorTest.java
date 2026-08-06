package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.decent.model.*;
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
class DecentSamplerPresetGeneratorTest {
    @Test
    void canSerializeToXML() throws JAXBException, IOException {
        DecentSamplerPreset decentSamplerPreset = new DecentSamplerPreset();
        Sample sample = new Sample();
        sample.setLowNote(10);
        sample.setHiNote(64);
        sample.setPath("/toto/titi.wav");

        RoundRobinGroup dsGroup = new RoundRobinGroup();
        dsGroup.setTags("RoundRobin");
        dsGroup.setReleaseTimeInSec(4.5f);
        dsGroup.setLoVel(10);
        dsGroup.setHiVel(127);
        dsGroup.getSamples()
                .add(sample);

        decentSamplerPreset.getGroups()
                .add(dsGroup);

        MidiControlChange cc = new MidiControlChange(1, List.of(
                new Binding("amp", "group", 0, "A", "AMP_VOLUME", "table", "0,1;64,0;128,0"),
                new Binding("amp", "group", 0, "B", "AMP_VOLUME", "table", "0,0;64,1;128,0"),
                new Binding("amp", "group", 0, "C", "AMP_VOLUME", "table", "0,0;64,0;128,1")));
        decentSamplerPreset.getMidi()
                .getMidiControlChangeList()
                .add(cc);

        JAXBContext jaxbContext = JAXBContext.newInstance(DecentSamplerPreset.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        String xml;
        try (StringWriter sw = new StringWriter()) {
            jaxbMarshaller.marshal(decentSamplerPreset, sw);
            xml = sw.toString();
        }
        log.info(xml);
        assertEquals("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <DecentSampler>
                    <groups>
                        <group release="4.5" tags="RoundRobin" loVel="10" hiVel="127">
                            <sample path="/toto/titi.wav" hiNote="64" loNote="10"/>
                        </group>
                    </groups>
                    <midi>
                        <cc number="1">
                            <binding type="amp" level="group" position="0" tags="A" parameter="AMP_VOLUME" translation="table" translationTable="0,1;64,0;128,0"/>
                            <binding type="amp" level="group" position="0" tags="B" parameter="AMP_VOLUME" translation="table" translationTable="0,0;64,1;128,0"/>
                            <binding type="amp" level="group" position="0" tags="C" parameter="AMP_VOLUME" translation="table" translationTable="0,0;64,0;128,1"/>
                        </cc>
                    </midi>
                </DecentSampler>
                """, xml);
    }

    @Test
    void canGenerateDecentSamplerModel() throws JAXBException, IOException {
        DecentSamplerPresetGenerator decentSamplerPresetGenerator = new DecentSamplerPresetGenerator();
        SynthRipperConfiguration conf = new SynthRipperConfiguration();
        List<RecordedSynthNote> sampleBatch = List.of(
                RecordedSynthNote.builder()
                        .note(new MidiZone(45, 45, 45))
                        .velocity(new MidiZone(0, 127, 127))
                        .controlChange(MidiPreset.NO_CC)
                        .releaseTimeInSec(1.25f)
                        .file(new File("output/toto.wav"))
                        .build(),
                RecordedSynthNote.builder()
                        .note(new MidiZone(45, 45, 45))
                        .velocity(new MidiZone(0, 127, 127))
                        .controlChange(1)
                        .ccValue(new MidiZone(1, 64, 64))
                        .releaseTimeInSec(1.25f)
                        .file(new File("output/toto.wav"))
                        .build(),
                RecordedSynthNote.builder()
                        .note(new MidiZone(45, 45, 45))
                        .velocity(new MidiZone(0, 127, 127))
                        .controlChange(1)
                        .ccValue(new MidiZone(65, 127, 127))
                        .releaseTimeInSec(1.25f)
                        .file(new File("output/toto.wav"))
                        .build()
        );
        DecentSamplerPreset decentSamplerPreset = decentSamplerPresetGenerator.forgeDecentSamplerPreset(new File("output/toto.dspreset"), sampleBatch);

        JAXBContext jaxbContext = JAXBContext.newInstance(DecentSamplerPreset.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        String xml;
        try (StringWriter sw = new StringWriter()) {
            jaxbMarshaller.marshal(decentSamplerPreset, sw);
            xml = sw.toString();
        }
        log.info(xml);
        assertEquals("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <DecentSampler>
                    <groups>
                        <group name="A2" release="1.25" modVolume="0.0" tags="vel127 CC001-064" loVel="0" hiVel="127">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45"/>
                        </group>
                        <group name="A2" release="1.25" modVolume="0.0" tags="vel127 CC001-127" loVel="0" hiVel="127">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45"/>
                        </group>
                        <group name="A2" release="1.25" modVolume="1.0" tags="vel127 NoCC" loVel="0" hiVel="127">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45"/>
                        </group>
                    </groups>
                    <midi>
                        <cc number="1">
                            <binding type="amp" level="group" tags="CC001-127" parameter="AMP_VOLUME" translation="table" translationTable="0,0;65,1;128,1"/>
                            <binding type="amp" level="group" tags="CC001-064" parameter="AMP_VOLUME" translation="table" translationTable="0,0;1,1;64,1;128,0"/>
                        </cc>
                    </midi>
                </DecentSampler>
                """, xml);
    }
}
