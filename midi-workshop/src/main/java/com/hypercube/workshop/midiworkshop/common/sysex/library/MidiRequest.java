package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A {@link MidiRequest} is just a string of hexadecimal bytes
 * <p>Note: ranges and strings are not resolved at this point, so {@link #value} can be something like "F0 43 20 7A 'LM  0065PF' 0000000000000000000000000000 00 [0-63]    F7"</p>
 * <p>{@link SysExBuilder#parse(String)} will completely resolve the value and produce real Midi events</p>
 */
@RequiredArgsConstructor
@Getter
public class MidiRequest {
    /**
     * Name of the request (usefull to forge user-friendly messages)
     */
    private final String name;
    /**
     * String template containing the payload of the request
     */
    private final String value;
    /**
     * Expected response size of the request. Can be null
     */
    private final Integer size;
}
