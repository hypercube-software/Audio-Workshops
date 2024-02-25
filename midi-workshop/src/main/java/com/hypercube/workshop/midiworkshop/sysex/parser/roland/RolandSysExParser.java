package com.hypercube.workshop.midiworkshop.sysex.parser.roland;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.DeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.model.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.sysex.parser.SysExParser;
import com.hypercube.workshop.midiworkshop.sysex.parser.roland.command.RolandCommandParser;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;
import org.jline.utils.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class RolandSysExParser implements SysExParser {
    @Override
    public DeviceModel parse(Manufacturer manufacturer, CustomByteBuffer buffer) {
        int deviceId = buffer.getByte();

        RolandDeviceModel model = RolandDeviceModel.get(buffer.getByte());

        if (model.getMemory() == null) {
            var deviceMemory = MemoryMapParser.load(new File("midi-workshop/sysex/%s/%s.mmap".formatted(manufacturer.name(), model.getName())));
            deviceMemory.dumpMemoryMap(new File("midi-workshop/sysex//%s/%s.map".formatted(manufacturer.name(), model.getName())));
            model.setMemory(deviceMemory);
        }

        RolandCommandParser cmd = RolandCommand.get(getCommand(buffer))
                .getParser();
        buffer.mark();
        int bodySize = 0;
        while (buffer.remaining() > 0 && buffer.getByte() != SYSEX_END) {
            bodySize++;
        }
        buffer.reset();
        byte[] body = buffer.getBytes(bodySize);
        cmd.parse(deviceId, model, new CustomByteBuffer(ByteBuffer.wrap(body)));
        int sum = IntStream.range(0, body.length)
                .boxed()
                .map(i -> Integer.valueOf(body[i]))
                .mapToInt(Integer::intValue)
                .sum() & 0x7F;
        if (sum != 0) {
            Log.error("Checksum error");
        }
        int end = buffer.getByte();
        assert (end == SYSEX_END);
        return model;
    }

    /**
     * Roland Command can be extended with 0x00 (0x01 is not the same than 0x0001, so we convert that by 0x0100)
     */
    private int getCommand(CustomByteBuffer buffer) {
        for (int i = 0; i < 3; i++) {
            int b = buffer.getByte();
            if (b != 0) {
                return b << i;
            }
        }
        throw new MidiError("Unable to parse command id");
    }
}


