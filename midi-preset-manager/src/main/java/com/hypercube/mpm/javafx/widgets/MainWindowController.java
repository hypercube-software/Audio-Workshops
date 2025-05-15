package com.hypercube.mpm.javafx.widgets;

import com.hypercube.mpm.javafx.event.DeviceChangedEvent;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class MainWindowController extends Controller<MainWindow, Void> implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        addEventListener(DeviceChangedEvent.class, this::onDeviceChanged);
    }

    private void onDeviceChanged(DeviceChangedEvent deviceChangedEvent) {
        log.info("Changed ! " + deviceChangedEvent.getDeviceIndex());
    }
}
