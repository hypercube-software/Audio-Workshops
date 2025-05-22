package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
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
        // GIVEN
        DecentSampler decentSampler = new DecentSampler();
        Sample sample = new Sample();
        sample.setLowNote(10);
        sample.setHiNote(64);
        sample.setLoVel(10);
        sample.setHiVel(127);
        sample.setPath("/toto/titi.wav");

        Groups groups = new Groups();
        groups.setTags("Velocity");

        RoundRobinGroup dsGroup = new RoundRobinGroup();
        dsGroup.setTags("RoundRobin");
        dsGroup.setReleaseTimeInSec(4.5f);
        dsGroup.getSamples()
                .add(sample);
        groups.getRoundRobinGroups()
                .add(dsGroup);

        decentSampler.getGroups()
                .add(groups);

        MidiControlChange cc = new MidiControlChange(1, List.of(
                new Binding("amp", "group", 0, "A", "AMP_VOLUME", "table", "0,1;64,0;128,0"),
                new Binding("amp", "group", 0, "B", "AMP_VOLUME", "table", "0,0;64,1;128,0"),
                new Binding("amp", "group", 0, "C", "AMP_VOLUME", "table", "0,0;64,0;128,1")));
        decentSampler.getMidi()
                .getMidiControlChangeList()
                .add(cc);

        // WHEN
        JAXBContext jaxbContext = JAXBContext.newInstance(DecentSampler.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        String xml;
        try (StringWriter sw = new StringWriter()) {
            jaxbMarshaller.marshal(decentSampler, sw);
            xml = sw.toString();
        }
        // THEN
        log.info(xml);
        assertEquals("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <DecentSampler>
                    <groups tags="Velocity">
                        <group release="4.5" tags="RoundRobin">
                            <sample path="/toto/titi.wav" hiNote="64" loNote="10" loVel="10" hiVel="127"/>
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
        // GIVEN
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
        DecentSampler decentSampler = decentSamplerPresetGenerator.forgeDecentSamplerPreset(conf, new File("output/toto.dspreset"), sampleBatch);

        // WHEN
        JAXBContext jaxbContext = JAXBContext.newInstance(DecentSampler.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        String xml;
        try (StringWriter sw = new StringWriter()) {
            jaxbMarshaller.marshal(decentSampler, sw);
            xml = sw.toString();
        }
        // THEN
        log.info(xml);
        assertEquals("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <DecentSampler>
                    <groups tags="vel127">
                        <group name="A2" release="1.25" modVolume="0.0" tags="CC001-064">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45" loVel="0" hiVel="127"/>
                        </group>
                        <group name="A2" release="1.25" modVolume="0.0" tags="CC001-127">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45" loVel="0" hiVel="127"/>
                        </group>
                        <group name="A2" release="1.25" modVolume="1.0" tags="NoCC">
                            <sample trigger="attack" path="toto.wav" rootNote="45" hiNote="45" loNote="45" loVel="0" hiVel="127"/>
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