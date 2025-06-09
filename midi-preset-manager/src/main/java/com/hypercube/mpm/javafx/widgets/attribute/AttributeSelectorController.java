package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import com.hypercube.util.javafx.view.lists.DefaultCellFactory;
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
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class AttributeSelectorController extends Controller<AttributeSelector, MainModel> implements Initializable {
    @FXML
    Label label;

    @FXML
    ListView attributes;

    private List<String> acceptedFileTypes = List.of();

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
        } else if (property.equals("selectedItems")) {
            Property<?> selectedItemsProperty = resolvePath(newValue + "Property");
            selectedItemsProperty.addListener(this::onModelSelectedItemsChange);
        } else if (property.equals("title")) {
            label.setText(newValue);
        } else if (property.equals("allowDrop")) {
            if (newValue.equals("true")) {
                attributes.setOnDragOver(this::onDragOver);
                attributes.setOnDragEntered(this::onDragEntered);
                attributes.setOnDragExited(this::onDragExited);
                attributes.setOnDragDropped(this::onDragDropped);
            }
        } else if (property.equals("acceptedFileTypes")) {
            acceptedFileTypes = Optional.ofNullable(getView().getAcceptedFileTypes())
                    .map(str ->
                            Arrays.stream(str
                                            .split(","))
                                    .map(String::toLowerCase)
                                    .toList())
                    .orElse(List.of());
        } else if (property.equals("labelMethod")) {
            try {
                Class<?> clazz = Class.forName(getView().getItemType());
                attributes.setCellFactory(DefaultCellFactory.forge(newValue, clazz));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> files = db.getFiles()
                    .stream()
                    .filter(f -> hasRightFileExtension(f))
                    .toList();
            files.forEach(f -> log.info(f.toString()));
            fireEvent(FilesDroppedEvent.class, files);
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private void onDragExited(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
//
        }
        event.consume();
    }

    private void onDragEntered(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
//
        }
        event.consume();
    }

    private void onDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() && db.getFiles()
                .stream()
                .filter(f -> hasRightFileExtension(f))
                .findFirst()
                .isPresent()) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    private boolean hasRightFileExtension(File f) {
        for (String ext : acceptedFileTypes) {
            if (f.getName()
                    .toLowerCase()
                    .endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setModel(MainModel.getObservableInstance());
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
            MultipleSelectionModel selectionModel = attributes.getSelectionModel();
            if (selectedIndices.size() == 0) {
                selectionModel
                        .clearSelection();
            } else {
                selectedIndices.forEach(idx -> selectionModel.select((int) idx));
            }
        }
    }

    /**
     * Called when the model change, not the view. So when there is a programmatic update, not a user click
     *
     * @param observable
     */
    private void onModelSelectedItemsChange(Observable observable) {
        log.info("onModelSelectedItemsChange {} for {}", observable, getView().getTitle());
        List<Object> selectedItems = null;
        if (observable instanceof IntegerProperty integerProperty) {
            int value = integerProperty.getValue();
            selectedItems = List.of(value);
        } else if (observable instanceof StringProperty stringProperty) {
            String value = stringProperty.getValue();
            selectedItems = value == null ? List.of() : List.of(value);
        } else if (observable instanceof SimpleListProperty simpleListProperty) {
            ObservableList<Object> value = simpleListProperty.getValue();
            selectedItems = value == null ? List.of() : value;
        }
        MultipleSelectionModel selectionModel = attributes.getSelectionModel();
        selectionModel
                .clearSelection();
        if (selectedItems != null) {
            selectedItems
                    .forEach(item -> {
                        selectionModel.select(item);
                    });

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
        List items = attributes.getSelectionModel()
                .getSelectedItems()
                .stream()
                .toList();
        log.info("Selected item on {}: {}", getView()
                .getTitle(), indexes.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, getView().dataSourceProperty()
                .get(), indexes, items);
    }
}
