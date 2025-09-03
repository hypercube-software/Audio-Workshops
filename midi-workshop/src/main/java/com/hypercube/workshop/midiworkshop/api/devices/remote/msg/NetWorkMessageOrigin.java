package com.hypercube.workshop.midiworkshop.api.devices.remote.msg;

public enum NetWorkMessageOrigin {
    UDP, TCP, BOTH;

    public NetWorkMessageOrigin opposite() {
        return switch (this) {
            case UDP -> TCP;
            case TCP -> UDP;
            case BOTH -> BOTH;
        };
    }
}
