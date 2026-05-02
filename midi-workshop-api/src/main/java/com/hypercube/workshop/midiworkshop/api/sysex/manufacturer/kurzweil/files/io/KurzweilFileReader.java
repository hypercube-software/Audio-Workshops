package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KurzweilFile;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample.KFSoundBlock;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample.KFSoundBlockHeader;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KurzweilFileReader implements Closeable {
    private final KFHeaderDeserializer kfHeaderDeserializer = new KFHeaderDeserializer();
    private final RandomAccessFile stream;
    private final FileChannel channel;
    private final File file;

    public KurzweilFileReader(File file) {
        try {
            this.stream = new RandomAccessFile(file, "r");
            this.channel = stream.getChannel();
            this.file = file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void dumpKurzweilFile(KurzweilFile kurzweilFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = mapper.writeValueAsString(kurzweilFile);
            log.info("KurzweilFile JSON: {}", json);
            File targetDir = new File("target");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            mapper.writeValue(new File(targetDir, "kurzweil_dump.json"), kurzweilFile);
        } catch (IOException e) {
            log.error("Failed to serialize KurzweilFile to JSON", e);
        }
    }

    public KurzweilFile read() {
        KFHeader header = kfHeaderDeserializer.deserialize(readRawData(32));
        List<KFObject> objects = new ArrayList<>();
        KFObjectDeserializer objectDeserializer = new KFObjectDeserializer();
        // Simple files are made of 1 block containing multiple objects
        for (; ; ) {
            int rawBlockSize = readInt32();
            if (rawBlockSize >= 0) {
                break;
            }
            int blockSize = Math.abs(rawBlockSize) - 4; // size includes itself
            RawData data = readRawData(blockSize);
            objects.addAll(objectDeserializer.deserializeObjects(data));
        }

        try {
            List<KFSoundBlock> soundBlocks = objects.stream()
                    .filter(s -> s.getType() == KObject.SOUND_BLOCK)
                    .map(s -> (KFSoundBlock) s)
                    .toList();
            if (!soundBlocks.isEmpty()) {
                try {
                    log.info("Read samples at {} OffsetSampleData={}",
                            Long.toHexString(channel.position()),
                            Long.toHexString(header.getOffsetSampleData()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            for (KFSoundBlock block : soundBlocks) {
                int sampleID = 0;
                for (KFSoundBlockHeader h : block.getHeaders()) {
                    long pos = header.getOffsetSampleData() + h.sampleStart() * 2;
                    log.info("Read sample block '{}' of {} samples at pos {}...", block.getName(), h.sampleLength(), Long.toHexString(pos));
                    byte[] sampleData = new byte[(int) h.sampleLength() * 2];
                    stream.seek(pos);
                    long remainingBytes = stream.length() - stream.getFilePointer();
                    if (remainingBytes >= sampleData.length) {
                        stream.readFully(sampleData);
                        Files.write(Path.of("./target/sample_%dKhz_16Bits_BE_%s_%d.pcm".formatted(h.sampleFrequency(), block.getName(), sampleID)), sampleData);
                    } else {
                        log.error("ERROR, this file does not contains the sample ! (EOF)");
                    }
                    sampleID++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        KurzweilFile kurzweilFile = new KurzweilFile(file, header, objects);

        dumpKurzweilFile(kurzweilFile);

        return kurzweilFile;
    }

    public RawData readRawData(int size) {
        long pos = positionLong();
        byte[] content = readBytes(size);
        return new RawData(content, pos);
    }

    public byte[] readBytes(int size) {
        byte[] data = new byte[size];
        try {
            stream.readFully(data);
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int readInt32() {
        try {
            return stream.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long positionLong() {
        try {
            return channel.position();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int capacity() {
        try {
            return (int) channel.size();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
