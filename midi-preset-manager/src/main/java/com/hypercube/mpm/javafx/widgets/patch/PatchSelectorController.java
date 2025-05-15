package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class PatchSelectorController extends Controller<PatchSelector, ObservableMainModel> implements Initializable {

    @FXML
    ListView patchList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        patchList.setCellFactory(new Callback<ListView<String>, PatchListCell>() {
            @Override
            public PatchListCell call(ListView listView) {
                return new PatchListCell();
            }
        });
        setModel(ObservableMainModel.getGetInstance());
    }
}
