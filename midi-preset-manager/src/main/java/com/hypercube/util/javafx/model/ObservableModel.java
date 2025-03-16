package com.hypercube.util.javafx.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ObservableModel<M> {
    private ObjectProperty<M> root = new SimpleObjectProperty<>();

    public final M getRoot() {
        return root.get();
    }

    public final void setRoot(M value) {
        root.set(ModelHelper.forgeMMVM(value));
    }

    public ObjectProperty<M> rootProperty() {
        return root;
    }
}
