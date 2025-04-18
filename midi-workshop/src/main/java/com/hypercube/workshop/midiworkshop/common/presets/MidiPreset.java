package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering.FROM_ONE;

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
@EqualsAndHashCode(of = {"title", "commands", "controlChanges", "channel"})
public final class MidiPreset {

    public static final int BANK_SELECT_MSB = 0;
    public static final int BANK_SELECT_LSB = 32;
    public static final int NO_CC = -1;
    private static Pattern PRESET_REGEXP = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?");
    private static final Pattern COMMAND_REGEXP = Pattern.compile("\\s*([A-F0-9]{2})");
    /**
     * Identifier of the preset
     */
    @Setter
    private MidiPresetIdentity id;
    /**
     * Midi channel where we activate this preset (relevant for drums which are most of the time at channel 10)
     */
    private final int channel;
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

    public MidiPreset(String deviceMode, String bank, String id, int channel, List<MidiMessage> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bank, id, null);
        this.channel = channel;
        this.commands = commands;
        this.controlChanges = controlChanges;
        this.drumKitNotes = drumKitNotes;
        this.midiBankFormat = midiBankFormat;
        if (controlChanges == null) {
            throw new MidiConfigError("ControlChanges cannot be null, use List.of(NO_CC)");
        }
    }

    public MidiPreset(String deviceMode, String bankName, String id, int channel, List<MidiMessage> commands, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, id, null);
        this.channel = channel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = drumKitNotes;
    }

    public MidiPreset(String deviceMode, String bankName, String id, int channel, List<MidiMessage> commands, MidiBankFormat midiBankFormat) {
        this.id = new MidiPresetIdentity(deviceMode, bankName, id, null);
        this.channel = channel;
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
        };
    }

    public String getConfig() {
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> "%d".formatted(getProgram());
            case BANK_MSB_PRG -> "%d-%d".formatted(getBankMSB(), getProgram());
            case BANK_LSB_PRG -> "%d-%d".formatted(getBankLSB(), getProgram());
            case BANK_MSB_LSB_PRG -> "%d-%d-%d".formatted(getBankMSB(), getBankLSB(), getProgram());
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
        return "%01d-%03d".formatted(bank, getProgram());
    }

    public int getProgram() {
        return getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(ShortMessage::getData1)
                .findFirst()
                .orElse(-1);
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

    public static MidiPreset of(File configFile, int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String title, List<CommandMacro> macros, List<String> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes) {
        List<MidiMessage> messages = commands.stream()
                .flatMap(cmd -> parseCommand(configFile, channel, midiBankFormat, presetNumbering, macros, cmd))
                .toList();
        return new MidiPreset(null, null, title, channel, messages, controlChanges, drumKitNotes, midiBankFormat);
    }

    public static MidiPreset of(MidiDeviceDefinition device, MidiDeviceMode deviceMode, MidiDeviceBank bank, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        return new MidiPreset(deviceMode.getName(), bank.getName(), null, channel, forgeCommands(device, bank, program), device.getPresetFormat());
    }

    public static MidiPreset of(String title, MidiDeviceDefinition device, int bankMSB, int bankLSB, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        String bankName = String.format("$%X", (bankMSB << 8) | bankLSB);
        return new MidiPreset(null, bankName, title, channel, forgeCommands(device, bankMSB, bankLSB, program), List.of(), List.of(), device.getPresetFormat());
    }

    public static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, MidiDeviceBank bank, int program) {
        MidiBankFormat midiBankFormat = device.getPresetFormat();
        String bankCommand = bank.getCommand();
        if (bankCommand
                .contains("(")) {
            String commandCall = bankCommand
                    .replace("program", "" + program);
            CommandCall call = CommandCall.parse(device.getDefinitionFile(), commandCall);
            CommandMacro macro = device.getMacro(call);
            String cmd = macro.expand(call);
            return parseCommand(device.getDefinitionFile(), getDefaultChannel(device.getPresetNumbering()), midiBankFormat, device.getPresetNumbering(), device.getMacros(), cmd).toList();
        } else {
            int bankId = device.getBankId(bank.getName());
            return switch (midiBankFormat) {
                case NO_BANK_PRG -> forgeCommands(device, 0, 0, program);
                case BANK_MSB_PRG -> forgeCommands(device, bankId, 0, program);
                case BANK_LSB_PRG -> forgeCommands(device, 0, bankId, program);
                case BANK_MSB_LSB_PRG -> {
                    int msb = (bankId >> 8) & 0x7F;
                    int lsb = (bankId >> 0) & 0x7F;
                    yield forgeCommands(device, msb, lsb, program);
                }
            };
        }
    }

    private static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, int msb, int lsb, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        String definition = switch (device.getPresetFormat()) {
            case NO_BANK_PRG -> "%d".formatted(program);
            case BANK_MSB_PRG -> "%d-%d".formatted(msb, program);
            case BANK_LSB_PRG -> "%d-%d".formatted(lsb, program);
            case BANK_MSB_LSB_PRG -> "%d-%d-%d".formatted(msb, lsb, program);
        };

        try {
            return parsePresetSelectCommand(channel, device.getPresetFormat(), device.getPresetNumbering(), definition).toList();
        } catch (MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    private static int getDefaultChannel(MidiPresetNumbering presetNumbering) {
        return switch (presetNumbering) {
            case FROM_ZERO -> 0;
            case FROM_ONE -> 1;
        };
    }

    /**
     * One command template can produce multiple {@link MidiMessage}, this is why this method return a stream
     * <p>For instance, we often select a bankName, then select a program change. Two messages must be sent</p>
     *
     * @param channel         MIDI channel where to activate this preset
     * @param midiBankFormat  Which kind of MIDI message must be used to select this preset
     * @param presetNumbering Does the numbers starts from 0 or 1 (for convenience)
     * @param macros          Macros which can be used to generate the commands
     * @param definition      Textual definition of the command template
     * @return
     */
    private static Stream<MidiMessage> parseCommand(File configFile, int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, List<CommandMacro> macros, String definition) {
        try {
            String expandedDefinition = getExpandedDefinition(configFile, macros, definition);
            return Arrays.stream(expandedDefinition.split(";"))
                    .map(String::trim)
                    .flatMap(def -> {
                        if (def.startsWith("F0")) {
                            return parseSysExCommand(def);
                        } else if (def.startsWith("B0") || def.startsWith("C0")) {
                            return parseMidiCommand(def);
                        }
                        return parsePresetSelectCommand(channel, midiBankFormat, presetNumbering, def);
                    });
        } catch (NumberFormatException | MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    /**
     * Given a command definition, resolve all macro in it
     *
     * @param configFile Where this definition is defined (used to log errors)
     * @param macros     list of known macros
     * @param definition The definition to parse
     * @return
     */
    private static String getExpandedDefinition(File configFile, List<CommandMacro> macros, String definition) {
        return macros.stream()
                .filter(m -> m.matches(definition))
                .findFirst()
                .map(m -> {
                    return m.expand(CommandCall.parse(configFile, definition));
                })
                .orElse(definition);
    }

    /**
     * Parse a preset definition and generate the right sequence of midi message to select a patch.
     * <ul>
     *     <li>A program change message</li>
     *     <li>A bankName select MSB message followed by a program change message</li>
     *     <li>A bankName select LSB message followed by a program change message</li>
     *     <li>A bankName select MSB message followed by a bankName select LSB message followed by a program change message</li>
     * </ul>
     *
     * @param channel         which midi channel to use for the midi messages
     * @param midiBankFormat  which format to generate
     * @param presetNumbering how numbers are expressed in the definition
     * @param definition      the definition of the preset
     * @return a stream of Midi messages to select the patch on the given midi channel
     * @throws InvalidMidiDataException
     */
    private static Stream<MidiMessage> parsePresetSelectCommand(int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String definition) {
        Matcher matcher = PRESET_REGEXP.matcher(definition);
        if (matcher.find()) {
            List<Integer> ids = preparePresetSelectIdentifiers(matcher);
            int zeroBasedProgram = getProgramNumber(presetNumbering, ids);
            int zeroBasedChannel = getZeroBasedNumber("channel", presetNumbering, channel);
            try {
                return switch (midiBankFormat) {
                    case NO_BANK_PRG -> programChangeOnly(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_MSB_PRG -> bankMsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_LSB_PRG -> bankLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_MSB_LSB_PRG ->
                            bankMsbLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                };
            } catch (InvalidMidiDataException e) {
                throw new MidiError(e);
            }
        }
        throw new MidiConfigError("Unexpected preset select command: " + definition);
    }

    private static int getProgramNumber(MidiPresetNumbering presetNumbering, List<Integer> ids) {
        return getZeroBasedNumber("program change", presetNumbering, ids.getLast());
    }

    private static int getZeroBasedNumber(String field, MidiPresetNumbering presetNumbering, int value) {
        int valueOffset = switch (presetNumbering) {
            case FROM_ONE -> 1;
            case FROM_ZERO -> 0;
        };
        if (presetNumbering == FROM_ONE && value == 0) {
            throw new MidiError("You are using a zero based '%s' and at the same time '%s'".formatted(field, FROM_ONE.name()));
        }
        // MIDI values always from 0 (channels, program numbers...)
        return value - valueOffset;
    }

    private static Stream<MidiMessage> bankMsbLsbThenProgramChange(String definition, int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 3) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 3, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_LSB_PRG, ids.size(), definition));
        }
        int bankMSB = ids.get(0);
        int bankLSB = ids.get(1);
        return Stream.of(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> bankLsbThenProgramChange(String definition, int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_LSB_PRG, ids.size(), definition));
        }
        int bankLSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> bankMsbThenProgramChange(String definition, int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        int bankMSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> programChangeOnly(String definition, int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 1) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 1, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    /**
     * A preset selector can have a maximum of 3 identifiers:
     * <ul>
     *     <li>prg: program change only</li>
     *     <li>lsb,prg: bankName select LSB then program change</li>
     *     <li>msb,lsb,prg: bankName select MSB,LSB then program change</li>
     * </ul>
     *
     * @return A list of identifiers, the last one is always the program change
     */
    private static List<Integer> preparePresetSelectIdentifiers(Matcher matcher) {
        String id1 = matcher.group("id1");
        String id2 = matcher.group("id2");
        String id3 = matcher.group("id3");
        List<Integer> ids = Stream.of(id1, id2, id3)
                .filter(id -> id != null)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return ids;
    }

    private static Stream<MidiMessage> parseMidiCommand(String definition) {
        Matcher matcher = COMMAND_REGEXP.matcher(definition);
        List<Byte> list = new ArrayList<>();
        while (matcher.find()) {
            try {
                String byteValue = matcher.group(1);
                list.add((byte) Integer.parseInt(byteValue, 16));
            } catch (NumberFormatException e) {
                throw new MidiError("Unexpected sysex command definition: " + definition);
            }
        }
        try {
            return Stream.of(new ShortMessage(list.get(0), list.get(1), list.get(2)));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private static Stream<MidiMessage> parseSysExCommand(String definition) {
        Matcher matcher = COMMAND_REGEXP.matcher(definition);
        List<Byte> list = new ArrayList<>();
        while (matcher.find()) {
            try {
                String byteValue = matcher.group(1);
                list.add((byte) Integer.parseInt(byteValue, 16));
            } catch (NumberFormatException e) {
                throw new MidiError("Unexpected sysex command definition: " + definition);
            }
        }
        byte[] payload = new byte[list.size()];
        IntStream.range(0, list.size())
                .forEach(i -> payload[i] = (byte) list.get(i));
        try {
            return Stream.of(new SysexMessage(payload, payload.length));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    @Override
    public String toString() {
        return "MidiPreset[" +
                "title=" + id + ", " +
                "channel=" + channel + ", " +
                "commands=" + commands + ", " +
                "controlChanges=" + controlChanges + ", " +
                "drumKitNotes=" + drumKitNotes + ']';
    }


}
