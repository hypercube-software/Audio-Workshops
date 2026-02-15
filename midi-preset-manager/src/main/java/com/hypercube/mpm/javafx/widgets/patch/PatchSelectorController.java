package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.app.DeviceToolBox;
import com.hypercube.mpm.app.PatchesManager;
import com.hypercube.mpm.javafx.event.ScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.javafx.widgets.WidgetIdentifiers;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.sun.javafx.binding.SelectBinding;
import javafx.beans.Observable;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class PatchSelectorController extends Controller<PatchSelector, MainModel> implements Initializable {
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    DeviceToolBox deviceToolBox;

    @FXML
    TableView patchList;
    @FXML
    TextField searchBox;
    @FXML
    PatchScore scoreFilter;
    @FXML
    TableColumn colName;
    @FXML
    TableColumn colMode;
    @FXML
    TableColumn colBank;
    @FXML
    TableColumn<Patch, String> colCategory;
    @FXML
    TableColumn colScore;
    @FXML
    TableColumn colCommand;
    SimpleStringProperty currentPatchNameFilterProperty;
    // boolean used to distinguish user action and programmatic action
    private boolean userAction = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());

        colName.setCellValueFactory(new PropertyValueFactory<Patch, String>("name"));
        colMode.setCellValueFactory(new PropertyValueFactory<Patch, String>("mode"));
        colBank.setCellValueFactory(new PropertyValueFactory<Patch, String>("bank"));
        colCategory.setCellValueFactory(new PropertyValueFactory<Patch, String>("category"));
        colCategory.setEditable(true);
        colCategory.setOnEditCommit(cellEditEvent -> {
            Patch item = cellEditEvent.getRowValue();
            String newValue = cellEditEvent.getNewValue();
            if (!item.getCategory()
                    .equals(newValue)) {
                patchesManager.changePatchCategory(item, newValue);
            }
        });

        colScore.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Patch, Patch>, ObservableValue<Patch>>) patch -> new SimpleObjectProperty<>(patch.getValue()));
        colScore.setCellFactory((Callback<TableColumn<Patch, Patch>, TableCell<Patch, Patch>>) param -> new PatchListCell());
        colCommand.setCellValueFactory(new PropertyValueFactory<Patch, String>("command"));

        bindingManager.observePath("model.currentDeviceState.currentSearchOutput", this::onSearchOutputChanged);
        bindingManager.observePath("model.currentDeviceState.currentPatch", this::onSelectedPatchChanged);
        bindingManager.observePath("model.modeCategories", this::onModeCategoriesChanged);
        ObservableValue currentPatchProperty = bindingManager.resolvePropertyPath("model.modeCategories");
        currentPatchProperty.addListener(this::onModeCategoriesChanged);

        currentPatchNameFilterProperty = resolvePath("model.currentPatchNameFilter");
        searchBox.textProperty()
                .bindBidirectional(currentPatchNameFilterProperty);

        addEventListener(ScoreChangedEvent.class, this::onScoreChangedEventChanged);

        patchList.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            userAction = true;
        });
        patchList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            userAction = true;
        });
        patchList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (userAction) {
                        onSelectedItemChange();
                        userAction = false;
                    }
                });
    }

    private void onModeCategoriesChanged(Observable observable) {
        String[] array = getModel().getModeCategories()
                .stream()
                .map(MidiPresetCategory::name)
                .toArray(String[]::new);
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(array));
    }

    private void onSearchOutputChanged(Observable observable) {
        log.info("SearchOutput updated");
        ObservableList list = (ObservableList) ((SelectBinding.AsObject<?>) observable).get();
        patchList.setItems(list != null ? list : new SimpleListProperty());
        // since the list is updated, try to update the selection
        ObservableValue currentPatchProperty = bindingManager.resolvePropertyPath("model.currentDeviceState.currentPatch");
        if (currentPatchProperty != null) {
            onSelectedPatchChanged(currentPatchProperty);
        }
    }

    /**
     * Update the view selection, when the model change
     */
    private void onSelectedPatchChanged(Observable observable) {
        ObservableValue<? extends Patch> patchProperty = (ObservableValue<? extends Patch>) observable;
        log.info("onSelectedPatchChange {} for Patches", patchProperty);
        Patch newValue = patchProperty.getValue();
        // scrollTo have the tendency to put items on top, so we don't call it if the user just selected the item
        if (!userAction) {
            patchList.scrollTo(newValue);
        }
        // Tricky code: it is not advised to modify the selection during a selectionChanged event
        // so we use runLaterOnJavaFXThread
        runLaterOnJavaFXThread(() -> {
            var sm = patchList.getSelectionModel();
            Patch currentPatch = (Patch) sm.getSelectedItem();
            if (currentPatch == null || !currentPatch.equals(newValue)) {
                sm.clearSelection();
                if (newValue != null) {
                    sm.select(newValue);
                }
            }
        });
    }

    private void onScoreChangedEventChanged(ScoreChangedEvent scoreChangedEvent) {
        getModel().setCurrentPatchScoreFilter(scoreChangedEvent.getScore());
        refreshSearch();
    }

    @FXML
    private void onSearch(ActionEvent actionEvent) {
        refreshSearch();
    }

    private void refreshSearch() {
        fireEvent(SearchPatchesEvent.class, getModel().getCurrentPatchNameFilter(), getSearchScore());
    }

    private int getSearchScore() {
        return Integer.parseInt(scoreFilter.getScore());
    }

    private void onSelectedItemChange() {
        List<Patch> patches = patchList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .toList();

        log.info("Selected item:" + patches.stream()
                .map(Patch::getName)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, WidgetIdentifiers.WIDGET_ID_PATCH, List.of(), patches);
    }

    @FXML
    void onRestoreSysEx(ActionEvent actionEvent) {
        deviceToolBox.restoreSysEx(getView().getScene());
    }

    @FXML
    void onSaveSysEx(ActionEvent actionEvent) {
        deviceToolBox.updateOrCreatePatch();
    }

    @FXML
    void onDeletePatch(ActionEvent actionEvent) {
        Optional.ofNullable(getModel().getCurrentDeviceState()
                        .getCurrentPatch())
                .ifPresent(patch -> deviceToolBox.deleteCustomPatch(patch));
    }
}
