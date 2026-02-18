package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExChecksum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatchImporter {
    private final MidiDeviceLibrary midiDeviceLibrary;
    private final MidiDeviceRequester midiDeviceRequester;

    public void importSysex(MidiDeviceDefinition midiDeviceDefinition, String defaultMode, File file) {
        try {
            if (!file.exists()) {
                log.info("SYSEX not found: " + file.getAbsolutePath());
                return;
            }
            log.info("Parsing SYSEX: " + file.getAbsolutePath());
            List<byte[]> messages = collectSysExFromFile(file);
            log.info("{} messages found", messages.size());
            for (int presetId = 0; presetId < messages.size(); presetId++) {
                byte[] patchData = messages.get(presetId);

                OverrideContext ctx = applyOverrides(midiDeviceDefinition, defaultMode, patchData);

                ExtractedFields extractedFields = getFields(file, midiDeviceDefinition, ctx.mode(), presetId, messages.get(presetId));
                String patchName = Optional.ofNullable(extractedFields.getPatchName())
                        .orElse("");
                String spec = forgePatchSpec(ctx, extractedFields);
                String bankName = getBankName(file);
                String completeName = "%03d %s %s".formatted(presetId, spec, patchName)
                        .trim();
                completeName = completeName.replace("/", "-")
                        .replace(":", "-")
                        .replace("->", "➡")
                        .replace("<-", "⬅")
                        .replace("?", "❓")
                        .replace("  ", " ")
                        .replace("  ", " ")
                        .replace("\"", "'");
                File destFile = new File(midiDeviceDefinition.getDefinitionFile()
                        .getParentFile(), "%s/%s/%s/%s.syx".formatted(midiDeviceDefinition.getDeviceName(), ctx.mode()
                        .getName(), bankName, completeName));
                log.info("Generate " + destFile.getAbsolutePath());
                destFile.getParentFile()
                        .mkdirs();

                Files.write(destFile.toPath(), patchData);
            }
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private String forgePatchSpec(OverrideContext ctx, ExtractedFields extractedFields) {
        List<String> spec = new ArrayList<>();
        if (ctx.command() != null) {
            spec.add(ctx.command()
                    .replace("$", ""));
        }
        if (extractedFields.getCategory() != null) {
            spec.add(extractedFields.getCategory());
        }
        String result = spec.stream()
                .collect(Collectors.joining(","));
        if (result.length() > 0) {
            result = "[%s]".formatted(result);
        }
        return result;
    }

    private OverrideContext applyOverrides(MidiDeviceDefinition midiDeviceDefinition, String defaultMode, byte[] patchData) {
        MidiDeviceMode midiDeviceMode = midiDeviceDefinition.getMode(defaultMode)
                .orElseThrow();
        String command = null;
        if (midiDeviceDefinition.getPatchOverrides() != null) {
            // detect the mode of the patch if possible
            var electedOverrides = midiDeviceDefinition.getPatchOverrides()
                    .stream()
                    .filter(p -> p.matches(patchData) && p.mode() != null)
                    .distinct()
                    .toList();
            if (electedOverrides.size() > 1) {
                String modes = electedOverrides.stream()
                        .map(PatchOverride::mode)
                        .collect(Collectors.joining(","));
                throw new MidiConfigError("Conflicting mode found in patch overrides: " + modes);
            } else if (electedOverrides.size() == 1) {
                PatchOverride electedOverride = electedOverrides.getFirst();
                midiDeviceMode = midiDeviceDefinition.getMode(electedOverride.mode())
                        .orElseThrow();
                command = electedOverride.command();
            }
            // apply overrides
            midiDeviceDefinition.getPatchOverrides()
                    .forEach(patchOverride -> patchOverride(patchOverride, patchData));
        }
        return new OverrideContext(midiDeviceMode, command);
    }

    private void patchOverride(PatchOverride patchOverride, byte[] patchData) {
        if (!patchOverride.matches(patchData)) {
            log.info("Override '{}' not applied, condition is false: {}", patchOverride.name(), patchOverride.condition());
            return;
        }
        log.info("Apply override '{}'", patchOverride.name());
        patchOverride.overrides()
                .forEach(location -> {
                    if (location.value()
                            .startsWith("CK")) {
                        SysExChecksum sysExChecksum = new DefaultChecksum();
                        int offset = MidiEventBuilder.parseNumber(location.offset());
                        int size = Integer.parseInt(location.value()
                                .substring(2));
                        int start = offset - size;
                        for (int i = 0; i < size; i++) {
                            sysExChecksum.update(patchData[i + start]);
                        }
                        int ck = sysExChecksum.getValue();
                        log.info("Override checksum at pos 0x%04X with value 0x%02X instead of 0x%02X".formatted(
                                offset,
                                ck,
                                patchData[offset]
                        ));
                        patchData[offset] = (byte) ck;
                    } else {
                        int offset = MidiEventBuilder.parseNumber(location.offset());
                        String hexValues = location.value()
                                .startsWith("'") ? MidiEventBuilder.resolveASCIIStrings(location.value()) : "%02X".formatted(MidiEventBuilder.parseNumber(location.value()));
                        byte[] bytes = MidiEventBuilder.forgeBytesFromString(hexValues);
                        byte[] overrided = new byte[bytes.length];
                        String h = "";
                        for (int i = 0; i < overrided.length; i++) {
                            overrided[i] = patchData[offset + i];
                            h += "%02X".formatted((int) (overrided[i] & 0xFF));
                        }
                        log.info("Override %d bytes at pos 0x%04X with value 0x%s instead of 0x%s".formatted(
                                overrided.length,
                                offset,
                                hexValues,
                                h.trim()
                        ));
                        for (int i = 0; i < overrided.length; i++) {
                            patchData[offset + i] = bytes[i];
                        }
                    }
                });
    }

    private List<byte[]> collectSysExFromFile(File file) throws IOException, InvalidMidiDataException {
        if (file.getName()
                .toLowerCase()
                .endsWith(".mid")) {
            return collectSysExFromMidFile(file);
        } else if (file.getName()
                .toLowerCase()
                .endsWith(".syx")) {
            byte[] content = Files.readAllBytes(file.toPath());
            List<byte[]> messages = new ArrayList<>();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean inSysex = false;
            for (int i = 0; i < content.length; i++) {
                int value = content[i] & 0xFF;
                if (value == 0xF0) {
                    inSysex = true;
                    out.reset();
                }
                if (inSysex) {
                    out.write(value);
                }
                if (value == 0xF7) {
                    inSysex = false;
                    if (out.size() > 0) {
                        messages.add(out.toByteArray());
                    }
                }
            }
            return messages;
        } else {
            return List.of();
        }
    }

    private List<byte[]> collectSysExFromMidFile(File midiFile) throws InvalidMidiDataException, IOException {
        List<byte[]> sysexMessages = new ArrayList<>();

        Sequence sequence = MidiSystem.getSequence(midiFile);
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof SysexMessage) {
                    SysexMessage sysex = (SysexMessage) message;
                    byte[] fullSysexMessage = sysex.getMessage();
                    sysexMessages.add(fullSysexMessage);
                }
            }
        }
        return sysexMessages;
    }

    private String getBankName(File file) {
        String name = file.getName()
                .substring(0, file.getName()
                        .lastIndexOf('.'));
        return name.substring(0, 1)
                .toUpperCase() + name.substring(1);
    }

    private ExtractedFields getFields(File sysexFile, MidiDeviceDefinition device, MidiDeviceMode midiDeviceMode, int presetId, byte[] content) {
        ExtractedFields extractedFields = new ExtractedFields();
        if (midiDeviceMode.getQueryName() != null) {
            var sequences = CommandCall.parse(device.getDefinitionFile(), midiDeviceMode.getQueryName())
                    .stream()
                    .map(commandCall -> {
                        CommandMacro macro = device.getMacro(commandCall);
                        return midiDeviceRequester.forgeMidiRequestSequence(device, macro, commandCall);
                    })
                    .toList();
            var mapper = sequences.getFirst()
                    .getMidiRequests()
                    .getFirst()
                    .getMapper();
            CustomMidiEvent event = null;
            try {
                event = new CustomMidiEvent(new SysexMessage(content, content.length));
                mapper.extract(midiDeviceMode, extractedFields, event);
            } catch (InvalidMidiDataException e) {
                log.error("Unable to forge Sysex from patch {} in file {}", presetId, sysexFile);
            }
        }
        return extractedFields;
    }
}
