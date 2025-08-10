package com.hypercube.workshop.audioworkshop.api.insights.dft.gradient;

public record GradientColor(int r, int g, int b) {
    public static GradientColor fromHexa(String value) {
        if (value.startsWith("#")) {
            if (value.length() == 4) {
                String rs = value.substring(1, 2);
                String gs = value.substring(2, 3);
                String bs = value.substring(3, 4);
                value = "#" + rs + rs + gs + gs + bs + bs;
            }
            if (value.length() == 7) {
                int r = Integer.parseInt(value.substring(1, 3), 16);
                int g = Integer.parseInt(value.substring(3, 5), 16);
                int b = Integer.parseInt(value.substring(5, 7), 16);
                return new GradientColor(r, g, b);
            }
        }
        throw new IllegalArgumentException("Unexpected RGB value, expected format: #RGB or #RRGGBB");
    }

    public String toString() {
        return "#%02X%02X%02X".formatted(r & 0xFF, g & 0xFF, b & 0xFF);
    }

    public int toRGB24() {
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }
}
