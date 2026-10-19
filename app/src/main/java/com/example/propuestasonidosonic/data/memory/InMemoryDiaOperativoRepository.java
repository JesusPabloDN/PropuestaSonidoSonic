package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.model.DiaOperativo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class InMemoryDiaOperativoRepository implements DiaOperativoRepository {
    private static final Pattern FORMATO_FECHA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final Map<String, DiaOperativo> dias = new LinkedHashMap<>();
    private final MutableLiveData<List<DiaOperativo>> diasLiveData = new MutableLiveData<>(new ArrayList<>());

    public InMemoryDiaOperativoRepository() {}

    private void notificarCambios() {
        List<DiaOperativo> lista = new ArrayList<>();
        for (DiaOperativo d : dias.values()) {
            lista.add(new DiaOperativo(d));
        }
        diasLiveData.setValue(lista);
    }

    @Override
    public LiveData<List<DiaOperativo>> getDias() {
        return diasLiveData;
    }

    @Override
    public synchronized DiaOperativo obtenerDia(String fechaClave) {
        if (fechaClave == null) return null;
        DiaOperativo d = dias.get(fechaClave);
        return d != null ? new DiaOperativo(d) : null;
    }

    @Override
    public synchronized Map<String, DiaOperativo> obtenerTodosPorClave() {
        Map<String, DiaOperativo> copia = new HashMap<>();
        for (Map.Entry<String, DiaOperativo> entry : dias.entrySet()) {
            copia.put(entry.getKey(), new DiaOperativo(entry.getValue()));
        }
        return copia;
    }

    @Override
    public synchronized void guardarDia(DiaOperativo dia, Callback<DiaOperativo> callback) {
        // FIRESTORE: transacción
        if (dia == null || dia.getFechaClave() == null || !FORMATO_FECHA.matcher(dia.getFechaClave()).matches()) {
            if (callback != null) {
                callback.onError("Formato de fecha inválido. Se espera yyyy-MM-dd");
            }
            return;
        }

        DiaOperativo copia = new DiaOperativo(dia);
        // Eliminar duplicados dentro de cada lista
        if (copia.getAsistentesDisponiblesIds() != null) {
            copia.setAsistentesDisponiblesIds(new ArrayList<>(new java.util.LinkedHashSet<>(copia.getAsistentesDisponiblesIds())));
        }
        if (copia.getSociosNoDisponiblesIds() != null) {
            copia.setSociosNoDisponiblesIds(new ArrayList<>(new java.util.LinkedHashSet<>(copia.getSociosNoDisponiblesIds())));
        }
        dias.put(copia.getFechaClave(), copia);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new DiaOperativo(copia));
        }
    }

    @Override
    public synchronized void establecerDisponibilidadAsistente(String fechaClave, String asistenteId, boolean disponible, Callback<DiaOperativo> callback) {
        // FIRESTORE: transacción
        if (fechaClave == null || !FORMATO_FECHA.matcher(fechaClave).matches()) {
            if (callback != null) {
                callback.onError("Formato de fecha inválido. Se espera yyyy-MM-dd");
            }
            return;
        }
        if (asistenteId == null || asistenteId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Identificador de asistente inválido");
            }
            return;
        }

        DiaOperativo existente = dias.get(fechaClave);
        DiaOperativo target;
        if (existente != null) {
            target = new DiaOperativo(existente);
        } else {
            target = new DiaOperativo(fechaClave, new ArrayList<String>(), new ArrayList<String>());
        }

        List<String> asistentes = target.getAsistentesDisponiblesIds() != null
                ? new ArrayList<>(target.getAsistentesDisponiblesIds()) : new ArrayList<String>();

        if (disponible) {
            if (!asistentes.contains(asistenteId)) {
                asistentes.add(asistenteId);
            }
        } else {
            asistentes.remove(asistenteId);
        }
        target.setAsistentesDisponiblesIds(asistentes);

        dias.put(fechaClave, target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new DiaOperativo(target));
        }
    }

    @Override
    public synchronized void establecerDisponibilidadAsistenteVarios(List<String> fechaClaves, String asistenteId, boolean disponible, Callback<Void> callback) {
        // FIRESTORE: transacción
        if (asistenteId == null || asistenteId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Identificador de asistente inválido");
            }
            return;
        }
        if (fechaClaves == null || fechaClaves.isEmpty()) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }

        for (String fechaClave : fechaClaves) {
            if (fechaClave == null || !FORMATO_FECHA.matcher(fechaClave).matches()) {
                continue;
            }
            DiaOperativo existente = dias.get(fechaClave);
            DiaOperativo target;
            if (existente != null) {
                target = new DiaOperativo(existente);
            } else {
                target = new DiaOperativo(fechaClave, new ArrayList<String>(), new ArrayList<String>());
            }

            List<String> asistentes = target.getAsistentesDisponiblesIds() != null
                    ? new ArrayList<>(target.getAsistentesDisponiblesIds()) : new ArrayList<String>();

            if (disponible) {
                if (!asistentes.contains(asistenteId)) {
                    asistentes.add(asistenteId);
                }
            } else {
                asistentes.remove(asistenteId);
            }
            target.setAsistentesDisponiblesIds(asistentes);
            dias.put(fechaClave, target);
        }

        notificarCambios();
        if (callback != null) {
            callback.onSuccess(null);
        }
    }
}
