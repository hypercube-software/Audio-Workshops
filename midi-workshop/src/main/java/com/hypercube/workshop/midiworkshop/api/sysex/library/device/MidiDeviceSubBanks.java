package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.IntStream;

@Setter
@Getter
public class MidiDeviceSubBanks {
    private String fromMode;
    private String midiChannels;
    private MidiDeviceMode mode;

    public List<Integer> getChannels() {
        String[] v = midiChannels.split("-");
        int start = Integer.parseInt(v[0]);
        int end = v.length == 2 ? Integer.parseInt(v[1]) : start;
        return IntStream.rangeClosed(start, end)
                .boxed()
                .toList();
    }
}
