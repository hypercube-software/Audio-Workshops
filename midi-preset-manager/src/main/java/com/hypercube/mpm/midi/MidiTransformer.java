package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.ControllerValueType;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.*;

/**
 * This class convert an incoming stream of controllers to a new one according to a specifications in {@link MidiDeviceDefinition#getControllers()}
 * <ul>
 *     <li>It is very important to understand <b>this class is statefull</b>. We need to keep track of what we receive to generate the output events</li>
 *     <li>For instance a 14 bits NRPN id, followed by a 14 bits NRPN value, take 4 MIDI events to be detected</li>
 *     <li>this mean the method {@link #transform(int, CustomMidiEvent)} can return an empty list before outputting something</li>
 * </ul>
 */
@Slf4j
public class MidiTransformer {
    /**
     * Which "jargon" we are reading for
     */
    private final MidiDeviceDefinition inputDevice;
    /**
     * Which "jargon" we are translating to
     */
    private final MidiDeviceDefinition outputDevice;
    /**
     * Used by the GUI to display CC/NRPN ids to the end user (it's just informative)
     */
    private final MidiTransformerListener listener;
    /**
     * Keep track of 14 bits controllers
     */
    private final MidiTransformerState midiTransformerState = new MidiTransformerState();
    /**
     * 14 bits to 7 bits CC mapping
     */
    private final Set<Integer> ccMSB = new HashSet<>();
    /**
     * 14 bits to 7 bits CC mapping
     * Gives the MSB CC id bound to an LSB CC id
     */
    private final Map<Integer, Integer> ccLSBtoMSB = new HashMap<>();
    /**
     * 14 bits to 7 bits CC mapping
     * Gives the MSB CC value bound to an MSB CC id
     */
    private final Map<Integer, Integer> ccMSBtoValue = new HashMap<>();
    /**
     * Which mapping must be applied given an incoming controller event
     */
    private final Map<MidiControllerId, MidiControllerMapping> mappings = new HashMap<>();
    /**
     * Try to not create an empty list on each call :-)
     */
    private final List<CustomMidiEvent> empty = List.of();

    public MidiTransformer(MidiDeviceDefinition inputDevice, MidiDeviceDefinition outputDevice, MidiTransformerListener listener) {
        this.inputDevice = inputDevice;
        this.outputDevice = outputDevice;
        this.listener = listener;
        if (inputDevice != null && outputDevice != null) {
            autoSetupMappings();
        }
    }

    /**
     * Inspect the controllers definitions of each device and see which one have the same name
     * <p>When it is the case, install a mapping between them</p>
     */
    private void autoSetupMappings() {
        log.info("Autosetup controllers mappings %s (%d cc) => %s (%d cc)".formatted(inputDevice.getDeviceName(), inputDevice.getControllers()
                .size(), outputDevice.getDeviceName(), outputDevice.getControllers()
                .size()));
        inputDevice.getControllers()
                .forEach(inputController -> {
                    outputDevice.getControllers()
                            .stream()
                            .filter(outputController -> outputController.getName()
                                    .equals(inputController.getName()))
                            .findFirst()
                            .ifPresent(outputController -> {
                                addControllerMapping(new MidiControllerMapping(inputController, outputController));
                            });
                });
    }

    /**
     * Update the hashmap according to the declared mapping
     */
    public void addControllerMapping(MidiControllerMapping mapping) {
        var src = mapping.getSrc();
        var dst = mapping.getDst();
        log.info("Install controller mapping: " + src.toString() + " => " + dst.toString());
        mappings.put(mapping.getId(), mapping);
        if (src.getType() == ControllerValueType.CC_MSB_LSB) {
            ccLSBtoMSB.put(src.getLSB(), src.getMSB());
            ccMSB.add(src.getMSB());
        }
    }

    /**
     * This is the main method, transforming on the fly incoming events to new ones.
     * <ul>
     *     <li>It has to go fast</li>
     *     <li>It is a state machine based on the state {@link #midiTransformerState}</li>
     * </ul>>
     */
    public List<CustomMidiEvent> transform(int outputChannel, CustomMidiEvent event) {

        MidiMessage msg = event.getMessage();

        byte[] inComingPayload = msg
                .getMessage();
        int command = inComingPayload[0] & 0xF0;
        if (inComingPayload.length != 3 || command == 0xF0) {
            return List.of(event); // Active Sensing, System or SysEx are untouched
        }
        int inputChannel = inComingPayload[0] & 0x0F;
        int data1 = inComingPayload.length > 1 ? inComingPayload[1] & 0xFF : 0;
        int data2 = inComingPayload.length > 2 ? inComingPayload[2] & 0xFF : 0;
        if (outputChannel == -1) {
            outputChannel = inputChannel;
        }
        try {
            if (command == ShortMessage.CONTROL_CHANGE) {
                if (data1 == 0x63) { // NRPN MSB PARAM
                    midiTransformerState.nrpnMSBParam = data2;
                } else if (data1 == 0x62) {  // NRPN LSB PARAM
                    midiTransformerState.nrpnLSBParam = data2;
                } else if (data1 == 0x06) {  // NRPN MSB VALUE
                    midiTransformerState.nrpnMSBValue = data2;
                    return onNRPNValue(outputChannel);
                } else if (data1 == 0x26) {  // NRPN LSB VALUE
                    midiTransformerState.nrpnLSBValue = data2;
                    return onNRPNValue(outputChannel);
                } else {
                    int ccId = data1; // MSB or LSB, we don't know yet
                    int ccValue7Bits = data2;
                    Integer msbId = ccLSBtoMSB.get(ccId);
                    if (msbId != null) {
                        // ccId is the LSB, we can map the 14 bits CC to 7 bits now
                        return onDoubleCC(outputChannel, msbId, ccId, ccValue7Bits);
                    } else if (ccMSB.contains(ccId)) {
                        // ccId is the MSB, we store its value for later
                        ccMSBtoValue.put(ccId, ccValue7Bits); // save this cc for the DoubleCC recognition
                    } else {
                        return onSingleCC(outputChannel, ccId, ccValue7Bits);
                    }
                }
            } else {
                // Typically notes
                return List.of(new CustomMidiEvent(new ShortMessage(command, outputChannel, data1, data2)));
            }
        } catch (InvalidMidiDataException e) {
            log.error("Unexpected error", e);
        }
        return empty;
    }

    /**
     * Notify the GUI for a newly detected controller
     */
    private void notifyListener(String msg) {
        Optional.ofNullable(listener)
                .ifPresent(l -> l.onControllerEvent("%s %s".formatted(Optional.ofNullable(inputDevice)
                        .map(MidiDeviceDefinition::getDeviceName)
                        .orElse("?"), msg)));
    }

    /**
     * Called when a 7 bits CC is detected
     */
    private List<CustomMidiEvent> onSingleCC(int outputChannel, int ccId, int lsbValue) throws InvalidMidiDataException {
        notifyListener("  CC   $%02X/%d = %d".formatted(ccId, ccId, lsbValue));
        var mapping = mappings.get(new MidiControllerId(ControllerValueType.CC, ccId));
        return Optional.ofNullable(mapping)
                .map(m -> mapAndRescaleControllerValue(outputChannel, m, new MidiControllerValue(0, lsbValue)))
                .orElse(empty);
    }

    /**
     * Called when a 14 bits CC is detected
     */
    private List<CustomMidiEvent> onDoubleCC(int outputChannel, int msbId, int lsbId, int lsbValue) throws InvalidMidiDataException {
        int msbValue = ccMSBtoValue.get(msbId); // retrieve the first CC
        int value14bits = (msbValue << 7) + lsbValue; // merge it with the second CC
        int ccId = msbId << 8 | lsbId;
        int realCCId = msbId << 7 | lsbId;
        notifyListener("CC $%04X/%d = %d".formatted(ccId, realCCId, value14bits));
        var mapping = mappings.get(new MidiControllerId(ControllerValueType.CC_MSB_LSB, ccId));
        return Optional.ofNullable(mapping)
                .map(m -> mapAndRescaleControllerValue(outputChannel, m, new MidiControllerValue(msbValue, lsbValue)))
                .orElse(empty);
    }

    /**
     * Called when a 14 bits or 7 bits NRPN is detected
     * <p>
     * It is important to understand that some devices don't send NRPN id for each NRPN values
     * <p>they just send the id one time because it is just enough
     * <p>Also some devices just send the MSB value, some just the LSB value, some both (14 bits values)
     * <ul>
     *     <li>MSB value is known to be coarse value</li>
     *     <li>LSB value is known to be fine value</li>
     * </ul>
     */
    private List<CustomMidiEvent> onNRPNValue(int outputChannel) throws InvalidMidiDataException {
        int nrpnId = midiTransformerState.getNrpnId();
        int value7bits = midiTransformerState.getValue7Bits();
        int value = midiTransformerState.value14BitReady() ? midiTransformerState.getValue14Bits() : value7bits;

        if (nrpnId != -1) {
            if (midiTransformerState.is14BitsNrpnId()) {
                notifyListener("NRPN $%04X/%d = %d".formatted(nrpnId, midiTransformerState.getRealNrpnId(), value));
            } else {
                notifyListener("NRPN   $%02X/%d = %d".formatted(nrpnId, midiTransformerState.getRealNrpnId(), value));
            }
        }
        if (midiTransformerState.value14BitReady()) {
            var mapping = mappings.get(new MidiControllerId(ControllerValueType.NRPN_MSB_LSB, nrpnId));
            return Optional.ofNullable(mapping)
                    .map(m -> {
                        int msbValue = midiTransformerState.nrpnMSBValue;
                        int lsbValue = midiTransformerState.nrpnLSBValue;
                        midiTransformerState.consumeNRPNValue();
                        return mapAndRescaleControllerValue(outputChannel, m, new MidiControllerValue(msbValue, lsbValue));
                    })
                    .orElse(empty);
        } else {
            var mapping = mappings.get(new MidiControllerId(ControllerValueType.NRPN_MSB, nrpnId));
            if (mapping == null) {
                mapping = mappings.get(new MidiControllerId(ControllerValueType.NRPN_LSB, nrpnId));
            }
            return Optional.ofNullable(mapping)
                    .map(m -> {
                        midiTransformerState.consumeNRPNValue();
                        return mapAndRescaleControllerValue(outputChannel, m, new MidiControllerValue(0, value));
                    })
                    .orElse(empty);
        }

    }

    /**
     * Once the controller is detected, we rescale its value to the target one
     */
    private List<CustomMidiEvent> mapAndRescaleControllerValue(int outputChannel, MidiControllerMapping mapping, MidiControllerValue inputValue) {
        int controllerId = mapping.getDst()
                .getId();
        var value = mapping.rescaleValue(inputValue);
        boolean sameNRPN = midiTransformerState.currentOutputNRPN == controllerId;
        if (!sameNRPN && mapping.getDst()
                .isNRPN()) {
            midiTransformerState.currentOutputNRPN = controllerId;
        }
        return switch (mapping.getDst()
                .getType()) {
            case CC -> MidiControllerFactory.forge7BitsCC(outputChannel, controllerId, value);
            case CC_MSB_LSB -> MidiControllerFactory.forge14BitsCC(outputChannel, controllerId, value);
            case NRPN_MSB ->
                    MidiControllerFactory.forge7bitsNRPN(outputChannel, sameNRPN, controllerId, value, ControllerValueType.NRPN_MSB);
            case NRPN_LSB ->
                    MidiControllerFactory.forge7bitsNRPN(outputChannel, sameNRPN, controllerId, value, ControllerValueType.NRPN_LSB);
            case NRPN_MSB_LSB -> MidiControllerFactory.forge14bitsNRPN(outputChannel, sameNRPN, controllerId, value);
        };

    }


}
