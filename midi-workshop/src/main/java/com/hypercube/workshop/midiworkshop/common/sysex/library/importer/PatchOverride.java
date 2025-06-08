package com.hypercube.workshop.midiworkshop.common.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;

import java.util.List;

public record PatchOverride(String name, String mode, OverrideCondition condition, List<OverrideLocation> overrides) {
    public boolean matches(byte[] patchData) {
        if (condition == null) {
            return true;
        }
        int offset = SysExBuilder.parseNumber(condition.offset());
        String hexValues = SysExBuilder.resolveASCIIStrings(condition.value());
        byte[] bytes = SysExBuilder.forgeBytesFromString(hexValues);
        for (int i = 0; i < bytes.length; i++) {
            if (patchData[i + offset] != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
