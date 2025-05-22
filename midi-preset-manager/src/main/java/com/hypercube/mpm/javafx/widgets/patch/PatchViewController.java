package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

@Slf4j
public class PatchViewController extends Controller<PatchView, Void> implements Initializable {

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

    List<ImageView> stars;

    Patch currentPatch;

    public void update(Patch item, boolean empty) {
        if (!empty) {
            patchName.setText(item.getName());
            updateScore(item.getScore());
            this.currentPatch = item;
        } else {
            patchName.setText("");
            updateScore(-1);
            this.currentPatch = null;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stars = List.of(star1, star2, star3, star4, star5);
        IntStream.range(0, stars.size())
                .forEach(index ->
                        stars.get(index)
                                .setOnMouseClicked(e -> onMouseClick(index)));
    }

    private void onMouseClick(int index) {
        if (currentPatch != null) {
            int score = (currentPatch.getScore() == index + 1) ? 0 : index + 1;
            log.info(this.toString() + " Set score " + score + " on patch " + currentPatch.getName());
            currentPatch.setScore(score);
            updateScore(score);
        }
    }

    private void updateScore(int score) {
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
