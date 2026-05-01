package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.checksum.SimpleSumChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KurzweilFile;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class KurzweilFileConverter {
    public void toSysEx(KurzweilFile input, File outputFolder) {
        outputFolder.mkdirs();
        var programs = input.objects()
                .stream()
                .filter(o -> o.getType() == KObject.PROGRAM)
                .map(o -> (KFProgram) o)
                .toList();
        for (var program : programs) {
            File outputFile = new File(outputFolder, "%3d - %s.syx".formatted(program.getObjectId(), program.getName()));
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                out.write(0xF0);
                out.write(0x07); // manufacturer Kurzweil
                out.write(0x00); // device ID
                out.write(0x78); // produce ID K2600
                out.write(0x09);
                writeShort(out, KObject.PROGRAM.getType());// Command ID WRITE
                writePacked14Bits(out, program.getObjectId());
                byte[] unpacked = program.getSegmentContent();
                byte[] packed = packNibble(unpacked);
                writePacked24Bit(out, unpacked.length);
                out.write(0x00); // mode
                for (byte c : program.getName()
                        .getBytes(StandardCharsets.US_ASCII)) {
                    out.write(c);
                }
                out.write(0x00); // end of string
                out.write(0x00); // format NIBBLE
                SimpleSumChecksum chk = new SimpleSumChecksum();
                for (int value : packed) {
                    chk.update(value);
                    out.write(value);
                }
                out.write(chk.getValue());
                out.write(0xF7);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        int lsb = (value >> 0) & 0x7F;
        int msb = (value >> 7) & 0x7F;
        out.write(msb);
        out.write(lsb);
    }

    private void writePacked24Bit(FileOutputStream out, int value) throws IOException {
        int lsb = (value >> 0) & 0x7F;
        int msb = (value >> 7) & 0x7F;
        int hsb = (value >> 14) & 0x7F;
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
