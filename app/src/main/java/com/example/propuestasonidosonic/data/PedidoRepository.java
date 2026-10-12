package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.Pedido;

import java.util.List;

public interface PedidoRepository {
    LiveData<List<Pedido>> getPedidos();
    Pedido getPedidoPorId(String id);
    List<Pedido> obtenerTodos();
    void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback);
    void cancelarReserva(String pedidoId, String motivo, Callback<Pedido> callback);
    int liberarApartadosVencidos();
    List<Pedido> obtenerReservasActivasDeOperador(String operadorId, long desdeMillis, long hastaMillis);
}

