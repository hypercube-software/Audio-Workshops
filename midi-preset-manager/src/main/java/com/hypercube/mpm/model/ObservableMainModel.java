package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.ObservableModel;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import lombok.Getter;

public class ObservableMainModel extends ObservableModel<MainModel> {
    @Getter
    private static ObservableMainModel getInstance = new ObservableMainModel();

    private ObservableMainModel() {

        setRoot(initModel());
    }

    private MainModel initModel() {
        var model = new MainModel();
        MidiDeviceLibrary midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(this.getClass()));
        model.setDevices(midiDeviceLibrary.getDevices()
                .values()
                .stream()
                .map(d -> d.getDeviceName())
                .toList());
        return model;
    }

}
