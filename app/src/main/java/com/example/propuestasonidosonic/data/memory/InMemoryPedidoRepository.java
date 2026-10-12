package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.model.Pedido;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPedidoRepository implements PedidoRepository {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final MutableLiveData<List<Pedido>> pedidosLiveData = new MutableLiveData<>(new ArrayList<>());
    private long consecutivoNumero = 1001;

    public InMemoryPedidoRepository() {}

    private void notificarCambios() {
        pedidosLiveData.setValue(new ArrayList<>(pedidos));
    }

    @Override
    public LiveData<List<Pedido>> getPedidos() {
        return pedidosLiveData;
    }

    @Override
    public synchronized void getPedidoPorId(String id, Callback<Pedido> callback) {
        for (Pedido p : pedidos) {
            if (p.getId().equals(id)) {
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Pedido no encontrado");
    }

    @Override
    public synchronized void crearPedido(Pedido pedido, Callback<Pedido> callback) {
        // FIRESTORE: aquí irá una transacción
        if (pedido.getNumero() == 0) {
            pedido.setNumero(consecutivoNumero++);
        }
        pedido.setFechaCreacion(System.currentTimeMillis());
        pedido.setFechaActualizacion(System.currentTimeMillis());
        pedidos.add(pedido);
        notificarCambios();
        callback.onSuccess(pedido);
    }

    @Override
    public synchronized void actualizarPedido(Pedido pedido, Callback<Pedido> callback) {
        // FIRESTORE: aquí irá una transacción
        for (int i = 0; i < pedidos.size(); i++) {
            if (pedidos.get(i).getId().equals(pedido.getId())) {
                pedido.setFechaActualizacion(System.currentTimeMillis());
                pedidos.set(i, pedido);
                notificarCambios();
                callback.onSuccess(pedido);
                return;
            }
        }
        callback.onError("Pedido no encontrado");
    }

    @Override
    public synchronized void confirmarPedido(String pedidoId, String socioResponsableId, Callback<Pedido> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Pedido p : pedidos) {
            if (p.getId().equals(pedidoId)) {
                p.setEstado(Pedido.ESTADO_CONFIRMADO);
                p.setFechaActualizacion(System.currentTimeMillis());
                notificarCambios();
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Pedido no encontrado");
    }

    @Override
    public synchronized void cancelarPedido(String pedidoId, Callback<Pedido> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Pedido p : pedidos) {
            if (p.getId().equals(pedidoId)) {
                p.setEstado(Pedido.ESTADO_CANCELADO);
                p.setFechaActualizacion(System.currentTimeMillis());
                notificarCambios();
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Pedido no encontrado");
    }

    @Override
    public synchronized void marcarEntregado(String pedidoId, Callback<Pedido> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Pedido p : pedidos) {
            if (p.getId().equals(pedidoId)) {
                p.setEstado(Pedido.ESTADO_ENTREGADO);
                p.setFechaActualizacion(System.currentTimeMillis());
                notificarCambios();
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Pedido no encontrado");
    }
}
