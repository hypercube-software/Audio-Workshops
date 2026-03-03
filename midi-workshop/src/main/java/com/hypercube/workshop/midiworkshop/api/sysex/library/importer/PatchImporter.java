package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseFieldType;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExChecksum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class is used to import external sysEx files into the library ('*.sys' and '*.mid' files are supported)
 * <p>It is able to modify them using various {@link PatchOverride}</p>
 * <ul>
 *     <li>To make sure the SysEx modify only the Edit Buffer of the device</li>
 *     <li>To override the name of the patch if needed (typically when we want to save the current patch from the device)
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PatchImporter {
    public static final String $OVERRIDE_PATCH_NAME = "$PatchName";
    private final MidiDeviceLibrary midiDeviceLibrary;
    private final MidiDeviceRequester midiDeviceRequester;

    /**
     * Import an external SysEx file and "fix it" using {@link PatchOverride}
     * <ul>
     *     <li>All SysEx payloads are extracted from the file and treated independently</li>
     *     <li>Patch names are extracted from them</li>
     * </ul>
     */
    public void importSysExFile(MidiDeviceDefinition midiDeviceDefinition, String defaultMode, File file) {
        try {
            if (!file.exists()) {
                log.info("SysEx not found: {}", file.getAbsolutePath());
                return;
            }
            log.info("Parsing SysEx: {}", file.getAbsolutePath());
            List<byte[]> messages = collectSysExFromFile(file);
            log.info("{} messages found", messages.size());
            for (int presetId = 0; presetId < messages.size(); presetId++) {
                byte[] patchData = messages.get(presetId);

                OverrideContext ctx = OverrideContext.builder()
                        .device(midiDeviceDefinition)
                        .defaultMode(defaultMode)
                        .payload(patchData)
                        .build();
                applyOverrides(ctx);

                ExtractedFields extractedFields = getFields(file, ctx, presetId, messages.get(presetId));
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
                        .getParentFile(), "%s/%s/%s/%s.syx".formatted(midiDeviceDefinition.getDeviceName(), ctx.getMode()
                        .getName(), bankName, completeName));
                log.info("Generate {}", destFile.getAbsolutePath());
                destFile.getParentFile()
                        .mkdirs();

                Files.write(destFile.toPath(), patchData);
            }
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    /**
     * Store a SysEx response from the device, overriding the patch name
     */
    public void importSysExPayload(MidiDeviceDefinition device, String defaultMode, String patchName, int categoryCode, byte[] response, File patchFile) throws IOException {
        OverrideContext ctx = OverrideContext.builder()
                .device(device)
                .payload(response)
                .patchName(patchName.replace(".syx", ""))
                .categoryCode(categoryCode)
                .defaultMode(defaultMode)
                .build();
        applyOverrides(ctx);
        Files.write(patchFile.toPath(), ctx.getPayload());
    }

    /**
     * Given what the overrides can provide and the {@link OverrideContext#defaultMode}, determine the mode the patch will belong to
     */
    private void resolveModeAndCommand(OverrideContext ctx) {
        final MidiDeviceDefinition midiDeviceDefinition = ctx.getDevice();
        final byte[] payload = ctx.getPayload();
        var overridesProvidingMode = midiDeviceDefinition.getPatchOverrides()
                .stream()
                .filter(p -> p.matches(payload) && p.mode() != null)
                .toList();
        var overrides = overridesProvidingMode.stream()
                .map(PatchOverride::mode)
                .distinct()
                .toList();
        if (overrides.size() > 1) {
            String modes = String.join(",", overrides);
            throw new MidiConfigError("Conflicting mode found in patch overrides: " + modes + ". You need to use a condition per mode.");
        } else if (overrides.size() == 1) {
            PatchOverride electedOverride = overridesProvidingMode.getFirst();
            String mode = electedOverride.mode();
            ctx.setMode(midiDeviceDefinition.getMode(mode)
                    .orElseThrow(() -> new MidiConfigError("For device %s, there is an override with unknown mode '%s'".formatted(ctx.getDevice()
                            .getDeviceName(), mode))));
            ctx.setCommand(electedOverride.command());
        } else {
            // none of the overrides indicate a mode so we use the default one
            ctx.setMode(midiDeviceDefinition.getMode(ctx.getDefaultMode())
                    .orElseThrow());
        }
    }

    private void logIntegerOverride(byte[] overridden, int offset, String hexValues) {
        StringBuilder h = new StringBuilder();
        for (byte b : overridden) {
            h.append("%02X".formatted((int) (b & 0xFF)));
        }
        log.info("Override %d bytes at pos 0x%04X with value 0x%s instead of 0x%s".formatted(
                overridden.length,
                offset,
                hexValues,
                h.toString()
                        .trim()
        ));
    }

    private void applyChecksum(OverrideContext ctx, byte[] patchData, OverrideLocation location) {

        SysExChecksum sysExChecksum = ctx.getDevice()
                .newCheckSum();
        
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
    }

    private void applyInteger(OverrideContext ctx, byte[] patchData, OverrideLocation location) {
        int offset = MidiEventBuilder.parseNumber(location.offset());
        final String hexValues;
        if (location.type() == MidiResponseFieldType.CATEGORY) {
            hexValues = "%02X".formatted(ctx.getCategoryCode());
        } else {
            hexValues = location.value()
                    .startsWith("'") ? MidiEventBuilder.resolveASCIIStrings(location.value()) : "%02X".formatted(MidiEventBuilder.parseNumber(location.value()));
        }
        byte[] bytes = MidiEventBuilder.forgeBytesFromString(hexValues);
        byte[] overridden = new byte[bytes.length];
        System.arraycopy(patchData, offset, overridden, 0, overridden.length);
        logIntegerOverride(overridden, offset, hexValues);
        System.arraycopy(bytes, 0, patchData, offset, overridden.length);
    }

    private String forgePatchSpec(OverrideContext ctx, ExtractedFields extractedFields) {
        List<String> spec = new ArrayList<>();
        Optional.ofNullable(ctx.getCommand())
                .ifPresent(cmd -> spec.add(cmd
                        .replace("$", "")));
        if (extractedFields.getCategory() != null) {
            spec.add(extractedFields.getCategory());
        }
        String result = String.join(",", spec);
        if (!result.isEmpty()) {
            result = "[%s]".formatted(result);
        }
        return result;
    }

    private void applyOverrides(OverrideContext ctx) {
        Optional.ofNullable(ctx.getDevice()
                        .getPatchOverrides())
                .ifPresent(patchOverrides -> {
                    resolveModeAndCommand(ctx);
                    patchOverrides.forEach(patchOverride -> patchOverride(ctx, patchOverride));
                });
    }

    private void patchOverride(OverrideContext ctx, PatchOverride patchOverride) {
        final byte[] payload = ctx.getPayload();
        if (!patchOverride.matches(payload)) {
            log.info("Override '{}' not applied, condition is false: {}", patchOverride.name(), patchOverride.condition());
            return;
        }
        log.info("Apply override '{}'", patchOverride.name());
        patchOverride.overrides()
                .forEach(location -> {
                    if (location.value()
                            .startsWith("CK")) {
                        applyChecksum(ctx, payload, location);
                    } else if (location.type() == null || MidiResponseFieldType.INTEGER == location.type() || MidiResponseFieldType.CATEGORY == location.type()) {
                        applyInteger(ctx, payload, location);
                    } else if (MidiResponseFieldType.STRING == location.type()) {
                        applyString(ctx, payload, location);
                    }
                });
    }

    private void applyString(OverrideContext ctx, byte[] patchData, OverrideLocation location) {
        String strValue = $OVERRIDE_PATCH_NAME.equals(location.value()) ? ctx.getPatchName() : location.value();
        if (strValue != null) {
            int size = MidiEventBuilder.parseNumber(location.size());
            int offset = MidiEventBuilder.parseNumber(location.offset());
            byte[] valueBytes = strValue.getBytes(StandardCharsets.US_ASCII);
            for (int i = 0; i < size; i++) {
                byte value = (i >= valueBytes.length) ? (byte) ' ' : valueBytes[i];
                patchData[offset + i] = value;
            }
        }
    }

    /**
     * Extract SysEx payloads from *.syx or *.mid file
     */
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
            boolean inSysEx = false;
            for (int i = 0; i < content.length; i++) {
                int value = content[i] & 0xFF;
                if (value == 0xF0) {
                    inSysEx = true;
                    out.reset();
                }
                if (inSysEx) {
                    out.write(value);
                }
                if (value == 0xF7) {
                    inSysEx = false;
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
        List<byte[]> sysExMessages = new ArrayList<>();

        Sequence sequence = MidiSystem.getSequence(midiFile);
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof SysexMessage sysEx) {
                    byte[] fullSysExMessage = sysEx.getMessage();
                    sysExMessages.add(fullSysExMessage);
                }
            }
        }
        return sysExMessages;
    }

    private String getBankName(File file) {
        String name = file.getName()
                .substring(0, file.getName()
                        .lastIndexOf('.'));
        return name.substring(0, 1)
                .toUpperCase() + name.substring(1);
    }

    private ExtractedFields getFields(File sysexFile, OverrideContext ctx, int presetId, byte[] content) {
        final MidiDeviceDefinition device = ctx.getDevice();
        final MidiDeviceMode midiDeviceMode = ctx.getMode();
        ExtractedFields extractedFields = new ExtractedFields();
        if (midiDeviceMode.getQueryName() != null) {
            var sequences = CommandCall.parse(device.getDefinitionFile(), device, midiDeviceMode.getQueryName())
                    .stream()
                    .map(commandCall -> midiDeviceRequester.forgeMidiRequestSequence(device, commandCall))
                    .toList();
            var mapper = sequences.getFirst()
                    .getMidiRequests()
                    .getFirst()
                    .getMapper();
            try {
                final CustomMidiEvent event = new CustomMidiEvent(new SysexMessage(content, content.length));
                mapper.extract(midiDeviceMode, extractedFields, event);
            } catch (InvalidMidiDataException e) {
                log.error("Unable to forge SysEx from patch {} in file {}", presetId, sysexFile);
            }
        }
        return extractedFields;
    }
}
