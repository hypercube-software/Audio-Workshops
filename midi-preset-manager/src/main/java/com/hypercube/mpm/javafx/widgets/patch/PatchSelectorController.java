package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.javafx.event.ScoreChangedEvent;
import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j

public class PatchSelectorController extends Controller<PatchSelector, ObservableMainModel> implements Initializable {

    @FXML
    ListView patchList;
    @FXML
    TextField searchBox;
    @FXML
    PatchScore scoreFilter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
        patchList.setCellFactory(new Callback<ListView<String>, PatchListCell>() {
            @Override
            public PatchListCell call(ListView listView) {
                return new PatchListCell();
            }
        });
        ObservableValue<List<Patch>> patchesProperty = resolvePath("controller.model.root.patchesProperty");
        SimpleStringProperty currentPatchNameFilterProperty = resolvePath("controller.model.root.currentPatchNameFilterProperty");
        SimpleIntegerProperty currentPatchIndexProperty = resolvePath("controller.model.root.currentPatchIndexProperty");
        patchList.itemsProperty()
                .bind(patchesProperty);
        searchBox.textProperty()
                .bind(currentPatchNameFilterProperty);
        searchBox.setOnAction(this::onSearch);
        currentPatchIndexProperty.addListener(this::onModelSelectedPatchIndexeChange);
        addEventListener(ScoreChangedEvent.class, this::onScoreChangedEventChanged);
    }

    private void onModelSelectedPatchIndexeChange(ObservableValue<? extends Number> integerProperty, Number oldValue, Number newValue) {
        log.info("onModelSelectedPatchIndexeChange {} for Patches", integerProperty);
        int value = newValue.intValue();
        if (value == -1) {
            patchList.getSelectionModel()
                    .clearSelection();
        } else {
            patchList.getSelectionModel()
                    .selectIndices(value);
            patchList.scrollTo(value);
        }
    }

    private void onScoreChangedEventChanged(ScoreChangedEvent scoreChangedEvent) {
        getModel().getRoot()
                .setCurrentPatchScoreFilter(scoreChangedEvent.getScore());
        refreshSearch();
    }

    private void onSearch(ActionEvent actionEvent) {
        getModel().getRoot()
                .setCurrentPatchNameFilter(searchBox.getText());
        refreshSearch();
    }

    private void refreshSearch() {
        fireEvent(SearchPatchesEvent.class, searchBox.getText(), getSearchScore());
    }

    private int getSearchScore() {
        return Integer.parseInt(scoreFilter.getScore());
    }

    public void onMouseClicked(Event event) {
        onSelectedItemChange();
    }

    public void onKeyReleased(Event event) {
        onSelectedItemChange();
    }

    private void onSelectedItemChange() {
        List<Integer> indexes = patchList.getSelectionModel()
                .getSelectedIndices()
                .stream()
                .toList();

        log.info("Selected item:" + indexes.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, "controller.model.root.patches", indexes);
    }
}
