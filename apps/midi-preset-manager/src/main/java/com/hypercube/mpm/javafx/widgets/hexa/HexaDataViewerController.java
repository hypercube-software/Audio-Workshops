package com.hypercube.mpm.javafx.widgets.hexa;

import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
import com.hypercube.util.javafx.controller.DialogController;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

@Slf4j
public class HexaDataViewerController extends DialogController<HexaDataViewer, Void> {

    public static final int MAX_PAYLOAD_SIZE = 1024 * 1024;
    public static final int ROW_SIZE = 16;
    @FXML
    private ListView<byte[]> hexListView;

    private byte[] fullPayload;

    // Auto-scroll management
    private AnimationTimer scrollTimer;
    private double scrollVelocity = 0;

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

        hexListView.setCellFactory(lv -> new HexRowCell());

        setupAutoScroll();
    }

    public void clear() {
        hexListView.getItems()
                .clear();
        fullPayload = null;
    }

    public void onGridHeightChange(Integer oldValue, Integer newValue) {
        if (newValue != null) {
            hexListView.setPrefHeight(newValue * 40);
        }
    }

    public void onDataChange(DataViewerPayload oldValue, DataViewerPayload newValue) {
        hexListView.getItems()
                .clear();
        if (newValue != null) {
            fullPayload = getPayload(newValue);
            ObservableList<byte[]> rows = FXCollections.observableArrayList();
            for (int i = 0; i < fullPayload.length; i += ROW_SIZE) {
                int end = Math.min(i + ROW_SIZE, fullPayload.length);
                rows.add(Arrays.copyOfRange(fullPayload, i, end));
            }
            hexListView.setItems(rows);
        } else {
            fullPayload = null;
        }
    }

    public void onSelectionStartChange(Integer oldValue, Integer newValue) {
        hexListView.refresh();
        updateSelectionText();
    }

    public void onSelectionEndChange(Integer oldValue, Integer newValue) {
        hexListView.refresh();
        updateSelectionText();
    }

    public void onUnpackStartChange(Integer oldValue, Integer newValue) {
        hexListView.refresh();
    }

    public void onUnpackEndChange(Integer oldValue, Integer newValue) {
        hexListView.refresh();
    }

    private void setupAutoScroll() {
        scrollTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate > 0) {
                    long elapsed = now - lastUpdate;
                    if (elapsed > 50_000_000) { // Update every 50ms approx
                        if (scrollVelocity != 0) {
                            scrollListView();
                        }
                        lastUpdate = now;
                    }
                } else {
                    lastUpdate = now;
                }
            }
        };

        hexListView.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            double y = event.getY();
            double height = hexListView.getHeight();
            double threshold = 40.0;

            if (y < threshold) {
                scrollVelocity = -1;
                scrollTimer.start();
            } else if (y > height - threshold) {
                scrollVelocity = 1;
                scrollTimer.start();
            } else {
                scrollVelocity = 0;
                // Don't stop timer yet, it might be needed if mouse moves back to edge
            }
        });

        hexListView.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, event -> {
            scrollVelocity = 0;
            scrollTimer.stop();
        });
    }

    private void scrollListView() {
        HexaDataViewer view = getView();
        Integer selectionEnd = view.getSelectionEnd();
        if (selectionEnd == null || selectionEnd == -1) return;

        int currentRow = selectionEnd / ROW_SIZE;
        int targetRow = currentRow + (int) scrollVelocity;

        if (targetRow >= 0 && targetRow < hexListView.getItems()
                .size()) {
            // Update selectionEnd to the next row same column
            int newSelectionEnd = targetRow * ROW_SIZE + (selectionEnd % ROW_SIZE);
            // Ensure we don't go out of bounds of the actual data
            if (fullPayload != null && newSelectionEnd < fullPayload.length) {
                view.setSelectionEnd(newSelectionEnd);
            } else if (fullPayload != null) {
                view.setSelectionEnd(fullPayload.length - 1);
            }

            hexListView.scrollTo(targetRow);
            updateSelectionText();
            hexListView.refresh();
        }
    }

    private void resetSelection() {
        getView().setSelectionStart(-1);
        getView().setSelectionEnd(-1);
        updateSelectionText();
        hexListView.refresh();
        scrollVelocity = 0;
        scrollTimer.stop();
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
                scrollVelocity = 0;
                scrollTimer.stop();
                scene.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }
        };
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, releaseHandler);
    }

    private void updateSelectionText() {
        HexaDataViewer view = getView();
        Integer selectionStart = view.getSelectionStart();
        Integer selectionEnd = view.getSelectionEnd();
        if (selectionStart == null || selectionEnd == null || selectionStart == -1 || selectionEnd == -1) {
            view.setSelection("");
        } else {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            int selectionSize = end + 1 - start;
            view.setSelection("Selection: [$%02X,$%02X] or [%02d,%02d] Size: $%02X %02d".formatted(start, end,
                    start, end, selectionSize, selectionSize));
        }
    }

    private String toASCII(int b) {
        if (b >= 32 && b <= 127) {
            return "" + (char) b;
        } else {
            return "";
        }
    }

    private class HexRowCell extends ListCell<byte[]> {
        private final HBox hbox = new HBox(2);
        private final HexaDataCell[] cells = new HexaDataCell[ROW_SIZE];

        public HexRowCell() {
            for (int i = 0; i < ROW_SIZE; i++) {
                cells[i] = new HexaDataCell();
                hbox.getChildren()
                        .add(cells[i]);
            }
            setGraphic(hbox);
            setStyle("-fx-background-color: transparent;");
        }

        @Override
        protected void updateItem(byte[] item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                int rowIndex = getIndex();
                for (int i = 0; i < ROW_SIZE; i++) {
                    HexaDataCell cell = cells[i];
                    if (i < item.length) {
                        cell.setVisible(true);
                        int byteIndex = rowIndex * ROW_SIZE + i;
                        cell.setHexaValue("%02X".formatted(item[i]));
                        cell.setAsciiValue(toASCII(item[i] & 0xFF));

                        // Event handlers for selection
                        cell.setOnDragDetected(event -> {
                            cell.startFullDrag();
                            getView().setSelectionStart(byteIndex);
                            getView().setSelectionEnd(byteIndex);
                            updateSelectionText();
                            hexListView.refresh();
                            abortDragDetect(cell);
                        });
                        cell.setOnMouseDragEntered(event -> {
                            getView().setSelectionEnd(byteIndex);
                            updateSelectionText();
                            hexListView.refresh();
                        });
                        cell.setOnMouseClicked(event -> {
                            resetSelection();
                        });

                        // Highlight
                        updateCellHighlight(cell, byteIndex);
                    } else {
                        cell.setVisible(false);
                    }
                }
                setGraphic(hbox);
            }
        }

        private void updateCellHighlight(HexaDataCell cell, int byteIndex) {
            HexaDataViewer view = getView();
            Integer selectionStart = view.getSelectionStart();
            Integer selectionEnd = view.getSelectionEnd();
            var styleClass = cell.getStyleClass();

            // Highlight selection
            if (selectionStart != null && selectionEnd != null && selectionStart != -1 && selectionEnd != -1) {
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                if (byteIndex >= start && byteIndex <= end) {
                    if (!styleClass.contains("hex-cell-highlight")) {
                        styleClass.add("hex-cell-highlight");
                    }
                } else {
                    styleClass.remove("hex-cell-highlight");
                }
            } else {
                styleClass.remove("hex-cell-highlight");
            }

            // Highlight unpack
            Integer unpackStart = view.getUnpackStart();
            Integer unpackEnd = view.getUnpackEnd();
            if (unpackStart != null && unpackEnd != null && byteIndex >= unpackStart && byteIndex <= unpackEnd) {
                if (!styleClass.contains("unpack_cell")) {
                    styleClass.add("unpack_cell");
                }
            } else {
                styleClass.remove("unpack_cell");
            }
        }
    }
}
