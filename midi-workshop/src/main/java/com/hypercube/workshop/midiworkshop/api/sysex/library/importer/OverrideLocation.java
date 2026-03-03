package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseFieldType;

/**
 * Where and how to modify a section of SysEx payload
 *
 * @param type   indicate what kind of data need to be changed
 * @param offset indicate where to change, in hexadecimal
 * @param size   optional size of the destination (if it is different from the value)
 * @param value  indicate what to inject, it can be various things like: hexadecimal value, string value, checksum...
 */
public record OverrideLocation(MidiResponseFieldType type, String offset, String size, String value) {
}
