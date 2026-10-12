package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.model.Pago;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryPagoRepository implements PagoRepository {
    private final List<Pago> pagos = new ArrayList<>();
    private final MutableLiveData<List<Pago>> todosLosPagosLiveData = new MutableLiveData<>(new ArrayList<>());
    private final Map<String, MutableLiveData<List<Pago>>> pagosPorPedidoMap = new HashMap<>();

    public InMemoryPagoRepository() {}

    private void notificarCambios(String pedidoId) {
        todosLosPagosLiveData.setValue(new ArrayList<>(pagos));
        if (pedidoId != null) {
            MutableLiveData<List<Pago>> liveData = pagosPorPedidoMap.get(pedidoId);
            if (liveData != null) {
                List<Pago> filtrados = new ArrayList<>();
                for (Pago p : pagos) {
                    if (pedidoId.equals(p.getPedidoId())) {
                        filtrados.add(p);
                    }
                }
                liveData.setValue(filtrados);
            }
        }
    }

    @Override
    public synchronized LiveData<List<Pago>> getPagosPorPedido(String pedidoId) {
        if (!pagosPorPedidoMap.containsKey(pedidoId)) {
            pagosPorPedidoMap.put(pedidoId, new MutableLiveData<>(new ArrayList<>()));
        }
        notificarCambios(pedidoId);
        return pagosPorPedidoMap.get(pedidoId);
    }

    @Override
    public LiveData<List<Pago>> getTodosLosPagos() {
        return todosLosPagosLiveData;
    }

    @Override
    public synchronized void registrarPago(Pago pago, Callback<Pago> callback) {
        // FIRESTORE: aquí irá una transacción
        pagos.add(pago);
        notificarCambios(pago.getPedidoId());
        callback.onSuccess(pago);
    }
}
