package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.Observable;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class AttributeSelectorController extends Controller<AttributeSelector, ObservableMainModel> implements Initializable {
    @FXML
    Label label;

    @FXML
    ListView attributes;

    @Override
    public void onPropertyChange(View<?> widget, String property, ObservableValue<? extends String> observable, String oldValue, String newValue) {
        super.onPropertyChange(widget, property, observable, oldValue, newValue);
        if (property.equals("dataSource")) {
            // The dataSource is changed, we bound the corresponding property to the ListView
            SimpleListProperty<String> prop = (SimpleListProperty<String>) resolvePath(newValue + "Property");
            attributes.itemsProperty()
                    .bind(prop);
        } else if (property.equals("allowMultiSelection")) {
            attributes.getSelectionModel()
                    .setSelectionMode(Boolean.parseBoolean(newValue) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
        attributes.getSelectionModel()
                .getSelectedIndices()
                .addListener(this::onSelectedItemChange);
        label.textProperty()
                .bind(getView().titleProperty());
    }

    private void onSelectedItemChange(Observable observable) {
        List indexes = attributes.getSelectionModel()
                .getSelectedIndices()
                .stream()
                .toList();

        log.info("Selected item:" + indexes.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, getView().dataSourceProperty()
                .get(), indexes);
    }
}
