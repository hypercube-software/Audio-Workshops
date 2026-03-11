package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import java.util.Arrays;
import java.util.Optional;

public enum Product {
    K2600(0x78);

    private final int id;

    private Product(int id) {
        this.id = id;
    }

    public static Optional<Product> fromCode(int productId) {
        return Arrays.stream(Product.values())
                .filter(c -> c.id == productId)
                .findFirst();
    }
}
