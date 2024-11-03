package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.regex.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigMidiPreset implements IConfigMidiPreset {
    private static Pattern presetRegExp = Pattern.compile("(?<command>(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(-(?<id3>[0-9]+))?)(\\s+(?<title>.*))?");

    private String title;
    private List<String> commands;

    public static ConfigMidiPreset fromShortSpec(String shortSpec) {
        var matcher = presetRegExp.matcher(shortSpec);
        if (matcher.find()) {
            return new ConfigMidiPreset(matcher.group("title"), List.of(matcher.group("command")));
        } else {
            throw new IllegalArgumentException("Unexpected format: " + shortSpec);
        }
    }

    @Override
    public MidiPreset forgeMidiPreset(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering) {
        return MidiPreset.of(midiBankFormat, presetNumbering, title, commands);
    }
}
