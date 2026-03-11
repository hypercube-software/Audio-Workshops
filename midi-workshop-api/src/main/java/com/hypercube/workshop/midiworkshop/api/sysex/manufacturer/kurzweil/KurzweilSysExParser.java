package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.SimpleSumChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class KurzweilSysExParser extends ManufacturerSysExParser {

    public static final int SCREEN_TEXT_SIZE = 320 + 1; // 320 chars + final 0
    public static final int PIXELS_PER_BYTE = 6;

    private static void parseLCDPixels(SysExReader reader) {
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
    }

    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }

    public void parse(Manufacturer manufacturer, byte[] response) {
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
        switch (command) {
            case LOAD -> parseLOAD(command, reader);
            case SCREEN_REPLY -> parseScreenReply(command, reader);
        }
    }

    public List<String> splitBySize(String text, int size) {
        return Arrays.stream(text.split("(?<=\\G.{" + size + "})"))
                .toList();
    }

    private void parseScreenReply(Command command, SysExReader reader) {
        int remaining = reader.remaining();
        if (remaining == SCREEN_TEXT_SIZE + 1) {
            parseLCDText(reader);
            int f7 = reader.getByte();
        } else if (remaining == 2561 + 1) {
            parseLCDPixels(reader);
        }
    }

    private void parseLCDText(SysExReader reader) {
        byte[] ascii = reader.getBytes(SCREEN_TEXT_SIZE - 1);
        String text = new String(ascii, StandardCharsets.US_ASCII);
        StringBuffer sb = new StringBuffer();
        splitBySize(text, 40).forEach(l ->
                sb.append(l + "\n"));
        String allText = sb.toString();
        log.info("\n" + allText);
    }

    private void parseLOAD(Command command, SysExReader reader) {
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
        Format format = Format.fromCode(reader.getByte());
        log.info("Format: {}", format);
        byte[] payload = getBitStream(reader);
        byte[] unpacked = switch (format) {
            case STREAM -> readStream(payload);
            case NIBBLE -> readNibbleStream(payload);
        };
        log.info("Unpacked size  : 0x%06X bytes".formatted(unpacked.length));
        try {
            Files.write(Path.of("target/output %s %s.dat".formatted(command, format)), unpacked);
        } catch (IOException e) {
            throw new MidiError(e);
        }
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
            while (reader.remaining() > 2) {
                int v = reader.getByte();
                sysExChecksum.update(v);
                out.write(v);
            }
            byte[] payload = out.toByteArray();
            int chk = reader.getByte();
            int f0 = reader.getByte();
            int checksum = sysExChecksum.getValue();
            log.info("Checksum %02X is ".formatted(chk) + ((checksum == chk) ? "right" : "wrong"));
            log.info("BitStream size : 0x%06X bytes".formatted(payload.length));
            return payload;
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }
}
