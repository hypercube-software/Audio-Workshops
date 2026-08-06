package com.hypercube.workshop.synthripper.preset.sfz;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import com.hypercube.workshop.synthripper.preset.sfz.model.SfzPreset;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;

@Component
public class SfzPresetGenerator implements PresetGenerator {
    @Override
    public String getAlias() {
        return "SFZ";
    }

    @Override
    public void generate(SynthRipperConfiguration conf, List<RecordedSynthNote> sampleBatch) {
        var recordsPerPresets = sampleBatch.stream()
                .collect(groupingBy(RecordedSynthNote::getPreset));
        recordsPerPresets.forEach((preset, recordedSamples) -> {
            String presetId = preset.getShortId();
            File sfzFile = new File("%s/%s %s.sfz".formatted(conf.getOutputDir(), presetId, preset.getId()
                    .name()));
            try (PrintWriter out = new PrintWriter(new FileOutputStream(sfzFile))) {
                out.println("// %s".formatted(preset.getId()));
                out.print(forgeSfzPreset(sfzFile, recordedSamples).content());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public SfzPreset forgeSfzPreset(File sfzFile, List<RecordedSynthNote> recordedSynthNotes) {
        StringWriter out = new StringWriter();
        out.append("<control>\n")
                .append("default_path=./\n")
                .append("<global>\n")
                .append("\n");

        var recordsPerVelocity = recordedSynthNotes.stream()
                .collect(groupingBy(RecordedSynthNote::getVelocity));
        recordsPerVelocity.forEach((velocity, recordedSamplesPerVelocity) -> {
            var recordsPerCC = recordedSamplesPerVelocity.stream()
                    .collect(groupingBy(RecordedSynthNote::getControlChange));
            recordsPerCC.forEach((cc, recordedSamplesPerControlChange) -> {
                if (cc == MidiPreset.NO_CC) {
                    writeGroup(out, sfzFile, velocity, recordedSamplesPerControlChange);
                } else {
                    var recordsPerCCValue = recordedSamplesPerControlChange.stream()
                            .collect(groupingBy(RecordedSynthNote::getCcValue));
                    recordsPerCCValue.forEach((ccValue, recordedSamplesPerCCValue) -> {
                        writeCCGroup(out, sfzFile, velocity, cc, ccValue, recordedSamplesPerCCValue);
                    });
                }
            });
        });
        return new SfzPreset(out.toString());
    }

    private void writeGroup(StringWriter out, File sfzFile, MidiZone velocity, List<RecordedSynthNote> recordedSamples) {
        out.append("<group>\n")
                .append("lovel=%d\n".formatted(velocity.low()))
                .append("hivel=%d\n".formatted(velocity.high()))
                .append("\n");

        for (RecordedSynthNote sample : recordedSamples) {
            writeRegion(out, sfzFile, sample);
        }
    }

    private void writeCCGroup(StringWriter out, File sfzFile, MidiZone velocity, int cc, MidiZone ccZone, List<RecordedSynthNote> recordedSamples) {
        out.append("<group>\n")
                .append("lovel=%d\n".formatted(velocity.low()))
                .append("hivel=%d\n".formatted(velocity.high()))
                .append("loCC%d=%d\n".formatted(cc, ccZone.low()))
                .append("hiCC%d=%d\n".formatted(cc, ccZone.high()))
                .append("\n");

        for (RecordedSynthNote sample : recordedSamples) {
            writeRegion(out, sfzFile, sample);
        }
    }

    private void writeRegion(StringWriter out, File sfzFile, RecordedSynthNote sample) {
        out.append("<region>\n");

        String path = sfzFile.getParentFile()
                .toPath()
                .relativize(sample.getFile()
                        .toPath())
                .toString()
                .replace("\\", "/");
        out.append("sample=")
                .append(path)
                .append("\n");

        MidiZone note = sample.getNote();
        out.append("lokey=%d\n".formatted(note.low()))
                .append("pitch_keycenter=%d\n".formatted(note.value()))
                .append("hikey=%d\n".formatted(note.high()));

        out.append("ampeg_release=%.3f\n".formatted(sample.getReleaseTimeInSec()));

        Optional.ofNullable(sample.getLoopSetting())
                .ifPresentOrElse(l -> {
                    out.append("loop_mode=loop_sustain\n")
                            .append("loop_start=")
                            .append(Long.toString(l.getSampleStart()))
                            .append("\n")
                            .append("loop_end=")
                            .append(Long.toString(l.getSampleEnd()))
                            .append("\n");
                }, () -> out.append("loop_mode=no_loop\n"));

        out.append("\n");
    }
}
