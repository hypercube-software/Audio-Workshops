package com.hypercube.workshop.synthripper.preset.sfz;

import com.hypercube.workshop.synthripper.AbstractSynthRipperTest;
import com.hypercube.workshop.synthripper.SynthRipper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class SfzPresetGeneratorsFullTest extends AbstractSynthRipperTest {
    private int countOccurrences(String text, String token) {
        return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    @Test
    void canGenerateSfzPresets() throws IOException {
        // GIVEN
        SynthRipper synthRipper = forgeSynthRipper("src/test/resources/config/config-DS-330.yml");
        SfzPresetGenerator sfzPresetGenerator = new SfzPresetGenerator();

        // WHEN
        var batch = synthRipper.generateBatch();
        var sfzPreset = sfzPresetGenerator.forgeSfzPreset(new File("output/preset.sfz"), batch);
        String content = sfzPreset.content();
        log.info(content);

        // THEN
        assertEquals(8, synthRipper.getConf()
                .getSelectedPresets()
                .size());
        assertEquals(8 * 2 * 2, batch.size());
        assertTrue(content.contains("default_path=./"));
        assertEquals(2, countOccurrences(content, "<group>"));
        assertEquals(8 * 2 * 2, countOccurrences(content, "<region>"));
        assertEquals(8 * 2 * 2, countOccurrences(content, "loop_mode="));
    }
}
