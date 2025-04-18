package com.hypercube.mpm;

import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.hypercube")
public class MidiPresetManagerApplication {

    public static void main(String[] args) {
        Application.launch(JavaFXApplication.class, args);
    }

}
