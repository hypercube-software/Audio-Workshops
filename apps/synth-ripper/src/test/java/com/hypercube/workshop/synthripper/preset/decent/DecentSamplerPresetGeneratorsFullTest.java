package com.hypercube.workshop.synthripper.preset.decent;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetIdentity;
import com.hypercube.workshop.synthripper.AbstractSynthRipperTest;
import com.hypercube.workshop.synthripper.SynthRipper;
import com.hypercube.workshop.synthripper.model.LoopSetting;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.preset.decent.model.DecentSamplerPreset;
import com.hypercube.workshop.synthripper.preset.decent.model.RoundRobinGroup;
import com.hypercube.workshop.synthripper.preset.decent.model.Sample;
import com.hypercube.workshop.synthripper.preset.decent.model.TriggerMode;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void loopedNoteGetsANonLoopingReleaseSample() throws IOException {
        // GIVEN a recorded note with a loop (the test wav has a release tail after loop end)
        LoopSetting loop = new LoopSetting();
        loop.setSampleStart(80262);
        loop.setSampleEnd(275625);
        loop.setLoopLength(275625 - 80262);
        RecordedSynthNote note = RecordedSynthNote.builder()
                .note(new MidiZone(36, 36, 36))
                .velocity(new MidiZone(0, 127, 127))
                .controlChange(MidiPreset.NO_CC)
                .releaseTimeInSec(1.25f)
                .file(new File("src/test/resources/loop/071 B3 Stream - Velo 064.wav"))
                .loopSetting(loop)
                .build();

        // WHEN the Decent Sampler preset is generated
        DecentSamplerPresetGenerator generator = new DecentSamplerPresetGenerator();
        DecentSamplerPreset preset = generator.forgeDecentSamplerPreset(new File("output/test.dspreset"), List.of(note));

        // THEN the group contains the looped attack sample
        assertEquals(1, preset.getGroups()
                .size());
        RoundRobinGroup group = preset.getGroups()
                .getFirst();
        assertEquals(2, group.getSamples()
                .size(), "a looped note must also generate a release sample");

        Sample sustain = group.getSamples()
                .getFirst();
        assertEquals(TriggerMode.ATTACK, sustain.getTrigger());
        assertEquals(Boolean.TRUE, sustain.getLoopEnabled());
        assertEquals(Long.valueOf(80262), sustain.getLoopStart());
        assertEquals(Long.valueOf(275625), sustain.getLoopEnd());
        // sustain voice must stop at note-off so the release sample takes over
        assertEquals(Float.valueOf(1f), sustain.getSustain());
        // release envelope triggers the crossfade into the release sample
        assertNotNull(sustain.getRelease());
        assertTrue(sustain.getRelease() > 0f, "sustain must have a release envelope, got " + sustain.getRelease());

        // AND a non-looping release sample plays the recorded tail once at full level
        Sample release = group.getSamples()
                .get(1);
        assertEquals(TriggerMode.RELEASE, release.getTrigger());
        assertTrue(release.getLoopEnabled() == null || !release.getLoopEnabled(),
                "release sample must not loop");
        assertEquals(Integer.valueOf(275625), release.getStart());
        assertNotNull(release.getEnd());
        assertTrue(release.getEnd() > release.getStart(), "release tail must extend past the loop end");
        assertEquals(Float.valueOf(1f), release.getSustain());
        // the release fades in over the same duration the sustain releases: loop↔tail crossfade
        assertEquals(sustain.getRelease(), release.getAttack(),
                "sustain release and release attack must overlap for a smooth crossfade");
    }
}
