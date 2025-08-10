package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;

import java.util.List;

/**
 * PatchOverride are used to enforce a SYSEX message to operate ONLY on the Edit Buffer
 *
 * @param name      Name of the override
 * @param mode      Device mode
 * @param command   Midi command to select the right edit buffer (required on some devices). Expected format is 000000 for {@link MidiBankFormat#BANK_MSB_LSB_PRG}
 * @param condition Apply the override only if some bytes match in the payload
 * @param overrides Which bytes will be overrided
 */
public record PatchOverride(String name, String mode, String command, OverrideCondition condition,
                            List<OverrideLocation> overrides) {

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
