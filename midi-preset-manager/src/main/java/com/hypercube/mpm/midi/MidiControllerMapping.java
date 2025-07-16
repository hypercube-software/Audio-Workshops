package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceController;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "id")
public final class MidiControllerMapping {
    private final MidiControllerId id;
    private final MidiDeviceController src;
    private final MidiDeviceController dst;

    public MidiControllerMapping(MidiDeviceController src, MidiDeviceController dst) {
        this.src = src;
        this.dst = dst;
        this.id = new MidiControllerId(src.getType(), src.getId());
    }

    public MidiControllerValue rescaleValue(MidiControllerValue inputValue) {
        int srcMin = src.getMinValue();
        int srcMax = src.getMaxValue();
        int dstMin = dst.getMinValue();
        int dstMax = dst.getMaxValue();

        int inputValueInt = src.isSigned() ? inputValue.to32bitsSignedValue() : inputValue.to32BitsUnsignedValue();

        // Rescale the input value from the source range to the destination range
        // Formula: output = (input - srcMin) * (dstMax - dstMin) / (srcMax - srcMin) + dstMin
        double rescaledValueDouble = (double) (inputValueInt - srcMin) * (dstMax - dstMin) / (srcMax - srcMin) + dstMin;

        // Ensure the rescaled value is within the destination range
        int rescaledValueInt = (int) Math.round(rescaledValueDouble);
        rescaledValueInt = Math.max(dstMin, Math.min(dstMax, rescaledValueInt));

        return MidiControllerValue.from32BitsSignedValue(rescaledValueInt);
    }
}
