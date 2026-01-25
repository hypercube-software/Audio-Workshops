package com.hypercube.mpm.javafx.widgets.hexa;

import com.hypercube.util.javafx.controller.DialogController;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.IntStream;

@Slf4j
public class HexaDataViewerController extends DialogController<HexaDataViewer, Void> {

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
                HexaDataCell cellLabel = new HexaDataCell();
                cellLabel.setHexaValue("%02X".formatted(response[i]));
                cellLabel.setAsciiValue("%s".formatted(toASCII(response[i] & 0xFF)));
                int finalI = i;
                cellLabel.addEventHandler(MouseEvent.DRAG_DETECTED, event -> {
                    cellLabel.startFullDrag();
                    getView().setSelectionStart(finalI);
                    abortDragDetect(cellLabel);
                });
                cellLabel.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, event -> {
                    getView().setSelectionEnd(finalI);
                    updateSelection();
                });
                cellLabel.addEventHandler(MouseDragEvent.MOUSE_CLICKED, event -> {
                    resetSelection();
                });

                hexTilePane.getChildren()
                        .add(cellLabel);
            }
        }
    }

    private void resetSelection() {
        getView().setSelectionStart(-1);
        getView().setSelectionEnd(-1);
        updateSelection();
    }

    private boolean hasParent(Node node, Class<?> clazz) {
        while (node != null) {
            if (clazz.isInstance(node)) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void abortDragDetect(HexaDataCell cellLabel) {
        Scene scene = cellLabel.getScene();
        EventHandler<MouseEvent> releaseHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent e) {
                if (!hasParent(e.getPickResult()
                        .getIntersectedNode(), HexaDataCell.class)) {
                    resetSelection();
                }
                scene.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }
        };
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, releaseHandler);
    }

    private void updateSelection() {
        var children = hexTilePane.getChildren();
        HexaDataViewer view = getView();
        Integer selectionStart = view.getSelectionStart();
        Integer selectionEnd = view.getSelectionEnd();
        IntStream.range(0, children
                        .size())
                .forEach(i ->
                {
                    var styleClass = children
                            .get(i)
                            .getStyleClass();
                    if (i >= selectionStart && i <= selectionEnd) {
                        if (!styleClass.contains("hex-cell-highlight")) {
                            styleClass.add("hex-cell-highlight");
                        }
                    } else {
                        styleClass
                                .remove("hex-cell-highlight");
                    }
                });
        if (selectionStart == -1 || selectionEnd == -1) {
            view.setSelection("");
        } else {
            view.setSelection("[$%02X,$%02X] [%02d,%02d]".formatted(selectionStart, selectionEnd,
                    selectionStart, selectionEnd));
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
