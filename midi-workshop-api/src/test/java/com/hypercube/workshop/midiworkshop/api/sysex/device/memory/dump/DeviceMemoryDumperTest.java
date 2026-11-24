package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void dumpSe02CompositePreset(@TempDir Path tempDir) throws IOException {
        DeviceMemory memory = MemoryMapParser.load(new File("../sysex/Roland/SE-02/SE-02.mmap"));
        memory.writeByte("Default", MemoryInt28.from("05000000", true), 0x0C);
        memory.writeByte("Default", MemoryInt28.from("05000001", true), 0x08);

        String name = "Inbound";
        for (int i = 0; i < name.length(); i++) {
            memory.writeByte("Default", MemoryInt28.from("05000150", true)
                    .add(i), name.charAt(i));
        }

        Path dump = tempDir.resolve("se02.txt");
        MemorySpace patch = new MemorySpace("Patch",
                List.of(memory.getMemoryMap("Patch Edit Buffer")));
        DeviceMemory patchMemory = new DeviceMemory("SE-02 Patch", List.of(patch));
        new DeviceMemoryDumper(patchMemory).dumpMemory(dump.toFile());

        String content = Files.readString(dump);
        assertTrue(content.lines()
                .anyMatch(line -> line.contains("C8") && line.contains("Parameters/COM_BENDRANGE")), content);
        assertTrue(content.lines()
                .anyMatch(line -> line.contains("Inbound") && line.contains("Name/Name")), content);
    }
}
