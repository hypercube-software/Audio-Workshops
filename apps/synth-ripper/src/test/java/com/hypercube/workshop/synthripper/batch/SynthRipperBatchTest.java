package com.hypercube.workshop.synthripper.batch;

import com.hypercube.workshop.midiworkshop.api.MidiNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.standard.XGPresetsContainer;
import com.hypercube.workshop.synthripper.AbstractSynthRipperTest;
import com.hypercube.workshop.synthripper.SynthRipper;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class SynthRipperBatchTest extends AbstractSynthRipperTest {

    @Test
    void generateBatchFromConfigCS2x() throws IOException {
        // GIVEN
        SynthRipper synthRipper = forgeSynthRipper("src/test/resources/config/config-CS2x.yml");

        // WHEN
        var batch = synthRipper.generateBatch();

        // THEN the batch is generated from the presets and notes declared in the config
        assertEquals(15, synthRipper.getConf()
                .getSelectedPresets()
                .size());
        assertEquals(15 * 2 * 2, batch.size());
        assertEquals(List.of(36, 48), batch.stream()
                .map(RecordedSynthNote::getNote)
                .map(MidiZone::value)
                .distinct()
                .toList());
        assertEquals(List.of(64, 127), batch.stream()
                .map(RecordedSynthNote::getVelocity)
                .map(MidiZone::value)
                .distinct()
                .toList());
        assertTrue(batch.stream()
                .allMatch(rs -> rs.getName()
                        .contains(" %s".formatted(MidiNote.fromValue(rs.getNote()
                                        .value())
                                .name()))));
        assertTrue(batch.stream()
                .allMatch(rs -> rs.getControlChange() == MidiPreset.NO_CC));
    }

    @Test
    void generateBatchFromConfigDs330() throws IOException {
        // GIVEN
        SynthRipper synthRipper = forgeSynthRipper("src/test/resources/config/config-DS-330.yml");

        // WHEN
        var batch = synthRipper.generateBatch();

        // THEN the batch is generated from the presets and notes declared in the config
        assertEquals(8, synthRipper.getConf()
                .getSelectedPresets()
                .size());
        assertEquals(8 * 2 * 2, batch.size());
        assertEquals(List.of(36, 48), batch.stream()
                .map(RecordedSynthNote::getNote)
                .map(MidiZone::value)
                .distinct()
                .toList());
        assertEquals(List.of(64, 127), batch.stream()
                .map(RecordedSynthNote::getVelocity)
                .map(MidiZone::value)
                .distinct()
                .toList());
        assertTrue(batch.stream()
                .allMatch(rs -> rs.getName()
                        .contains(" %s".formatted(MidiNote.fromValue(rs.getNote()
                                        .value())
                                .name()))));
        assertTrue(batch.stream()
                .allMatch(rs -> rs.getControlChange() == MidiPreset.NO_CC));
    }

    @Test
    void generateBatchForDrumkit() throws IOException {
        // GIVEN
        SynthRipper synthRipper = forgeSynthRipper("src/test/resources/config/config-CS2x-kit.yml");
        XGPresetsContainer xgPresetsContainer = new XGPresetsContainer();
        var expectedPreset = xgPresetsContainer.getPresets()
                .stream()
                .filter(p -> p.name()
                        .equals(synthRipper.getConf()
                                .getMidi()
                                .getLowestPreset()))
                .findFirst()
                .orElseThrow();
        int expectedBatchSize = expectedPreset.drumMap()
                .size() * 2; // 2 velocity layers
        // WHEN
        var batch = synthRipper.generateBatch();

        // THEN the batch is generated from the presets and notes declared in the config
        assertEquals(expectedBatchSize, batch.size());
    }

}
