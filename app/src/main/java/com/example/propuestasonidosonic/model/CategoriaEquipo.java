package com.example.propuestasonidosonic.model;

import com.example.propuestasonidosonic.R;

import java.util.Arrays;
import java.util.List;

public final class CategoriaEquipo {
    public static final String BOCINAS = "Bocinas";
    public static final String SUBWOOFERS = "Subwoofers";
    public static final String MICROFONOS = "Micrófonos";
    public static final String MEZCLADORAS = "Mezcladoras";
    public static final String CONTROLADORES = "Controladores";
    public static final String LUCES = "Luces";
    public static final String CABLEADO = "Cableado";
    public static final String ACCESORIOS = "Accesorios";
    public static final String OTRO = "Otro";

    public static final List<String> TODAS_LAS_CATEGORIAS = Arrays.asList(
            BOCINAS, SUBWOOFERS, MICROFONOS, MEZCLADORAS, CONTROLADORES, LUCES, CABLEADO, ACCESORIOS, OTRO
    );

    private CategoriaEquipo() {}

    public static int getImagenPorDefecto(String categoria) {
        if (categoria == null) {
            return R.drawable.ic_equipo_generico;
        }
        switch (categoria) {
            case BOCINAS:
                return R.drawable.bocinajbl;
            case SUBWOOFERS:
                return R.drawable.subwoofer;
            case MICROFONOS:
                return R.drawable.microfonosm;
            case MEZCLADORAS:
                return R.drawable.mezcladora;
            case CONTROLADORES:
                return R.drawable.controlador;
            case LUCES:
                return R.drawable.lucesled;
            case CABLEADO:
            case ACCESORIOS:
            case OTRO:
            default:
                return R.drawable.ic_equipo_generico;
        }
    }
}
