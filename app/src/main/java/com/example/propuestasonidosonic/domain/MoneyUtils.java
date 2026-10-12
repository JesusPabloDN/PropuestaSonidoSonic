package com.example.propuestasonidosonic.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    public static final double TOLERANCIA = 0.005;

    private MoneyUtils() {}

    public static double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static boolean iguales(double a, double b) {
        return Math.abs(a - b) < TOLERANCIA;
    }
}
