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
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.view.lists.ListHelper;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@JavaFXSpringController
@SuppressWarnings({"rawtypes", "unused", "unchecked"})
public class PatchSelectorController extends Controller<PatchSelector, MainModel> {
    @Autowired
    PatchesManager patchesManager;
    @Autowired
    DeviceToolBox deviceToolBox;

    @FXML
    TableView<Patch> patchList;
    @FXML
    TextField searchBox;
    @FXML
    PatchScore scoreFilter;
    @FXML
    TableColumn<Patch, String> colName;
    @FXML
    TableColumn<Patch, String> colMode;
    @FXML
    TableColumn<Patch, String> colBank;
    @FXML
    TableColumn<Patch, String> colCategory;
    @FXML
    TableColumn<Patch, Patch> colScore;
    @FXML
    TableColumn<Patch, String> colCommand;
    SimpleStringProperty currentPatchNameFilterProperty;

    @Override
    @SuppressWarnings("unchecked")
    public void onViewLoaded() {
        setModel(MainModel.getObservableInstance());

        ListHelper.configureColumn(colName, "name");
        ListHelper.configureColumn(colMode, "mode");
        ListHelper.configureColumn(colBank, "bank");
        ListHelper.configureColumn(colCategory, "category");
        ListHelper.configureColumn(colCommand, "command");

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


        // current patch update:
        // one listener on the model
        bindingManager.observePath("model.currentDeviceState.currentPatch", this::onSelectedPatchChanged);
        // one listener on the user interaction
        ListHelper.addSelectionChangeByUserListener(patchList, this::onSelectedItemChangeByUser);


        bindingManager.observePath("model.currentDeviceState.currentSearchOutput", this::onSearchOutputChanged);
        bindingManager.observePath("model.modeCategories", this::onModeCategoriesChanged);
        ObservableValue<?> currentPatchProperty = bindingManager.resolvePropertyPath("model.modeCategories");
        currentPatchProperty.addListener(this::onModeCategoriesChanged);

        currentPatchNameFilterProperty = resolvePath("model.currentPatchNameFilter");
        searchBox.textProperty()
                .bindBidirectional(currentPatchNameFilterProperty);

        addEventListener(ScoreChangedEvent.class, this::onScoreChangedEventChanged);

    }

    private void onModeCategoriesChanged(Observable observable) {
        String[] array = getModel().getModeCategories()
                .stream()
                .map(MidiPresetCategory::name)
                .toArray(String[]::new);
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(array));
    }

    private void onSearchOutputChanged(Observable observable) {
        ObservableList<Patch> list = Optional.ofNullable((ObservableList<Patch>) ((SelectBinding.AsObject<?>) observable).get())
                .orElse(new SimpleListProperty<>());
        log.info("SearchOutput updated with {} patches", list.size());
        patchList.setItems(list);
        // since the list is updated, try to update the selection
        ObservableValue<Patch> currentPatchProperty = bindingManager.resolvePropertyPath("model.currentDeviceState.currentPatch");
        if (currentPatchProperty != null) {
            onSelectedPatchChanged(currentPatchProperty);
        }
    }

    /**
     * Update the view selection, when the model change by user interaction or programmatically
     */
    private void onSelectedPatchChanged(Observable observable) {
        ObservableValue<? extends Patch> patchProperty = (ObservableValue<? extends Patch>) observable;
        log.info("onSelectedPatchChange {} for Patches", patchProperty);
        Patch newValue = patchProperty.getValue();
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

    private void onSelectedItemChangeByUser(boolean userAction, Patch oldValue, Patch newValue) {
        if (userAction) {
            Patch patch = getSelectedPatch();
            String name = patch != null ? patch.getName() : "<none>";
            log.info("Item selected by user: {}", name);
            List<Patch> selection = patch == null ? List.of() : List.of(patch);
            List<Integer> selectedIndexes = getSelectedPatchIndex();
            fireEvent(SelectionChangedEvent.class, WidgetIdentifiers.WIDGET_ID_PATCH, selectedIndexes, selection);
        }
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


    private List<Integer> getSelectedPatchIndex() {
        return new ArrayList(patchList.getSelectionModel()
                .getSelectedIndices());
    }

    private Patch getSelectedPatch() {
        return (Patch) patchList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .findFirst()
                .orElse(null);
    }

    @FXML
    void onRestoreSysEx(ActionEvent actionEvent) {
        deviceToolBox.restoreSysEx(getView().getScene());
    }

    @FXML
    void onSaveSysEx(ActionEvent actionEvent) {
        deviceToolBox.updateOrCreatePatch(getSelectedPatch());
    }

    @FXML
    void onDeletePatch(ActionEvent actionEvent) {
        Optional.ofNullable(getModel().getCurrentDeviceState()
                        .getCurrentPatch())
                .ifPresent(patch -> deviceToolBox.deleteCustomPatch(patch));
    }
}
