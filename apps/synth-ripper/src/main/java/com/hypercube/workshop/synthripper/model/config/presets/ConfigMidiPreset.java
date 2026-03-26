package com.hypercube.workshop.synthripper.model.config.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.synthripper.model.config.MidiSettings;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.model.config.yaml.IConfigMidiPreset;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hypercube.workshop.synthripper.model.config.MidiSettings.USE_DEFAULT_MIDI_CHANNEL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigMidiPreset implements IConfigMidiPreset {
    private static final Pattern PRESET_REGEXP = Pattern.compile("(?<command>(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?)(\\s+CC\\((?<CCLIST>[^)]+)\\))?\\s+(?<title>.*)");
    private static final Pattern DRUMKIT_NOTE_REGEXP = Pattern.compile("(?<note>[0-9]+)\\s+(?<title>.+)");
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
        var matcher = PRESET_REGEXP.matcher(shortSpec);
        if (matcher.find()) {
            var ccListGroup = matcher.group("CCLIST");
            var ccList = Optional.ofNullable(ccListGroup)
                    .map(l -> Arrays.stream("%d,%s".formatted(MidiPreset.NO_CC, l)
                                    .split(","))
                            .map(Integer::parseInt)
                            .toList())
                    .orElse(List.of(MidiPreset.NO_CC));
            List<String> noDrumkit = List.of();
            String command = prepareCommand(matcher.group("command"));
            return new ConfigMidiPreset(matcher.group("title"), -1, List.of(command), ccList, noDrumkit);
        } else {
            throw new IllegalArgumentException("Unexpected format: " + shortSpec);
        }
    }

    /**
     * MidiPresetBuilder no longer support decimal numbers unless the format is "a-b" or "a-b-c"
     * <p>This method convert decimal to hexadecimal patch numbers
     */
    private static String prepareCommand(String command) {
        if (command.contains("-")) {
            return command;
        } else {
            int value = Integer.parseInt(command, 10);
            return Integer.toHexString(value);
        }
    }

    @Override
    public MidiPreset forgeMidiPreset(SynthRipperConfiguration config) {
        final MidiSettings midiSettings = config.getMidi();
        final int midiChannel = (channel == USE_DEFAULT_MIDI_CHANNEL) ? midiSettings.getZeroBasedChannel() : getZeroBasedChannel();
        final List<DrumKitNote> drumKitNotes = drumkitNotes.stream()
                .map(this::forgeDrumKitNote)
                .toList();
        final List<CommandMacro> macros = midiSettings.getCommands();
        return MidiPresetBuilder.parse(config.getDevice(), midiChannel, title, macros, commands, controlChanges, drumKitNotes);
    }

    public int getZeroBasedChannel() {
        return channel - 1;
    }

    private DrumKitNote forgeDrumKitNote(String spec) {
        Matcher m = DRUMKIT_NOTE_REGEXP.matcher(spec);
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
