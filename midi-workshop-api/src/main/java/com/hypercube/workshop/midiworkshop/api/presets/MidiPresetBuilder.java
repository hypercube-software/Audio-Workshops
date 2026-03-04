package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@UtilityClass
public class MidiPresetBuilder {
    private static final Pattern COMMAND_REGEXP = Pattern.compile("\\s*([A-F0-9]{2})");
    private static final Pattern PRESET_REGEXP = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?");

    /**
     * Used by SynthRipper and MPM. TODO: since SynthRipper don't use MidiDeviceLibrary macros, we need this method
     */
    public static MidiPreset parse(MidiDeviceDefinition device, int zeroBasedChannel, String title, List<CommandMacro> macros, List<String> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes) {
        var ctx = BuildContext.of(device, zeroBasedChannel);
        ctx.setMidiMessages(forgeCommands(macros, commands, ctx));
        return new MidiPreset(null, null, title, ctx.getChannel(), ctx.getMidiMessages(), controlChanges, drumKitNotes, ctx.getMidiBankFormat(), ctx.getIdentifiers());
    }

    /**
     * Main entry point. Used by {@link com.hypercube.workshop.midiworkshop.api.presets.crawler.MidiPresetCrawler}
     */
    public static MidiPreset parse(MidiDeviceDefinition device, MidiDeviceMode deviceMode, MidiDeviceBank bank, int program) {
        var ctx = BuildContext.of(device, bank.getZeroBasedChannel());
        ctx.setMidiMessages(forgeCommands(ctx, bank, program));
        return new MidiPreset(deviceMode.getName(), bank.getName(), null, ctx.getChannel(), ctx.getMidiMessages(), ctx.getMidiBankFormat(), ctx.getIdentifiers());
    }

    /**
     * Used by SteinbergScriptFileParser, to generate sound canvas presets. TODO: remove that
     */
    public static MidiPreset parse(String title, MidiDeviceDefinition device, int zeroBasedChannel, int bankMSB, int bankLSB, int program) {
        String bankName = String.format("$%X", (bankMSB << 8) | bankLSB);
        var ctx = BuildContext.of(device, zeroBasedChannel)
                .setIdentifiers(bankMSB, bankLSB, program);
        ctx.setMidiMessages(forgeCommands(ctx));
        return new MidiPreset(null, bankName, title, ctx.getChannel(), ctx.getMidiMessages(), List.of(), List.of(), ctx.getMidiBankFormat(), ctx.getIdentifiers());
    }

    /**
     * Used by {@link com.hypercube.workshop.midiworkshop.api.presets.yamaha.XGSpecParser} which is hidden in Unit tests. TODO: remove that
     */
    public static MidiPreset parse(String bankName, String presetName, String presetCategory, MidiDeviceDefinition device, int zeroBasedChannel, int bankMSB, int bankLSB, int program) {
        var ctx = BuildContext.of(device, zeroBasedChannel)
                .setIdentifiers(bankMSB, bankLSB, program);
        ctx.setMidiMessages(forgeCommands(ctx));
        return new MidiPreset(null, bankName, presetName, presetCategory, ctx.getChannel(), ctx.getMidiMessages(), new ArrayList<>(), new ArrayList<>(), device.getPresetFormat(), ctx.getIdentifiers());
    }

    private static List<MidiMessage> forgeCommands(List<CommandMacro> macros, List<String> commands, BuildContext ctx) {
        return commands.stream()
                .flatMap(cmd -> parseCommand(ctx, macros, cmd))
                .toList();
    }

    private static List<MidiMessage> forgeCommands(BuildContext ctx, MidiDeviceBank bank, int program) {
        var device = ctx.getDevice();
        MidiBankFormat midiBankFormat = device.getPresetFormat();
        String bankCommand = bank.getCommand();
        if (bankCommand != null && bankCommand
                .contains("(")) {
            String commandCall = bankCommand
                    .replace("program", "$%02X".formatted(program));
            return CommandCall.parse(device.getDefinitionFile(), device, commandCall)
                    .stream()
                    .flatMap(call -> {
                        CommandMacro macro = device.getMacro(call);
                        String cmd = macro.expand(call);
                        return parseCommand(ctx, device.getMacros(), cmd);
                    })
                    .toList();
        } else {
            int bankId = midiBankFormat == MidiBankFormat.NO_BANK_PRG ? -1 : device.getBankId(bank.getName());
            int zeroBasedChannel = bank.getZeroBasedChannel();
            return switch (midiBankFormat) {
                case NO_BANK_PRG -> forgeCommands(ctx.setIdentifiers(0, 0, program));
                case BANK_MSB_PRG, BANK_PRG_PRG -> forgeCommands(ctx.setIdentifiers(bankId, 0, program));
                case BANK_LSB_PRG -> forgeCommands(ctx.setIdentifiers(0, bankId, program));
                case BANK_MSB_LSB_PRG -> {
                    int msb = (bankId >> 8) & 0x7F;
                    int lsb = (bankId >> 0) & 0x7F;
                    yield forgeCommands(ctx.setIdentifiers(msb, lsb, program));
                }
            };
        }
    }

    private static List<MidiMessage> forgeCommands(BuildContext ctx) {
        var ids = ctx.getIdentifiers();
        String definition = switch (ctx.getDevice()
                .getPresetFormat()) {
            case NO_BANK_PRG -> "%d".formatted(ids.getPrg());
            case BANK_MSB_PRG, BANK_PRG_PRG -> "%d-%d".formatted(ids.getMsb(), ids.getPrg());
            case BANK_LSB_PRG -> "%d-%d".formatted(ids.getLsb(), ids.getPrg());
            case BANK_MSB_LSB_PRG -> "%d-%d-%d".formatted(ids.getMsb(), ids.getLsb(), ids.getPrg());
        };

        try {
            return parsePresetSelectCommand(ctx, definition).toList();
        } catch (MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    /**
     * One command template can produce multiple {@link MidiMessage}, this is why this method return a stream
     * <p>For instance, we often select a bankName, then select a program change. Two messages must be sent</p>
     *
     * @param ctx        build context
     * @param macros     Macros which can be used to generate the commands
     * @param definition Textual definition of the command template
     */
    private static Stream<MidiMessage> parseCommand(BuildContext ctx, List<CommandMacro> macros, String definition) {
        try {
            String expandedDefinition = getExpandedDefinitions(ctx, macros, definition);
            return Arrays.stream(expandedDefinition.split(";"))
                    .map(String::trim)
                    .flatMap(def -> {
                        if (def.startsWith("F0")) {
                            return parseSysExCommand(def);
                        } else if (def.startsWith("B0") || def.startsWith("C0")) {
                            return parseMidiCommand(def);
                        }
                        return parsePresetSelectCommand(ctx, def);
                    });
        } catch (NumberFormatException | MidiError e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    /**
     * Given a list of command definition separated by ";", resolve all macro in it
     *
     * @param ctx         build context
     * @param macros      list of known macros
     * @param definitions The definitions to parse, can be a list of macro calls A();B();C()
     */
    private String getExpandedDefinitions(BuildContext ctx, List<CommandMacro> macros, String definitions) {
        String expanded = Arrays.stream(definitions.split(";"))
                .map(definition -> getExpandedDefinition(ctx, macros, definition))
                .collect(Collectors.joining(";"));
        if (expanded.contains("(")) {
            return getExpandedDefinitions(ctx, macros, expanded);
        }
        return expanded;
    }

    /**
     * Given a single command definition, resolve all macro in it
     *
     * @param ctx        build context
     * @param macros     list of known macros
     * @param definition The definition to parse, something like A(12 00)
     */
    private String getExpandedDefinition(BuildContext ctx, List<CommandMacro> macros, String definition) {
        return Optional.ofNullable(macros)
                .map(nonNullMacros -> nonNullMacros.stream()
                        .filter(m -> m.matches(definition))
                        .findFirst()
                        .map(m -> CommandCall.parse(ctx.getDevice()
                                        .getDefinitionFile(), ctx.getDevice(), definition)
                                .stream()
                                .map(m::expand)
                                .collect(Collectors.joining(";")))
                        .orElse(definition))
                .orElse(definition);
    }

    /**
     * Parse a preset definition according to {@link MidiBankFormat} and generate the right sequence of midi message to select a patch.
     * <ul>
     *     <li>A program change message</li>
     *     <li>A bank select MSB message followed by a program change message</li>
     *     <li>A bank select LSB message followed by a program change message</li>
     *     <li>A bank select MSB message followed by a bank select LSB message followed by a program change message</li>
     *     <li>Two program change messages</li>
     * </ul>
     *
     * @param ctx        build context
     * @param definition the definition of the preset
     * @return a stream of Midi messages to select the patch on the given midi channel
     */
    private static Stream<MidiMessage> parsePresetSelectCommand(BuildContext ctx, String definition) {

        List<Integer> ids = parsePresetSelector(ctx, definition);
        if (!ids.isEmpty()) {
            try {
                return switch (ctx.getMidiBankFormat()) {
                    case NO_BANK_PRG -> programChangeOnly(ctx, definition, ids);
                    case BANK_MSB_PRG -> bankMsbThenProgramChange(ctx, definition, ids);
                    case BANK_LSB_PRG -> bankLsbThenProgramChange(ctx, definition, ids);
                    case BANK_MSB_LSB_PRG -> bankMsbLsbThenProgramChange(ctx, definition, ids);
                    case BANK_PRG_PRG -> doubleProgramChange(ctx, definition, ids);
                };
            } catch (InvalidMidiDataException e) {
                throw new MidiError(e);
            }
        } else {
            throw new MidiConfigError("Unexpected preset select command: " + definition);
        }
    }

    private static Stream<MidiMessage> bankMsbLsbThenProgramChange(BuildContext ctx, String definition, List<Integer> ids) throws InvalidMidiDataException {
        if (ids.size() != 3) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 3, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_LSB_PRG, ids.size(), definition));
        }
        ctx.setIdentifiers(ids.get(0), ids.get(1), ids.getLast());
        return Stream.of(new ShortMessage(ShortMessage.CONTROL_CHANGE, ctx.getChannel(), MidiPreset.BANK_SELECT_MSB, ctx.identifiers.getMsb()),
                new ShortMessage(ShortMessage.CONTROL_CHANGE, ctx.getChannel(), MidiPreset.BANK_SELECT_LSB, ctx.identifiers.getLsb()),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getPrg(), 0));
    }

    private static Stream<MidiMessage> bankLsbThenProgramChange(BuildContext ctx, String definition, List<Integer> ids) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_LSB_PRG, ids.size(), definition));
        }
        ctx.setIdentifiers(null, ids.getFirst(), ids.getLast());
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, ctx.getChannel(), MidiPreset.BANK_SELECT_LSB, ctx.identifiers.getLsb()),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getPrg(), 0));
    }

    private static Stream<MidiMessage> bankMsbThenProgramChange(BuildContext ctx, String definition, List<Integer> ids) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 2, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        ctx.setIdentifiers(ids.getFirst(), null, ids.getLast());
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, ctx.getChannel(), MidiPreset.BANK_SELECT_MSB, ctx.identifiers.getMsb()),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getPrg(), 0));
    }

    private static Stream<MidiMessage> programChangeOnly(BuildContext ctx, String definition, List<Integer> ids) throws InvalidMidiDataException {
        if (ids.size() != 1) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 1, it is %d: %s".formatted(MidiBankFormat.BANK_MSB_PRG, ids.size(), definition));
        }
        ctx.setIdentifiers(null, null, ids.getLast());
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getPrg(), 0));
    }

    private static Stream<MidiMessage> doubleProgramChange(BuildContext ctx, String definition, List<Integer> ids) throws InvalidMidiDataException {
        if (ids.size() != 2) {
            throw new MidiConfigError("Unexpected number of values given %s, should be 1, it is %d: %s".formatted(MidiBankFormat.BANK_PRG_PRG, ids.size(), definition));
        }
        Integer first = ids.getFirst();
        ctx.setIdentifiers(null, first, ids.getLast());
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getLsb(), 0),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, ctx.getChannel(), ctx.identifiers.getPrg(), 0));
    }

    /**
     * A preset selector can have a maximum of 3 identifiers:
     * <ul>
     *     <li>prg: program change only</li>
     *     <li>lsb,prg: bank select LSB then program change</li>
     *     <li>prg,prg: double program change (Yamaha TG-77)</li>
     *     <li>msb,lsb,prg: bank select MSB,LSB then program change</li>
     * </ul>
     *
     * @return A list of identifiers, the last one is always the program change
     */
    public static List<Integer> parsePresetSelector(MidiBankFormat presetFormat, String definition) {
        return parsePresetSelector(BuildContext.of(presetFormat), definition);
    }

    private static List<Integer> parsePresetSelector(BuildContext ctx, String definition) {
        int expectedStringSize = switch (ctx.getMidiBankFormat()) {
            case NO_BANK_PRG -> 2;
            case BANK_MSB_PRG, BANK_LSB_PRG, BANK_PRG_PRG -> 4;
            case BANK_MSB_LSB_PRG -> 6;
        };
        if (!definition.contains("-") && expectedStringSize > 2 && definition.length() == expectedStringSize) {
            // Hexadecimal definition
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < definition.length(); i += 2) {
                String hexPair = definition.substring(i, i + 2);
                try {
                    ids.add(Integer.parseInt(hexPair, 16));
                } catch (NumberFormatException e) {
                    throw new MidiConfigError("Unable to parse hexadecimal command definition:" + definition);
                }
            }
            chekIds(ctx, definition, ids);
            return ids;
        } else {
            // Decimal definition with "-" as separator
            Matcher matcher = PRESET_REGEXP.matcher(definition);
            if (matcher.find()) {
                String id1 = matcher.group("id1");
                String id2 = matcher.group("id2");
                String id3 = matcher.group("id3");
                List<Integer> ids = Stream.of(id1, id2, id3)
                        .filter(Objects::nonNull)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                chekIds(ctx, definition, ids);
                return ids;
            } else {
                throw new MidiConfigError("Unable to parse command definition:" + definition);
            }
        }
    }

    private static void chekIds(BuildContext ctx, String definition, List<Integer> ids) {
        if (!ids.stream()
                .filter(id -> id > 127)
                .toList()
                .isEmpty()) {
            throw new MidiError("The command definition '%s' contains out of range values: '%s' given MidiBankFormat: %s".formatted(definition, ids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining()), ctx.getMidiBankFormat()
                    .name()));
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
                throw new MidiError("Unexpected sysEx command definition: " + definition);
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
                throw new MidiError("Unexpected sysEx command definition: " + definition);
            }
        }
        byte[] payload = new byte[list.size()];
        IntStream.range(0, list.size())
                .forEach(i -> payload[i] = list.get(i));
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

            for (byte datum : data) {
                evt.write(datum);
                if (datum == (byte) 0xF7) {
                    byte[] payload = evt.toByteArray();
                    MidiMessage sysEx = new SysexMessage(payload, payload.length);
                    commands.add(sysEx);
                    evt.reset();
                }
            }
            return new MidiPreset(deviceMode, bank, sysExFile.getName(), 0, commands, List.of(MidiPreset.NO_CC), null, null, null);
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    @Getter
    @Setter
    private class BuildContext {
        MidiDeviceDefinition device;
        private Integer channel;
        private List<MidiMessage> midiMessages;
        private MidiBankFormat midiBankFormat;
        private PresetIdentifiers identifiers;

        private BuildContext(MidiBankFormat midiBankFormat) {
            this.midiBankFormat = midiBankFormat;
        }

        private BuildContext(MidiDeviceDefinition device, int channel, PresetIdentifiers identifiers) {
            this.device = device;
            this.channel = channel;
            this.identifiers = identifiers;
            this.midiBankFormat = device.getPresetFormat();
        }

        private BuildContext(MidiDeviceDefinition device, int channel) {
            this.device = device;
            this.channel = channel;
            this.midiBankFormat = device.getPresetFormat();
        }

        public static BuildContext of(MidiDeviceDefinition device, int channel) {
            return new BuildContext(device, channel);
        }

        public static BuildContext of(MidiBankFormat midiBankFormat) {
            return new BuildContext(midiBankFormat);
        }

        public BuildContext setIdentifiers(Integer msb, Integer lsb, Integer prg) {
            this.identifiers = new PresetIdentifiers(msb, lsb, prg);
            return this;
        }

    }
}
