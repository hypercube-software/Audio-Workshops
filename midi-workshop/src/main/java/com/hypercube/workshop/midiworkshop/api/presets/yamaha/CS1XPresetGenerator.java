package com.hypercube.workshop.midiworkshop.api.presets.yamaha;

import com.hypercube.workshop.midiworkshop.MidiWorkshopApplication;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CS1XPresetGenerator {
    private final Pattern VOICE_DEFINITION = Pattern.compile("(\\d+)\\s(.+)");
    private List<String> categoriesXG = List.of("Piano",
            "Chromatic",
            "Percussion",
            "Organ",
            "Guitar",
            "Bass",
            "Strings",
            "Ensemble",
            "Brass",
            "Reed",
            "Pipe",
            "Synth Lead",
            "Synth Pad",
            "Synth Effects",
            "Ethnic",
            "Percussive",
            "Sound Effects");
    private List<String> categoryCodes = List.of(
            "Pf", "Piano",
            "Cp", "Chromatic Percussion",
            "Or", "Organ",
            "Gt", "Guitar",
            "Ba", "Bass",
            "St", "Strings",
            "En", "Ensemble",
            "Br", "Brass",
            "Rd", "Reed",
            "Pi", "Pipe",
            "Ld", "Synth Lead",
            "Pd", "Synth Pad",
            "Fx", "Synth FX",
            "Et", "Ethnic",
            "Pc", "Percussive",
            "Se", "Sound FX",
            "Dr", "Drums",
            "Sc", "Synth Comping",
            "Vo", "Vocal",
            "Co", "Combination",
            "Wv", "Material Wave",
            "Sq", "Sequence");

    private static byte[] generatePerformance(String title, int category) {
        MidiEventBuilder sb = new MidiEventBuilder(new DefaultChecksum());
        int byteCount = 0x2E;
        sb.write(0xF0, 0x43, 0x00, 0x4B, byteCount >>> 7, byteCount & 0x7F);
        sb.write(0x60, 0x00, 0x00); // Current Performance Common
        sb.beginChecksum();
        for (int chIdx = 0; chIdx < 8; chIdx++) {
            char c = chIdx < title.length() ? title.charAt(chIdx) : ' ';
            sb.write(c);
        }
        sb.write(category);
        sb.write(100); // volume
        sb.write(0x40, 0x40, 0x40, 0x40, 0x40, 0x40);
        sb.write(0x40, 0x40, 0x40, 0x40, 0x40, 0x40);
        sb.write(0x40, 0x40, 0x40, 0x40, 0x40, 0x40);
        sb.write(0x00, 0x00);
        sb.write(0x00, 0x00);
        sb.write(0x00, 0x00);
        sb.write(0x00, 0x00);
        sb.write(0x40);
        sb.write(0x40);
        sb.write(0x40);
        sb.write(0x40);
        sb.write(0x00);
        sb.write(0x00, 110); // BPM 110
        sb.write(9); // arpeg UpDnBlOct
        sb.write(7); // 1/16
        sb.write(0x00); // OFF
        sb.writeChecksum();
        sb.write(0xF7);
        return sb.buildBuffer();
    }

    public void dumpCS1XVoices(String deviceName) {
        MidiDeviceLibrary library = new MidiDeviceLibrary();
        library.load(ConfigHelper.getApplicationFolder(MidiWorkshopApplication.class));
        MidiDeviceDefinition device = library.getDevice(deviceName)
                .orElseThrow(() -> new MidiConfigError("Device not declared in the library: " + deviceName));
        MidiDeviceMode mode = device.getDeviceModes()
                .values()
                .stream()
                .findFirst()
                .get();
        String categories = readResource("Yamaha-voices/CS1X-raw.txt");
        List<String> lines = readResourceLines("Yamaha-voices/CS1X.txt");
        List<String> bankNames = new ArrayList<>();
        Map<String, List<String>> banks = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank())
                break;
            banks.put(line, new ArrayList<>());
            bankNames.add(line);
        }
        for (int b = 0; b < bankNames.size(); b++) {
            int offset = bankNames.size() + 1 + b * 128;
            String bankName = bankNames.get(b);
            for (int i = 0; i < 128; i++) {
                int l = i + offset;
                String line = lines.get(l);
                var m = VOICE_DEFINITION.matcher(line);
                if (!m.matches()) {
                    throw new MidiConfigError("Invalid voice definition at line %d: %s".formatted(l + 1, line));
                }
                int bank = device.getBankId(bankName);
                int program = Integer.parseInt(m.group(1)) - 1;
                String title = m.group(2)
                        .trim();
                if (title.equals("--")) {
                    continue;
                }
                int categoryIndex = searchCategory(device, mode, bankName, program, categories, title);
                System.out.println("%s \"%d-%d\" : \"%s\"".formatted(bankName, bank, program, title));
                String cleanTitle = title.replace("\"", "'")
                        .replace("/", "-")
                        .replace("*", "-");
                MidiPresetCategory category = device.getCategory(mode, categoryIndex);
                String filePath = "%s/%s/PerformanceMode/%s/%4X-%03d [%s] %s.syx".formatted(device.getBrand(), device.getDeviceName(), bankName, bank, program, category.name(), cleanTitle);
                File file = new File("devices-library/" + filePath);
                file.getParentFile()
                        .mkdirs();
                // CS1X Native Bulk Dump
                try (OutputStream out = new FileOutputStream(file)) {
                    out.write(generatePerformance(title, categoryIndex));
                    for (int layer = 0; layer < 4; layer++) {
                        out.write(generateLayer(layer, bank, program));
                    }
                } catch (IOException e) {
                    throw new MidiError(e);
                }
            }
        }
    }

    public String escapeRegex(String literal) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if ("\\.^$|()[]{}+*?".indexOf(c) != -1) {
                sb.append("\\");
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private List<String> readResourceLines(String path) {
        URL resource = this.getClass()
                .getClassLoader()
                .getResource(path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .toList();
        } catch (Exception e) {
            throw new MidiConfigError(e);
        }
    }

    private byte[] readResourceBytes(String path) {
        URL resource = this.getClass()
                .getClassLoader()
                .getResource(path);
        try {
            return resource.openStream()
                    .readAllBytes();
        } catch (IOException e) {
            throw new MidiConfigError(e);
        }
    }

    private String readResource(String path) {
        return new String(readResourceBytes(path), StandardCharsets.UTF_8);
    }

    private int searchCategory(MidiDeviceDefinition device, MidiDeviceMode mode, String bankName, int program, String categories, String title) {
        String category = bankName.startsWith("XG") ? categoriesXG.get(program / 8) : searchCategoryCode(device, categories, title);
        if (category == null & title.charAt(title.length() - 2) == ' ') {
            String shortName = title.substring(0, title.length() - 2) + title.substring(title.length() - 1);
            category = searchCategoryCode(device, categories, shortName);
        }
        if (category == null) {
            String prefix = title.substring(0, 2);
            category = mode.getCategories()
                    .stream()
                    .filter(c -> c.matches(prefix))
                    .findFirst()
                    .map(MidiPresetCategory::name)
                    .orElse(null);
        }
        return device.getCategoryCode(mode, category);
    }

    private String searchCategoryCode(MidiDeviceDefinition device, String categories, String title) {
        String pattern = "\\s([a-zA-Z]{2})\\s(%s)".formatted(escapeRegex(title));
        final Pattern CAT_DEFINITION = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        final var m = CAT_DEFINITION.matcher(categories);
        if (m.find()) {
            String catCode = m.group(1);
            for (int i = 0; i < categoryCodes.size(); i += 2) {
                if (categoryCodes.get(i)
                        .equals(catCode)) {
                    return categoryCodes.get(i + 1);
                }
            }
        }
        return null;
    }

    private byte[] generateLayer(int l, int bank, int voice) {
        int bankMSB = bank >>> 8;
        int bankLSB = bank & 0x7F;
        MidiEventBuilder sb = new MidiEventBuilder(new DefaultChecksum());
        int byteCount = 0x29;
        sb.write(0xF0, 0x43, 0x00, 0x4B, byteCount >>> 7, byteCount & 0x7F);
        sb.write(0x60, l + 1, 0x00); // Current Performance Common
        sb.beginChecksum();
        sb.write(bankMSB);
        sb.write(bankLSB);
        sb.write(voice);
        sb.write(1); // play mode
        sb.write(0x40); // transpose
        sb.write(0x08, 0x00); // detune
        sb.write(100); // volume
        sb.write(0x40, 0x40, 0x40);
        sb.write(0x00, 0x7F); // tessiture
        sb.write(0x00); // chorus send
        sb.write(0x00); // reverb send
        sb.write(0x00); // variation send
        sb.write(2); // LFO
        sb.write(0x40, 0x40, 0x40, 0x40, 0x40);
        sb.write(l == 0 ? 1 : 0); // Enabled
        sb.write(0x40, 0x40, 0x40, 0x40);
        sb.write(0x01, 0x7F); // Velo range
        sb.write(0x40, 0x40);
        sb.write(3); // LFO type
        sb.write(0x40, 0x40, 0x40);
        sb.write(0x40, 0x40, 0x40, 0x40);
        sb.write(0x40, 0x40);
        sb.writeChecksum();
        sb.write(0xF7);
        return sb.buildBuffer();
    }
}
