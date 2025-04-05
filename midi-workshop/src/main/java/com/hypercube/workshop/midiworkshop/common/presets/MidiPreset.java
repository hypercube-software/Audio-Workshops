package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.util.ArrayList;
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
 *     <li>bank select message</li>
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
     * Name of the preset
     */
    private final String title;
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
     * How bank select is used
     */
    private final MidiBankFormat midiBankFormat;

    public MidiPreset(String title, int channel, List<MidiMessage> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.title = title;
        this.channel = channel;
        this.commands = commands;
        this.controlChanges = controlChanges;
        this.drumKitNotes = drumKitNotes;
        this.midiBankFormat = midiBankFormat;
        if (controlChanges == null) {
            throw new MidiConfigError("ControlChanges cannot be null, use List.of(NO_CC)");
        }
    }

    public MidiPreset(String title, int channel, List<MidiMessage> commands, List<DrumKitNote> drumKitNotes, MidiBankFormat midiBankFormat) {
        this.title = title;
        this.channel = channel;
        this.commands = commands;
        this.midiBankFormat = midiBankFormat;
        this.controlChanges = List.of(NO_CC);
        this.drumKitNotes = drumKitNotes;
    }

    public MidiPreset(String title, int channel, List<MidiMessage> commands, MidiBankFormat midiBankFormat) {
        this.title = title;
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

    public String getId() {
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
        return new MidiPreset(title, channel, messages, controlChanges, drumKitNotes, midiBankFormat);
    }

    public static MidiPreset of(MidiDeviceDefinition device, int bank, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        return new MidiPreset("NoName", channel, forgeCommands(device, bank, program), device.getPresetFormat());
    }

    public static MidiPreset of(String title, MidiDeviceDefinition device, int bankMSB, int bankLSB, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        return new MidiPreset(title, channel, forgeCommands(device, bankMSB, bankLSB, program), List.of(), List.of(), device.getPresetFormat());
    }

    public static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, int bank, int program) {
        MidiBankFormat midiBankFormat = device.getPresetFormat();
        MidiPresetNumbering presetNumbering = device.getPresetNumbering();
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> forgeCommands(device, 0, 0, program);
            case BANK_MSB_PRG -> forgeCommands(device, bank, 0, program);
            case BANK_LSB_PRG -> forgeCommands(device, 0, bank, program);
            case BANK_MSB_LSB_PRG -> {
                int msb = (bank >> 8) & 0x7F;
                int lsb = (bank >> 0) & 0x7F;
                yield forgeCommands(device, msb, lsb, program);
            }
        };
    }

    private static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, int msb, int lsb, int program) {
        int channel = getDefaultChannel(device.getPresetNumbering());
        String definition = "%d-%d-%d".formatted(msb, lsb, program);
        try {
            return parsePresetSelectCommand(channel, device.getPresetFormat(), device.getPresetNumbering(), definition).toList();
        } catch (InvalidMidiDataException e) {
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
     * <p>For instance, we often select a bank, then select a program change. Two messages must be sent</p>
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
            if (expandedDefinition.startsWith("F0")) {
                return parseSysExCommand(expandedDefinition);
            }
            return parsePresetSelectCommand(channel, midiBankFormat, presetNumbering, expandedDefinition);
        } catch (NumberFormatException | InvalidMidiDataException e) {
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
     *     <li>A bank select MSB message followed by a program change message</li>
     *     <li>A bank select LSB message followed by a program change message</li>
     *     <li>A bank select MSB message followed by a bank select LSB message followed by a program change message</li>
     * </ul>
     *
     * @param channel         which midi channel to use for the midi messages
     * @param midiBankFormat  which format to generate
     * @param presetNumbering how numbers are expressed in the definition
     * @param definition      the definition of the preset
     * @return a stream of Midi messages to select the patch on the given midi channel
     * @throws InvalidMidiDataException
     */
    private static Stream<MidiMessage> parsePresetSelectCommand(int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String definition) throws InvalidMidiDataException {
        Matcher matcher = PRESET_REGEXP.matcher(definition);
        if (matcher.find()) {
            List<Integer> ids = preparePresetSelectIdentifiers(matcher);
            int zeroBasedProgram = getProgramNumber(presetNumbering, ids);
            int zeroBasedChannel = getZeroBasedNumber("channel", presetNumbering, channel);
            return switch (midiBankFormat) {
                case NO_BANK_PRG -> programChangeOnly(definition, zeroBasedChannel, ids, zeroBasedProgram);
                case BANK_MSB_PRG -> bankMsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                case BANK_LSB_PRG -> bankLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                case BANK_MSB_LSB_PRG ->
                        bankMsbLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
            };
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
     *     <li>lsb,prg: bank select LSB then program change</li>
     *     <li>msb,lsb,prg: bank select MSB,LSB then program change</li>
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

    private static Stream<MidiMessage> parseSysExCommand(String definition) throws InvalidMidiDataException {
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
        return Stream.of(new SysexMessage(payload, payload.length));
    }

    @Override
    public String toString() {
        return "MidiPreset[" +
                "title=" + title + ", " +
                "channel=" + channel + ", " +
                "commands=" + commands + ", " +
                "controlChanges=" + controlChanges + ", " +
                "drumKitNotes=" + drumKitNotes + ']';
    }


}
