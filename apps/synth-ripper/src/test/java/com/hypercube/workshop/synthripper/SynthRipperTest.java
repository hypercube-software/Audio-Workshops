package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.synthripper.model.config.MidiSettings;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.decent.DecentSamplerPresetGenerator;
import com.hypercube.workshop.synthripper.preset.decent.model.DecentSampler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SynthRipperTest {
    @Mock
    private MidiDeviceLibrary midiDeviceLibrary;
    @Mock
    private MidiDeviceDefinition device;

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

    @BeforeEach
    void setup() {
        when(midiDeviceLibrary.getDevice(any())).thenReturn(Optional.of(device));
    }

    @Test
    void canGenerateBatchAndDecentSamplerModel() {
        SynthRipper synthRipper = new SynthRipper(List.of(new DecentSamplerPresetGenerator()));
        SynthRipperConfiguration conf = new SynthRipperConfiguration();
        conf.setMidiDeviceLibrary(midiDeviceLibrary);
        conf.setDevice("BOSS-DS330");
        conf.setProjectName("BOSS-DS330");
        synthRipper.conf = conf;

        Map<String, MidiDeviceMode> deviceModes = new LinkedHashMap<>();
        MidiDeviceMode mode = new MidiDeviceMode();
        mode.setName("GS Standard");
        deviceModes.put("GS Standard", mode);

        Map<String, MidiDeviceBank> banks = new LinkedHashMap<>();
        MidiDeviceBank bank = new MidiDeviceBank();
        bank.setName("SC");
        bank.setCommand("00");
        bank.setChannel(1);
        banks.put("SC", bank);

        MidiDevicePreset p1 = new MidiDevicePreset(new File("."), "Pad 5 (bowed)", "005C", "Synth Pad", null, List.of());
        MidiDevicePreset p2 = new MidiDevicePreset(new File("."), "DrumKit", "005D", "Drum", null, List.of());
        bank.setPresets(List.of(p1, p2));
        mode.setBanks(banks);

        when(device.getDeviceModes()).thenReturn(deviceModes);
        when(device.getPresetFormat()).thenReturn(com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat.BANK_MSB_PRG);

        var midiSettings = new MidiSettings();
        midiSettings.setCcPerNote(2);
        midiSettings.setLowestPreset("Pad 5 (bowed)");
        midiSettings.setHighestPreset("DrumKit");
        midiSettings.setLowestNote("C2");
        midiSettings.setHighestNote("C3");
        midiSettings.setVelocityPerNote(3);
        midiSettings.setNotesPerOctave(2);
        conf.setMidi(midiSettings);

        DecentSamplerPresetGenerator decentSamplerPresetGenerator = new DecentSamplerPresetGenerator();

        var batch = synthRipper.generateBatch();
        var model = decentSamplerPresetGenerator.forgeDecentSamplerPreset(conf, new File("output/preset.dspreset"), batch);
        log.info(toXML(model));

        assertEquals(2, conf.getSelectedPresets()
                .size());
        assertEquals(0, model.getMidi()
                .getMidiControlChangeList()
                .size());
        assertEquals(18, batch.size());
        assertEquals(MidiPreset.NO_CC, batch.get(0)
                .getControlChange());
    }
}
