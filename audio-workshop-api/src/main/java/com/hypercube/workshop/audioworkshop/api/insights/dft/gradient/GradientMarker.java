package com.hypercube.workshop.audioworkshop.api.insights.dft.gradient;

public record GradientMarker(GradientColor color, double percent) {
    public static GradientMarker fromHexa(String color, double percent) {
        return new GradientMarker(GradientColor.fromHexa(color), percent);
    }
}
