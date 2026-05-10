package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.HexFormat;

/**
 * Custom Jackson serializer to convert a byte array into a hexadecimal string.
 */
public class HexSerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String hexString = HexFormat.ofDelimiter(" ")
                .formatHex(value);

        gen.writeString(hexString);
    }
}
