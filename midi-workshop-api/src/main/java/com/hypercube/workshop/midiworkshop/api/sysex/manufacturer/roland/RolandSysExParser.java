package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.command.RolandCommandParser;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;

/**
 * Read a roland SysEx which is made of Roland Commands
 */
@Slf4j
public class RolandSysExParser extends ManufacturerSysExParser {
    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        int deviceId = buffer.getByte();

        // Most Roland devices use a 1-byte model ID. The SE-02 uses a 4-byte
        // model ID (00 00 00 44), so a leading zero identifies the wide form.
        int deviceModel = buffer.getByte();
        int modelByteCount = 1;
        if (deviceModel == 0) {
            buffer.getByte();
            buffer.getByte();
            deviceModel = buffer.getByte();
            modelByteCount = 4;
        }
        RolandDevice device = Manufacturer.ROLAND.getDevice(deviceModel);

        if (device.getMemory() == null) {
            device.loadMemoryMap();
        }

        int command = modelByteCount == 4 ? buffer.getByte() : getCommand(buffer);
        RolandCommandParser cmd = RolandCommand.get(command)
                .getParser();
        buffer.mark();
        int bodySize = 0;
        while (buffer.remaining() > 0 && buffer.getByte() != SYSEX_END) {
            bodySize++;
        }
        buffer.reset();
        byte[] body = buffer.getBytes(bodySize);
        cmd.parse(deviceId, device, new SysExReader(ByteBuffer.wrap(body)));
        int sum = IntStream.range(0, body.length)
                .boxed()
                .map(i -> Integer.valueOf(body[i]))
                .mapToInt(Integer::intValue)
                .sum() & 0x7F;
        if (sum != 0) {
            log.error("Checksum error");
        }
        int end = buffer.getByte();
        assert (end == SYSEX_END);
        return device;
    }

    /**
     * Roland Command can be extended with 0x00 (0x01 is not the same than 0x0001, so we convert that by 0x0100)
     */
    private int getCommand(SysExReader buffer) {
        for (int i = 0; i < 3; i++) {
            int b = buffer.getByte();
            if (b != 0) {
                return b << i;
            }
        }
        throw new MidiError("Unable to parse command id");
    }
}


