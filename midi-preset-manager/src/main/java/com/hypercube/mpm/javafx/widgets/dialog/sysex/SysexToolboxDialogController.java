package com.hypercube.mpm.javafx.widgets.dialog.sysex;

import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.javafx.widgets.hexa.DataViewerPayload;
import com.hypercube.mpm.javafx.widgets.hexa.HexaDataViewer;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class SysexToolboxDialogController extends DialogController<SysexToolboxDialog, Void> {
    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    DeviceToolBox deviceToolBox;

    List<MidiDeviceDefinition> devices;
    @FXML
    ComboBox<MidiDeviceDefinition> deviceSelector;
    @FXML
    TextField textCommand;

    @FXML
    private HexaDataViewer hexResponse;
    @FXML
    private HexaDataViewer hexCommand;

    @FXML
    public void onSaveButton(ActionEvent event) {
        close();
    }

    @FXML
    public void onCancelButton(ActionEvent event) {
        close();
    }

    @FXML
    public void onCommandKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            onSendCommand(null);
        }
    }

    @FXML
    public void onSendCommand(ActionEvent event) {
        hexResponse.getCtrl()
                .clear();
        runLongTask(() -> {
            Optional.ofNullable(deviceSelector.getSelectionModel()
                            .getSelectedItem())
                    .flatMap(midiDeviceDefinition -> deviceToolBox.request(midiDeviceDefinition, textCommand.getText(), this::onDeviceRequest))
                    .ifPresent(response -> onDeviceResponse(response));
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        var cfg = configurationFactory.getProjectConfiguration();
        List<String> midiInPorts = cfg.getMidiPortsManager()
                .getInputs()
                .stream()
                .map(MidiInDevice::getName)
                .toList();
        List<String> midiOutPorts = cfg.getMidiPortsManager()
                .getOutputs()
                .stream()
                .map(MidiOutDevice::getName)
                .toList();

        devices = cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .filter(d -> d.getInputMidiDevice() != null && d.getOutputMidiDevice() != null)
                .sorted(Comparator.comparing(MidiDeviceDefinition::getBrand, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MidiDeviceDefinition::getDeviceName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        deviceSelector.setItems(FXCollections.observableArrayList(devices));
        if (MainModel.getObservableInstance()
                .getCurrentDeviceState() != null) {
            var name = MainModel.getObservableInstance()
                    .getCurrentDeviceState()
                    .getId()
                    .getName();
            devices.stream()
                    .filter(d -> d.getDeviceName()
                            .equals(name))
                    .findFirst()
                    .ifPresent(d -> deviceSelector.getSelectionModel()
                            .select(d));
        }

        deviceSelector.setConverter(new StringConverter<MidiDeviceDefinition>() {
            @Override
            public String toString(MidiDeviceDefinition midiDeviceDefinition) {
                return Optional.ofNullable(midiDeviceDefinition)
                        .map(midiDeviceDefinition1 -> "%s %s".formatted(midiDeviceDefinition.getBrand(), midiDeviceDefinition.getDeviceName()))
                        .orElse("");
            }

            @Override
            public MidiDeviceDefinition fromString(String s) {
                return null;
            }
        });
    }

    private void onDeviceRequest(byte[] request) {
        runOnJavaFXThread(() -> hexCommand.setData(new DataViewerPayload(request)));
    }

    private void onDeviceResponse(byte[] response) {
        runOnJavaFXThread(() -> hexResponse.setData(new DataViewerPayload(response)));
    }
}
