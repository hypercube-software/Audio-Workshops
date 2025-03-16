package com.hypercube.util.javafx.view;

import com.hypercube.util.javafx.controller.Controller;

/**
 * Generated widgets will implement this interface for convenience
 *
 * @param <C> Type used for the controller bound to this widget
 */
public interface View<C extends Controller<?, ?>> {
    C getController();
}
