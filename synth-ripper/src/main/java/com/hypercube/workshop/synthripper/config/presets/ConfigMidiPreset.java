package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.yaml.IConfigMidiPreset;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
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
    /**
     * Name of the preset
     */
    private String title;
    /**
     * Midi channel in the range [1-16] not [0-15] or -1 to use the default ones
     */
    private int channel = USE_DEFAULT_MIDI_CHANNEL;
    /**
     * Commands to select the preset
     */
    private List<String> commands = List.of();
    /**
     * Control changes to apply to the preset
     */
    private List<Integer> controlChanges = List.of(MidiPreset.NO_CC);
    /**
     * If the preset is a drum kit, gives list of note number and names
     */
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
    public MidiPreset forgeMidiPreset(File configFile, MidiSettings midiSettings) {
        final int midiChannel = (channel == USE_DEFAULT_MIDI_CHANNEL) ? midiSettings.getZeroBasedChannel() : getZeroBasedChannel();
        final List<DrumKitNote> drumKitNotes = drumkitNotes.stream()
                .map(this::forgeDrumKitNote)
                .toList();
        final List<CommandMacro> macros = midiSettings.getCommands();
        return MidiPresetBuilder.parse(configFile, midiChannel, midiSettings.getPresetFormat(), title, macros, commands, controlChanges, drumKitNotes);
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

    public int getZeroBasedChannel() {
        return channel - 1;
    }
}
