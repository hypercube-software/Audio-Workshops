package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.synthripper.preset.decent.model.DecentSampler;
import com.hypercube.workshop.synthripper.preset.decent.model.Group;
import com.hypercube.workshop.synthripper.preset.decent.model.Sample;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.jline.utils.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecentSamplerPresetGeneratorTest {
    @Test
    @Disabled
    void cs1x() throws IOException {
        var p = Pattern.compile("([0-9]+)\\s+(.+)(\\s[0-9]\\s).+");
        var lines = Files.readAllLines(Path.of("D:\\github-checkout\\Audio-Workshops\\synth-ripper\\cs1x.txt"));
        int bank = 0;
        for (int l = 0; l < lines.size(); l++) {
            String txt = lines.get(l);
            if (txt.length() == 0) {
                bank++;
            } else {
                var m = p.matcher(txt);
                if (m.find()) {
                    String program = m.group(1);
                    String name = m.group(2)
                            .trim();
                    name = name.replace("D r ", "Dr ")
                            .replace("P f ", "Pf ")
                            .replace("G t ", "Gt ")
                            .replace("B r ", "Br ")
                            .replace("S t ", "St ")
                            .replace("E t ", "Et ");
                    if (name.charAt(2) == ' ') {
                        name = name.substring(0, 2) + name.substring(3);
                    }
                    String bankStr = bank == 0 ? "63-64-" : "63-65-";
                    System.out.println("    - " + bankStr + program + " " + name);
                } else {
                    System.out.println("BUG: " + txt);
                }
            }
        }
    }

    @Test
    void canSerializeToXML() throws JAXBException, IOException {
        // GIVEN
        DecentSampler decentSampler = new DecentSampler();
        Sample sample = new Sample();
        sample.setLowNote(10);
        sample.setHiNote(64);
        sample.setPath("/toto/titi.wav");
        Group dsGroup = new Group();
        dsGroup.getSamples()
                .add(sample);
        dsGroup.setReleaseTimeInSec(4.5f);
        decentSampler.getGroups()
                .add(dsGroup);
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
        Log.info(xml);
        assertEquals("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <DecentSampler>
                    <groups>
                        <group loVel="0" hiVel="0" release="4.5">
                            <sample path="/toto/titi.wav" hiNote="64" loNote="10"/>
                        </group>
                    </groups>
                </DecentSampler>
                """, xml);
    }
}