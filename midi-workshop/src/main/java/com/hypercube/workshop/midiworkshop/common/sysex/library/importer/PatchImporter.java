package com.hypercube.workshop.midiworkshop.common.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExChecksum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class PatchImporter {
    private final MidiDeviceLibrary midiDeviceLibrary;

    public void importSysex(MidiDeviceDefinition midiDeviceDefinition, String mode, File file) {
        try {
            MidiDeviceMode midiDeviceMode = midiDeviceDefinition.getMode(mode)
                    .orElseThrow();
            if (!file.exists()) {
                log.info("SYSEX not found: " + file.getAbsolutePath());
                return;
            }
            log.info("Parsing SYSEX: " + file.getAbsolutePath());
            List<byte[]> messages = collectSysExFromFile(file);
            log.info("{} messages found", messages.size());
            for (int presetId = 0; presetId < messages.size(); presetId++) {
                byte[] patchData = messages.get(presetId);
                ExtractedFields extractedFields = getFields(file, midiDeviceDefinition, midiDeviceMode, presetId, messages.get(presetId));
                String patchName = Optional.ofNullable(extractedFields.getPatchName())
                        .orElse("");
                String category = Optional.ofNullable(extractedFields.getCategory())
                        .map(c -> "[%s]".formatted(c))
                        .orElse("");
                String bankName = getBankName(file);
                String completeName = "%03d %s %s".formatted(presetId, category, patchName)
                        .trim();
                completeName = completeName.replace("/", "-")
                        .replace(":", "-")
                        .replace("\"", "'");
                File destFile = new File(midiDeviceDefinition.getDefinitionFile()
                        .getParentFile(), "%s/%s/%s/%s.syx".formatted(midiDeviceDefinition.getDeviceName(), mode, bankName, completeName));
                log.info("Generate " + destFile.getAbsolutePath());
                destFile.getParentFile()
                        .mkdirs();
                if (midiDeviceDefinition.getPatchOverrides() != null) {
                    midiDeviceDefinition.getPatchOverrides()
                            .forEach(patchOverride -> {
                                patchOverride(patchOverride, patchData);
                            });
                }
                Files.write(destFile.toPath(), patchData);
            }
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
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
                        int offset = SysExBuilder.parseNumber(location.offset());
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
                        int offset = SysExBuilder.parseNumber(location.offset());
                        String hexValues = location.value()
                                .startsWith("'") ? SysExBuilder.resolveASCIIStrings(location.value()) : "%02X".formatted(SysExBuilder.parseNumber(location.value()));
                        byte[] bytes = SysExBuilder.forgeBytesFromString(hexValues);
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
                        return midiDeviceLibrary.forgeMidiRequestSequence(device.getDefinitionFile(), device.getDeviceName(), macro, commandCall);
                    })
                    .toList();
            var mapper = sequences.getFirst()
                    .getMidiRequests()
                    .getFirst()
                    .getMapper();
            CustomMidiEvent event = null;
            try {
                event = new CustomMidiEvent(new SysexMessage(content, content.length), 0);
                mapper.extract(midiDeviceMode, extractedFields, event);
            } catch (InvalidMidiDataException e) {
                log.error("Unable to forge Sysex from patch {} in file ", presetId, sysexFile);
            }
        }
        return extractedFields;
    }
}
