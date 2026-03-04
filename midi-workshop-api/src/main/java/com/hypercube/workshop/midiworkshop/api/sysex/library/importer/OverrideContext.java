package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OverrideContext {
    /**
     * This override belongs to this device
     */
    private MidiDeviceDefinition device;
    /**
     * If the mode cannot be found in the payload, use this one
     */
    private String defaultMode;
    /**
     * Which mode this patch belongs
     */
    private MidiDeviceMode mode;
    /**
     * How to query the patch
     */
    private String command;
    /**
     * Which patch name should we inject in the payload
     */
    private String patchName;
    /**
     * Which category code should we inject in the payload
     */
    private int categoryCode;
    /**
     * The payload we are working on
     */
    private byte[] payload;
}
