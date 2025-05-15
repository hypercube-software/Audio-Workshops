package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.DeviceChangedEvent;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class AttributeSelectorController extends Controller<AttributeSelector, ObservableMainModel> implements Initializable {
    @FXML
    Button button;

    @FXML
    ListView attributes;

    @Override
    public void onPropertyChange(View<?> widget, String property, ObservableValue<? extends String> observable, String oldValue, String newValue) {
        super.onPropertyChange(widget, property, observable, oldValue, newValue);
        if (property.equals("dataSource")) {
            attributes.itemsProperty()
                    .setValue(resolvePath(newValue));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
        attributes.getSelectionModel()
                .selectedIndexProperty()
                .addListener(this::onSelectedItemChange);
    }

    private void onSelectedItemChange(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
        log.info("Selected item:" + newValue);
        fireEvent(DeviceChangedEvent.class, (int) newValue);
    }
}
