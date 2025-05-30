package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.javafx.event.ScoreChangedEvent;
import com.hypercube.util.javafx.controller.Controller;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
public class PatchScoreController extends Controller<PatchScore, Void> implements Initializable {

    @Autowired
    ApplicationContext applicationContext;

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

    private List<ImageView> stars;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stars = List.of(star1, star2, star3, star4, star5);
        IntStream.range(0, stars.size())
                .forEach(index ->
                        stars.get(index)
                                .setOnMouseClicked(e -> onMouseClick(index)));
        getView().scoreProperty()
                .addListener(this::onScoreChange);
    }

    private void onScoreChange(Observable observable) {
        updateScore(getScore());
    }

    private int getScore() {
        return getView().getScore() == null ? 0 : Integer.parseInt(getView().getScore());
    }

    private void onMouseClick(int index) {
        int score = (getScore() == index + 1) ? 0 : index + 1;
        log.info(this.toString() + " Set score " + score);
        updateScore(score);
        fireEvent(ScoreChangedEvent.class, score);
    }

    private void updateScore(int score) {
        for (int i = 0; i < stars.size(); i++) {
            ImageView imageView = stars.get(i);
            imageView.visibleProperty()
                    .set(score >= 0);
            int starScore = i + 1;
            float opacity = score >= starScore ? 1f : 0.2f;
            String css = String.format(Locale.ENGLISH, "-fx-opacity: %f;", opacity);
            imageView.setStyle(css);
        }
    }
}
