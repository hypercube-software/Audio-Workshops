package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.alesis;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseField;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;

/**
 * I'm pretty sure all fields are in little-indian despite the manual say nothing about it (only Device Inquiry response is officially stated as "LSB first")
 * <p>This is why readInvertedBits is used</p>
 */
public class AlesisSysExParser extends ManufacturerSysExParser {
    private static final String characterTable = " !\"#$%&’()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[¥]^_`abcdefghijklmnopqrstuvwxyz{|}→←";

    public String getString(MidiResponseField field, byte[] payload) {
        BitStreamReader bsr = new BitStreamReader(payload);
        String name = "";
        bsr.readBits(field.getOffset());
        for (int i = 0; i < field.getSize(); i++) {
            name += getChar(bsr.readInvertedBits(7));
        }
        return name;
    }

    public void dumpASCIITable() {
        for (int i = 0; i < characterTable.length(); i++) {
            String padded = BitStreamReader.getBinary7(i);
            String reverted = BitStreamReader.getBinary7Inverted(i);
            System.out.println("Code of %2d %c: %s reverted: %s".formatted(i, characterTable.charAt(i), padded, reverted));
        }
    }

    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }

    int getCharCode(Character c) {
        return characterTable.indexOf(c) & 0x7F; // 6 bit code
    }

    String getChar(int code) {
        if (characterTable.length() > code) {
            char c = characterTable.charAt(code);
            if (c == '|') {
                c = ' '; // '|' is reserved for our own usage during preset storage
            }
            return "" + c;
        } else {
            return "\uD83D\uDC80";
        }
    }
}
