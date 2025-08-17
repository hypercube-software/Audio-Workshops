package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.*;
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
        attributes.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (userAction) {
                        onSelectedItemChange();
                        userAction = false;
                    }
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
            Observable selectedItem = bindingManager.resolvePropertyPath(getView().getSelectedItems());
            if (selectedItem != null) {
                onModelSelectedItemsChange(selectedItem);
            }
        });
    }

    public void onAllowMultiSelectionChange(String oldValue, String newValue) {
        attributes.getSelectionModel()
                .setSelectionMode(Boolean.parseBoolean(newValue) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
    }

    public void onSelectedItemsChange(String oldValue, String newValue) {
        bindingManager.observePath(newValue, this::onModelSelectedItemsChange);
    }

    public void onTitleChange(String oldValue, String newValue) {
        label.setText(newValue);
    }

    public void onAllowDropChange(String oldValue, String newValue) {
        if (newValue.equals("true")) {
            attributes.setOnDragOver(this::onDragOver);
            attributes.setOnDragEntered(this::onDragEntered);
            attributes.setOnDragExited(this::onDragExited);
            attributes.setOnDragDropped(this::onDragDropped);
        }
    }

    public void onAcceptedFileTypesChange(String oldValue, String newValue) {
        acceptedFileTypes = Optional.ofNullable(getView().getAcceptedFileTypes())
                .map(str ->
                        Arrays.stream(str
                                        .split(","))
                                .map(String::toLowerCase)
                                .toList())
                .orElse(List.of());
    }

    public void onLabelMethodChange(String oldValue, String newValue) {
        try {
            Class<?> clazz = Class.forName(getView().getItemType());
            attributes.setCellFactory(DefaultCellFactory.forge(newValue, clazz));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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

    /**
     * Called when the model change, not the view. So when there is a programmatic update, not a user click
     *
     * @param observable
     */
    private void onModelSelectedItemsChange(Observable observable) {
        log.info("onModelSelectedItemsChange {} for {}", getView().getSelectedItems(), getView().getTitle());
        List<Object> selectedItems = null;

        if (observable instanceof IntegerProperty integerProperty) {
            int value = integerProperty.getValue();
            selectedItems = List.of(value);
        } else if (observable instanceof StringProperty stringProperty) {
            String value = stringProperty.getValue();
            selectedItems = value == null ? List.of() : List.of(value);
        } else if (observable instanceof ObjectProperty objectProperty) {
            Object value = objectProperty.getValue();
            selectedItems = value == null ? List.of() : List.of(value);
        } else if (observable instanceof SimpleListProperty simpleListProperty) {
            ObservableList<Object> value = simpleListProperty.getValue();
            selectedItems = value == null ? List.of() : value;
        } else if (observable instanceof SelectBinding.AsObject<?> pathObserver) {
            var value = pathObserver.getValue();
            if (value == null) {
                selectedItems = List.of();
            } else if (value instanceof List listValue) {
                selectedItems = listValue;
            } else {
                selectedItems = List.of(value);
            }
        } else {
            return;
        }
        MultipleSelectionModel selectionModel = attributes.getSelectionModel();
        selectionModel
                .clearSelection();
        if (selectedItems != null) {
            if (selectedItems.size() > 1) {
                selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
            }
            selectedItems
                    .forEach(item -> {
                        selectionModel.select(item);
                    });

        }
    }

    /**
     * Called when the user do something, may be the selection didn't change, but we fire the event anyway
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

        fireEvent(SelectionChangedEvent.class, getView().getId(), indexes, items);
    }
}
