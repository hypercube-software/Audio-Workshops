package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
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
    public static void dump(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = mapper.writeValueAsString(obj);
            String simpleName = obj.getClass()
                    .getSimpleName();
            if (obj instanceof KFProgram prg) {
                simpleName += "_" + prg.getName();
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
        // see official readme.txt where they talk about "offset to data"
        // offset to jump over name include its own size
        int ofs = in.readShort() - 2;
        int beforeName = in.getBytePos();
        byte[] chars = in.readBytes(ofs);
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < ofs; i++) {
            int ch = chars[i];
            if (ch != 0) {
                name.append((char) ch);
            } else {
                break;
            }
        }
        int afterName = in.getBytePos();
        assert (ofs == afterName - beforeName);
        return name.toString();
    }
}
