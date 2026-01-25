package com.hypercube.mpm.javafx.widgets.dialog.sysex;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.app.RequestStatus;
import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class SysexToolboxDialogController extends DialogController<SysexToolboxDialog, Void> {
    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    DeviceToolBox deviceToolBox;
    @Autowired
    DeviceStateManager deviceStateManager;

    List<MidiDeviceDefinition> devices;
    DataViewerPayload response;

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
        if (response != null && response.data() != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save SysEx response");

            fileChooser.getExtensionFilters()
                    .addAll(
                            new FileChooser.ExtensionFilter("System Exclusive", "*.syx"),
                            new FileChooser.ExtensionFilter("All", "*.*")
                    );

            Window stage = deviceSelector.getScene()
                    .getWindow();

            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                try {
                    Files.write(file.toPath(), response.data());
                    GenericDialogController.info("Response Saved", """
                            The device response is successfully saved as SysEx file.
                            """);
                } catch (Exception e) {
                    GenericDialogController.error("Response nots saved", e.getMessage());
                }
            }
        }
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

        response = new DataViewerPayload(fakePayload());
        hexResponse.setData(response);
    }

    private void onDeviceRequest(RequestStatus status) {
        runOnJavaFXThread(() -> {
            if (status.errorMessage() != null) {
                getView().setErrorMessage(status.errorMessage());
            } else {
                getView().setErrorMessage("");
                hexCommand.setData(new DataViewerPayload(status.payload()));
            }
        });
    }

    private void onDeviceResponse(byte[] response) {
        this.response = new DataViewerPayload(response);
        runOnJavaFXThread(() -> hexResponse.setData(this.response));
    }

    @FXML
    void onReloadMidiDeviceLibrary() {
        deviceStateManager.reloadMidiDeviceLibrary();
    }

    byte[] fakePayload() {
        byte[] data = new byte[255];
        Random random = new Random();

        for (int i = 0; i < data.length; i++) {
            if (random.nextFloat() < 0.7) {
                data[i] = (byte) (random.nextInt(94) + 33);
            } else {
                data[i] = (byte) random.nextInt(256);
            }
        }
        return data;
    }
}
