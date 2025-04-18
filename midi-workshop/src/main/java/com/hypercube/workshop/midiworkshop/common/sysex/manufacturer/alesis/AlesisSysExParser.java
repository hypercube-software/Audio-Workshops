package com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.alesis;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetIdentity;
import com.hypercube.workshop.midiworkshop.common.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.response.MidiResponseField;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.common.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.common.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExReader;

public class AlesisSysExParser extends ManufacturerSysExParser {
    private static final String characterTable = " !\"#$%&’()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[¥]^_`abcdefghijklmnopqrstuvwxyz{|}→←";

    /**
     * This key is the right one to use. I found it after hours of reverse engineering
     */
    private static final String officialKeyReverted = """
            0 A1 A2 A3 A4 A5 A6 A7
            0 B2 B3 B4 B5 B6 B7 A0
            0 C3 C4 C5 C6 C7 B0 B1
            0 D4 D5 D6 D7 C0 C1 C2
            0 E5 E6 E7 D0 D1 D2 D3
            0 F6 F7 E0 E1 E2 E3 E4
            0 G7 F0 F1 F2 F3 F4 F5
            0 G0 G1 G2 G3 G4 G5 G6
            """;

    /**
     * This key can be found in the service manual and does not work
     */
    private static final String officialKey = """
            0 A6 A5 A4 A3 A2 A1 A0
            0 B5 B4 B3 B2 B1 B0 A7
            0 C4 C3 C2 C1 C0 B7 B6
            0 D3 D2 D1 D0 C7 C6 C5
            0 E2 E1 E0 D7 D6 D5 D4
            0 F1 F0 E7 E6 E5 E4 E3
            0 G0 F7 F6 F5 F4 F3 F2
            0 G7 G6 G5 G4 G3 G2 G1
            """;

    int getCharCode(Character c) {
        return characterTable.indexOf(c) & 0x7F; // 6 bit code
    }

    String getChar(int code) {
        if (characterTable.length() > code) {
            return "" + characterTable.charAt(code);
        } else {
            return "\uD83D\uDC80";
        }
    }

    public String getString(MidiResponseField field, byte[] payload) {
        BitStreamReader bsr = new BitStreamReader(payload);
        String name = "";
        bsr.readBits(field.getOffset());
        for (int i = 0; i < field.getSize(); i++) {
            name += getChar(bsr.readInvertedBits(7));
        }
        return name;
    }

    public MidiPresetIdentity getProgramName(MidiDeviceDefinition device, MidiDeviceMode mode, String currentBankName, byte[] decodedBuffer) {
        BitStreamReader bsr = new BitStreamReader(decodedBuffer);
        String name = "";
        bsr.readBits(8);
        for (int i = 0; i < 10; i++) {
            name += getChar(bsr.readInvertedBits(7));
        }
        bsr.readBits(3);
        int groupId = bsr.readInvertedBits(6);
        String category = device.getCategoryName(mode, groupId);
        return new MidiPresetIdentity(mode.getName(), currentBankName, name, category);
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
}
