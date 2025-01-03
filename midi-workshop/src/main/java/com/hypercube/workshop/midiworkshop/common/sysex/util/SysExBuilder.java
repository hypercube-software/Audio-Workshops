package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to construct a SysEx ith fluent API style
 */
@RequiredArgsConstructor
public class SysExBuilder {
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

    public static List<CustomMidiEvent> parse(String definition) throws InvalidMidiDataException {
        List<CustomMidiEvent> result = new ArrayList<>();
        String rawString = resolveASCIIStrings(definition);
        List<SysExRange> ranges = collectRanges(rawString);
        if (ranges.size() > 1) {
            throw new MidiError("SysEx template does not support more than one range for now");
        }
        if (ranges.size() > 0) {
            for (SysExRange range : ranges) {
                for (int i = range.from(); i <= range.to(); i++) {
                    String currentString = rawString.replace(range.value(), "%02X".formatted(i));
                    result.add(forgeMidiEvent(currentString));
                }
            }
            return result;
        } else {
            return List.of(forgeMidiEvent(rawString));
        }
    }

    private static CustomMidiEvent forgeMidiEvent(String rawString) throws InvalidMidiDataException {
        CheckSumDef checksum = new CheckSumDef();
        List<Integer> data = new ArrayList<>();
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
        return new CustomMidiEvent(new SysexMessage(result, result.length), -1);
    }

    private static List<SysExRange> collectRanges(String payload) {
        List<SysExRange> result = new ArrayList<>();
        var ranges = Pattern.compile("\\[(?<from>[0-9]+)-(?<to>[0-9]+)\\]");
        var matcher = ranges.matcher(payload);
        while (matcher.find()) {
            int from = getFrom(matcher);
            int to = Integer.parseInt(matcher.group("to"));
            SysExRange r = new SysExRange(matcher.group(), matcher.start(), from, to);
            result.add(r);
        }
        return result;
    }

    private static int getFrom(Matcher matcher) {
        return Integer.parseInt(matcher.group("from"));
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
