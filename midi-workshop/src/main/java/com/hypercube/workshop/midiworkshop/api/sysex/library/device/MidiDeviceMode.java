package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetNaming;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

@Getter
@Setter
public class MidiDeviceMode {
    /**
     * Name of the mode, typically "multi mode" or "voice mode"
     */
    private String name;
    /**
     * How to activate the mode, typically a macro name
     */
    private String command;
    /**
     * How to get the patch name for this mode, typically a macro name
     */
    private String queryName;
    /**
     * Optional macro to run before querying name (enter edit mode typically)
     */
    private String preQueryName;
    /**
     * Optional macro to run after querying name (exit edit mode typically)
     */
    private String postQueryName;
    /**
     * Override the default naming for the entire device
     */
    private MidiPresetNaming presetNaming;
    /**
     * Banks belonging to this mode
     */
    private Map<String, MidiDeviceBank> banks = new HashMap<>();
    /**
     * A specific mode can use its own categories (This can be found in Korg devices)
     */
    private List<MidiPresetCategory> categories = new ArrayList<>();
    /**
     * Contains a range specifying which midi channels can be used in this mode
     * <p>0-15</p>
     */
    private String midiChannels = "0-15";
    /**
     * Used for "multi" to select a single patch inside a specific channel
     * <p>Indicate which mode has to be used to provide the banks</p>
     */
    private MidiDeviceSubBanks subBanks;

    @Getter(AccessLevel.NONE)
    private Map<Integer, List<MidiDeviceBank>> banksMap;

    public Optional<MidiDeviceBank> getBank(String name) {
        return Optional.ofNullable(banks.get(name));
    }

    /**
     * Get mode channels accepting specific presets for this mode
     */
    public List<Integer> getChannels() {
        String[] v = midiChannels.split("-");
        int start = Integer.parseInt(v[0]);
        int end = v.length == 2 ? Integer.parseInt(v[1]) : start;
        return IntStream.rangeClosed(start, end)
                .boxed()
                .toList();
    }

    /**
     * Get all channels, include channels accepting multi and channels accepting presets
     */
    public List<Integer> getAllChannels() {
        List<Integer> modeChannels = getChannels();
        if (subBanks != null) {
            List<Integer> allChannels = new ArrayList<>(modeChannels);
            allChannels.addAll(subBanks.getChannels());
            return allChannels.stream()
                    .sorted()
                    .distinct()
                    .toList();
        } else {
            return modeChannels;
        }
    }

    public List<MidiDeviceBank> getBanksForChannel(int channel) {
        if (banksMap == null) {
            refreshBanksMap();
        }
        return banksMap.getOrDefault(channel, List.of());
    }

    /**
     * Called when the user add or remove a custom bank
     */
    public void refreshBanksMap() {
        banksMap = new HashMap<>();
        var modeBanks = banks.values()
                .stream()
                .toList();
        getChannels().forEach(ch -> banksMap.put(ch, modeBanks));
        if (subBanks != null) {
            var channelBank = subBanks.getMode()
                    .getBanks()
                    .values()
                    .stream()
                    .toList();
            subBanks.getChannels()
                    .forEach(ch -> banksMap.put(ch, channelBank));
        }
    }
}
