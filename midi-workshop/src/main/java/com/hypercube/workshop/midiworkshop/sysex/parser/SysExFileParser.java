package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.DeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.model.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Component
public class SysExFileParser {
    /**
     * Read a SYSEX file and dump the content of the device memory
     *
     * @param input  SYSEX file (*.syx)
     * @param output Memory dump file (*.dump)
     */
    public void parse(File input, File output) {
        try {
            byte[] data = Files.readAllBytes(input.toPath());
            CustomByteBuffer bb = new CustomByteBuffer(ByteBuffer.wrap(data));

            DeviceModel device = null;
            while (bb.remaining() > 0) {
                int status = bb.getByte();
                assert (status == SysExParser.SYSEX_START);
                Manufacturer manufacturer = Manufacturer.get(bb.getByte());
                DeviceModel currentDevice = manufacturer.parse(bb);
                if (device == null) {
                    device = currentDevice;
                }
            }
            Optional.ofNullable(device)
                    .ifPresent(d -> d.getMemory()
                            .dumpMemory(output));
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
