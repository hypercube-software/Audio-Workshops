package com.hypercube.mpm.javafx.widgets.hexa;

import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
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

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

@Slf4j
public class HexaDataViewerController extends DialogController<HexaDataViewer, Void> {

    public static final int MAX_PAYLOAD_SIZE = 256;
    @FXML
    private TilePane hexTilePane;
    @FXML
    private ScrollPane hexScrollPane;

    private static byte[] getPayload(DataViewerPayload newValue) {
        byte[] payload = newValue.data();
        if (payload.length > MAX_PAYLOAD_SIZE) {
            GenericDialogController.warn("Huge payload received", "Current payload of %d bytes will be truncated to %d bytes".formatted(payload.length, MAX_PAYLOAD_SIZE));
            return Arrays.copyOfRange(payload, 0, MAX_PAYLOAD_SIZE);
        } else {
            return payload;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        HexaDataViewer view = getView();
        view.setSelectionEnd(-1);
        view.setSelectionStart(-1);
        view.setUnpackStart(-1);
        view.setUnpackEnd(-1);
    }

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
            byte[] response = getPayload(newValue);
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
            updateUnpackCells();
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

    private void updateUnpackCells() {
        var children = hexTilePane.getChildren();
        HexaDataViewer view = getView();
        Integer selectionStart = view.getUnpackStart();
        Integer selectionEnd = view.getUnpackEnd();
        IntStream.range(0, children
                        .size())
                .forEach(i ->
                {
                    var styleClass = children
                            .get(i)
                            .getStyleClass();
                    if (selectionStart != null && selectionEnd != null && i >= selectionStart && i <= selectionEnd) {
                        if (!styleClass.contains("unpack_cell")) {
                            styleClass.add("unpack_cell");
                        }
                    } else {
                        styleClass
                                .remove("unpack_cell");
                    }
                });
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
        //
        // Update also the selection text
        //
        if (selectionStart == -1 || selectionEnd == -1) {
            view.setSelection("");
        } else {
            int selectionSize = selectionEnd + 1 - selectionStart;
            view.setSelection("Selection: [$%02X,$%02X] or [%02d,%02d] Size: $%02X %02d".formatted(selectionStart, selectionEnd,
                    selectionStart, selectionEnd, selectionSize, selectionSize));
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
