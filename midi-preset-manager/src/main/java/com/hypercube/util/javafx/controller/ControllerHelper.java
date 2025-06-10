package com.hypercube.util.javafx.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This class does the magic to make SceneBuilder happy
 * <ul>
 *     <li>When the widget is created, the FXML will be loaded</li>
 *     <li>The controller bound to this widget is created if springContext is set</li>
 *     <li>When SceneBuilder is used, springContext will be null, so the controller is not created</li>
 *     <li>Finally we bind the widget to the controller using {@link Node#setUserData(Object)}</li>
 * </ul>
 */
@UtilityClass
public class ControllerHelper {
    @Getter
    @Setter
    private static boolean nonSceneBuilderLaunch = false;

    @Getter
    @Setter
    private static ConfigurableApplicationContext springContext;

    public static <T extends Node> T loadFXML(T widget) {
        return loadFXML(widget, widget.getClass()
                .getName() + "Controller");
    }

    public static <T extends Node, View> T loadFXML(T widget, String controller) {
        var viewClass = widget.getClass();
        var loader = new FXMLLoader(viewClass.getResource(viewClass.getSimpleName() + ".fxml"));
        try {
            Controller<T, ?> controllerInstance = null;
            if (springContext != null) {
                controllerInstance = (Controller<T, ?>) springContext
                        .getAutowireCapableBeanFactory()
                        .createBean(Class.forName(controller));
            } else {
                controllerInstance = (Controller<T, ?>) Class.forName(controller)
                        .getConstructor()
                        .newInstance();
            }
            if (!nonSceneBuilderLaunch) {
                System.out.println("==============================================================");
                System.out.println("SceneBuilder context");
                System.out.println("View      : " + widget.getClass()
                        .getName());
                System.out.println("Controller: " + controller);
                System.out.println("==============================================================");
            }
            loader.setController(controllerInstance);
            controllerInstance.setView(widget);
            widget.setUserData(controllerInstance);
            loader.setRoot(widget);
            T view = loader.load();
            assert (view == widget);
            return view;
        } catch (Exception e) {
            throw new ControllerHelperException("Unable to load " + viewClass.getSimpleName(), e);
        }
    }

    /**
     * Convience method to retreive the controller from a widget (via {@link Node#getUserData()}
     *
     * @param widget The widget
     * @param <T>    The controller class
     * @return The instance of the controller bound to this widget
     */
    public static <T extends Controller> T getController(Node widget) {
        return (T) widget.getUserData();
    }
}
