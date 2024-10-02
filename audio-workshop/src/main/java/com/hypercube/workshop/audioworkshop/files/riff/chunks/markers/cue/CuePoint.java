package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue;

/**
 * Note: The chunkStart and blockStart fields are set to 0 for an uncompressed WAVE
 *
 * @param identifier   Identifier used in PLST chunk, start for 1, not 0
 * @param position     The position field specifies the position of the cue point within the PLST chunk
 * @param chunkId
 * @param chunkStart
 * @param blockStart
 * @param sampleOffset
 */
public record CuePoint(
        int identifier,
        int position,
        String chunkId,
        int chunkStart,
        int blockStart,
        int sampleOffset) {
}
