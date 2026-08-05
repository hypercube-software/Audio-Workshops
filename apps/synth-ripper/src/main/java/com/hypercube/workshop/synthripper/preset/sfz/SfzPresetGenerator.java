package com.hypercube.workshop.synthripper.preset.sfz;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.MidiZone;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
                out.println("<control>");
                out.println("default_path=./");
                out.println("<global>");
                out.println();

                var recordsPerVelocity = recordedSamples.stream()
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
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeGroup(PrintWriter out, File sfzFile, MidiZone velocity, List<RecordedSynthNote> recordedSamples) {
        out.println("<group>");
        out.println("lovel=%d".formatted(velocity.low()));
        out.println("hivel=%d".formatted(velocity.high()));
        out.println();

        for (RecordedSynthNote sample : recordedSamples) {
            writeRegion(out, sfzFile, sample);
        }
    }

    private void writeCCGroup(PrintWriter out, File sfzFile, MidiZone velocity, int cc, MidiZone ccZone, List<RecordedSynthNote> recordedSamples) {
        out.println("<group>");
        out.println("lovel=%d".formatted(velocity.low()));
        out.println("hivel=%d".formatted(velocity.high()));
        out.println("loCC%d=%d".formatted(cc, ccZone.low()));
        out.println("hiCC%d=%d".formatted(cc, ccZone.high()));
        out.println();

        for (RecordedSynthNote sample : recordedSamples) {
            writeRegion(out, sfzFile, sample);
        }
    }

    private void writeRegion(PrintWriter out, File sfzFile, RecordedSynthNote sample) {
        out.println("<region>");

        String path = sfzFile.getParentFile()
                .toPath()
                .relativize(sample.getFile()
                        .toPath())
                .toString()
                .replace("\\", "/");
        out.println("sample=" + path);

        MidiZone note = sample.getNote();
        out.println("lokey=%d".formatted(note.low()));
        out.println("pitch_keycenter=%d".formatted(note.value()));
        out.println("hikey=%d".formatted(note.high()));

        out.println("ampeg_release=%.3f".formatted(sample.getReleaseTimeInSec()));

        Optional.ofNullable(sample.getLoopSetting())
                .ifPresentOrElse(l -> {
                    out.println("loop_mode=loop_sustain");
                    out.println("loop_start=" + l.getSampleStart());
                    out.println("loop_end=" + l.getSampleEnd());
                }, () -> out.println("loop_mode=no_loop"));

        out.println();
    }
}
