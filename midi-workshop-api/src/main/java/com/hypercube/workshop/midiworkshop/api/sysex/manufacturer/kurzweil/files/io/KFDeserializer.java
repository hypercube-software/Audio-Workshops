package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

@Slf4j
public abstract class KFDeserializer {
    public static void dump(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            //mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = mapper.writeValueAsString(obj);
            String simpleName = obj.getClass()
                    .getSimpleName();
            log.info("Kurzweil object {} JSON: {}", simpleName, json);
            File targetDir = new File("./target");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            Files.writeString(new File(targetDir, "kurzweil_dump_%s.json".formatted(simpleName)).toPath(), json);
        } catch (IOException e) {
            log.error("Failed to serialize KurzweilFile to JSON", e);
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
