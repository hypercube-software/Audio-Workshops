package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import lombok.experimental.UtilityClass;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@UtilityClass
public class MidiPresetBuilder {
    private static Pattern PRESET_REGEXP = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?");
    private static final Pattern COMMAND_REGEXP = Pattern.compile("\\s*([A-F0-9]{2})");

    public static MidiPreset parse(File configFile, int zeroBasedChannel, MidiBankFormat midiBankFormat, String title, List<CommandMacro> macros, List<String> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes) {
        List<MidiMessage> messages = commands.stream()
                .flatMap(cmd -> parseCommand(configFile, zeroBasedChannel, midiBankFormat, macros, cmd))
                .toList();
        return new MidiPreset(null, null, title, zeroBasedChannel, messages, controlChanges, drumKitNotes, midiBankFormat);
    }

    public static MidiPreset parse(MidiDeviceDefinition device, MidiDeviceMode deviceMode, MidiDeviceBank bank, int program) {
        return new MidiPreset(deviceMode.getName(), bank.getName(), null, bank.getZeroBasedChannel(), forgeCommands(device, bank, program), device.getPresetFormat());
    }

    public static MidiPreset parse(String title, MidiDeviceDefinition device, int zeroBasedChannel, int bankMSB, int bankLSB, int program) {
        String bankName = String.format("$%X", (bankMSB << 8) | bankLSB);
        return new MidiPreset(null, bankName, title, zeroBasedChannel, forgeCommands(device, zeroBasedChannel, bankMSB, bankLSB, program), List.of(), List.of(), device.getPresetFormat());
    }

    public static MidiPreset parse(String bankName, String presetName, String presetCategory, MidiDeviceDefinition device, int zeroBasedChannel, int bankMSB, int bankLSB, int program) {
        return new MidiPreset(null, bankName, presetName, presetCategory, zeroBasedChannel, forgeCommands(device, zeroBasedChannel, bankMSB, bankLSB, program), new ArrayList<>(), new ArrayList<>(), device.getPresetFormat());
    }

    public static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, MidiDeviceBank bank, int program) {
        MidiBankFormat midiBankFormat = device.getPresetFormat();
        String bankCommand = bank.getCommand();
        if (bankCommand != null && bankCommand
                .contains("(")) {
            String commandCall = bankCommand
                    .replace("program", "$%02X".formatted(program));
            return CommandCall.parse(device.getDefinitionFile(), commandCall)
                    .stream()
                    .flatMap(call -> {
                        CommandMacro macro = device.getMacro(call);
                        String cmd = macro.expand(call);
                        return parseCommand(device.getDefinitionFile(), 0, midiBankFormat, device.getMacros(), cmd);
                    })
                    .toList();
        } else {
            int bankId = midiBankFormat == MidiBankFormat.NO_BANK_PRG ? -1 : device.getBankId(bank.getName());
            int zeroBasedChannel = bank.getZeroBasedChannel();
            return switch (midiBankFormat) {
                case NO_BANK_PRG -> forgeCommands(device, zeroBasedChannel, 0, 0, program);
                case BANK_MSB_PRG, BANK_PRG_PRG -> forgeCommands(device, zeroBasedChannel, bankId, 0, program);
                case BANK_LSB_PRG -> forgeCommands(device, zeroBasedChannel, 0, bankId, program);
                case BANK_MSB_LSB_PRG -> {
                    int msb = (bankId >> 8) & 0x7F;
                    int lsb = (bankId >> 0) & 0x7F;
                    yield forgeCommands(device, zeroBasedChannel, msb, lsb, program);
                }
            };
        }
    }

    private static List<MidiMessage> forgeCommands(MidiDeviceDefinition device, int zeroBasedChannel, int msb, int lsb, int program) {
        String definition = switch (device.getPresetFormat()) {
            case NO_BANK_PRG -> "%d".formatted(program);
            case BANK_MSB_PRG, BANK_PRG_PRG -> "%d-%d".formatted(msb, program);
            case BANK_LSB_PRG -> "%d-%d".formatted(lsb, program);
            case BANK_MSB_LSB_PRG -> "%d-%d-%d".formatted(msb, lsb, program);
        };

        try {
            return parsePresetSelectCommand(zeroBasedChannel, device.getPresetFormat(), definition).toList();
        } catch (MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    /**
     * One command template can produce multiple {@link MidiMessage}, this is why this method return a stream
     * <p>For instance, we often select a bankName, then select a program change. Two messages must be sent</p>
     *
     * @param zeroBasedChannel MIDI channel where to activate this preset
     * @param midiBankFormat   Which kind of MIDI message must be used to select this preset
     * @param macros           Macros which can be used to generate the commands
     * @param definition       Textual definition of the command template
     * @return
     */
    private static Stream<MidiMessage> parseCommand(File configFile, int zeroBasedChannel, MidiBankFormat midiBankFormat, List<CommandMacro> macros, String definition) {
        try {
            String expandedDefinition = getExpandedDefinitions(configFile, macros, definition);
            return Arrays.stream(expandedDefinition.split(";"))
                    .map(String::trim)
                    .flatMap(def -> {
                        if (def.startsWith("F0")) {
                            return parseSysExCommand(def);
                        } else if (def.startsWith("B0") || def.startsWith("C0")) {
                            return parseMidiCommand(def);
                        }
                        return parsePresetSelectCommand(zeroBasedChannel, midiBankFormat, def);
                    });
        } catch (NumberFormatException | MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    /**
     * Given a list of command definition, resolve all macro in it
     *
     * @param configFile  Where this definition is defined (used to log errors)
     * @param macros      list of known macros
     * @param definitions The definitions to parse, can be a lsit of macro calls A();B();C()
     * @return
     */
    private String getExpandedDefinitions(File configFile, List<CommandMacro> macros, String definitions) {
        String expanded = Arrays.stream(definitions.split(";"))
                .map(definition -> getExpandedDefinition(configFile, macros, definition))
                .collect(Collectors.joining(";"));
        if (expanded.contains("(")) {
            return getExpandedDefinitions(configFile, macros, expanded);
        }
        return expanded;
    }

    private String getExpandedDefinition(File configFile, List<CommandMacro> macros, String definition) {
        return Optional.ofNullable(macros)
                .map(nonNullMacros -> nonNullMacros.stream()
                        .filter(m -> m.matches(definition))
                        .findFirst()
                        .map(m -> CommandCall.parse(configFile, definition)
                                .stream()
                                .map(commandCall -> m.expand(commandCall))
                                .collect(Collectors.joining(";")))
                        .orElse(definition))
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
     * @param zeroBasedChannel which midi channel to use for the midi messages
     * @param midiBankFormat   which format to generate
     * @param definition       the definition of the preset
     * @return a stream of Midi messages to select the patch on the given midi channel
     * @throws InvalidMidiDataException
     */
    private static Stream<MidiMessage> parsePresetSelectCommand(int zeroBasedChannel, MidiBankFormat midiBankFormat, String definition) {

        List<Integer> ids = preparePresetSelectIdentifiers(midiBankFormat, definition);
        if (!ids.isEmpty()) {
            int zeroBasedProgram = getProgramNumber(ids);
            try {
                return switch (midiBankFormat) {
                    case NO_BANK_PRG -> programChangeOnly(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_MSB_PRG -> bankMsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_LSB_PRG -> bankLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_MSB_LSB_PRG ->
                            bankMsbLsbThenProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                    case BANK_PRG_PRG -> doubleProgramChange(definition, zeroBasedChannel, ids, zeroBasedProgram);
                };
            } catch (InvalidMidiDataException e) {
                throw new MidiError(e);
            }
        } else {
            throw new MidiConfigError("Unexpected preset select command: " + definition);
        }
    }

    private static int getProgramNumber(List<Integer> ids) {
        return ids.getLast();
    }

    private static Stream<MidiMessage> bankMsbLsbThenProgramChange(String definition, int zeroBasedChannel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 3) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 3, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_LSB_PRG, ids.size(), definition));
        }
        int bankMSB = ids.get(0);
        int bankLSB = ids.get(1);
        return Stream.of(new ShortMessage(ShortMessage.CONTROL_CHANGE, zeroBasedChannel, MidiPreset.BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.CONTROL_CHANGE, zeroBasedChannel, MidiPreset.BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, program, 0));
    }

    private static Stream<MidiMessage> bankLsbThenProgramChange(String definition, int zeroBasedChannel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_LSB_PRG, ids.size(), definition));
        }
        int bankLSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, zeroBasedChannel, MidiPreset.BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, program, 0));
    }

    private static Stream<MidiMessage> bankMsbThenProgramChange(String definition, int zeroBasedChannel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        int bankMSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, zeroBasedChannel, MidiPreset.BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, program, 0));
    }

    private static Stream<MidiMessage> programChangeOnly(String definition, int zeroBasedChannel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 1) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 1, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, program, 0));
    }

    private static Stream<MidiMessage> doubleProgramChange(String definition, int zeroBasedChannel, List<Integer> ids, int program) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 1, it is %d: %s".formatted(MidiBankFormat.BANK_PRG_PRG, ids.size(), definition));
        }
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, ids.getFirst(), 0),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, zeroBasedChannel, program, 0));
    }

    /**
     * A preset selector can have a maximum of 3 identifiers:
     * <ul>
     *     <li>prg: program change only</li>
     *     <li>lsb,prg: bankName select LSB then program change</li>
     *     <li>prg,prg: double program change (Yamaha TG-77)</li>
     *     <li>msb,lsb,prg: bankName select MSB,LSB then program change</li>
     * </ul>
     *
     * @return A list of identifiers, the last one is always the program change
     */
    private static List<Integer> preparePresetSelectIdentifiers(MidiBankFormat midiBankFormat, String definition) {
        int expectedSize = switch (midiBankFormat) {
            case NO_BANK_PRG -> 2;
            case BANK_MSB_PRG -> 4;
            case BANK_LSB_PRG -> 4;
            case BANK_MSB_LSB_PRG -> 6;
            case BANK_PRG_PRG -> 4;
        };
        if (!definition.contains("-") && definition.length() == expectedSize) {
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < definition.length(); i += 2) {
                String hexPair = definition.substring(i, i + 2);
                try {
                    result.add(Integer.parseInt(hexPair, 16));
                } catch (NumberFormatException e) {
                    throw new MidiConfigError("Unable to parse hexadecimal command definition:" + definition);
                }
            }
            return result;
        } else {
            Matcher matcher = PRESET_REGEXP.matcher(definition);
            if (matcher.find()) {
                String id1 = matcher.group("id1");
                String id2 = matcher.group("id2");
                String id3 = matcher.group("id3");
                List<Integer> ids = Stream.of(id1, id2, id3)
                        .filter(id -> id != null)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                return ids;
            } else {
                throw new MidiConfigError("Unable to parse command definition:" + definition);
            }
        }
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

    public static MidiPreset fromSysExFile(String deviceMode, String bank, File sysExFile) {
        List<MidiMessage> commands = new ArrayList<>();
        try {
            byte[] data = Files.readAllBytes(sysExFile.toPath());
            ByteArrayOutputStream evt = new ByteArrayOutputStream();

            for (int i = 0; i < data.length; i++) {
                evt.write(data[i]);
                if (data[i] == (byte) 0xF7) {
                    byte[] payload = evt.toByteArray();
                    MidiMessage sysex = new SysexMessage(payload, payload.length);
                    commands.add(sysex);
                    evt.reset();
                }
            }
            return new MidiPreset(deviceMode, bank, sysExFile.getName(), 0, commands, List.of(MidiPreset.NO_CC), null, null);
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }
}
