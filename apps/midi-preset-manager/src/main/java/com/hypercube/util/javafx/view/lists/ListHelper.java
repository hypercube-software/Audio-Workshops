package com.hypercube.util.javafx.view.lists;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@UtilityClass
public class ListHelper {
    /**
     * If you want to be notified when selection is changed ONLY from user interaction and not programmatically
     * <p>This is a kind of thing JavaFX does not handle at all...sadly</p>
     */
    public static <S> void addSelectionChangeByUserListener(TableView<S> dataList, ChangeListener<S> listener) {
        // we use a closure to keep an hidden internal state
        HiddenState state = new HiddenState();
        dataList.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> state.setUserAction(true));
        dataList.addEventFilter(KeyEvent.KEY_PRESSED, event -> state.setUserAction(true));
        dataList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (state.isUserAction()) {
                        state.setUserAction(false);
                        listener.changed(observable, oldValue, newValue);
                    }
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

    @Getter
    @Setter
    private class HiddenState {
        private boolean userAction;
    }

}
