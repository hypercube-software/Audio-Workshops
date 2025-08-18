package com.hypercube.mpm;

import com.hypercube.mpm.cli.MidiPresetManagerCliApplication;
import com.hypercube.mpm.javafx.bootstrap.JavaFXApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Locale;

@SpringBootApplication
@ComponentScan("com.hypercube")
public class MidiPresetManagerApplication {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en"));
        try {
            if (args.length != 0) {
                MidiPresetManagerCliApplication.main(args);
            } else {
                Application.launch(JavaFXApplication.class, args);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
