package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KurzweilFile;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class KurzweilFileReader implements Closeable {
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

    public KurzweilFile read() {
        KFHeader header = KFHeaderDeserializer.deserialize(readRawData(32));
        List<KFObject> objects = new ArrayList<>();
        for (; ; ) {
            int rawBlockSize = readInt32();
            if (rawBlockSize >= 0) {
                break;
            }
            int blockSize = Math.abs(rawBlockSize) - 4; // include the size in itself
            RawData data = readRawData(blockSize);
            objects.add(KFObjectDeserializer.deserialize(data));
        }
        return new KurzweilFile(file, header, objects);
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
