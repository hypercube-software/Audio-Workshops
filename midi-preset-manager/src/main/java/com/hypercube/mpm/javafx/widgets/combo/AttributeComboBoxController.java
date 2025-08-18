package com.hypercube.mpm.javafx.widgets.combo;

import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.lists.DefaultCellFactory;
import com.sun.javafx.binding.SelectBinding;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
public class AttributeComboBoxController extends Controller<AttributeComboBox, MainModel> implements Initializable {
    @FXML
    Label label;
    @FXML
    ComboBox attributes;
    // boolean used to distinguish user action and programmatic action
    private boolean userAction = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
        attributes.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            userAction = true;
        });
        attributes.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            userAction = true;
        });
    }

    public void onDataSourceChange(String oldValue, String newValue) {
        // The dataSource is set, we bound the corresponding property to the ListView
        bindingManager.observePath(newValue, this::onDataChange);
    }

    public void onDataChange(Observable observable) {
        runOnJavaFXThread(() -> {
            ObservableList<String> list = (ObservableList<String>) ((SelectBinding.AsObject) observable).getValue();
            if (list != null) {
                log.info("Datasource {} for {} just changed with {} items", getView().getDataSource(), getView().getTitle(), list.size());
            }
            attributes.setItems(list != null ? list : new SimpleListProperty());
            // since the date source changed, we can update the selection
            Observable selectedItem = bindingManager.resolvePropertyPath(getView().getSelectedItem());
            if (selectedItem != null) {
                onModelSelectedItemsChange(selectedItem);
            }
        });
    }

    public void onSelectedItemChange(String oldValue, String newValue) {
        bindingManager.observePath(newValue, this::onModelSelectedItemsChange);
    }

    public void onTitleChange(String oldValue, String newValue) {
        label.setText(newValue);
    }

    public void onLabelMethodChange(String oldValue, String newValue) {
        try {
            Class<?> clazz = Class.forName(getView().getItemType());
            attributes.setCellFactory(DefaultCellFactory.forge(newValue, clazz));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onAction(ActionEvent event) {
        if (userAction) {
            onSelectedItemChange();
            userAction = false;
        }
    }

    /**
     * Called when the model change, not the view. So when there is a programmatic update, not a user click
     *
     * @param observable
     */
    private void onModelSelectedItemsChange(Observable observable) {
        log.info("{}::onModelSelectedItemsChange {} for {}", this.getClass()
                .getSimpleName(), getView().getSelectedItem(), getView().getTitle());
        Object selectedItem = null;

        if (observable instanceof IntegerProperty integerProperty) {
            int value = integerProperty.getValue();
            selectedItem = value;
        } else if (observable instanceof StringProperty stringProperty) {
            String value = stringProperty.getValue();
            selectedItem = value;
        } else if (observable instanceof ObjectProperty objectProperty) {
            Object value = objectProperty.getValue();
            selectedItem = value == value;
        } else if (observable instanceof SimpleListProperty simpleListProperty) {
            ObservableList<Object> value = simpleListProperty.getValue();
            selectedItem = value.isEmpty() ? null : value.getFirst();
        } else if (observable instanceof SelectBinding.AsObject<?> pathObserver) {
            var value = pathObserver.getValue();
            if (value == null) {
                selectedItem = null;
            } else if (value instanceof List listValue) {
                selectedItem = listValue.isEmpty() ? null : listValue.getFirst();
            } else {
                selectedItem = value;
            }
        } else {
            return;
        }
        SingleSelectionModel selectionModel = attributes.getSelectionModel();
        selectionModel
                .clearSelection();
        if (selectedItem != null) {
            selectionModel.select(selectedItem);
        }
    }

    /**
     * Called when the user do something, may be the selection didn't change, but we fire the event anyway
     */
    private void onSelectedItemChange() {
        int index = attributes.getSelectionModel()
                .getSelectedIndex();
        Object item = attributes.getSelectionModel()
                .getSelectedItem();
        log.info("Selected item on {}: {}", getView()
                .getTitle(), index);

        fireEvent(SelectionChangedEvent.class, getView().getId(), index == -1 ? List.of() : List.of(index), item == null ? List.of() : List.of(item));
    }
}
