package com.hypercube.workshop.synthripper.preset.sfz.model;

/**
 * In-memory representation of a SFZ preset.
 *
 * @param content the raw SFZ code
 */
public record SfzPreset(String content) {
    @Override
    public String toString() {
        return content;
    }
}
