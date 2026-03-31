package com.hypercube.mpm.javafx.widgets.attribute;

import com.hypercube.mpm.javafx.event.EditButtonClickedEvent;
import com.hypercube.mpm.javafx.event.FilesDroppedEvent;
import com.hypercube.mpm.javafx.event.SelectionChangedEvent;
import com.hypercube.mpm.javafx.widgets.button.IconButton;
import com.hypercube.mpm.model.MainModel;
import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.controller.JavaFXSpringController;
import com.hypercube.util.javafx.view.View;
import com.hypercube.util.javafx.view.lists.DefaultCellFactory;
import com.hypercube.util.javafx.view.lists.ListHelper;
import com.sun.javafx.binding.SelectBinding;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@JavaFXSpringController
public class AttributeSelectorController extends Controller<AttributeSelector, MainModel> {
    @FXML
    Label label;

    @FXML
    ListView<Object> attributes;

    @FXML
    IconButton addButton;
    @FXML
    IconButton removeButton;

    private List<String> acceptedFileTypes = List.of();

    @Override
    public void onViewLoaded() {
        setModel(MainModel.getObservableInstance());
        addSelectionListener(attributes);
    }

    public void onDataSourceChange(String oldValue, String newValue) {
        // The dataSource is set, we bound the corresponding property to the ListView
        bindingManager.observePath(newValue, this::onDataChange);
    }

    @FXML
    public void onEditButtonClick(ActionEvent event) {
        var iconButton = (IconButton) event.getSource();
        var buttonClickEvent = forgeEvent(EditButtonClickedEvent.class, getView().getId(), iconButton.getId());
        Optional.ofNullable(getView().onEditButtonClickProperty()
                        .getValue())
                .ifPresent(handler -> handler.handle((ActionEvent) buttonClickEvent));
    }

    public void onDataChange(Observable observable) {
        runOnJavaFXThread(() -> {
            ObservableList<Object> list = (ObservableList<Object>) ((SelectBinding.AsObject) observable).getValue();
            if (list != null) {
                log.info("Datasource {} for {} just changed with {} items", getView().getDataSource(), getView().getTitle(), list.size());
            }
            attributes.setItems(list != null ? list : new SimpleListProperty<>());
            // since the data source changed, we can update the selection
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
        attributes.setCellFactory(DefaultCellFactory.forge(newValue, Object.class));
    }

    @Override
    public void onPropertyChange(View<?> widget, String property, ObservableValue<?> observable, Object oldValue, Object newValue) {
        super.onPropertyChange(widget, property, observable, oldValue, newValue);
        if ("editButtons".equals(property)) {
            Boolean enable = (Boolean) newValue;
            addButton.setVisible(enable);
            addButton.setManaged(enable);
            removeButton.setVisible(enable);
            removeButton.setManaged(enable);
        }
    }

    /**
     * JavaFX does not provide a simple way to distinguish selection change between user or program
     * <p>Here a way to do it</p>
     */
    private void addSelectionListener(ListView<Object> widget) {
        ListHelper.addSelectionChangeByUserListener(widget, (userAction, oldValue, newValue) -> {
            // It is CRUCIAL to runLater because we are in the middle of a list update
            // Any selection change inside the callback would raise a UnsupportedOperationExceptionPlatform.runLater(() -> {
            if (userAction) {
                Platform.runLater(this::onUserSelectedItems);
            }
        });
    }

    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> files = db.getFiles()
                    .stream()
                    .filter(this::hasRightFileExtension)
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
                .anyMatch(this::hasRightFileExtension)) {
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
     */
    private void onModelSelectedItemsChange(Observable observable) {
        log.info("onModelSelectedItemsChange {} for {}", getView().getSelectedItems(), getView().getTitle());
        final List<Object> selectedItems;

        switch (observable) {
            case IntegerProperty integerProperty -> {
                int value = integerProperty.getValue();
                selectedItems = List.of(value);
            }
            case StringProperty stringProperty -> {
                String value = stringProperty.getValue();
                selectedItems = value == null ? List.of() : List.of(value);
            }
            case ObjectProperty<?> objectProperty -> {
                Object value = objectProperty.getValue();
                selectedItems = value == null ? List.of() : List.of(value);
            }
            case SimpleListProperty simpleListProperty -> {
                ObservableList<Object> value = simpleListProperty.getValue();
                selectedItems = value == null ? List.of() : value;
            }
            case SelectBinding.AsObject<?> pathObserver -> {
                var value = pathObserver.getValue();
                if (value == null) {
                    selectedItems = List.of();
                } else if (value instanceof List listValue) {
                    selectedItems = listValue;
                } else {
                    selectedItems = List.of(value);
                }
            }
            case null, default -> {
                return;
            }
        }
        ListHelper.selectItems(attributes, selectedItems);
    }


    /**
     * Called when the user do something, may be the selection didn't change, but we fire the event anyway
     */
    private void onUserSelectedItems() {
        List<Integer> indexes = ListHelper.getSelectedIndexes(attributes);
        List<Object> items = ListHelper.getSelectedItems(attributes);
        log.info("Selected item on {}: {}", getView()
                .getTitle(), indexes.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));

        fireEvent(SelectionChangedEvent.class, getView().getId(), indexes, items);
    }
}
