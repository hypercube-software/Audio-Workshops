package com.hypercube.mpm.javafx.widgets.dialog.ports;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialog;
import com.hypercube.util.javafx.controller.DialogController;
import com.hypercube.util.javafx.controller.DialogIcon;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
public class DevicesPortsDialogController extends DialogController<DevicesPortsDialog, Void> {
    @Autowired
    ProjectConfiguration cfg;

    @FXML
    TableView<MidiDeviceDefinition> deviceList;

    @FXML
    TableColumn<MidiDeviceDefinition, String> colName;
    @FXML
    TableColumn<MidiDeviceDefinition, String> colBrand;
    @FXML
    TableColumn<MidiDeviceDefinition, String> colMidiIn;
    @FXML
    TableColumn<MidiDeviceDefinition, String> colMidiOut;
    @FXML
    TableColumn<MidiDeviceDefinition, String> colDAWOut;

    List<MidiDeviceDefinition> devices;

    @FXML
    public void onSaveButton(ActionEvent event) {
        validate();
        save();
        close();
    }

    @FXML
    public void onCancelButton(ActionEvent event) {
        validate();
        close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
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
        setUpColumn(colName, null);
        setUpColumn(colBrand, null);
        setUpColumn(colMidiIn, midiInPorts);
        setUpColumn(colMidiOut, midiOutPorts);
        setUpColumn(colDAWOut, midiInPorts);

        devices = cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .sorted(Comparator.comparing(MidiDeviceDefinition::getBrand, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MidiDeviceDefinition::getDeviceName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        deviceList.setItems(FXCollections.observableArrayList(devices));
    }

    private void save() {
        devices.stream()
                .filter(d -> d.getInputMidiDevice() != null || d.getOutputMidiDevice() != null || d.getDawMidiDevice() != null)
                .forEach(this::saveUserDevice);
    }

    private void saveUserDevice(MidiDeviceDefinition midiDeviceDefinition) {
        File definitionFile = midiDeviceDefinition.getDefinitionFile();
        File userFile = new File(definitionFile.getAbsolutePath()
                .replace(".yml", "-user.yml"));
        if (!userFile.exists()) {
            try (PrintWriter out = new PrintWriter(userFile)) {
                out.println("deviceName: \"%s\"".formatted(midiDeviceDefinition.getDeviceName()));
                out.println("brand: \"%s\"".formatted(midiDeviceDefinition.getBrand()));
                out.println("#");
                out.println("# MIDI port used to receive messages from the device");
                out.println("#");
                if (midiDeviceDefinition.getInputMidiDevice() != null) {
                    out.println("inputMidiDevice: \"%s\"".formatted(midiDeviceDefinition.getInputMidiDevice()));
                } else {
                    out.println("inputMidiDevice: null");
                }
                out.println("#");
                out.println("# MIDI port used to send messages to the device");
                out.println("#");
                if (midiDeviceDefinition.getOutputMidiDevice() != null) {
                    out.println("outputMidiDevice: \"%s\"".formatted(midiDeviceDefinition.getOutputMidiDevice()));
                } else {
                    out.println("outputMidiDevice: null");
                }
                out.println("#");
                out.println("# Virtual MIDI port used to convert back 8 bit CC from DAW to 16 CC to the device");
                out.println("#");
                if (midiDeviceDefinition.getDawMidiDevice() != null) {
                    out.println("dawMidiDevice: \"%s\"".formatted(midiDeviceDefinition.getDawMidiDevice()));
                } else {
                    out.println("dawMidiDevice: null");
                }
            } catch (FileNotFoundException e) {
                throw new ApplicationError(e);
            }
        }
    }

    private boolean validate() {
        var undefinedDevices = devices.stream()
                .filter(d ->
                        (d.getInputMidiDevice() != null && cfg.getMidiPortsManager()
                                .getInput(d.getInputMidiDevice())
                                .isEmpty())
                                ||
                                (d.getOutputMidiDevice() != null && cfg.getMidiPortsManager()
                                        .getOutput(d.getOutputMidiDevice())
                                        .isEmpty())
                )
                .toList();
        if (!undefinedDevices.isEmpty()) {
            var dlg = DialogController.buildDialog(GenericDialog.class, JavaFXApplication.getMainStage(), DialogIcon.WARNING, true);
            dlg.updateText("Configuration issue", """
                    Some devices use unknown Midi ports.
                    This mean you didn't switch on those before starting this application.
                    You need to click on menu "Rescan MIDI Ports" to use them.
                    """);
            dlg.showAndWait();
            return false;
        } else {
            return true;
        }
    }

    private void onColumnEditCommit(TableColumn.CellEditEvent<MidiDeviceDefinition, String> event) {
        MidiDeviceDefinition device = event.getTableView()
                .getItems()
                .get(event.getTablePosition()
                        .getRow());
        String fieldName = (String) event.getTableColumn()
                .getUserData();
        String newValue = event.getNewValue();
        if (newValue.equals("<EMPTY>")) {
            newValue = null;
        }
        switch (fieldName) {
            case "inputMidiDevice" -> device.setInputMidiDevice(newValue);
            case "outputMidiDevice" -> device.setOutputMidiDevice(newValue);
            case "dawMidiDevice" -> device.setDawMidiDevice(newValue);
        }
    }

    private void setUpColumn(TableColumn<MidiDeviceDefinition, String> column, List<String> values) {
        String fieldName = (String) column.getUserData();
        column.setCellValueFactory(new PropertyValueFactory<MidiDeviceDefinition, String>(fieldName));
        if (values != null) {
            ArrayList<String> viewValues = new ArrayList<>(values);
            viewValues.add(0, "<EMPTY>");
            ObservableList<String> observableValues = FXCollections.observableArrayList(viewValues);
            column.setCellFactory(ComboBoxTableCell.forTableColumn(observableValues));
            column.setOnEditCommit(this::onColumnEditCommit);
        }
    }
}
