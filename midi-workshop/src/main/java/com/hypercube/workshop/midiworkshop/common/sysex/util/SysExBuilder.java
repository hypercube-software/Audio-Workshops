package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility class to construct a SysEx ith fluent API style
 */
@RequiredArgsConstructor
@Slf4j
public class SysExBuilder {
    public static final Pattern DECIMAL_OR_HEX_NUMBER = Pattern.compile("((0x|\\$)(?<hexadecimal>[0-9A-F]+))|(?<decimal>[0-9]+)");
    public static final Pattern nibbles = Pattern.compile("\\s+(?<high>[0-9A-F])\\s+(?<low>[0-9A-F])\\s+");


    private static class State {
        private boolean updateChecksum;
    }

    private final SysExChecksum sysExChecksum;
    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private final State state = new State();

    public SysExBuilder write(int... values) {
        Arrays.stream(values)
                .forEach(value -> {
                    byteStream.write(value);
                    if (state.updateChecksum) {
                        sysExChecksum.update(value);
                    }

                });
        return this;
    }

    public SysExBuilder beginChecksum() {
        state.updateChecksum = true;
        return this;
    }

    public SysExBuilder writeChecksum() {
        state.updateChecksum = false;
        byteStream.write(sysExChecksum.getValue());
        return this;
    }

    public byte[] buildBuffer() {
        return byteStream.toByteArray();
    }

    /**
     * Forge a MIDI event that can be send to a read MIDI Device
     *
     * @return the MID Event
     * @throws InvalidMidiDataException
     */
    public CustomMidiEvent buildMidiEvent() throws InvalidMidiDataException {
        byte[] data = byteStream.toByteArray();
        return new CustomMidiEvent(new SysexMessage(data, data.length), -1);
    }

    @Getter
    @Setter
    private static class CheckSumDef {
        int size;
        int position;

        public int getStartPosition() {
            return position - size;
        }
    }

    public static List<CustomMidiEvent> parse(String definitions) throws InvalidMidiDataException {
        return Arrays.stream(definitions.split(";"))
                .flatMap(definition -> {
                    List<CustomMidiEvent> result = new ArrayList<>();
                    String rawString = resolveASCIIStrings(definition);
                    List<SysExRange> ranges = collectRanges(rawString);
                    if (ranges.size() > 1) {
                        throw new MidiError("SysEx template does not support more than one range for now");
                    }
                    if (ranges.size() > 0) {
                        for (SysExRange range : ranges) {
                            for (int i = range.from(); i <= range.to(); i++) {
                                String hexaFormat = "%0" + range.size() + "X";
                                String hexaValue = hexaFormat.formatted(i);
                                String currentString = rawString.replace(range.value(), hexaValue);
                                result.add(forgeMidiEvent(currentString));
                            }
                        }
                        return result.stream();
                    } else {
                        return Stream.of(forgeMidiEvent(rawString));
                    }
                })
                .toList();
    }

    private static CustomMidiEvent forgeMidiEvent(String inputRawString) {
        CheckSumDef checksum = new CheckSumDef();
        List<Integer> data = new ArrayList<>();
        String rawString = aggregateNibbles(inputRawString);
        Arrays.stream(rawString.split(" "))
                .forEach(chunk -> {
                    if (chunk.startsWith("CK")) {
                        try {
                            checksum.setPosition(data.size());
                            checksum.setSize(Integer.parseInt(chunk.substring(2)));
                        } catch (NumberFormatException e) {
                            throw new MidiError("Unable to parse checksum definition: " + chunk);
                        }
                    } else {
                        if (chunk.length() % 2 != 0) {
                            throw new MidiError("Malformed dump request: should contain a list of hexa bytes like 00 or FE: " + rawString);
                        }

                        for (int i = 0; i < chunk.length(); i += 2) {
                            int b = Integer.parseInt(chunk.substring(i, i + 2), 16);
                            data.add(b);
                        }
                    }
                });
        SysExBuilder sb = new SysExBuilder(new DefaultChecksum());
        for (int i = 0; i < data.size(); i++) {
            if (i == checksum.getStartPosition()) {
                sb.beginChecksum();
            } else if (i == checksum.getPosition()) {
                sb.writeChecksum();
            }
            sb.write(data.get(i));
        }
        byte[] result = sb.buildBuffer();
        try {
            return new CustomMidiEvent(new SysexMessage(result, result.length), -1);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    /**
     * We allow nibbles (half byte) in the payload, so we glue them to make a byte
     *
     * @param rawString string containing nibbles
     * @return string without nibbles
     */
    private static String aggregateNibbles(String rawString) {
        for (; ; ) {
            var m = nibbles.matcher(rawString);
            if (m.find()) {
                String low = m.group("low");
                String high = m.group("high");
                rawString = rawString.substring(0, m.start()) + " " + high + low + " " + rawString.substring(m.end());
            } else {
                break;
            }
        }
        return rawString;
    }

    private static List<SysExRange> collectRanges(String payload) {
        List<SysExRange> result = new ArrayList<>();
        final String HEXA_OR_DECIMAL = "[0-9A-Fx$]+";
        var ranges = Pattern.compile("\\[(?<from>%s)-(?<to>%s)\\]".formatted(HEXA_OR_DECIMAL, HEXA_OR_DECIMAL));
        var matcher = ranges.matcher(payload);
        while (matcher.find()) {
            String fromStr = matcher.group("from");
            String toStr = matcher.group("to");
            int size = computeRangeSize(fromStr, toStr);
            int from = parseNumber(fromStr);
            int to = parseNumber(toStr);
            SysExRange r = new SysExRange(matcher.group(), size, matcher.start(), from, to);
            result.add(r);
        }
        return result;
    }

    private static int computeRangeSize(String fromStr, String toStr) {
        if (fromStr.startsWith("$")) {
            return fromStr.substring(1)
                    .length();
        } else if (fromStr.startsWith("0x")) {
            return fromStr.substring(2)
                    .length();
        } else {
            return 2;
        }

    }

    /**
     * Convenient method to parse numbers in various formats: 0xFF or $FF or 255
     *
     * @throws MidiError is the number is incorrect
     */
    public static int parseNumber(String number) {
        var m = DECIMAL_OR_HEX_NUMBER.matcher(number);
        if (m.find()) {
            String hexadecimal = m.group("hexadecimal");
            String decimal = m.group("decimal");
            if (hexadecimal != null) {
                return Integer.parseInt(hexadecimal, 16);
            } else if (decimal != null) {
                return Integer.parseInt(decimal, 10);
            }
        }
        throw new MidiError("Expected a number in the form $FF or 0xFF or 00 but got: " + number);
    }

    private static String resolveASCIIStrings(String payload) {
        var asciiString = Pattern.compile("'[^']+'");
        var matcher = asciiString.matcher(payload);
        Map<String, String> remplacement = new HashMap<>();
        while (matcher.find()) {
            String str = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            String hexaString = "";
            var chars = str.toCharArray();
            for (int idx = 1; idx < chars.length - 1; idx++) {
                String hex = "%02X".formatted((int) chars[idx]);
                hexaString += hex;
            }
            remplacement.put(str, hexaString);
        }
        for (Map.Entry<String, String> entry : remplacement.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            payload = payload.replace(k, v);
        }
        return payload;
    }
}
