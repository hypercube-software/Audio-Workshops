package com.hypercube.mpm.javafx.widgets.button;

import com.hypercube.util.javafx.controller.Controller;
import com.hypercube.util.javafx.view.View;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

public class IconButtonController extends Controller<IconButton, Void> {
    @FXML
    Region region;

    @FXML
    Tooltip tooltip;

    @Override
    public void onPropertyChange(View<?> widget, String property, ObservableValue<?> observable, Object oldValue, Object newValue) {
        super.onPropertyChange(widget, property, observable, oldValue, newValue);
        if ("iconClass".equals(property)) {
            region.getStyleClass()
                    .setAll(getView().getIconClass());
        }
        if ("tooltipText".equals(property)) {
            tooltip.setText(getView().getTooltipText());
        }
    }
}
