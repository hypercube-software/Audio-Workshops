package com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives;

/**
 * Represent the dimension of an array
 *
 * @param size how many entries are possible
 * @param name optional name for the dimension, it can be null
 */
public record MemoryArrayIndex(int size, String name) {
}
