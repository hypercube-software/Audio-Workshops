package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

/**
 * Indicate what to found and where in the SYSEX payload
 *
 * @param offset Expected format is $14 or 0x14
 * @param value  Expected format is a train of hexa bytes or a string 'LM  0065DR' (typically used in Yamaha devices)
 */
public record OverrideCondition(String offset, String value) {
}
