package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hypercube.workshop.synthripper.config.MidiSettings.USE_DEFAULT_MIDI_CHANNEL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigMidiPreset implements IConfigMidiPreset {
    private static Pattern presetRegExp = Pattern.compile("(?<command>(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?)(\\s+CC\\((?<CCLIST>[^)]+)\\))?\\s+(?<title>.*)");
    private static Pattern drumkitNoteExp = Pattern.compile("(?<note>[0-9]+)\\s+(?<title>.+)");
    private String title;
    private int channel = USE_DEFAULT_MIDI_CHANNEL;
    private List<String> commands = List.of();
    private List<Integer> controlChanges = List.of(MidiPreset.NO_CC);
    private List<String> drumkitNotes = List.of();

    public static ConfigMidiPreset fromShortSpec(String shortSpec) {
        var matcher = presetRegExp.matcher(shortSpec);
        if (matcher.find()) {
            var ccListGroup = matcher.group("CCLIST");
            var ccList = Optional.ofNullable(ccListGroup)
                    .map(l -> Arrays.stream("%d,%s".formatted(MidiPreset.NO_CC, l)
                                    .split(","))
                            .map(Integer::parseInt)
                            .toList())
                    .orElse(List.of(MidiPreset.NO_CC));
            List<String> noDrumkit = List.of();
            return new ConfigMidiPreset(matcher.group("title"), -1, List.of(matcher.group("command")), ccList, noDrumkit);
        } else {
            throw new IllegalArgumentException("Unexpected format: " + shortSpec);
        }
    }

    @Override
    public MidiPreset forgeMidiPreset(MidiSettings midiSettings) {
        return MidiPreset.of(channel == USE_DEFAULT_MIDI_CHANNEL ? midiSettings.getChannel() : channel, midiSettings.getPresetFormat(), midiSettings.getPresetNumbering(), title, midiSettings.getCommands(), commands, controlChanges, drumkitNotes.stream()
                .map(this::forgeDrumKitNote)
                .toList());
    }

    private DrumKitNote forgeDrumKitNote(String spec) {
        Matcher m = drumkitNoteExp.matcher(spec);
        if (m.find()) {
            try {
                return new DrumKitNote(m.group("title"), Integer.parseInt(m.group("note")));
            } catch (NumberFormatException e) {
                throw new MidiError("Unexpected drumkit note definition:" + spec, e);
            }
        } else {
            throw new MidiError("Unexpected drumkit note definition:" + spec);
        }
    }
}
