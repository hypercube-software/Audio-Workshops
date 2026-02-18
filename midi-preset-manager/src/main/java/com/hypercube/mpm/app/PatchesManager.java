package com.hypercube.mpm.app;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible to send patch changes to the device and handle favorites (when the user put a score on a patch)
 */
@Service
@Slf4j
public class PatchesManager {
    private final MainModel model;
    private final ConfigurationFactory configurationFactory;

    public PatchesManager(ConfigurationFactory configurationFactory) {
        this.configurationFactory = configurationFactory;
        this.model = MainModel.getObservableInstance();
    }

    public List<Integer> forgeCommand(MidiBankFormat presetFormat, String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return Collections.emptyList();
        }

        if (hexString.length() % 2 != 0) {
            log.warn("Can't forge bytes due to impair length: '{}'", hexString);
            return Collections.emptyList();
        }

        List<Integer> byteValues = new ArrayList<>();
        for (int i = 0; i < hexString.length(); i += 2) {
            String byteStr = hexString.substring(i, i + 2);
            int byteValue = Integer.parseInt(byteStr, 16);
            byteValues.add(byteValue);
        }
        int expectedSize = switch (presetFormat) {
            case NO_BANK_PRG -> 1;
            case BANK_MSB_PRG, BANK_LSB_PRG, BANK_PRG_PRG -> 2;
            case BANK_MSB_LSB_PRG -> 3;
        };
        if (byteValues.size() != expectedSize) {
            throw new MidiConfigError("Unexpected command size given preset format '%s' for patch '%s', expected %d digits, got %d".formatted(presetFormat, hexString, expectedSize, byteValues.size()));
        }
        return byteValues;
    }

    /**
     * Update the view regarding the list of patches matching the current filters
     * <p>This operation is not trivial, it is not just a search, because we
     * need to merge 2 patches lists in one:</p>
     * <ul>
     *     <li>The list of patches from the current device mode. Those don't have a score</li>
     *     <li>The list of favorite patches from the user, with a given score on each of them</li>
     * </ul>
     * <p>This method also update the info bar on the bottom of the UI</p>
     */
    public void refreshPatches() {
        var cfg = configurationFactory.getProjectConfiguration();
        if (model.getCurrentDeviceState() != null) {
            if (model.getCurrentDeviceState()
                    .getId()
                    .getName() == null)
                return;
            var device = cfg.getMidiDeviceLibrary()
                    .getDevice(model.getCurrentDeviceState()
                            .getId()
                            .getName())
                    .orElseThrow();
            int channel = model.getCurrentDeviceState()
                    .getId()
                    .getChannel();
            log.info("Search patches for device '{}' and channel {}", device.getDeviceName(), channel);
            List<Patch> patches = List.of();

            String currentModeName = model.getCurrentDeviceState()
                    .getId()
                    .getMode();
            if (currentModeName != null) {
                log.info("Search patches for mode {} on channel {}", currentModeName, channel);
                // Note: it is possible that selectedBankNames is not yet updated at this point
                // so midiDeviceMode will be null because it points to the previous selected device
                List<String> selectedBankNames = model.getCurrentDeviceState()
                        .getSelectedBankNames();
                MidiDeviceMode midiDeviceMode = device.getDeviceModes()
                        .get(currentModeName);
                if (midiDeviceMode != null) {
                    // Patches are stored by banks as String to keep space, we call them "presets"
                    // Once filtered, we convert string presets to a class Patch with score 0
                    // Then the score is updated configurationFactory.getFavorite()
                    // Finally they are sorted by name
                    List<MidiDeviceBank> banksForChannel = midiDeviceMode
                            .getBanksForChannel(channel);
                    log.info("Scan patches and filter patches from {} banks...", banksForChannel.size());
                    patches = banksForChannel
                            .parallelStream()
                            .filter(bank -> selectedBankNames.isEmpty() || selectedBankNames
                                    .contains(bank.getName()))
                            .flatMap(bank -> bank.getPresets()
                                    .stream()
                                    .filter(preset -> patchCategoryMatches(preset) && patchNameMatches(preset))
                                    .map(preset ->
                                            configurationFactory.getScoredPatchFromFavorite(forgePatch(device.getDeviceName(), currentModeName, channel, bank.getName(), preset)))
                                    .filter(this::patchScoreMatches))
                            .sorted(Comparator.comparing(Patch::getName))
                            .toList();
                }
            }
            // Update the info bar
            model.setInfo("%s | MIDI OUT '%s' | %d patches".formatted(device.getDeviceName(), device.getOutputMidiDevice(), patches.size()));
            // store the search output in the current observable state
            model.getCurrentDeviceState()
                    .setCurrentSearchOutput(patches);
        } else {
            log.warn("Nothing to search, currentDeviceState is null");
        }
    }

    public void onPatchScoreChanged(Patch patch) {
        configurationFactory.updateFavorites(patch);
    }

    /**
     * Save the user selection to restore it when the application start
     */
    public void saveSelectedPatchToConfig(Patch selectedPatch) {
        var cfg = configurationFactory.getProjectConfiguration();
        var stateId = selectedPatch != null ? selectedPatch.getDeviceStateId() : model.getCurrentDeviceState()
                .getId();
        final List<Patch> list;
        list = cfg.getSelectedPatches()
                .stream()
                .filter(sp -> !sp.getDeviceStateId()
                        .equals(stateId))
                .collect(Collectors.toList());
        if (selectedPatch != null) {
            list.add(selectedPatch);
        }
        cfg.setSelectedPatches(list);
        configurationFactory.saveConfig();
    }

    /**
     * Called when the user select a patch. Update the device accordingly via MIDI
     *
     * @param selectedPatch patch selected by the user
     */
    public void setCurrentPatch(Patch selectedPatch) {
        model.getCurrentDeviceState()
                .setCurrentPatch(selectedPatch);
        saveSelectedPatchToConfig(selectedPatch);
        sendPatchToDevice(selectedPatch);
    }

    public void sendPatchToDevice(Patch selectedPatch) {
        if (selectedPatch == null) {
            return;
        }
        var cfg = configurationFactory.getProjectConfiguration();
        var device = cfg.getMidiDeviceLibrary()
                .getDevice(selectedPatch.getDevice())
                .orElseThrow();
        log.info("Send patch '{}' to '{}' on channel {} via MIDI port '{}'", selectedPatch.getName(),
                selectedPatch.getDevice(), selectedPatch.getChannel(),
                device.getOutputMidiDevice());
        MidiOutDevice port = model.getCurrentDeviceState()
                .getMidiOutDevice();
        if (port == null) {
            log.info("Port not open: {}", device.getOutputMidiDevice());
            return;
        }
        if (selectedPatch.getFilename() != null) {
            File filename = new File(device.getDefinitionFile()
                    .getParent(), "%s/%s/%s/%s".formatted(selectedPatch.getDevice(), selectedPatch.getMode(), selectedPatch.getBank(), selectedPatch.getFilename()));
            if (filename.exists()) {
                log.info(filename.getAbsolutePath());
                MidiPreset midiPreset = MidiPresetBuilder.fromSysExFile(selectedPatch.getMode(), selectedPatch.getBank(), filename);
                selectEditBuffer(selectedPatch, device, port);
                port.sendPresetChange(midiPreset);
            } else {
                log.error("Patch file no longer exists: {}", filename.getAbsolutePath());
            }
        } else {
            MidiPreset midiPreset = MidiPresetBuilder.parse(device.getDefinitionFile(), selectedPatch.getChannel(),
                    device.getPresetFormat(),
                    selectedPatch.getName(),
                    device.getMacros(),
                    List.of(selectedPatch.getCommand()), List.of(MidiPreset.NO_CC), null);
            port.sendPresetChange(midiPreset);
        }
    }

    public void changePatchCategory(Patch patch, String newCategory) {
        var cfg = configurationFactory.getProjectConfiguration();
        var def = patch.getDefinitionFile();
        if (def == null) {
            return;
        }
        log.info("Update patch category '%s' in %s".formatted(newCategory, def.toString()));
        patch.setCategory(newCategory);
        if (def
                .endsWith(".yaml") || def
                .endsWith(".yml")) {
            MidiDevicePreset preset = cfg.getMidiDeviceLibrary()
                    .getDevice(patch.getDevice())
                    .flatMap(device -> device.getBank(patch.getBank()))
                    .flatMap(bank -> bank.getPresets()
                            .stream()
                            .filter(p -> p.command()
                                    .equals(patch.getCommand()))
                            .findFirst())
                    .get();
            preset.category(patch.getCategory());
            String prefix = "%s | ".formatted(patch.getCommand());
            String suffix = " | %s".formatted(patch.getName());
            try {
                Path definitionPath = Path.of(def);
                List<String> lines = Files.readAllLines(definitionPath);
                for (int i = 0; i < lines.size(); i++) {
                    String l = lines.get(i);
                    int idx1 = l.indexOf(prefix);
                    int idx2 = l.indexOf(suffix);
                    if (idx1 != -1 && idx2 != 2) {
                        log.info(l);
                        idx1 += prefix.length();
                        String newLine = l.substring(0, idx1) + patch.getCategory() + l.substring(idx2);
                        log.info(newLine);
                        lines.set(i, newLine);
                    }
                }
                Files.write(definitionPath, lines);
            } catch (IOException e) {
                throw new ApplicationError(e);
            }
        } else if (def
                .endsWith(".syx")) {
            MidiDevicePreset preset = cfg.getMidiDeviceLibrary()
                    .getDevice(patch.getDevice())
                    .flatMap(device -> device.getBank(patch.getBank()))
                    .flatMap(bank -> bank.getPresets()
                            .stream()
                            .filter(p -> p.filename()
                                    .equals(patch.getFilename()))
                            .findFirst())
                    .get();
            preset.category(patch.getCategory());
            String prefix = "%s,".formatted(patch.getCommand());
            String suffix = "] %s".formatted(patch.getName());
            int idx1 = def.indexOf(prefix);
            int idx2 = def.indexOf(suffix);
            if (idx1 != -1 && idx2 != 2) {
                idx1 += prefix.length();
                String filePath = def.substring(0, idx1) + patch.getCategory() + def.substring(idx2);
                File actual = new File(def);
                File newFile = new File(filePath);
                log.info("rename file to {}", filePath);
                if (actual.renameTo(newFile)) {
                    preset.definitionFile(newFile);
                    preset.filename(filePath);
                    patch.setDefinitionFile(filePath);
                    patch.setFilename(filePath);
                } else {
                    log.error("Unable to rename !");
                }
            }
        }
    }

    private Patch forgePatch(String deviceName, String currentModeName, int channel, String bankName, MidiDevicePreset preset) {
        return new Patch(preset.definitionFile()
                .getAbsolutePath(), deviceName, currentModeName, bankName, preset.name(), preset.category(), preset.command(), preset.filename(), channel, 0);
    }

    private boolean patchScoreMatches(Patch patch) {
        return patch.getScore() >= model.getCurrentPatchScoreFilter();
    }

    private boolean patchNameMatches(MidiDevicePreset preset) {
        return model.getCurrentPatchNameFilter() == null || preset.name()
                .contains(model.getCurrentPatchNameFilter());
    }

    private boolean patchCategoryMatches(MidiDevicePreset preset) {
        return model.getCurrentDeviceState()
                .getCurrentSelectedCategories()
                .isEmpty() ||
                model.getCurrentDeviceState()
                        .getCurrentSelectedCategories()
                        .stream()
                        .anyMatch(c -> c.name()
                                .equals(preset.category()));
    }

    /**
     * Some devices need to first select a factory patch before sending a SysEx Edit Buffer update
     */
    private void selectEditBuffer(Patch selectedPatch, MidiDeviceDefinition device, MidiOutDevice port) {
        if (selectedPatch.getCommand() != null) {
            List<Integer> digits = forgeCommand(device.getPresetFormat(), selectedPatch.getCommand());
            int channel = selectedPatch.getChannel();
            log.info("Select Edit Buffer on channel %d with preset format %s: %s".formatted(channel, device.getPresetFormat(), digits.stream()
                    .map("$%02X"::formatted)
                    .collect(Collectors.joining(","))));
            switch (device.getPresetFormat()) {
                case NO_BANK_PRG -> port.sendProgramChange(channel, digits.getLast());
                case BANK_MSB_PRG -> {
                    port.sendBankMSB(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_LSB_PRG -> {
                    port.sendBankLSB(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_MSB_LSB_PRG -> {
                    port.sendBankMSB(channel, digits.getFirst());
                    port.sendBankLSB(channel, digits.get(1));
                    port.sendProgramChange(channel, digits.getLast());
                }
                case BANK_PRG_PRG -> {
                    port.sendProgramChange(channel, digits.getFirst());
                    port.sendProgramChange(channel, digits.getLast());
                }
            }
        }
    }
}
