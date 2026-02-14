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
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    CheckBox unpackCheckBox;
    @FXML
    Label responseLabel;

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
                    GenericDialogController.error("Response not saved", e.getMessage());
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
            getSelectedDevice()
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

    private Optional<MidiDeviceDefinition> getSelectedDevice() {
        return Optional.ofNullable(deviceSelector.getSelectionModel()
                .getSelectedItem());
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
        ManufacturerSysExParser sysExParser = new ManufacturerSysExParser();
        MidiDeviceDefinition device = getSelectedDevice().get();
        byte[] unpackedResponse = unpackCheckBox.isSelected() ? sysExParser.unpackMidiBuffer(device, response) : response;
        this.response = new DataViewerPayload(unpackedResponse);
        runOnJavaFXThread(() -> {
            copyResponseInClipboard(unpackedResponse);
            updatePackedZone(device, unpackedResponse);
            updateResponseLabel(device);
            hexResponse.setData(this.response);
        });
    }

    private void copyResponseInClipboard(byte[] response) {
        String value = IntStream.range(0, response.length)
                .mapToObj(i -> String.format("%02X", response[i]))
                .collect(Collectors.joining(" "));
        String chars = IntStream.range(0, response.length)
                .mapToObj(i -> {
                    int b = response[i] & 0xFF;
                    return (b >= 32 && b <= 126) ? String.valueOf((char) b) : " ";
                })
                .collect(Collectors.joining("  "));
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(value + "\n" + chars);
        clipboard.setContent(content);
    }

    private void updatePackedZone(MidiDeviceDefinition device, byte[] unpackedResponse) {
        if (!unpackCheckBox.isSelected() && device.getDecodingKey() != null) {
            hexResponse.setUnpackStart(device.getDecodingKey()
                    .getStart());
            hexResponse.setUnpackEnd(unpackedResponse.length - 1 - device.getDecodingKey()
                    .getEnd());
        } else {
            hexResponse.setUnpackStart(-1);
            hexResponse.setUnpackEnd(-1);
        }
    }

    private void updateResponseLabel(MidiDeviceDefinition device) {
        if (device.getDecodingKey() == null) {
            responseLabel.setText("Response:");
        } else if (unpackCheckBox.isSelected()) {
            responseLabel.setText("Unpacked Response:");
        } else {
            responseLabel.setText("Packed Response:");
        }
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
