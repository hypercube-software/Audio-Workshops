package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
public abstract class KFDeserializer {
    public static String toJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dump(Object obj) {
        try {
            String json = toJson(obj);
            String simpleName = obj.getClass()
                    .getSimpleName();
            if (obj instanceof KFObject kfObject) {
                simpleName += "_" + kfObject.getObjectId() + "_" + kfObject.getName();
            }
            File targetDir = new File("./target");
            File file = new File(targetDir, "kurzweil_dump_%s.json".formatted(simpleName));
            log.info("Generate {}", file.getPath());
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            Files.writeString(file.toPath(), json);
        } catch (IOException e) {
            log.error("Failed to serialize KurzweilFile to JSON", e);
        }
    }

    protected void writeName(String name, BitStreamWriter out) {
        // maxLen is the "offset to data" (includes the short itself)
        byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);

        // 1. Calculate the raw offset including the offset in itself and the null terminator (+ 1)
        int rawOffset = 2 + nameBytes.length + 1;

        // 2. Round up to the next multiple of 2
        // (rawLength + 1) & ~1 is a fast bitwise way to round up to the next even number
        int roundedLength = (rawOffset + 1) & ~1;

        // 3. Apply the upper bound limit of 18 (which is already a multiple of 2)
        int offsetToData = Math.min(18, roundedLength);
        out.writeShort(offsetToData);
        for (int i = 0; i < offsetToData - 2; i++) {
            if (i < nameBytes.length) {
                out.writeByte(nameBytes[i]);
            } else {
                out.writeByte(0);
            }
        }
    }

    protected String readMagic(ByteBuffer buffer) {
        return "%c%c%c%c".formatted(buffer.get(), buffer.get(), buffer.get(), buffer.get());
    }

    protected int readUnsignedShort(ByteBuffer byteBuffer) {
        return byteBuffer.getShort() & 0xFFFF;
    }

    protected long readUnsignedInt(ByteBuffer byteBuffer) {
        return byteBuffer.getInt() & 0xFFFFFFFFL;
    }

    protected String readName(BitStreamReader in) {
        // "offset to data" counts its own 2 bytes, so the name area (null terminator + optional pad included)
        // is offsetToData - 2 bytes long.
        int nameAreaLength = in.readShort() - 2;
        byte[] area = in.readBytes(nameAreaLength);

        // The name ends at the first null terminator. Anything after it, up to the end of the area, is the pad.
        int terminator = 0;
        while (terminator < area.length && area[terminator] != 0) {
            terminator++;
        }
        if (terminator == area.length) {
            throw new IllegalStateException("Object name is not null-terminated inside its %d byte area".formatted(area.length));
        }
        int padLength = area.length - (terminator + 1);
        String name = new String(area, 0, terminator, StandardCharsets.US_ASCII);

        if (padLength > 1) {
            log.debug("Object name {} area has {} bytes of padding after the terminator, expected at most 1", name, padLength);
        }
        return name;
    }
}
