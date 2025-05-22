package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.javafx.event.SearchPatchesEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
        patchList.setCellFactory(new Callback<ListView<String>, PatchListCell>() {
            @Override
            public PatchListCell call(ListView listView) {
                return new PatchListCell();
            }
        });
        ObservableValue<List<Patch>> patchesProperty = (ObservableValue<List<Patch>>) resolvePath("controller.model.root.patchesProperty");
        patchList.itemsProperty()
                .bind(patchesProperty);
        patchList.getSelectionModel()
                .getSelectedIndices()
                .addListener(this::onSelectedItemChange);
        searchBox.textProperty()
                .bindBidirectional((Property<String>) resolvePath("controller.model.root.currentPatchNameFilterProperty"));
        searchBox.setOnAction(this::onSearch);
    }

    private void onSearch(ActionEvent actionEvent) {
        getModel().getRoot()
                .setCurrentPatchNameFilter(searchBox.getText());
        fireEvent(SearchPatchesEvent.class, searchBox.getText());
    }

    private void onSelectedItemChange(Observable observable) {
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
