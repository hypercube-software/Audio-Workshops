package com.hypercube.mpm.javafx.widgets.hexa;

import com.hypercube.util.javafx.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HexaDataCellController extends Controller<HexaDataCell, Void> {
    @FXML
    public Label cellText;
    @FXML
    public Label cellHexa;
}
