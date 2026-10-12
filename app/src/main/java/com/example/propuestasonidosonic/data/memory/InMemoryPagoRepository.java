package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.RelojSistema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InMemoryPagoRepository implements PagoRepository {
    private final List<Pago> pagos = new ArrayList<>();
    private final MutableLiveData<List<Pago>> todosLosPagosLiveData = new MutableLiveData<>(new ArrayList<>());
    private final Map<String, MutableLiveData<List<Pago>>> pagosPorPedidoMap = new HashMap<>();
    private final Reloj reloj;
    private long consecutivoFolio = 1;

    public InMemoryPagoRepository() {
        this(new RelojSistema());
    }

    public InMemoryPagoRepository(Reloj reloj) {
        this.reloj = reloj != null ? reloj : new RelojSistema();
    }

    private void notificarCambios(String pedidoId) {
        List<Pago> copiaTodos = new ArrayList<>();
        for (Pago p : pagos) {
            copiaTodos.add(new Pago(p));
        }
        todosLosPagosLiveData.setValue(copiaTodos);

        if (pedidoId != null) {
            MutableLiveData<List<Pago>> liveData = pagosPorPedidoMap.get(pedidoId);
            if (liveData != null) {
                List<Pago> filtrados = new ArrayList<>();
                for (Pago p : pagos) {
                    if (pedidoId.equals(p.getPedidoId())) {
                        filtrados.add(new Pago(p));
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
        // FIRESTORE: transacción
        if (pago == null) {
            if (callback != null) callback.onError("Pago nulo");
            return;
        }

        if (pago.getMonto() <= 0) {
            if (callback != null) callback.onError("El monto debe ser mayor a 0");
            return;
        }

        if (pago.getTipo() == null || pago.getTipo().trim().isEmpty()) {
            if (callback != null) callback.onError("Tipo de pago requerido");
            return;
        }

        if (pago.getMetodo() == null || pago.getMetodo().trim().isEmpty()) {
            if (callback != null) callback.onError("Método de pago requerido");
            return;
        }

        if (pago.getFolio() == null || pago.getFolio().trim().isEmpty()) {
            pago.setFolio(String.format(Locale.US, "R-%04d", consecutivoFolio++));
        }

        if (pago.getFecha() <= 0) {
            pago.setFecha(reloj.ahora());
        }

        Pago guardado = new Pago(pago);
        pagos.add(guardado);
        notificarCambios(guardado.getPedidoId());
        if (callback != null) {
            callback.onSuccess(new Pago(guardado));
        }
    }
}
