package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Optional;

import static com.hypercube.workshop.midiworkshop.sysex.util.SysExConstants.*;

@Slf4j
@Component
public class SysExFileParser {


    /**
     * Read a SYSEX file and dump the content of the device memory
     *
     * @param input SYSEX file (*.syx)
     */
    public Device parse(File input) {
        try {
            byte[] data = Files.readAllBytes(input.toPath());
            SysExReader sysExReader = new SysExReader(ByteBuffer.wrap(data));

            Device device = null;
            while (sysExReader.remaining() > 0) {
                int status = sysExReader.getByte();
                assert (status == SYSEX_START);
                int command = sysExReader.peekByte();
                if (command == SYSEX_NON_REALTIME) {
                    parseNonRealTime(sysExReader);
                } else if (command == SYSEX_REALTIME) {
                    parseRealTime(sysExReader);
                } else {
                    Manufacturer manufacturer = readManufacturerId(sysExReader);
                    Device currentDevice = manufacturer.parse(sysExReader);
                    if (device == null) {
                        device = currentDevice;
                    }
                }
            }
            return device;
        } catch (IOException e) {
            throw new MidiError("Unexpected error: %s".formatted(e.getMessage()), e);
        }
    }

    private void parseRealTime(SysExReader sysExReader) {
        int header = sysExReader.getByte();
        assert (header == SYSEX_REALTIME);
        throw new MidiError("REALTIME SYSEX parsing is not supported yet");
    }

    private void parseNonRealTime(SysExReader sysExReader) {
        int header = sysExReader.getByte();
        assert (header == SYSEX_NON_REALTIME);
        int deviceId = sysExReader.getByte();
        int command = sysExReader.getByte();
        if (command == SYSEX_GENERAL_INFORMATION) {
            readGeneralInformation(deviceId, sysExReader);
        } else {
            throw new MidiError("NON REALTIME SYSEX Command not supported yet: %02X".formatted(command));
        }
    }

    private void readGeneralInformation(int deviceId, SysExReader sysExReader) {
        int subCommand = sysExReader.getByte();
        if (subCommand == SYSEX_IDENTITY_RESPONSE) {
            Manufacturer manufacturer = readManufacturerId(sysExReader);
            var parser = Optional.ofNullable(manufacturer.getParser())
                    .orElseThrow(() -> new MidiError("Manufacturer %s has no SysEx parser yet".formatted(manufacturer.name())));
            parser.parseIdentityResponse(manufacturer, deviceId, sysExReader);
        } else {
            throw new MidiError("General Information sub command not supported yet: %02X".formatted(subCommand));
        }
    }

    private Manufacturer readManufacturerId(SysExReader sysExReader) {
        int manufacturerId = sysExReader.getByte();
        if (manufacturerId == 0) {
            int b1 = sysExReader.getByte();
            int b2 = sysExReader.getByte();
            manufacturerId = (b1 << 8) + b2;
        }
        return Manufacturer.get(manufacturerId);
    }
}
