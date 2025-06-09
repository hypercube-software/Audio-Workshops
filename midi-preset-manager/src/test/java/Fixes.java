import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.mpm.config.ConfigError;
import com.hypercube.mpm.config.Favorites;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Fixes {
    @Test
    @Disabled
    void fixFavorites() {
        MidiDeviceLibrary midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(new File("../devices-library"));
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            var favorties = mapper.readValue(new File("favorite-patches.yaml"), Favorites.class);
            favorties.getFavorites()
                    .forEach(patch -> {
                        var d = midiDeviceLibrary.getDevice(patch.getDevice())
                                .get();
                        if (d.getPresetFormat() == MidiBankFormat.NO_BANK_PRG) {
                            String cmd = "%02X".formatted(Integer.parseInt(patch.getCommand()));
                            patch.setCommand(cmd);
                        }
                    });
            mapper.writeValue(new File("favorite-patches-fixed.yaml"), favorties);
        } catch (IOException e) {
            throw new ConfigError(e);
        }
    }

    @Test
    @Disabled
    void fixDecimalPresets() throws IOException {
        Pattern p = Pattern.compile("\\\"(?<v>(?<v1>[0-9]+)(-(?<v2>[0-9]+))?(-(?<v3>[0-9]+))?) \\|");
        MidiDeviceLibrary midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(new File("../devices-library"));
        midiDeviceLibrary.getDevices()
                .values()
                .forEach(d -> {
                    String brand = d.getBrand();
                    String device = d.getDeviceName();
                    File presets = new File("../devices-library/%s/%s-presets.yml".formatted(brand, device));
                    File presetsFixed = new File("../devices-library/%s/%s-presets-fixed.yml".formatted(brand, device));
                    try {
                        if (presets.exists()) {
                            List<String> lines = Files.readAllLines(presets.toPath())
                                    .stream()
                                    .map(l -> {
                                        Matcher m = p.matcher(l);
                                        if (m.find()) {
                                            int start = m.start("v");
                                            int end = m.end("v");
                                            String value1 = m.group("v1");
                                            String value2 = m.group("v2");
                                            String value3 = m.group("v3");
                                            List<String> values = new ArrayList<>();
                                            if (value1 != null) {
                                                values.add(value1);
                                            }
                                            if (value2 != null) {
                                                values.add(value2);
                                            }
                                            if (value3 != null) {
                                                values.add(value3);
                                            }
                                            if (values.size() > 1) {
                                                String fixed = values.stream()
                                                        .map(v ->
                                                                "%02X".formatted(Integer.parseInt(v)))
                                                        .collect(Collectors.joining());
                                                String newLine = l.substring(0, start) + fixed + l.substring(end);
                                                return newLine;
                                            } else {
                                                return l;
                                            }
                                        } else {
                                            return l;
                                        }
                                    })
                                    .toList();
                            Files.write(presetsFixed.toPath(), lines);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
