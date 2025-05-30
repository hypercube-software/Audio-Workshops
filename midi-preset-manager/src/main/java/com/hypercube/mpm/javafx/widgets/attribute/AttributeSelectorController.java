package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.ObservableMainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
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
            SimpleListProperty<String> prop = resolvePath(newValue + "Property");
            attributes.itemsProperty()
                    .bind(prop);
        } else if (property.equals("allowMultiSelection")) {
            attributes.getSelectionModel()
                    .setSelectionMode(Boolean.parseBoolean(newValue) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        } else if (property.equals("selectedIndexes")) {
            Property<?> selectedIndexesProperty = resolvePath(newValue + "Property");
            selectedIndexesProperty.addListener(this::onModelSelectedIndexesChange);
        } else if (property.equals("title")) {
            label.setText(newValue);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(ObservableMainModel.getGetInstance());
    }

    /**
     * Called when the model change, not the view. So when there is a programmatic update, not a user click
     *
     * @param observable
     */
    private void onModelSelectedIndexesChange(Observable observable) {
        log.info("onModelSelectedIndexesChange {} for {}", observable, getView().getTitle());
        List<Integer> selectedIndices = null;
        if (observable instanceof IntegerProperty integerProperty) {
            int value = integerProperty.getValue();
            selectedIndices = List.of(value)
                    .stream()
                    .filter(idx -> idx != -1)
                    .toList();
        } else if (observable instanceof StringProperty stringProperty) {
            String value = stringProperty.getValue();
            int valueIndex = attributes.getItems()
                    .indexOf(value);
            selectedIndices = List.of(valueIndex)
                    .stream()
                    .filter(idx -> idx != -1)
                    .toList();
        } else if (observable instanceof SimpleListProperty simpleListProperty) {
            ObservableList<Integer> value = simpleListProperty.getValue();
            selectedIndices = value.stream()
                    .toList();
        }
        if (selectedIndices != null) {
            if (selectedIndices.size() == 0) {
                attributes.getSelectionModel()
                        .clearSelection();
            } else {
                selectedIndices.forEach(idx -> attributes.getSelectionModel()
                        .select((int) idx));
            }
        }
    }

    public void onMouseClicked(Event event) {
        onSelectedItemChange();
    }

    public void onKeyReleased(Event event) {
        onSelectedItemChange();
    }

    /**
     * Called when the user do something, may be the selection didn't change
     */
    private void onSelectedItemChange() {
        List indexes = attributes.getSelectionModel()
                .getSelectedIndices()
                .stream()
                .toList();

        log.info("Selected item on {}: {}", getView()
                .getTitle(), indexes.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, getView().dataSourceProperty()
                .get(), indexes);
    }
}
