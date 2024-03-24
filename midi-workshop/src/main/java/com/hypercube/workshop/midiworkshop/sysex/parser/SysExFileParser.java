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

import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.SYSEX_START;

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
            SysExReader bb = new SysExReader(ByteBuffer.wrap(data));

            Device device = null;
            while (bb.remaining() > 0) {
                int status = bb.getByte();
                assert (status == SYSEX_START);
                int manufacturerId = bb.getByte();
                if (manufacturerId == 0) {
                    int b1 = bb.getByte();
                    int b2 = bb.getByte();
                    manufacturerId = (b1 << 8) + b2;
                }
                Manufacturer manufacturer = Manufacturer.get(manufacturerId);
                Device currentDevice = manufacturer.parse(bb);
                if (device == null) {
                    device = currentDevice;
                }
            }
            return device;
        } catch (IOException e) {
            throw new MidiError("Unexpected error: %s".formatted(e.getMessage()), e);
        }
    }
}
