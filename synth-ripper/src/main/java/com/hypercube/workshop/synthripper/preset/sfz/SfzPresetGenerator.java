package com.hypercube.workshop.synthripper.preset.sfz;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

@Component
public class SfzPresetGenerator implements PresetGenerator {
    @Override
    public String getAlias() {
        return "SFZ";
    }

    @Override
    public void generate(SynthRipperConfiguration conf, List<RecordedSynthNote> sampleBatch) {
        sampleBatch.forEach(sample -> {
            MidiPreset preset = sample.getPreset();
            File sfzFile = new File("%s/%s %s.sfz".formatted(conf.getOutputDir(), preset.getId(), preset.title()));
            try (PrintWriter out = new PrintWriter(new FileOutputStream(sfzFile))) {
                final String SEPARATOR = "----------------------------------------------------------";
                out.println(SEPARATOR);
                out.println("%s".formatted(preset.title()));
                out.println(SEPARATOR);
                out.println("<control>");
                out.println("default_path=.");
                out.println("<global>");
                int prevVelo = -1;
                int nbVelocityPerNote = conf.getMidi()
                        .getVelocityPerNote();
                int velocityOffset = (int) Math.ceil(127.f / nbVelocityPerNote);
                for (int group = 1; group <= nbVelocityPerNote; group++) {
                    int startVelo = prevVelo + 1;
                    int endVelo = Math.min(127, velocityOffset * group);
                    out.println(SEPARATOR);
                    out.println(" Velocity layer %03d".formatted(endVelo));
                    out.println(SEPARATOR);
                    out.println("<group>");
                    out.println("lovel=" + startVelo);
                    out.println("hivel=" + endVelo);
                    prevVelo = endVelo;

                    out.println("");
                    MidiSettings midiSettings = conf.getMidi();
                    int lowestNote = midiSettings.getLowestNoteInt();
                    int highestNote = midiSettings.getHighestNoteInt();
                    int noteIncrement = 12 / midiSettings.getNotesPerOctave();
                    int nbRegion = Math.min(1, highestNote + 1 - lowestNote / noteIncrement);
                    for (int r = 0; r < nbRegion; r++) {
                        out.println("<region>");
                        int note = lowestNote + r * noteIncrement;
                        int begin = note;
                        int end = note + noteIncrement - 1;
                        if (r == 0) {
                            begin = 0;
                        }
                        if (r == nbRegion - 1) {
                            end = 127;
                        }
                        String path = sfzFile.getParentFile()
                                .toPath()
                                .relativize(sample.getFile()
                                        .toPath())
                                .toString()
                                .replace("\\", "/");

                        Optional.ofNullable(sample.getLoopSetting())
                                .ifPresentOrElse(l -> {
                                    out.println("loop_mode=loop_sustain");
                                    out.println("loop_start=" + l.getSampleStart());
                                    out.println("loop_end=" + l.getSampleEnd());
                                }, () -> out.println("loop_mode=no_loop"));

                        out.println("sample=" + path);
                        out.println("lokey=" + begin);
                        out.println("pitch_keycenter=" + note);
                        out.println("hikey=" + end);
                        out.println("");
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
