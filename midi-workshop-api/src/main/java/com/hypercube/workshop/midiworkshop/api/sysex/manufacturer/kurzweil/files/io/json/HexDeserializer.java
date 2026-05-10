package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HexFormat;

/**
 * Custom Jackson deserializer to convert a hexadecimal string back into a byte array.
 * This helper also cleans up any spacing characters within the hex string.
 */
public class HexDeserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String hexString = p.getText();

        if (hexString == null || hexString.isBlank()) {
            return new byte[0];
        }

        // Remove all whitespace characters (e.g., "04 01 00" -> "040100")
        String cleanHex = hexString.replace(" ", "");

        try {
            return HexFormat.of()
                    .parseHex(cleanHex);
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to parse spaced hexadecimal string: " + hexString, e);
        }
    }
}
