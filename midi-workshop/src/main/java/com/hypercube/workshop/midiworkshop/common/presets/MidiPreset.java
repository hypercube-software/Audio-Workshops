package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class describe how to get a specific sound. It can be used very simply with a single program change, but we can do much more, specifying a specific tempo via sysex before selecting a preset
 * <p>Supported commands to select a sound:</p>
 * <ul>
 *     <li>bank select message</li>
 *     <li>program change</li>
 *     <li>Sysex</li>
 * </ul>
 */
public record MidiPreset(String title,
                         List<MidiMessage> commands) {

    public static final int BANK_SELECT_MSB = 0;
    public static final int BANK_SELECT_LSB = 32;
    private static Pattern presetRegExp = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?");
    private static final Pattern commnadRegExp = Pattern.compile("\\s*([A-F0-9]{2})");

    private Optional<ShortMessage> getCommand(int command) {
        return commands.stream()
                .filter(cmd -> cmd instanceof ShortMessage)
                .map(cmd -> (ShortMessage) cmd)
                .filter(cmd -> cmd.getCommand() == command)
                .findFirst();
    }

    public String getId() {
        int bank = getBankMSB() << 7 | getBankLSB();
        return "%01d-%03d".formatted(bank, getProgram());
    }

    public int getProgram() {
        return getCommand(ShortMessage.PROGRAM_CHANGE)
                .map(cmd -> cmd.getData1())
                .orElse(-1);
    }

    public int getBankMSB() {
        return getCommand(ShortMessage.CONTROL_CHANGE).filter(cmd -> cmd.getData1() == BANK_SELECT_MSB)
                .map(cmd -> cmd.getData1())
                .orElse(0);
    }

    public int getBankLSB() {
        return getCommand(ShortMessage.CONTROL_CHANGE).filter(cmd -> cmd.getData1() == BANK_SELECT_LSB)
                .map(cmd -> cmd.getData1())
                .orElse(0);
    }

    public static MidiPreset of(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String title, List<String> commands) {
        return new MidiPreset(title, commands.stream()
                .flatMap(cmd -> parseCommand(midiBankFormat, presetNumbering, cmd))
                .toList());
    }

    private static Stream<MidiMessage> parseCommand(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String definition) {
        try {
            if (definition.startsWith("F0")) {
                return parseSysExCommand(definition);
            }
            return parsePresetSelectCommand(midiBankFormat, presetNumbering, definition);
        } catch (NumberFormatException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private static Stream<MidiMessage> parsePresetSelectCommand(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String definition) throws InvalidMidiDataException {
        Matcher matcher = presetRegExp.matcher(definition);
        if (matcher.find()) {
            List<Integer> ids = preparePresetSelectIdentifiers(matcher);
            int program = getProgramNumber(presetNumbering, ids);
            return switch (midiBankFormat) {
                case NO_BANK -> programChangeOnly(program);
                case BANK_MSB_PRG -> bankMsbThenProgramChange(ids, program);
                case BANK_LSB_PRG -> bankLsbThenProgramChange(ids, program);
                case BANK_MSB_LSB_PRG -> bankMsbLsbThenProgramChange(ids, program);
            };
        }
        throw new MidiError("Unexpected preset select command: " + definition);
    }

    private static int getProgramNumber(MidiPresetNumbering presetNumbering, List<Integer> ids) {
        int programOffset = switch (presetNumbering) {
            case FROM_ONE -> 1;
            case FROM_ZERO -> 0;
        };
        // MIDI programs start always from 0
        return ids.getLast() - programOffset;
    }

    private static Stream<MidiMessage> bankMsbLsbThenProgramChange(List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankMSB = ids.get(0);
        int bankLSB = ids.get(1);
        return Stream.of(new ShortMessage(ShortMessage.CONTROL_CHANGE, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.CONTROL_CHANGE, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, program, 0));
    }

    private static Stream<MidiMessage> bankLsbThenProgramChange(List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankLSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, program, 0));
    }

    private static Stream<MidiMessage> bankMsbThenProgramChange(List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankMSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, program, 0));
    }

    private static Stream<MidiMessage> programChangeOnly(int program) throws InvalidMidiDataException {
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, program, 0));
    }

    /**
     * A preset selector can a maximum of 3 identifiers:
     * <ul>
     *     <li>prg: program change only</li>
     *     <li>lsb,prg: bank select LSB then program change</li>
     *     <li>msb,lsb,prg: bank select MSB,LSB then program change</li>
     * </ul>
     *
     * @return A list of 3 identifiers, missing ones are 0, the last one is always the program change
     */
    private static List<Integer> preparePresetSelectIdentifiers(Matcher matcher) {
        String id1 = matcher.group("id1");
        String id2 = matcher.group("id2");
        String id3 = matcher.group("id3");
        List<Integer> ids = Stream.of(id1, id2, id3)
                .filter(id -> id != null)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        while (ids.size() != 3) {
            ids.add(0, 0);
        }
        return ids;
    }

    private static Stream<MidiMessage> parseSysExCommand(String definition) throws InvalidMidiDataException {
        Matcher matcher = commnadRegExp.matcher(definition);
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

}
