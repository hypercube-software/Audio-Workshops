package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Locale;

@Slf4j
public class PatchViewController extends Controller<PatchView, Void> {

    @Autowired
    ApplicationContext applicationContext;

    @FXML
    Label patchName;
    @FXML
    GridPane root;
    @FXML
    ImageView star1;
    @FXML
    ImageView star2;
    @FXML
    ImageView star3;
    @FXML
    ImageView star4;
    @FXML
    ImageView star5;

    public void update(Patch item, boolean empty) {
        if (!empty) {
            patchName.setText(item.getName());
            updateScore(item.getScore());
        } else {
            patchName.setText("");
            updateScore(-1);
        }
    }

    private void updateScore(int score) {
        List<ImageView> stars = List.of(star1, star2, star3, star4, star5);
        for (int i = 0; i < stars.size(); i++) {
            ImageView imageView = stars.get(i);
            imageView.visibleProperty()
                    .set(score >= 0);
            int starScore = i + 1;
            float opacity = score >= starScore ? 1f : 0.5f;
            String css = String.format(Locale.ENGLISH, "-fx-opacity: %f;", opacity);
            imageView.setStyle(css);
        }
    }
}
