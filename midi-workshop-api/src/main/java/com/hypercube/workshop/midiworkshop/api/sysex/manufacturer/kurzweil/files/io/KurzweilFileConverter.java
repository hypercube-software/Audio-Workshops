package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.SimpleSumChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KurzweilSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment.KFProgramSegmentDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KurzweilFile;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KFKeyMap;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlock;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
public class KurzweilFileConverter {
    public void toSysEx(KurzweilFile input, File outputFolder) {
        outputFolder.mkdirs();
        var programs = input.objects()
                .stream()
                .filter(o -> o.getType() == KObject.PROGRAM)
                .map(o -> (KFProgram) o)
                .filter(p -> p.getLayers()
                        .stream()
                        .anyMatch(l -> l.getCseg()
                                .getKeymap() >= 200))
                .toList();
        var samples = input.objects()
                .stream()
                .filter(o -> o.getType() == KObject.SOUND_BLOCK)
                .map(o -> (KFSoundBlock) o)
                .toList();
        var keymaps = input.objects()
                .stream()
                .filter(o -> o.getType() == KObject.KEYMAP)
                .map(o -> (KFKeyMap) o)
                .toList();
        for (var fileProgram : programs) {
            var midiProgram = forgeMidiProgram(fileProgram);
            File outputFile = new File(outputFolder, "%3d - %s.syx".formatted(midiProgram.getObjectId(), midiProgram.getName()));
            log.info("Generate {}", outputFile);

            BitStreamWriter o = new BitStreamWriter();
            KFProgramSegmentDeserializer d = new KFProgramSegmentDeserializer(midiProgram);
            d.serialize("", midiProgram, o);
            midiProgram.getData()
                    .setContent(o.toByteArray());
            midiProgram.getData()
                    .setPosition(0);
            midiProgram.setSegmentsStart(0);
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                for (var keymap : keymaps) {
                    if (midiProgram.contains(keymap)) {
                        writeObject(out, samples.getFirst());
                        writeObject(out, keymap);
                    }
                }
                dumpProgram(midiProgram, outputFolder);
                writeObject(out, midiProgram);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            KurzweilSysExParser kurzweilSysExParser = new KurzweilSysExParser();
            try {
                var r = kurzweilSysExParser.parse(Manufacturer.KURZWEIL, Files.readAllBytes(outputFile.toPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeNibblePayload(FileOutputStream out, byte[] packed) throws IOException {
        out.write(0x00); // format NIBBLE
        SimpleSumChecksum chk = new SimpleSumChecksum();
        for (int value : packed) {
            chk.update(value);
            out.write(value);
        }
        out.write(chk.getValue());
    }

    private void writeObject(FileOutputStream out, KFObject kObject) throws IOException {
        out.write(0xF0);
        out.write(0x07); // manufacturer Kurzweil
        out.write(0x00); // device ID
        out.write(0x78); // produce ID K2600
        out.write(0x09); // Command ID WRITE
        writeShort(out, kObject.getType()
                .getType()); // object type id
        writePacked14Bits(out, kObject.getObjectId()); // object id

        byte[] unpacked = kObject.getData()
                .getContent();
        byte[] packed = packNibble(unpacked);
        writePacked24Bit(out, unpacked.length);
        out.write(0x00); // override mode
        for (byte c : kObject.getName()
                .getBytes(StandardCharsets.US_ASCII)) {
            out.write(c);
        }
        out.write(0x00); // end of string
        writeNibblePayload(out, packed);
        out.write(0xF7);
    }

    private void dumpProgram(KFProgram midiProgram, File outputFolder) throws IOException {
        byte[] unpacked = midiProgram.getSegmentContent();
        File rawOutputFile = new File(outputFolder, "%3d - %s.unpacked".formatted(midiProgram.getObjectId(), midiProgram.getName()));
        Files.write(rawOutputFile.toPath(), unpacked);
    }

    private KFProgram forgeMidiProgram(KFProgram fileProgram) {
        String name = "/kurzweil/k2600r-default-program.json";

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule());
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        try (InputStream is = KurzweilFileConverter.class.getResourceAsStream(name)) {
            if (is == null) {
                throw new MidiError("Resource not found: " + name);
            }

            KFProgram defaultProgram = mapper.readValue(is, KFProgram.class);
            // prepare has many default layer than the source
            var defaultLayer = defaultProgram.getLayers()
                    .getFirst();
            defaultProgram.getLayers()
                    .clear();
            for (int i = 0; i < fileProgram.getLayers()
                    .size(); i++) {
                defaultProgram.getLayers()
                        .add(defaultLayer);
            }
            TreeMerger treeMerger = new TreeMerger();
            treeMerger.merge(fileProgram, defaultProgram);
            defaultProgram.getPgm()
                    .setFmt(4); // force format 4
            return defaultProgram;
        } catch (IOException e) {
            throw new MidiError("Error reading or parsing resource " + name, e);
        }
    }


    private byte[] packNibble(byte[] content) {
        byte[] result = new byte[content.length * 2];
        for (int i = 0; i < content.length; i++) {
            int value = content[i];
            int msb = (value >> 4) & 0xF;
            int lsb = (value >> 0) & 0xF;
            result[i * 2 + 0] = (byte) msb;
            result[i * 2 + 1] = (byte) lsb;
        }
        return result;
    }

    private void writePacked14Bits(FileOutputStream out, int value) throws IOException {
        int msb = (value >> 7) & 0x7F;
        int lsb = (value >> 0) & 0x7F;
        out.write(msb);
        out.write(lsb);
    }

    private void writePacked24Bit(FileOutputStream out, int value) throws IOException {
        int hsb = (value >> 14) & 0x7F;
        int msb = (value >> 7) & 0x7F;
        int lsb = (value >> 0) & 0x7F;
        out.write(hsb);
        out.write(msb);
        out.write(lsb);
    }

    private void writeShort(FileOutputStream out, int value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort((short) value);
        out.write(bb.array());
    }
}
