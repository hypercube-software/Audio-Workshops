package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.SimpleSumChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.*;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
        int size = reader.getInt24();
        int mode = reader.getByte();
        String name = reader.getString();
        StreamFormat format = StreamFormat.fromCode(reader.getByte());
        String objectName = KObject.fromType(objectType)
                .map(KObject::name)
                .orElse("Unknown !");
        log.info("Object: {}", objectName);
        log.info("Mode: {}", mode);
        log.info("Name: {}", name);
        log.info("Size  : %06X".formatted(size));
        log.info("Format: {}", format);
        byte[] payload = getBitStream(reader);
        byte[] unpacked = switch (format) {
            case STREAM -> readStream(payload);
            case NIBBLE -> readNibbleStream(payload);
        };
        int cheksum = reader.getByte();
        int f7 = reader.getByte();
        log.info("Unpacked size  : 0x%06X bytes".formatted(unpacked.length));

        return new ObjectWrite(objectType, objectId, size, mode, name, format, unpacked);
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
        int size = reader.getInt24();
        String objectName = KObject.fromType(objectType)
                .map(KObject::name)
                .orElse("Unknown !");
        log.info("Object: {}", objectName);
        log.info("Offset: %06X".formatted(offset));
        log.info("Size  : %06X".formatted(size));
        StreamFormat format = StreamFormat.fromCode(reader.getByte());
        log.info("Format: {}", format);
        byte[] payload = getBitStream(reader);
        byte[] unpacked = switch (format) {
            case STREAM -> readStream(payload);
            case NIBBLE -> readNibbleStream(payload);
        };
        log.info("Unpacked size  : 0x%06X bytes".formatted(unpacked.length));

        return new ObjectLoad(objectType, objectId, size, offset, format, unpacked);
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

    private byte[] getBitStream(SysExReader reader) {
        SysExChecksum sysExChecksum = new SimpleSumChecksum();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while (reader.remaining() != 2) {
                int v = reader.getByte();
                sysExChecksum.update(v);
                out.write(v);
            }
            byte[] payload = out.toByteArray();
            log.info("BitStream size : 0x%06X bytes".formatted(payload.length));
            return payload;
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
