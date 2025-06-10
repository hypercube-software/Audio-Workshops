package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.javafx.event.ScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j

public class PatchSelectorController extends Controller<PatchSelector, MainModel> implements Initializable {

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
    TableColumn colCategory;
    @FXML
    TableColumn colScore;
    @FXML
    TableColumn colCommand;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());

        colName.setCellValueFactory(new PropertyValueFactory<Patch, String>("name"));
        colMode.setCellValueFactory(new PropertyValueFactory<Patch, String>("mode"));
        colBank.setCellValueFactory(new PropertyValueFactory<Patch, String>("bank"));
        colCategory.setCellValueFactory(new PropertyValueFactory<Patch, String>("category"));
        colScore.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Patch, Patch>, ObservableValue<Patch>>) patch -> new SimpleObjectProperty<>(patch.getValue()));
        colScore.setCellFactory((Callback<TableColumn<Patch, Patch>, TableCell<Patch, Patch>>) param -> new PatchListCell());
        colCommand.setCellValueFactory(new PropertyValueFactory<Patch, String>("command"));

        SimpleStringProperty currentPatchNameFilterProperty = resolvePath("controller.model.currentPatchNameFilterProperty");
        ObservableValue<List<Patch>> patchesProperty = resolvePath("controller.model.currentDeviceState.currentSearchOutputProperty");
        SimpleObjectProperty<Patch> currentPatchProperty = resolvePath("controller.model.currentDeviceState.currentPatchProperty");
        patchList.itemsProperty()
                .bind(patchesProperty);
        searchBox.textProperty()
                .bindBidirectional(currentPatchNameFilterProperty);
        searchBox.setOnAction(this::onSearch);
        currentPatchProperty.addListener(this::onSelectedPatchChange);
        addEventListener(ScoreChangedEvent.class, this::onScoreChangedEventChanged);
    }

    private void onSelectedPatchChange(ObservableValue<? extends Patch> patchProperty, Patch oldValue, Patch newValue) {
        log.info("onSelectedPatchChange {} for Patches", patchProperty);
        var sm = patchList.getSelectionModel();
        Patch currentPatch = (Patch) sm.getSelectedItem();
        if (currentPatch == null || !currentPatch.equals(newValue)) {
            sm.clearSelection();
            if (newValue != null) {
                sm.select(newValue);
                patchList.scrollTo(newValue);
            }
        }
    }

    private void onScoreChangedEventChanged(ScoreChangedEvent scoreChangedEvent) {
        getModel().setCurrentPatchScoreFilter(scoreChangedEvent.getScore());
        refreshSearch();
    }

    private void onSearch(ActionEvent actionEvent) {
        getModel().setCurrentPatchNameFilter(searchBox.getText());
        refreshSearch();
    }

    private void refreshSearch() {
        fireEvent(SearchPatchesEvent.class, searchBox.getText(), getSearchScore());
    }

    private int getSearchScore() {
        return Integer.parseInt(scoreFilter.getScore());
    }

    public void onMouseClicked(MouseEvent event) {
        onSelectedItemChange();
    }

    public void onKeyReleased(KeyEvent event) {
        onSelectedItemChange();
    }

    private void onSelectedItemChange() {
        List<Patch> patches = patchList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .toList();

        log.info("Selected item:" + patches.stream()
                .map(Patch::getName)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, "controller.model.patches", List.of(), patches);
    }
}
