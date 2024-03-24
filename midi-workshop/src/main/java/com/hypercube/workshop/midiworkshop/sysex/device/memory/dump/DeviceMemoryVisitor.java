package com.hypercube.workshop.midiworkshop.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;

public interface DeviceMemoryVisitor {
    void onNewEntry(String path, MemoryField field, MemoryInt24 addr);
}
