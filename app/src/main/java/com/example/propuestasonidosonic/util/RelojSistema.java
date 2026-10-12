package com.example.propuestasonidosonic.util;

public class RelojSistema implements Reloj {
    @Override
    public long ahora() {
        return System.currentTimeMillis();
    }
}
