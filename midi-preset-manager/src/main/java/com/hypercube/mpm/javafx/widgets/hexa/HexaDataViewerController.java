package com.hypercube.mpm.javafx.widgets.hexa;

import com.hypercube.mpm.javafx.widgets.dialog.sysex.SysexToolboxDialog;
import com.hypercube.util.javafx.controller.DialogController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HexaDataViewerController extends DialogController<SysexToolboxDialog, Void> {

    @FXML
    private TilePane hexTilePane;
    @FXML
    private ScrollPane hexScrollPane;

    public void clear() {
        hexTilePane.getChildren()
                .clear();
    }

    public void onGridHeightChange(Integer oldValue, Integer newValue) {
        hexScrollPane.setMinViewportHeight(newValue * 20);
    }

    public void onDataChange(DataViewerPayload oldValue, DataViewerPayload newValue) {
        hexTilePane.getChildren()
                .clear();
        if (newValue != null) {
            byte[] response = newValue.data();
            for (int i = 0; i < response.length; i++) {
                Label cellLabel = new Label("%02X %s".formatted(response[i], toASCII(response[i] & 0xFF)));
                cellLabel.getStyleClass()
                        .add("hex-cell");
                if (i >= 3 && i <= 20) {
                    cellLabel.getStyleClass()
                            .add("hex-cell-highlight");
                }
                hexTilePane.getChildren()
                        .add(cellLabel);
            }
        }
    }

    private String toASCII(int b) {
        if (b >= 32 && b <= 127) {
            return "" + (char) b;
        } else {
            return "";
        }
    }
}
