package com.hypercube.workshop.syntheditor.infra.bus;

import com.hypercube.workshop.syntheditor.infra.bus.dto.ParameterUpdateDTO;

public interface SynthEditorBusListener {
    void onMsg(ParameterUpdateDTO parameterUpdateDTO);

    void onSessionClosed();
}
