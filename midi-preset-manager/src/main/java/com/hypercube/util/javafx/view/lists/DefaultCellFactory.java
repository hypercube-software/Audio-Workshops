package com.hypercube.util.javafx.view.lists;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * JavaFX does not provide a default cell factory for ListView like PropertyValueFactory for TableList
 * <p>Here's one</p>
 */
@UtilityClass
public class DefaultCellFactory {
    /**
     * @param labelMethodName getter to call to get the label of the current item in the ListView
     * @param itemClass       type of all items
     * @return the CellFactory
     */
    public static Callback<ListView, ListCell> forge(String labelMethodName, Class<?> itemClass) {
        final Method labelMethod;
        try {
            labelMethod = itemClass.getMethod(labelMethodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("labelMethod '%s' not found in class '%s'".formatted(labelMethodName, itemClass.getName()));
        }

        return new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                return new ListCell<Object>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            try {
                                String fieldValue = labelMethod.invoke(item)
                                        .toString();
                                setText(fieldValue);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
            }
        };
    }
}
