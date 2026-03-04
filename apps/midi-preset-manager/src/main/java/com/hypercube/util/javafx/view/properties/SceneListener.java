package com.hypercube.util.javafx.view.properties;

import javafx.scene.Scene;

public interface SceneListener {
    void onSceneAttach(Scene newValue);

    void onSceneDetach(Scene oldValue);
}
