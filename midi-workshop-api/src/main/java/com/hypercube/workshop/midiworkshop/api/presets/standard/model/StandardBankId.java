package com.hypercube.workshop.midiworkshop.api.presets.standard.model;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record StandardBankId(int msb, int lsb) {
    private static final Pattern PATTERN = Pattern.compile("^(?<msb>\\d+)-(?<lsb>\\d+)$");

    public static StandardBankId of(String definition) {
        Matcher m = PATTERN.matcher(definition);
        if (m.matches()) {
            return new StandardBankId(
                    Integer.parseInt(m.group("msb")),
                    Integer.parseInt(m.group("lsb")));
        }
        throw new MidiConfigError("Unexpected bank id definition: " + definition);
    }
}
