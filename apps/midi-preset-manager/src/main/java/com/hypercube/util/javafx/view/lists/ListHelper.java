package com.hypercube.util.javafx.view.lists;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@UtilityClass
@Slf4j
public class ListHelper {
    /**
     * If you want to be notified when selection is changed ONLY from user interaction and not programmatically
     * <p>This is a kind of thing JavaFX does not handle at all...sadly</p>
     */
    public static <S> void addSelectionChangeByUserListener(TableView<S> tableView, SelectionChangedListener<S> listener) {
        // we use a closure to keep a hidden internal state
        HiddenState state = new HiddenState();
        tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> state.setUserAction(true));
        tableView.addEventFilter(KeyEvent.KEY_PRESSED, event -> state.setUserAction(true));
        tableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    var selectedIndexes = ListHelper.getSelectedIndexes(tableView);
                    boolean userAction = state.isUserAction();
                    if (userAction) {
                        state.setUserAction(false);
                    } else {
                        if (!selectedIndexes.isEmpty()) {
                            int itemIndex = selectedIndexes.getFirst();
                            log.info("selection changed, make item index {} visible for list id '{}'", itemIndex, tableView.getId());
                            tableView.scrollTo(itemIndex);
                        }
                    }
                    listener.changed(userAction, oldValue, newValue);
                });
    }

    /**
     * If you want to be notified when selection is changed ONLY from user interaction and not programmatically
     * <p>This is a kind of thing JavaFX does not handle at all...sadly</p>
     */
    public static <S> void addSelectionChangeByUserListener(ListView<S> listView, SelectionChangedListener<S> listener) {
        // we use a closure to keep a hidden internal state
        HiddenState state = new HiddenState();
        listView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> state.setUserAction(true));
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> state.setUserAction(true));
        listView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    var selectedIndexes = ListHelper.getSelectedIndexes(listView);
                    boolean userAction = state.isUserAction();
                    if (userAction) {
                        state.setUserAction(false);
                    } else {
                        if (!selectedIndexes.isEmpty()) {
                            int itemIndex = selectedIndexes.getFirst();
                            log.info("selection changed, make item index {} visible for list id '{}'", itemIndex, listView.getId());
                            listView.scrollTo(itemIndex);
                        }
                    }
                    listener.changed(userAction, oldValue, newValue);
                });
    }

    public static <ItemType, ColumnType, Widget extends Node> void configureColumn(TableColumn<ItemType, ColumnType> col, String path, Supplier<Widget> widgetFactory, BiFunction<Widget, ColumnType, Void> widgetUpdater) {
        configureColumn(col, path);
        Callback<TableColumn<ItemType, ColumnType>, TableCell<ItemType, ColumnType>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ItemType, ColumnType> call(final TableColumn<ItemType, ColumnType> param) {
                return new TableCell<ItemType, ColumnType>() {
                    private final Widget widget = widgetFactory.get();

                    @Override
                    public void updateItem(ColumnType columnValue, boolean empty) {
                        super.updateItem(columnValue, empty);
                        if (empty) {
                            setGraphic(null);
                            widgetUpdater.apply(widget, null);
                        } else {
                            setGraphic(widget);
                            widgetUpdater.apply(widget, columnValue);
                        }
                    }
                };
            }
        };
        col.setCellFactory(cellFactory);
    }

    @SuppressWarnings("unchecked")
    public static <ItemType, ColumnType> void configureColumn(TableColumn<ItemType, ColumnType> col, String path) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(path);
        col.setCellValueFactory(cellData -> {
            ItemType item = cellData.getValue();
            try {
                return Optional.ofNullable(expression.getValue(item))
                        .map(value -> new SimpleObjectProperty<ColumnType>((ColumnType) value))
                        .orElse(null);
            } catch (SpelEvaluationException e) {
                return null;
            }
        });
    }

    public static <T> void allowMultiSelection(TableView<T> table) {
        table.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Return a writable list of selected items (whereas selectionModel return read only list)
     */
    public static <T> List<T> getSelectedItems(TableView<T> table) {
        return new ArrayList<>(table.getSelectionModel()
                .getSelectedItems());
    }

    /**
     * Return a writable list of selected items (whereas selectionModel return read only list)
     */
    public static <T> List<T> getSelectedItems(ListView<T> table) {
        return new ArrayList<>(table.getSelectionModel()
                .getSelectedItems());
    }

    /**
     * Return a writable list of selected items (whereas selectionModel return read only list)
     */
    public static <T> List<Integer> getSelectedIndexes(TableView<T> table) {
        return new ArrayList<>(table.getSelectionModel()
                .getSelectedIndices());
    }

    /**
     * Return a writable list of selected items (whereas selectionModel return read only list)
     */
    public static <T> List<Integer> getSelectedIndexes(ListView<T> table) {
        return new ArrayList<>(table.getSelectionModel()
                .getSelectedIndices());
    }

    /**
     * Select one or multiple items
     */
    public <T> void selectItems(TableView<T> table, List<T> selectedItems) {
        MultipleSelectionModel<T> selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        if (selectedItems.size() > 1) {
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        }
        selectedItems.forEach(selectionModel::select);
    }

    /**
     * Select one or multiple items
     */
    public <T> void selectItems(ListView<T> table, List<T> selectedItems) {
        MultipleSelectionModel<T> selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        if (selectedItems.size() > 1) {
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        }
        selectedItems.forEach(selectionModel::select);
    }

    @Getter
    @Setter
    private class HiddenState {
        private boolean userAction;
    }

}
