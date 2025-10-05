package com.hypercube.workshop.midiworkshop.api.sysex.util;

/**
 * This class is used to generate a bunch of Midi events for various values
 * <ul>
 *     <li>The so-called "range" is found in a message template (hexa string)</li>
 *     <li>{@link #from} and {@link #to} define the number of instances to generate from the template</li>
 *     <li>decimal values imply a nibble size of 2: "F043204C 08 [0-15] 00 F7"</li>
 *     <ul>
 *         <li>F043204C 08 00 00 F7</li>
 *         <li>F043204C 08 01 00 F7</li>
 *         <li>...</li>
 *         <li>F043204C 08 0F 00 F7</li>
 *     </ul>
 *     <li>hexadecimal values can express a precise size of nibbles: "F043204C 08 2 [$0-$F] 00 F7"</li>
 *     <ul>
 *         <li>F043204C 08 2 0 00 F7</li>
 *         <li>F043204C 08 2 1 00 F7</li>
 *         <li>...</li>
 *         <li>F043204C 08 2 F 00 F7</li>
 *     </ul>
 *     <li>same for "0x": "F043204C 08 2 [0x000-0x015] 00 F7"</li>
 * </ul>>
 *
 * @param value    original range value
 * @param size     number of hexadecimal digits to use (express the bit size of the value in nibble)
 * @param position where the original value appears in the template
 * @param from     range start
 * @param to       range end (included)
 */
public record SysExRange(String value, int size, int position, int from, int to) {
}
