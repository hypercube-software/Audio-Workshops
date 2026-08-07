package com.hypercube.workshop.midiworkshop.api.presets.standard;

import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardBank;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardBankId;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardPreset;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardPresetId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XGPresetsContainerTest {
    private final XGPresetsContainer container = new XGPresetsContainer();

    private StandardPreset findByBankAndPrg(int msb, int lsb, int prg) {
        return container.getPresets()
                .stream()
                .filter(p -> {
                    StandardPresetId pid = p.presetId();
                    return pid.bankId()
                            .msb() == msb
                            && pid.bankId()
                            .lsb() == lsb
                            && pid.prg() == prg;
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Preset not found: %d-%d-%d".formatted(msb, lsb, prg)));
    }

    @Test
    void shouldLoadBanks() {
        List<StandardBankId> bankIds = container.getBanks()
                .stream()
                .map(StandardBank::bankId)
                .toList();
        assertTrue(bankIds.contains(StandardBankId.of("0-0")), "Should contain bank 0-0");
        assertTrue(bankIds.contains(StandardBankId.of("127-0")), "Should contain bank 127-0 (drums)");
        assertTrue(bankIds.contains(StandardBankId.of("126-0")), "Should contain bank 126-0 (sfx kits)");
    }

    @Test
    void shouldLoadPresetsWithDrumMaps() {
        // Standard Kit has 72 drum notes
        StandardPreset standardKit = findByBankAndPrg(127, 0, 0);
        assertEquals("Standard Kit", standardKit.name());
        assertEquals(72, standardKit.drumMap()
                .size(), "Standard Kit should have 72 drum notes");

        // A regular preset should have an empty drumMap
        StandardPreset piano = findByBankAndPrg(0, 0, 0);
        assertEquals("Grand Piano", piano.name());
        assertTrue(piano.drumMap()
                .isEmpty(), "Grand Piano should have no drum notes");
    }

    @Test
    void shouldLoadSfxKitsWithDrumMaps() {
        StandardPreset sfxKit1 = findByBankAndPrg(126, 0, 0);
        assertEquals("SFX Kit 1", sfxKit1.name());
        assertEquals(24, sfxKit1.drumMap()
                .size());

        StandardPreset sfxKit2 = findByBankAndPrg(126, 0, 1);
        assertEquals("SFX Kit 2", sfxKit2.name());
        assertEquals(34, sfxKit2.drumMap()
                .size());
    }

    @Test
    void shouldLoadVariousKitSizes() {
        StandardPreset technoKitLo = findByBankAndPrg(126, 0, 18);
        assertEquals("Techno Kit Lo", technoKitLo.name());
        assertEquals(39, technoKitLo.drumMap()
                .size());

        StandardPreset sakuraKit = findByBankAndPrg(126, 0, 32);
        assertEquals("Sakura Kit", sakuraKit.name());
        assertEquals(23, sakuraKit.drumMap()
                .size());

        StandardPreset chinaKit = findByBankAndPrg(126, 0, 34);
        assertEquals("China Kit", chinaKit.name());
        assertEquals(19, chinaKit.drumMap()
                .size());
    }

    @Test
    void drumMapNotesShouldHaveValidData() {
        StandardPreset standardKit = findByBankAndPrg(127, 0, 0);
        DrumKitNote firstNote = standardKit.drumMap()
                .getFirst();
        assertEquals("Surdo Mute", firstNote.title());
        assertEquals(13, firstNote.note());

        DrumKitNote lastNote = standardKit.drumMap()
                .getLast();
        assertEquals("Bell Tree", lastNote.title());
        assertEquals(84, lastNote.note());
    }
}
