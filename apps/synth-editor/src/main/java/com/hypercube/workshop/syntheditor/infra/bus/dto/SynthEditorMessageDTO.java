package com.hypercube.workshop.syntheditor.infra.bus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class SynthEditorMessageDTO {
    private SynthEditorMessageType type;
    private String msg;
}
