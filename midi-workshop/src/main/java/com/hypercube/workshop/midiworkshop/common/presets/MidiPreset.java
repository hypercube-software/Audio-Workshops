package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class describe how to get a specific sound or preset for a device. It can be used very simply with a single program change,
 * but we can do much more, specifying a specific tempo via sysex, or CC before selecting a preset
 * <p>Supported commands to select a sound:</p>
 * <ul>
 *     <li>bankName select message</li>
 *     <li>program change</li>
 *     <li>Sysex</li>
 * </ul>
 * {@link #controlChanges} indicate this preset respond to specific control changes
 * {@link #drumKitNotes} if not empty, indicate this preset load a Drumkit
 */
@Getter
@EqualsAndHashCode(of = {"id.title", "commands", "controlChanges", "channel"})
public final class MidiPreset {

    public static final int BANK_SELECT_MSB = 0;
    public static final int BANK_SELECT_LSB = 32;
    public static final int NO_CC = -1;
    /**
     * Identifier of the preset 'device, command, name, category'
     */
    @Setter
    private MidiPresetIdentity id;
    /**
     * Zero based [0-15] Midi channel where we activate this preset (relevant for drums which are most of the time at channel 10, so 09 if zero based)
     */
    private final int zeroBasedChannel;
    /**
     * How to activate this preset
     */
    private final List<MidiMessage> commands;
    /**
     * Which CC are used by this preset
     */
    private final List<Integer> controlChanges;
    /**
     * If the preset is a drumkit, here are the drum kit notes to record
     */
    private final List<DrumKitNote> drumKitNotes;
    /**
     * How bankName select is used
     */
    private final MidiBankFormat midiBankFormat;

    public MidiPreset(String deviceMode, String bank, String id, int zeroBasedChannel, List<MidiMessage> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bank, id, null);
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.controlChanges = controlChanges;
        this.drumKitNotes = drumKitNotes;
        this.midiBankFormat = midiBankFormat;
        if (controlChanges == null) {
            throw new MidiConfigError("ControlChanges cannot be null, use List.of(NO_CC)");
        }
    }

    public MidiPreset(String deviceMode, String bankName, String id, int zeroBasedChannel, List<MidiMessage> commands, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, id, null);
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = drumKitNotes;
    }

    public MidiPreset(String deviceMode, String bankName, String id, int zeroBasedChannel, List<MidiMessage> commands, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, id, null);
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = List.of();
    }

    public int getBank() {
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> 0;
            case BANK_MSB_PRG -> getBankMSB();
            case BANK_LSB_PRG -> getBankLSB();
            case BANK_MSB_LSB_PRG -> getBankMSB() << 8 | getBankLSB();
            case BANK_PRG_PRG -> getFirstProgram();
        };
    }

    public String getCommand() {
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> "%02X".formatted(getLastProgram());
            case BANK_MSB_PRG -> "%02X%02X".formatted(getBankMSB(), getLastProgram());
            case BANK_LSB_PRG -> "%02X%02X".formatted(getBankLSB(), getLastProgram());
            case BANK_MSB_LSB_PRG -> "%02X%02X%02X".formatted(getBankMSB(), getBankLSB(), getLastProgram());
            case BANK_PRG_PRG -> "%02X%02X".formatted(getFirstProgram(), getLastProgram());
        };
    }

    /**
     * Retrieve a Midi message by its command number
     *
     * @param command Midi command ({@link ShortMessage#PROGRAM_CHANGE}, {@link ShortMessage#CONTROL_CHANGE}...)
     * @return
     */
    private Stream<ShortMessage> getCommand(int command) {
        return commands.stream()
                .filter(cmd -> cmd instanceof ShortMessage)
                .map(cmd -> (ShortMessage) cmd)
                .filter(cmd -> cmd.getCommand() == command);
    }

    public String getShortId() {
        int bank = getBankMSB() << 8 | getBankLSB();
        return "%01d-%03d".formatted(bank, getLastProgram());
    }

    public int getLastProgram() {
        var ctrl = getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage::getData1)
                .toList();
        if (ctrl.size() == 0) {
            return -1;
        } else {
            return ctrl.getLast();
        }
    }

    public int getFirstProgram() {
        var ctrl = getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage::getData1)
                .toList();
        if (ctrl.size() == 0) {
            return -1;
        } else {
            return ctrl.getFirst();
        }
    }

    public int getBankMSB() {
        return getCommand(ShortMessage.CONTROL_CHANGE).filter(cmd -> cmd.getData1() == BANK_SELECT_MSB)
                .map(ShortMessage::getData2)
                .findFirst()
                .orElse(0);
    }

    public int getBankLSB() {
        return getCommand(ShortMessage.CONTROL_CHANGE).filter(cmd -> cmd.getData1() == BANK_SELECT_LSB)
                .map(ShortMessage::getData2)
                .findFirst()
                .orElse(0);
    }

    @Override
    public String toString() {
        return "MidiPreset[" +
                "title=" + id + ", " +
                "channel=" + zeroBasedChannel + ", " +
                "commands=" + commands + ", " +
                "controlChanges=" + controlChanges + ", " +
                "drumKitNotes=" + drumKitNotes + ']';
    }


}
