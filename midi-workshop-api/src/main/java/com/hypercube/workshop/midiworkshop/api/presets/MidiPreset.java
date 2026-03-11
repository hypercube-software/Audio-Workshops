package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class describe how to get a specific sound or preset for a device. It can be used very simply with a single program change,
 * but we can do much more, specifying a specific tempo via sysex, or CC before selecting a preset
 * <p>Supported commands to select a sound:</p>
 * <ul>
 *     <li>bankName select message</li>
 *     <li>program change</li>
 *     <li>SysEx</li>
 * </ul>
 * {@link #controlChanges} indicate this preset respond to specific control changes
 * {@link #drumKitNotes} if not empty, indicate this preset load a Drumkit
 */
@Getter
@EqualsAndHashCode(of = {"id"})
public final class MidiPreset {

    public static final int BANK_SELECT_MSB = 0;
    public static final int BANK_SELECT_LSB = 32;
    /**
     * Regular preset without any CC
     */
    public static final int NO_CC = -1;
    /**
     * Zero based [0-15] Midi channel where we activate this preset (relevant for drums which are most of the time at channel 10, so 09 if zero based)
     */
    private final int zeroBasedChannel;
    /**
     * A list of MIDI messages that will activate this preset on the device. Can be a SysEx or Bank Select or Program Change
     */
    private final List<MidiMessage> commands;
    /**
     * Which CC are used by this preset
     */
    private final List<Integer> controlChanges;
    /**
     * If the preset is a drum kit, here are the drum kit notes to record
     */
    private final List<DrumKitNote> drumKitNotes;
    /**
     * How bankName select is used
     */
    private final MidiBankFormat midiBankFormat;
    /**
     * Provides the (msb,lsb,prg) contains in {@link #commands}. It is null for SysEx
     */
    private final PresetIdentifiers identifiers;
    /**
     * Identifier of the preset 'device, bank, name, category'
     */
    @Setter
    private MidiPresetIdentity id;

    /**
     * Default constructor with all fields
     */
    public MidiPreset(String deviceMode, String bank, String presetName, String presetCategory, int zeroBasedChannel, List<MidiMessage> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat, PresetIdentifiers identifiers) {
        this.id = new MidiPresetIdentity(deviceMode, bank, presetName, presetCategory);
        this.identifiers = identifiers;
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.controlChanges = controlChanges;
        this.drumKitNotes = drumKitNotes;
        this.midiBankFormat = midiBankFormat;
        if (controlChanges == null) {
            throw new MidiConfigError("ControlChanges cannot be null, use List.of(NO_CC)");
        }
    }

    /**
     * Preset with control changes and DrumKit but without category
     */
    public MidiPreset(String deviceMode, String bank, String presetName, int zeroBasedChannel, List<MidiMessage> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat, PresetIdentifiers identifiers) {
        this.id = new MidiPresetIdentity(deviceMode, bank, presetName, null);
        this.identifiers = identifiers;
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.controlChanges = controlChanges;
        this.drumKitNotes = drumKitNotes;
        this.midiBankFormat = midiBankFormat;
        if (controlChanges == null) {
            throw new MidiConfigError("ControlChanges cannot be null, use List.of(NO_CC)");
        }
    }

    /**
     * Preset with DrumKit notes and without category
     */
    public MidiPreset(String deviceMode, String bankName, String presetName, int zeroBasedChannel, List<MidiMessage> commands, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat, PresetIdentifiers identifiers) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, presetName, null);
        this.identifiers = identifiers;
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = drumKitNotes;
    }

    /**
     * Preset without category or DrumKit notes
     */
    public MidiPreset(String deviceMode, String bankName, String presetName, int zeroBasedChannel, List<MidiMessage> commands, MidiBankFormat midiBankFormat, PresetIdentifiers identifiers) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, presetName, null);
        this.identifiers = identifiers;
        this.zeroBasedChannel = zeroBasedChannel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = new ArrayList<>();
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

    public String getBankCommand() {
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> "";
            case BANK_MSB_PRG -> "%02X".formatted(getBankMSB());
            case BANK_LSB_PRG -> "%02X".formatted(getBankLSB());
            case BANK_MSB_LSB_PRG -> "%02X%02X".formatted(getBankMSB(), getBankLSB());
            case BANK_PRG_PRG -> "%02X".formatted(getFirstProgram());
        };
    }

    public String getShortId() {
        int bank = getBankMSB() << 8 | getBankLSB();
        return "%01d-%03d".formatted(bank, getLastProgram());
    }

    public int getLastProgram() {
        var ctrl = getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage::getData1)
                .toList();
        if (ctrl.isEmpty()) {
            return -1;
        } else {
            return ctrl.getLast();
        }
    }

    public int getFirstProgram() {
        var ctrl = getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage::getData1)
                .toList();
        if (ctrl.isEmpty()) {
            return -1;
        } else {
            return ctrl.getFirst();
        }
    }

    public int getBankMSB() {
        return Optional.ofNullable(getIdentifiers())
                .map(PresetIdentifiers::getMsb)
                .orElseThrow(() -> new MidiError("There is no MSB for SysEx preset"));
    }

    public int getBankLSB() {
        return Optional.ofNullable(getIdentifiers())
                .map(PresetIdentifiers::getLsb)
                .orElseThrow(() -> new MidiError("There is no LSB for SysEx preset"));
    }

    public int getBankPrg() {
        return Optional.ofNullable(getIdentifiers())
                .map(PresetIdentifiers::getPrg)
                .orElseThrow(() -> new MidiError("There is no Program for SysEx preset"));
    }

    @Override
    public String toString() {
        return "MidiPreset[" +
                "id=" + id + ", " +
                "channel=" + zeroBasedChannel + ", " +
                "commands=" + commands + ", " +
                "controlChanges=" + controlChanges + ", " +
                "drumKitNotes=" + drumKitNotes + ']';
    }

    public Object getKurzweilObjectId() {
        int objectID = (getBankLSB() * 100) + getBankPrg();
        int high = (objectID >> 7) & 0x7F;
        int low = objectID & 0x7F;
        return (high << 8) | low;
    }

    /**
     * Retrieve a Midi message by its command number
     *
     * @param command Midi command ({@link ShortMessage#PROGRAM_CHANGE}, {@link ShortMessage#CONTROL_CHANGE}...)
     */
    private Stream<ShortMessage> getCommand(int command) {
        return commands.stream()
                .filter(cmd -> cmd instanceof ShortMessage)
                .map(cmd -> (ShortMessage) cmd)
                .filter(cmd -> cmd.getCommand() == command);
    }


}
