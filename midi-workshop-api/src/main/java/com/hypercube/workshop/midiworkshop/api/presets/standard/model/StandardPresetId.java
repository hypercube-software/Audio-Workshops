package com.hypercube.workshop.midiworkshop.api.presets.standard.model;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record StandardPresetId(StandardBankId bankId, int prg) {
    private static final Pattern PATTERN = Pattern.compile("^(?<msb>\\d+)-(?<lsb>\\d+)-(?<prg>\\d+)$");

    public static StandardPresetId of(String definition) {
        Matcher m = PATTERN.matcher(definition);
        if (m.matches()) {
            return new StandardPresetId(
                    StandardBankId.of(m.group("msb") + "-" + m.group("lsb")),
                    Integer.parseInt(m.group("prg")));
        }
        throw new MidiConfigError("Unexpected preset id definition: " + definition);
    }

    public boolean matches(MidiPreset midiPreset) {
        return midiPreset.getBankMSB() == bankId.msb()
                && midiPreset.getBankLSB() == bankId.lsb()
                && midiPreset.getBankPrg() == prg;
    }
}
