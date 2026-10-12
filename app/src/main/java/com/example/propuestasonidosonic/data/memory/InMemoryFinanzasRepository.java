package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.FinanzasRepository;
import com.example.propuestasonidosonic.model.MovimientoFinanciero;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFinanzasRepository implements FinanzasRepository {
    private final List<MovimientoFinanciero> movimientos = new ArrayList<>();
    private final MutableLiveData<List<MovimientoFinanciero>> movimientosLiveData = new MutableLiveData<>(new ArrayList<>());

    public InMemoryFinanzasRepository() {}

    private void notificarCambios() {
        movimientosLiveData.setValue(new ArrayList<>(movimientos));
    }

    @Override
    public LiveData<List<MovimientoFinanciero>> getMovimientos() {
        return movimientosLiveData;
    }

    @Override
    public synchronized void registrarMovimiento(MovimientoFinanciero movimiento, Callback<MovimientoFinanciero> callback) {
        // FIRESTORE: aquí irá una transacción
        movimientos.add(movimiento);
        notificarCambios();
        callback.onSuccess(movimiento);
    }

    @Override
    public synchronized void eliminarMovimiento(String id, Callback<Void> callback) {
        // FIRESTORE: aquí irá una transacción
        boolean removed = movimientos.removeIf(m -> m.getId().equals(id));
        if (removed) {
            notificarCambios();
            callback.onSuccess(null);
        } else {
            callback.onError("Movimiento no encontrado");
        }
    }
}
