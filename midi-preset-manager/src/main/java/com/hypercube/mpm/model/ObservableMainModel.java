package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.ObservableModel;
import lombok.Getter;

public class ObservableMainModel extends ObservableModel<MainModel> {
    @Getter
    private static ObservableMainModel getInstance = new ObservableMainModel();

    private ObservableMainModel() {
        setRoot(new MainModel());
    }

}
