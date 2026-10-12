package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.model.Paquete;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPaqueteRepository implements PaqueteRepository {
    private final List<Paquete> paquetes = new ArrayList<>();
    private final MutableLiveData<List<Paquete>> paquetesLiveData = new MutableLiveData<>(new ArrayList<>());

    public InMemoryPaqueteRepository() {}

    private void notificarCambios() {
        paquetesLiveData.setValue(new ArrayList<>(paquetes));
    }

    @Override
    public LiveData<List<Paquete>> getPaquetes() {
        return paquetesLiveData;
    }

    @Override
    public synchronized void getPaquetePorId(String id, Callback<Paquete> callback) {
        for (Paquete p : paquetes) {
            if (p.getId().equals(id)) {
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Paquete no encontrado");
    }

    @Override
    public synchronized void guardarPaquete(Paquete paquete, Callback<Paquete> callback) {
        // FIRESTORE: aquí irá una transacción
        boolean existe = false;
        for (int i = 0; i < paquetes.size(); i++) {
            if (paquetes.get(i).getId().equals(paquete.getId())) {
                paquetes.set(i, paquete);
                existe = true;
                break;
            }
        }
        if (!existe) {
            paquetes.add(paquete);
        }
        notificarCambios();
        callback.onSuccess(paquete);
    }

    @Override
    public synchronized void eliminarPaquete(String id, Callback<Void> callback) {
        // FIRESTORE: aquí irá una transacción
        boolean removed = paquetes.removeIf(p -> p.getId().equals(id));
        if (removed) {
            notificarCambios();
            callback.onSuccess(null);
        } else {
            callback.onError("Paquete no encontrado");
        }
    }
}
