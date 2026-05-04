package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.SimpleSumChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment.KFProgramSegmentDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.*;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import com.hypercube.workshop.midiworkshop.api.sysex.util.LCDScreenDump;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class KurzweilSysExParser extends ManufacturerSysExParser {

    public static final int SCREEN_TEXT_SIZE = 320 + 1; // 320 chars + final 0
    public static final int PIXELS_PER_BYTE = 6;

    private static ObjectScreenBitmap parseLCDPixels(SysExReader reader) {
        int lcd_width = 240;
        int lcd_height = 64;
        boolean[] pixelData = new boolean[lcd_width * lcd_height];

        for (int y = 0; y < lcd_height; y++) {
            int nbBlocks = lcd_width / PIXELS_PER_BYTE;
            for (int pixelBlock = 0; pixelBlock < nbBlocks; pixelBlock++) {
                int v = reader.getByte();
                for (int bit = PIXELS_PER_BYTE - 1, pixel = 0; bit >= 0; bit--, pixel++) {
                    int mask = 1 << bit;
                    boolean pixelValue = (v & mask) != 0;
                    pixelData[(pixelBlock * PIXELS_PER_BYTE) + pixel + (y * lcd_width)] = pixelValue;
                }
            }
        }

        BufferedImage image = LCDScreenDump.create(lcd_width, lcd_height, pixelData);
        try {
            ImageIO.write(image, "png", new File("target/lcd-screen.png"));
        } catch (IOException e) {
            throw new MidiError(e);
        }
        return new ObjectScreenBitmap(0, 0, pixelData.length, image);
    }

    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }

    public List<BaseObject> parse(Manufacturer manufacturer, byte[] responses) {
        return SysExReader.splitSysEx(responses)
                .stream()
                .map(response ->
                {
                    SysExReader reader = new SysExReader(ByteBuffer.wrap(response));
                    int f0 = reader.getByte();
                    int manufacturerId = reader.getByte();
                    if (manufacturerId != manufacturer.getCode()) {
                        throw new MidiError("Expected manufacturer code %d, got %d".formatted(manufacturer.getCode(), manufacturerId));
                    }
                    int deviceId = reader.getByte();
                    int productId = reader.getByte();
                    Product product = Product.fromCode(productId)
                            .orElse(null);
                    if (product == null) {
                        throw new MidiError("Unknown product code %02X".formatted(productId));
                    }
                    int commandId = reader.getByte();
                    Command command = Command.fromCode(commandId)
                            .orElse(null);
                    if (command == null) {
                        throw new MidiError("Unknown command code %02X".formatted(commandId));
                    }
                    log.info("Parsing Command %s(%02X) for product %s(%02X)".formatted(command.name(), commandId, product.name(), productId));
                    return switch (command) {
                        case LOAD -> parseLOAD(command, reader);
                        case SCREEN_REPLY -> parseScreenReply(command, reader);
                        case INFO -> parseINFO(command, reader);
                        case WRITE -> parseWRITE(command, reader);
                        default -> null;
                    };
                })
                .toList();
    }

    public List<String> splitBySize(String text, int size) {
        return Arrays.stream(text.split("(?<=\\G.{" + size + "})"))
                .toList();
    }

    private BaseObject parseWRITE(Command command, SysExReader reader) {
        int objectType = reader.getInt16();
        int objectId = reader.getInt16();
        int unpackedSize = reader.getInt24();
        int mode = reader.getByte();
        String name = reader.getString();
        int formatCode = reader.getByte();
        StreamFormat format = StreamFormat.fromCode(formatCode);
        String objectTypeName = KObject.fromType(objectType)
                .map(KObject::name)
                .orElse("Unknown !");
        log.info("Object: {}", objectTypeName);
        log.info("Mode: {}", mode);
        log.info("Name: {}", name);
        log.info("Unpacked Size  : {}", Long.toHexString(unpackedSize));
        log.info("Format: {}", format);
        byte[] payload = getBitStream(reader, format, unpackedSize);
        int checksum = reader.getByte();
        int f7 = reader.getByte();

        byte[] unpacked = getUnpacked(command, format, payload, checksum, objectTypeName);
        KFProgram program = new KFProgram(new RawData(unpacked, 0), objectId, name, 0);
        KFProgramSegmentDeserializer kfProgramSegmentDeserializer = new KFProgramSegmentDeserializer(program);
        kfProgramSegmentDeserializer.deserialize("", program);
        KFDeserializer.dump(program);
        return new ObjectWrite(objectType, objectId, unpackedSize, mode, name, format, unpacked);
    }

    @Nonnull
    private byte[] getUnpacked(Command command, StreamFormat format, byte[] payload, int checksum, String objectName) {
        byte[] unpacked = switch (format) {
            case STREAM -> readStream(payload);
            case NIBBLE -> readNibbleStream(payload);
        };
        SimpleSumChecksum chk = new SimpleSumChecksum();
        for (byte value : payload) {
            chk.update(value);
        }
        int expectedChecksum = chk.getValue();
        boolean checkSumOK = checksum == expectedChecksum;
        log.info("Unpacked size  : 0x%06X bytes , checksum OK ? = {}".formatted(unpacked.length), checkSumOK);
        try {
            Files.write(Path.of("Packed " + command.name() + " prg " + objectName + ".syx"), payload);
            Files.write(Path.of("Unpacked " + command.name() + " prg " + objectName + ".dat"), unpacked);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return unpacked;
    }

    private ObjectInfo parseINFO(Command command, SysExReader reader) {
        int objectType = reader.getInt16();
        int objectId = reader.getInt16();
        int size = reader.getInt24();
        boolean inRam = reader.getByte() == 1;
        String name = reader.getString();
        log.info("Object Type %04X Object ID %04X Size: %X InRAM: %s : %s".formatted(objectType, objectId, size, inRam, name));
        return new ObjectInfo(objectType, objectId, size, inRam, name);
    }

    private BaseObject parseScreenReply(Command command, SysExReader reader) {
        int remaining = reader.remaining();
        if (remaining == SCREEN_TEXT_SIZE + 1) {
            return parseLCDText(reader);
        } else if (remaining == 2561 + 1) {
            return parseLCDPixels(reader);
        }
        return null;
    }

    private ObjectScreenText parseLCDText(SysExReader reader) {
        byte[] ascii = reader.getBytes(SCREEN_TEXT_SIZE - 1);
        String text = new String(ascii, StandardCharsets.US_ASCII);
        StringBuffer sb = new StringBuffer();
        splitBySize(text, 40).forEach(l ->
                sb.append(l + "\n"));
        String allText = sb.toString();
        log.info("\n" + allText);
        int f7 = reader.getByte();
        return new ObjectScreenText(0, 0, allText.length(), allText);
    }

    private ObjectLoad parseLOAD(Command command, SysExReader reader) {
        int objectType = reader.getInt16();
        int objectId = reader.getInt16();
        int offset = reader.getInt24();
        int unpackedSize = reader.getInt24();
        String objectTypeName = KObject.fromType(objectType)
                .map(KObject::name)
                .orElse("Unknown !");
        log.info("Object: {}", objectTypeName);
        log.info("Offset: %06X".formatted(offset));
        log.info("Unpacked Size  : %06X".formatted(unpackedSize));
        StreamFormat format = StreamFormat.fromCode(reader.getByte());
        log.info("Format: {}", format);
        byte[] payload = getBitStream(reader, format, unpackedSize);
        int checksum = reader.getByte();
        int f7 = reader.getByte();
        byte[] unpacked = getUnpacked(command, format, payload, checksum, objectTypeName);
        return new ObjectLoad(objectType, objectId, unpackedSize, offset, format, unpacked);
    }

    private byte[] readNibbleStream(byte[] payload) {
        BitStreamWriter out = new BitStreamWriter();
        BitStreamReader in = new BitStreamReader(payload);
        int outputSizeBits = (payload.length * 8) - (payload.length * 4);
        while (out.getBitPos() != outputSizeBits) {
            int skip = in.readBits(4);
            int keep = in.readBits(4);
            out.writeBits(keep, 4);
        }
        return out.toByteArray();
    }

    private byte[] readStream(byte[] payload) {
        BitStreamWriter out = new BitStreamWriter();
        BitStreamReader in = new BitStreamReader(payload);
        int outputSizeBits = (payload.length * 8) - (payload.length * 1);
        while (out.getBitPos() != outputSizeBits) {
            int skip = in.readBit();
            int keep = in.readBits(7);
            out.writeBits(keep, 7);
        }
        return out.toByteArray();
    }

    private byte[] getBitStream(SysExReader reader, StreamFormat format, int unpackedSize) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int streamSize = reader.remaining() - 2; // remove checksum and final F7
            int theoreticalStreamSize = switch (format) {
                case StreamFormat.NIBBLE -> unpackedSize * 2;
                case STREAM -> (int) Math.ceil((unpackedSize * 8) / 7f);
            };
            if (theoreticalStreamSize != streamSize) {
                log.error("This stream seems truncated");
            }
            for (int i = 0; i < streamSize; i++) {
                out.write(reader.getByte());
            }
            byte[] payload = out.toByteArray();
            log.info("BitStream size : 0x{} bytes", Integer.toHexString(payload.length));
            return payload;
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
