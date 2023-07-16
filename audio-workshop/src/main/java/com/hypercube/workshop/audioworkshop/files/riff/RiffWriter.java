package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.io.SeekableBinaryOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class RiffWriter implements Closeable {
    private final File file;

    private SeekableBinaryOutputStream out;

    public RiffWriter(File file) throws IOException {
        this.file = file;
        open();
    }

    private void open() throws IOException {
        out = new SeekableBinaryOutputStream(new FileOutputStream(file));
        out.writeChunkId("RIFF");
        out.writeIntLE(0);
        out.writeChunkId("WAVE");
    }

    public void writeChunk(RiffChunk chunk, byte[] data) throws IOException {
        if (out.position() % 2 != 0)
            out.writeByte(0);
        out.writeChunkId(chunk.getId());
        out.writeIntLE(chunk.getContentSize());
        out.writeBytes(data);
    }

    public void setSize() throws IOException {
        long pos = out.position();
        out.seek(4);
        out.writeIntLE((int) (pos - 8));
        out.seek(pos);
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private void align() throws IOException {
        if (out.position() % 2 != 0) {
            out.writeByte(0);
        }
    }

    public void writeIXML(String xml) throws IOException {
        align();
        out.writeChunkId("iXML");
        byte[] data = xml.getBytes(StandardCharsets.US_ASCII);
        out.writeIntLE(data.length);
        out.writeBytes(data);
    }

    /**
     * Write a LIST CHUNK width multiple sub chunks INFO
     *
     * @param infos a list of key1,value1,key2,value2,key3,value3...
     * @throws IOException
     */
    public void writeLISTInfo(List<String> infos) throws IOException {
        align();
        long pos = out.position();
        out.writeChunkId("LIST");
        out.writeIntLE(0);
        out.writeChunkId("INFO");
        for (int i = 0; i < infos.size(); i += 2) {
            String field = infos.get(i + 0);
            String value = infos.get(i + 1);
            align();
            out.writeChunkId(field);
            byte[] str = value.getBytes(StandardCharsets.US_ASCII);
            out.writeIntLE(str.length);
            out.writeBytes(str);
        }
        long end = out.position();
        long size = end - pos - 8;
        out.seek(pos + 4);
        out.writeIntLE((int) size);
        out.seek(end);
    }
}
