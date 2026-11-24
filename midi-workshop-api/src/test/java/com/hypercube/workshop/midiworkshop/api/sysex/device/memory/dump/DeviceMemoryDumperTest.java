package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceMemoryDumperTest {

    @Test
    void readEachZoneInItsOwnMemorySpace(@TempDir Path tempDir) throws IOException {
        Path mmap = tempDir.resolve("overlapping-spaces.mmap");
        Files.writeString(mmap, """
                @Model Overlapping Spaces
                @Packed

                SpaceA
                {
                    @0000-0001 A
                    {
                        byte Value
                    }
                }

                SpaceB
                {
                    @0000-0001 B
                    {
                        byte Value
                    }
                }
                """);

        DeviceMemory memory = MemoryMapParser.load(mmap.toFile());
        memory.writeByte("SpaceA", MemoryInt28.from(0, 2), 0x11);
        memory.writeByte("SpaceB", MemoryInt28.from(0, 2), 0x22);

        Path dump = tempDir.resolve("memory.txt");
        new DeviceMemoryDumper(memory).dumpMemory(dump.toFile());

        String content = Files.readString(dump);
        assertTrue(content.contains("[SpaceA]"), content);
        assertTrue(content.contains("[SpaceB]"), content);
        assertTrue(content.lines()
                .anyMatch(line -> line.contains("11") && line.contains("A/Value")), content);
        assertTrue(content.lines()
                .anyMatch(line -> line.contains("22") && line.contains("B/Value")), content);
    }
}
