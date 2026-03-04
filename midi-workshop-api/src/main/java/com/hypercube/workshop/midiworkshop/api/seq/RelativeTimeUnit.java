package com.hypercube.workshop.midiworkshop.api.seq;

/**
 * Time in music is relative to the tempo. This mean everything can be reduced to fractions of a duration.
 * <p>The unit 1/1 correspond to 4 beats, and it is called "a whole", in french "une ronde"
 * <p>One beat correspond to 1/4, and it is called "a quarter", in french "une noire"
 *
 * @param numerator
 * @param denominator
 */
public record RelativeTimeUnit(int numerator, int denominator) {
    public static final RelativeTimeUnit _1_1 = new RelativeTimeUnit(1, 1);
    public static final RelativeTimeUnit _1_2 = new RelativeTimeUnit(1, 2);
    public static RelativeTimeUnit _1_4 = new RelativeTimeUnit(1, 4);
    public static final RelativeTimeUnit _1_8 = new RelativeTimeUnit(1, 8);
    public static RelativeTimeUnit _1_16 = new RelativeTimeUnit(1, 16);
    public static RelativeTimeUnit _1_64 = new RelativeTimeUnit(1, 64);

    public static RelativeTimeUnit _1_1_DOT = new RelativeTimeUnit(3, 2);
    public static RelativeTimeUnit _1_2_DOT = new RelativeTimeUnit(3, 4);
    public static RelativeTimeUnit _1_4_DOT = new RelativeTimeUnit(3, 8);
    public static RelativeTimeUnit _1_8_DOT = new RelativeTimeUnit(3, 16);
    public static RelativeTimeUnit _1_16_DOT = new RelativeTimeUnit(3, 64);
    public static RelativeTimeUnit _1_64_DOT = new RelativeTimeUnit(3, 128);

    public RelativeTimeUnit mult(int i) {
        return new RelativeTimeUnit(numerator * i, denominator);
    }

    public RelativeTimeUnit div(int i) {
        return new RelativeTimeUnit(numerator, denominator * i);
    }

    public RelativeTimeUnit div(RelativeTimeUnit n) {
        return new RelativeTimeUnit(numerator * n.denominator, denominator * n.numerator);
    }

    public RelativeTimeUnit plus(RelativeTimeUnit n) {
        return new RelativeTimeUnit(numerator * n.denominator + n.numerator * denominator, denominator * n.denominator);
    }

    public float toFloat() {
        return ((float) numerator) / denominator;
    }

    public int toInteger() {
        return numerator / denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    public RelativeTimeUnit minus(RelativeTimeUnit n) {
        return new RelativeTimeUnit(numerator * n.denominator - n.numerator * denominator, denominator * n.denominator);
    }
}
