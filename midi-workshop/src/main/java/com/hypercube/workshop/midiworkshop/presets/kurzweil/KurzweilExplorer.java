package com.hypercube.workshop.midiworkshop.presets.kurzweil;

import com.hypercube.workshop.midiworkshop.api.MidiNote;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiRequestResponse;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KurzweilSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.BaseObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.ObjectInfo;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.ObjectWrite;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class KurzweilExplorer {
    private final MidiDeviceLibrary library;
    private final MidiDeviceRequester midiDeviceRequester;
    private final MidiPortsManager midiPortsManager;
    private final KurzweilSysExParser kurzweilSysExParser = new KurzweilSysExParser();

    public void sendSample(String deviceName, int objectId) {

    }

    public void getSample(String deviceName, int objectId) {
        converseWith(deviceName, (device, input, output) -> {
            String sampleId = MidiEventBuilder.to14Bits(objectId, false);
            String cmtDirSample = "DIR($0106,%s)".formatted(sampleId);
            String cmdLoadSample = "READ($0106, %s, 00)".formatted(sampleId);
            ObjectInfo sampleInfo = query(device, input, output, cmtDirSample);
            log.info("Sample Block Name (via INFO) : {}, size {}", sampleInfo.getName(), "%02X".formatted(sampleInfo.getSize()));
            if (sampleInfo.getSize() > 0) {
                ObjectWrite sampleData = query(device, input, output, cmdLoadSample);
                log.info("Sample Block Name (via WRITE): {}, size {}", sampleData.getName(), "%02X".formatted(sampleData.getSize()));
                try {
                    Files.write(Path.of("Sample Info %d.dat".formatted(objectId)), sampleInfo.getName()
                            .getBytes());
                    Files.write(Path.of("Sample Block %d.dat".formatted(objectId)), sampleData.getPayload());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer b = ByteBuffer.wrap(sampleData.getPayload());
                b.order(ByteOrder.BIG_ENDIAN);
                // SFH struct start at 0xC
                b.position(0x0C);
                int rootNote = b.get() + 12;
                int mask = b.get();
                float amp1 = b.get() * 0.5f;
                float amp2 = b.get() * 0.5f;
                int pitch = 6147 - (b.getShort() & 0xFFFF);
                int nameOffset = b.getShort();
                // loop offsets ??
                // 87803 is encoded to $156FB but in the payload we have $C156FD, a jump of $C bytes/samples ?
                b.position(0x1C);
                int sos = b.getInt() & 0x000FFFFF;
                int alt = b.getInt() & 0x000FFFFF;
                int los = b.getInt() & 0x000FFFFF;
                int eos = b.getInt() & 0x000FFFFF;

                short rawDecay = b.getShort(0x30);
                int dbPerSecDecay = Math.abs(rawDecay) / 4;

                short rawRelease = b.getShort(0x34);
                int dbPerSecRelease = Math.abs(rawRelease) / 4;

                int loopType = (mask & 0b00000011); // normal, reverse, bidirect
                boolean ignoreRel = (mask & 0b00000100) != 0;
                boolean altSwitch = (mask & 0b00001000) != 0;
                boolean ramBased = (mask & 0b00010000) != 0;
                boolean shareWare = (mask & 0b00100000) != 0;
                boolean needsLoad = (mask & 0b01000000) != 0;
                boolean loopOff = (mask & 0b10000000) != 0;
                log.info("Root Note: {} = {}", rootNote, MidiNote.fromValue(rootNote)
                        .name());
                log.info("loopType: {}", loopType);
                log.info("ignoreRel: {}", ignoreRel);
                log.info("altSwitch: {}", altSwitch);
                log.info("ramBased: {}", ramBased);
                log.info("shareWare: {}", shareWare);
                log.info("needsLoad: {}", needsLoad);
                log.info("loopOff: {}", loopOff);

                log.info("amp1: {} dB (Volume adjust)", amp1);
                log.info("amp2: {} dB (Alt Volume Adjust)", amp2);
                log.info("pitch: {} cents", pitch);
                log.info("nameOffset: {}", nameOffset);
                log.info("Decay Rate : {} dB/sec", dbPerSecDecay);
                log.info("Release Rate : {} dB/sec", dbPerSecRelease);
                log.info("sos: {} (LOOP START en sample sur 24 bits ?)", sos);
                log.info("alt: {}  (LOOP END en samples sur 24 bits ?)", alt);
                log.info("los: {}", los);
                log.info("eos: {}", eos);

                try {
                    Files.write(Path.of("target/output %s %s.dat".formatted(sampleData.getClass()
                            .getSimpleName(), sampleData.getFormat())), sampleData.getPayload());

                } catch (IOException e) {
                    throw new MidiError(e);
                }
            } else {
                log.error("SampleId {} does not exists", sampleId);
            }
        });
    }

    public void listBanks(String deviceName) {
        converseWith(deviceName, (device, input, output) -> {
            CommandCall cmd = CommandCall.parse(device, "READ_PROGRAM_BANKS_NIBBLE()")
                    .getFirst();
            MidiRequestResponse response = midiDeviceRequester.queryAndAggregate(device, input, output, cmd, null);
            kurzweilSysExParser.parse(Manufacturer.KURZWEIL, response.response());
        });
    }

    private <T extends BaseObject> T query(MidiDeviceDefinition device, MidiInDevice input, MidiOutDevice output, String cmtDirSample) {
        return (T) midiDeviceRequester.query(device, input, output, CommandCall.parse(device, cmtDirSample)
                        .getFirst())
                .map(response -> {
                    return kurzweilSysExParser.parse(Manufacturer.KURZWEIL, response.response())
                            .getFirst();
                })
                .orElseThrow();
    }

    private void converseWith(String deviceName, DeviceConversation deviceConversation) {
        var device = library.getDevice(deviceName)
                .orElseThrow();
        try (var output = midiPortsManager.getOutput(device.getOutputMidiDevice())
                .orElse(null)) {
            if (output == null) {
                log.error("Output MIDI Device not found: %s".formatted(device.getOutputMidiDevice()));
                return;
            }
            try (var input = midiPortsManager.getInput(device.getInputMidiDevice())
                    .orElse(null)) {
                if (input == null) {
                    log.error("Input MIDI Device not found: %s".formatted(device.getInputMidiDevice()));
                    return;
                }
                output.open();
                input.open();
                deviceConversation.execute(device, input, output);
            }
        }
    }

    @PostConstruct
    private void init() {
        library.load(ConfigHelper.getApplicationFolder(this.getClass()));
        midiPortsManager.collectHardwareDevices();
    }
}
