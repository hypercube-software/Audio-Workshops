package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;

public interface DeviceMemoryVisitor {
    void onNewTopLevelMemoryMap(MemoryMap memoryMap);

    void onNewEntry(String path, MemoryField field, MemoryInt28 addr);
}
