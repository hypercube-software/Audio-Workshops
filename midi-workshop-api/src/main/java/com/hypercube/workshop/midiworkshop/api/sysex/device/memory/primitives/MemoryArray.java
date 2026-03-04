package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Describe the definition of an array
 *
 * <ul>
 * <li>An array can have multiple dimensions (indexes)
 * <li>Each dimension can have a name
 * </ul>
 * <p>Examples: "[64]", "[5 Function]", "[5 Part,4 Section]"
 */
public record MemoryArray(List<MemoryArrayIndex> indexes) {
    /**
     * @return the total count of entries given the dimensions of the array
     */
    public int size() {
        int total = 1;
        for (MemoryArrayIndex index : indexes) {
            total = total * index.size();
        }
        return total;
    }

    /**
     * Example: "Part[0]","Part[1]"... or "Part[0]/Section[0]","Part[0]/Section[1]"...
     *
     * @return return all the display names of array entries
     */
    public List<String> getAllEntries() {
        return getAllEntries(0);
    }

    private List<String> getAllEntries(int idx) {
        if (idx == indexes.size())
            return List.of();
        MemoryArrayIndex index = indexes.get(idx);
        return IntStream.range(0, index.size())
                .boxed()
                .flatMap(i -> {
                    var txt = index.name() + "[" + i + "]";
                    var nextEntries = getAllEntries(idx + 1);
                    if (nextEntries.isEmpty()) {
                        return Stream.of(txt);
                    } else {
                        return nextEntries.stream()
                                .map(e -> txt + "/" + e);
                    }
                })
                .toList();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (MemoryArrayIndex i : indexes) {
            if (sb.isEmpty()) {
                sb.append("[");
            } else {
                sb.append(",");

            }
            sb.append(i.size());
            if (!i.name()
                    .isEmpty()) {
                sb.append(" ")
                        .append(i.name());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
