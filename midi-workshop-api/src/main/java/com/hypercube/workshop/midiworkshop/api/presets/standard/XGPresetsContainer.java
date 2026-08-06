package com.hypercube.workshop.midiworkshop.api.presets.standard;

/**
 * Container for Yamaha XG presets aka Extended General Midi presets
 */
public class XGPresetsContainer extends StandardPresetsContainer {

    public XGPresetsContainer() {
        super("xg/XGBanks.txt", "xg/XGPatches.txt");
    }
}
