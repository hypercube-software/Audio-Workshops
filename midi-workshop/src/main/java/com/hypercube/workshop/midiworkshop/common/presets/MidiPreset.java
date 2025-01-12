package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.RequiredArgsConstructor;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering.FROM_ONE;

/**
 * This class describe how to get a specific sound. It can be used very simply with a single program change, but we can do much more, specifying a specific tempo via sysex before selecting a preset
 * <p>Supported commands to select a sound:</p>
 * <ul>
 *     <li>bank select message</li>
 *     <li>program change</li>
 *     <li>Sysex</li>
 * </ul>
 * {@link #controlChanges} indicate this preset respond to specific control changes
 * {@link #drumKitNotes} if not empty, indicate this preset load a drumkit
 */
@RequiredArgsConstructor
public final class MidiPreset {

    public static final int BANK_SELECT_MSB = 0;
    public static final int BANK_SELECT_LSB = 32;
    public static final int NO_CC = -1;
    private static Pattern presetRegExp = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?");
    private static final Pattern commnadRegExp = Pattern.compile("\\s*([A-F0-9]{2})");
    /**
     * Name of the preset
     */
    private final String title;
    /**
     * Midi channel where we activate this preset
     */
    private final int channel;
    /**
     * How to activate this preset
     */
    private final List<MidiMessage> commands;
    /**
     * Which CC must be used during the record of this preset
     */
    private final List<Integer> controlChanges;
    /**
     * If the preset is a drumkit, here are the drum kit notes to record
     */
    private final List<DrumKitNote> drumKitNotes;

    /**
     * Retreive a Mid message by its command number
     *
     * @param command Midi command ({@link ShortMessage#PROGRAM_CHANGE}, {@link ShortMessage#CONTROL_CHANGE}...)
     * @return
     */
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

    public static MidiPreset of(File configFile, int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String title, List<CommandMacro> macros, List<String> commands, List<Integer> controlChanges, List<DrumKitNote> drumKitNotes) {
        return new MidiPreset(title, channel, commands.stream()
                .flatMap(cmd -> parseCommand(configFile, channel, midiBankFormat, presetNumbering, macros, cmd))
                .toList(), controlChanges, drumKitNotes);
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
            String expandedDefinition = macros.stream()
                    .filter(m -> m.matches(definition))
                    .findFirst()
                    .map(m -> {
                        return m.expand(CommandCall.parse(configFile, definition));
                    })
                    .orElse(definition);
            if (expandedDefinition.startsWith("F0")) {
                return parseSysExCommand(expandedDefinition);
            }
            return parsePresetSelectCommand(channel, midiBankFormat, presetNumbering, expandedDefinition);
        } catch (NumberFormatException | InvalidMidiDataException e) {
            throw new MidiError("Faulty preset definition:" + definition, e);
        }
    }

    private static Stream<MidiMessage> parsePresetSelectCommand(int channel, MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String definition) throws InvalidMidiDataException {
        Matcher matcher = presetRegExp.matcher(definition);
        if (matcher.find()) {
            List<Integer> ids = preparePresetSelectIdentifiers(matcher);
            int zeroBasedProgram = getProgramNumber(presetNumbering, ids);
            int zeroBasedChannel = getZeroBasedNumber("channel", presetNumbering, channel);
            return switch (midiBankFormat) {
                case NO_BANK -> programChangeOnly(zeroBasedChannel, zeroBasedProgram);
                case BANK_MSB_PRG -> bankMsbThenProgramChange(zeroBasedChannel, ids, zeroBasedProgram);
                case BANK_LSB_PRG -> bankLsbThenProgramChange(zeroBasedChannel, ids, zeroBasedProgram);
                case BANK_MSB_LSB_PRG -> bankMsbLsbThenProgramChange(zeroBasedChannel, ids, zeroBasedProgram);
            };
        }
        throw new MidiError("Unexpected preset select command: " + definition);
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
            throw new MidiError("You are using a zero based %s and at the same time %s".formatted(field, FROM_ONE.name()));
        }
        // MIDI values always from 0 (channels, program numbers...)
        return value - valueOffset;
    }

    private static Stream<MidiMessage> bankMsbLsbThenProgramChange(int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankMSB = ids.get(0);
        int bankLSB = ids.get(1);
        return Stream.of(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> bankLsbThenProgramChange(int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankLSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_LSB, bankLSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> bankMsbThenProgramChange(int channel, List<Integer> ids, int program) throws InvalidMidiDataException {
        int bankMSB = ids.get(0);
        return Stream.of(
                new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, BANK_SELECT_MSB, bankMSB),
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
    }

    private static Stream<MidiMessage> programChangeOnly(int channel, int program) throws InvalidMidiDataException {
        return Stream.of(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0));
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

    public String title() {
        return title;
    }

    public int channel() {
        return channel;
    }

    public List<MidiMessage> commands() {
        return commands;
    }

    public List<Integer> controlChanges() {
        return controlChanges;
    }

    public List<DrumKitNote> drumKitNotes() {
        return drumKitNotes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MidiPreset) obj;
        return Objects.equals(this.title, that.title) &&
                this.channel == that.channel &&
                Objects.equals(this.commands, that.commands) &&
                Objects.equals(this.controlChanges, that.controlChanges) &&
                Objects.equals(this.drumKitNotes, that.drumKitNotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, channel, commands, controlChanges, drumKitNotes);
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
