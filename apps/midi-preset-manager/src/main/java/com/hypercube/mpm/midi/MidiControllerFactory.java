package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.ControllerValueType;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceController;
import lombok.experimental.UtilityClass;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.util.List;

@UtilityClass
public class MidiControllerFactory {
    static List<CustomMidiEvent> forge7BitsCC(int outputChannel, int cc, MidiControllerValue value) {
        try {
            return List.of(new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, cc, value.to7bitsSignedValue())));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    static List<CustomMidiEvent> forge14BitsCC(int outputChannel, int cc, MidiControllerValue value) {
        int ccMsbId = cc >> 8;
        int ccLsbId = cc & 0xFF;
        try {
            return List.of(
                    new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, ccMsbId, value.msb())),
                    new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, ccLsbId, value.lsb())));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    static List<CustomMidiEvent> forge14bitsNRPN(int outputChannel, boolean includeParamId, int nrpnId, MidiControllerValue value) {
        int nrpnMsbId = nrpnId >> 8;
        int nrpnLsbId = nrpnId & 0xFF;
        try {
            if (!includeParamId) {
                return List.of(
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_MSB_PARAM, nrpnMsbId)),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_LSB_PARAM, nrpnLsbId)),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_MSB_VALUE, value.msb())),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_LSB_VALUE, value.lsb()))
                );

            } else {
                return List.of(
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_MSB_VALUE, value.msb())),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_LSB_VALUE, value.lsb()))
                );
            }
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    static List<CustomMidiEvent> forge7bitsNRPN(int outputChannel, boolean includeParamId, int nrpnId, MidiControllerValue value, ControllerValueType type) {
        int nrpnMsbId = nrpnId >> 8;
        int nrpnLsbId = nrpnId & 0xFF;
        int nrpnValueType = switch (type) {
            case NRPN_MSB -> CustomMidiEvent.NRPN_MSB_VALUE;
            case NRPN_LSB -> CustomMidiEvent.NRPN_LSB_VALUE;
            case CC, CC_MSB_LSB, NRPN_MSB_LSB, SYSEX -> throw new IllegalArgumentException();
        };
        try {
            if (!includeParamId) {
                return List.of(
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_MSB_PARAM, nrpnMsbId)),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, CustomMidiEvent.NRPN_LSB_PARAM, nrpnLsbId)),
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, nrpnValueType, value.to7bitsSignedValue()))
                );
            } else {
                return List.of(
                        new CustomMidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, outputChannel, nrpnValueType, value.to7bitsSignedValue()))
                );
            }
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    public static List<CustomMidiEvent> forgeSYSEX(int outputChannel, boolean sameNRPN, MidiDeviceController controller, MidiControllerValue value) {

        try {
            byte[] payload = controller.getSysExTemplate()
                    .forgePayload(value);
            return List.of(new CustomMidiEvent(new SysexMessage(payload, payload.length)));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }
}
