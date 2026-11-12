package com.example.propuestasonidosonic.util;

import android.widget.ListAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public final class DropdownUtils {
    private DropdownUtils() {}

    public static int seleccionar(MaterialAutoCompleteTextView campo, int posicion) {
        if (campo == null) return 0;
        ListAdapter adapter = campo.getAdapter();
        if (adapter == null || adapter.getCount() == 0) return 0;
        int pos = (posicion >= 0 && posicion < adapter.getCount()) ? posicion : 0;
        campo.setText(String.valueOf(adapter.getItem(pos)), false);
        return pos;
    }
}
