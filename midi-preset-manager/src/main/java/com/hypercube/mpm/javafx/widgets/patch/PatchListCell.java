package com.hypercube.mpm.javafx.widgets.patch;

import com.hypercube.mpm.model.Patch;
import javafx.scene.control.ListCell;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PatchListCell extends ListCell<Patch> {
    private final PatchView view;

    public PatchListCell() {
        view = new PatchView();
        setGraphic(view);
    }

    @Override
    protected void updateItem(Patch item, boolean empty) {
        super.updateItem(item, empty);
        PatchViewController controller = (PatchViewController) view.getUserData();
        controller.update(item, empty);
    }
}
