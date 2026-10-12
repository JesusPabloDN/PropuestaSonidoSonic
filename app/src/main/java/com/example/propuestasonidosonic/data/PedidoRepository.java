package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.Pedido;
import java.util.List;

public interface PedidoRepository {
    LiveData<List<Pedido>> getPedidos();
    void getPedidoPorId(String id, Callback<Pedido> callback);
    void crearPedido(Pedido pedido, Callback<Pedido> callback);
    void actualizarPedido(Pedido pedido, Callback<Pedido> callback);
    void confirmarPedido(String pedidoId, String socioResponsableId, Callback<Pedido> callback);
    void cancelarPedido(String pedidoId, Callback<Pedido> callback);
    void marcarEntregado(String pedidoId, Callback<Pedido> callback);
}
