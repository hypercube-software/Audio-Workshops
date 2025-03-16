package com.hypercube.mpm;

import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MidiPresetManagerApplication {

    public static void main(String[] args) {
        Application.launch(JavaFXApplication.class, args);
    }

}
