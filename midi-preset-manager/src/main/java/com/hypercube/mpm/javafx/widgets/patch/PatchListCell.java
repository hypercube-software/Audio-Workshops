package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.model.Patch;
import javafx.scene.control.TableCell;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PatchListCell extends TableCell<Patch, Patch> {
    private final PatchScore view;

    public PatchListCell() {
        view = new PatchScore();
        setGraphic(view);
        setText(null);
    }

    @Override
    protected void updateItem(Patch item, boolean empty) {
        super.updateItem(item, empty);
        PatchScoreController controller = (PatchScoreController) view.getUserData();

        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(view);
            controller.update(item);
        }
    }
}
