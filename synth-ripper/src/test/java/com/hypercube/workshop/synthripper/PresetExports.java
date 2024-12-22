package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.synthripper.config.presets.ConfigMidiPreset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PresetExports {
    private final int CS1X_DRUMKIT_START_NOTE = 13;

    @Test
    @Disabled
    void cs1x_drumkits() throws IOException {
        ConfigMidiPreset capital = new ConfigMidiPreset("Capital Drumkit", 10, List.of("63-12-0"), List.of(), new ArrayList<>());
        ConfigMidiPreset techno = new ConfigMidiPreset("Capital Drumkit", 10, List.of("63-12-1"), List.of(), new ArrayList<>());
        ConfigMidiPreset electro = new ConfigMidiPreset("Capital Drumkit", 10, List.of("63-12-2"), List.of(), new ArrayList<>());
        var drumkits = List.of(capital, techno, electro);

        var p = Pattern.compile("(?<note>[0-9]+)\\s[CDEFGAB#]+\\s[0-9\\-]+(?<name>.+)");
        var lines = Files.readAllLines(Path.of("export/cs1x/drumkit-cs1x-capital.txt"));

        for (int l = 0; l < lines.size(); l++) {
            String line = lines.get(l);
            var m = p.matcher(line);
            if (m.find()) {
                int note = Integer.parseInt(m.group("note"));
                String name = m.group("name")
                        .trim();
                name = cleanupDrumKitNoteName(name);

                capital.getDrumkitNotes()
                        .add("%3d %s".formatted(note, name));
            } else {
                throw new RuntimeException("Unexpected format at line " + l + ": " + line);
            }
        }
        lines = Files.readAllLines(Path.of("export/cs1x/drumkit-cs1x-Techkit.txt"));
        for (int l = 0; l < lines.size(); l++) {
            setDrumKit(lines.get(l), l, capital, techno);
        }
        lines = Files.readAllLines(Path.of("export/cs1x/drumkit-cs1x-ElectroKit.txt"));
        for (int l = 0; l < lines.size(); l++) {
            setDrumKit(lines.get(l), l, capital, electro);
        }

        drumkits.forEach(dk -> {
            System.out.println("    - title: " + dk.getTitle());
            System.out.println("      channel: " + dk.getChannel());
            System.out.println("      commands:");
            dk.getCommands()
                    .forEach(c -> System.out.println("          - " + c));
            System.out.println("      drumkit:");
            dk.getDrumkitNotes()
                    .forEach(dkn -> System.out.println("          - " + dkn));
        });
    }

    private void setDrumKit(String line, int lineNumber, ConfigMidiPreset capital, ConfigMidiPreset techno) {
        String name = cleanupDrumKitNoteName(line);
        int note = lineNumber + CS1X_DRUMKIT_START_NOTE;
        String capitalDrumKitNote = capital.getDrumkitNotes()
                .get(lineNumber);
        techno.getDrumkitNotes()
                .add(name.length() == 0 ? capitalDrumKitNote : "%3d %s".formatted(note, name));
    }

    private static String cleanupDrumKitNoteName(String name) {
        name = name.replaceAll(" Sho$", " Shot");
        name = name.replaceAll(" Be$", " Bell");
        name = name.replaceAll(" Cli$", " Click");
        name = name.replaceAll(" H$", " High");
        name = name.replaceAll(" H ", " High ");
        name = name.replaceAll(" M$", " Middle");
        name = name.replaceAll(" M ", " Middle ");
        name = name.replaceAll(" L$", " Low");
        name = name.replaceAll(" CL$", " Closed");
        name = name.replaceAll(" Op$", " Open");
        name = name.replaceAll(" SN$", " Snare");
        name = name.replaceAll(" L ", " Low ");
        name = name.replaceAll(" HH ", " HiHat ");
        name = name.replaceAll(" Cymb$", " Cymbal");
        name = name.replaceAll(" HI1$", " High 1");
        name = name.replaceAll(" HI2$", " High 2");
        name = name.replaceAll(" SN1$", " Snare 1");
        name = name.replaceAll(" SN2$", " Snare 2");
        name = name.replaceAll(" SN3$", " Snare 3");
        name = name.replaceAll(" SN4$", " Snare 4");
        name = name.replaceAll(" SN5$", " Snare 5");
        name = name.replaceAll(" Kik1$", " Kick 1");
        name = name.replaceAll(" Kik2$", " Kick 2");
        name = name.replaceAll(" Kik3$", " Kick 3");
        name = name.replaceAll(" Kik3n$", " Kick 3n");
        name = name.replaceAll(" CL1$", " Close 1");
        name = name.replaceAll(" CL2$", " Close 2");
        name = name.replaceAll(" OP1$", " Open 1");
        name = name.replaceAll(" OP2$", " Open 2");
        return name;
    }


    @Test
    @Disabled
    void cs1x() throws IOException {
        var p = Pattern.compile("([0-9]+)\\s+(.+)(\\s[0-9]\\s).+");
        var lines = Files.readAllLines(Path.of("export/cs1x/cs1x-performances.txt"));
        int bank = 0;
        for (int l = 0; l < lines.size(); l++) {
            String txt = lines.get(l);
            if (txt.length() == 0) {
                bank++;
            } else {
                var m = p.matcher(txt);
                if (m.find()) {
                    String program = m.group(1);
                    String name = m.group(2)
                            .trim();
                    name = name.replace("D r ", "Dr ")
                            .replace("P f ", "Pf ")
                            .replace("G t ", "Gt ")
                            .replace("B r ", "Br ")
                            .replace("S t ", "St ")
                            .replace("E t ", "Et ");
                    if (name.charAt(2) == ' ') {
                        name = name.substring(0, 2) + name.substring(3);
                    }
                    String bankStr = bank == 0 ? "63-64-" : "63-65-";
                    System.out.println("    - " + bankStr + program + " " + name);
                } else {
                    System.out.println("BUG: " + txt);
                }
            }
        }
    }
}
