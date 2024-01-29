package com.hypercube.workshop.audioworkshop.files.flac;

import com.hypercube.workshop.audioworkshop.files.exceptions.AudioParserException;
import com.hypercube.workshop.audioworkshop.files.flac.meta.FlacApplicationMetadata;
import com.hypercube.workshop.audioworkshop.files.flac.meta.FlacMetadata;
import com.hypercube.workshop.audioworkshop.files.io.PositionalReadWriteStream;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// FLAC is always in Big Endian
@Slf4j
@SuppressWarnings({"unused", "StatementWithEmptyBody"})
public class FlacReader {

    private final FlacAudioInfo info;
    private final File inputFile;

    public FlacReader(File inputFile) {
        this.info = new FlacAudioInfo();
        this.inputFile = inputFile;
    }

    private String readID(PositionalReadWriteStream stream) throws IOException {
        byte[] name = stream.readNBytes(4);
        return new String(name, StandardCharsets.US_ASCII);
    }

    private String readID(ByteBuffer buffer) {
        byte[] name = new byte[4];
        buffer.get(name);
        return new String(name, StandardCharsets.US_ASCII);
    }

    public FlacAudioInfo parse() {
        FlacMetadataConsumer noOPConsumer = m -> {
        };
        if (parse(noOPConsumer, null)) {
            return info;
        } else {
            return null;
        }
    }

    public boolean parse(FlacMetadataConsumer consumer, FlacFramesConsumer frameConsumer) {
        try (PositionalReadWriteStream stream = new PositionalReadWriteStream(inputFile, false)) {
            String id = readID(stream);
            if (!id.equals("fLaC"))
                throw new AudioParserException("Not a FLAC File");

            while (readBlock(stream, consumer)) {
                // do nothing
            }

            int remainBytes = stream.capacity() - stream.positionUInt();
            byte[] data = new byte[1024 * 1024 * 50];
            for (; ; ) {
                int nbRead = stream.read(data);
                if (nbRead == -1)
                    break;
                if (frameConsumer != null) {
                    frameConsumer.onFrameData(data, nbRead);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return false;
        }
    }

    private int getUnsignedByte(PositionalReadWriteStream stream) throws IOException {
        return stream.getByte() & 0xFF;
    }

    private int getUnsignedByte(ByteBuffer buffer) {
        return buffer.get() & 0xFF;
    }

    private int get24BitInt(PositionalReadWriteStream stream) throws IOException {
        return (getUnsignedByte(stream) << 16 | getUnsignedByte(stream) << 8 | getUnsignedByte(stream));
    }

    private int get24BitInt(ByteBuffer buffer) {
        return (getUnsignedByte(buffer) << 16 | getUnsignedByte(buffer) << 8 | getUnsignedByte(buffer));
    }

    private boolean readBlock(PositionalReadWriteStream stream, FlacMetadataConsumer consumer) throws Exception {
        int type = stream.getByte();
        boolean lastOne = (type & 0x80) != 0;
        FlacBlockType flacType = FlacBlockType.values()[type & 0x7F];
        int blockSize = get24BitInt(stream);
        if (blockSize == 0)
            throw new AudioParserException(String.format("Empty block size for %s at %X", flacType, stream.positionUInt()));
        byte[] block = stream.readNBytes(blockSize);
        log.trace("Block type {}", flacType.toString());

        if (flacType != FlacBlockType.APPLICATION) {
            consumer.accept(new FlacMetadata(flacType, block, lastOne));
        }

        if (flacType == FlacBlockType.STREAMINFO) {
            readStreamInfo(block);
        } else if (flacType == FlacBlockType.SEEKTABLE) {
            // skip
        } else if (flacType == FlacBlockType.APPLICATION) {
            readApplication(flacType, block, lastOne, consumer);
        } else if (flacType == FlacBlockType.PADDING) {
            // skip
        } else if (flacType == FlacBlockType.VORBIS_COMMENT) {
            readVorbisComment(block);
        } else if (flacType == FlacBlockType.PICTURE) {
            readID3Picture(block);
        }
        return !lastOne;
    }


    private void readApplication(FlacBlockType type, byte[] data, boolean lastOne, FlacMetadataConsumer consumer) {
        ByteBuffer block = ByteBuffer.wrap(data);
        block.order(ByteOrder.BIG_ENDIAN);
        String id = readID(block);
        byte[] content = new byte[data.length - 4];
        block.get(content);
        try {
            consumer.accept(new FlacApplicationMetadata(type, content, lastOne, id));
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    // https://xiph.org/flac/format.html#metadata_block_vorbis_comment
    private void readVorbisComment(byte[] data) {
        ByteBuffer block = ByteBuffer.wrap(data);
        block.order(ByteOrder.LITTLE_ENDIAN);
        int vendorLength = block.getInt();
        String vendor = readFixedString(block, vendorLength, StandardCharsets.US_ASCII);
        log.trace("Vendor: {}", vendor);
        int userCommentListLength = block.getInt();
        for (int i = 0; i < userCommentListLength; i++) {
            int l = block.getInt();
            String comment = readFixedString(block, l, StandardCharsets.UTF_8);
            info.setComment(comment);
        }
        // the following framing bit is not part of FLAC
    }

    private void readID3Picture(byte[] data) {
        ByteBuffer block = ByteBuffer.wrap(data);
        block.order(ByteOrder.BIG_ENDIAN);
        int type = block.getInt();
        int mimeLength = block.getInt();
        byte[] mimeBytes = new byte[mimeLength];
        block.get(mimeBytes);
        String mime = new String(mimeBytes, StandardCharsets.US_ASCII);
        int descLength = block.getInt();
        byte[] descBytes = new byte[descLength];
        block.get(descBytes);
        String desc = new String(descBytes, StandardCharsets.UTF_8);
        int width = block.getInt();
        int height = block.getInt();
        int bitDepth = block.getInt();
        int colorCount = block.getInt();
        int dataSize = block.getInt();
        byte[] picData = new byte[dataSize];
        block.get(picData);

        FlacPicture p = new FlacPicture(FlacPictureType.values()[type],
                mime,
                desc,
                width,
                height,
                picData
        );
        info.getPictures()
                .add(p);
    }

    private String readFixedString(ByteBuffer buffer, int size, Charset charset) {
        byte[] data = new byte[size];
        buffer.get(data);
        int l = data.length;
        for (int i = 0; i < l; i++) {
            if (data[i] == 0) {
                l = i;
                break;
            }
        }
        return new String(data, 0, l, charset);
    }

    private void readStreamInfo(byte[] data) {
        ByteBuffer block = ByteBuffer.wrap(data);
        block.order(ByteOrder.BIG_ENDIAN);
        int minimumBlockSize = block.getShort();
        int maximumBlockSize = block.getShort();
        int minimumFrameSize = get24BitInt(block);
        int maximumFrameSize = get24BitInt(block);
        log.trace("minimumBlockSize: %d".formatted(minimumBlockSize));
        log.trace("maximumBlockSize: %d".formatted(maximumBlockSize));
        log.trace("minimumFrameSize: %d".formatted(minimumFrameSize));
        log.trace("maximumFrameSize: %d".formatted(maximumFrameSize));
        long bitfield = block.getLong();
        long sampleRate = (bitfield >> (36 + 5 + 3)) & 0x7FFFFL; // 20 bits
        long numChannels = ((bitfield >> (36 + 5)) & 0x7L) + 1; // 3 bits
        long bitPerSample = ((bitfield >> (36)) & 0x1FL) + 1; // 5 bits
        long totalSamples = (bitfield) & 0xFFFFFFFFFL; // 36 bits
        byte[] md5 = new byte[16];
        block.get(md5);
        log.trace("FLAC %d bits %d channels %dHz %d/0x%X samples".formatted(bitPerSample, numChannels, sampleRate,
                totalSamples, totalSamples));

        float duration = totalSamples / (float) sampleRate;

        var seconds = (long) duration;
        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;
        String durationString = String.format("%02d:%02d:%02d", hh, mm, ss);
        info.setSampleRate((int) sampleRate);
        info.setNumChannels((int) numChannels);
        info.setDuration((int) duration);
        info.setDurationString(durationString);
        info.setBitDepth((int) bitPerSample);
        info.setMd5(md5);
    }
}
