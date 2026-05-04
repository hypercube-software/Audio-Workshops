package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.util.Arrays;
import java.util.Optional;

@JsonIgnoreType
public record ProgramSegmentIdentifier(int rawValue, ProgramSegmentType type, int instanceId) {
    public static Optional<ProgramSegmentIdentifier> fromTag(int rawTag) {
        var tags = Arrays.stream(ProgramSegmentType.values())
                .sorted((o1, o2) -> Integer.compare(o1.getTag(), o2.getTag()))
                .toList();

        for (int i = 0; i < tags.size() - 1; i++) {
            ProgramSegmentType current = tags.get(i);
            ProgramSegmentType next = tags.get(i + 1);
            int base = current.getTag();
            int high = next.getTag() - 1;
            if (rawTag >= base && rawTag <= high) {
                int instanceId = rawTag - base;
                return Optional.of(new ProgramSegmentIdentifier(rawTag, current, instanceId));
            }
        }
        return Optional.empty();
    }
}
