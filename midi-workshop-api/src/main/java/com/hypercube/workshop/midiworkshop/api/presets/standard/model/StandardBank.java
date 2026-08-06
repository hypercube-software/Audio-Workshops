package com.hypercube.workshop.midiworkshop.api.presets.standard.model;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record StandardBank(StandardBankId bankId, String name) {
    private static final Pattern PATTERN = Pattern.compile("^(?<msb>\\d+)-(?<lsb>\\d+)(?:\\s+(?<name>.+))?$");

    public static StandardBank of(String definition) {
        Matcher m = PATTERN.matcher(definition);
        if (m.matches()) {
            StandardBankId bankId = StandardBankId.of(m.group("msb") + "-" + m.group("lsb"));
            String name = Optional.ofNullable(m.group("name"))
                    .orElse("");
            return new StandardBank(bankId, name);
        }
        throw new MidiConfigError("Unexpected bank definition: " + definition);
    }
}
