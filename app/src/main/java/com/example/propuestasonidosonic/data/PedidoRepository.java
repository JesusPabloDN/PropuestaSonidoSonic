package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;

import java.util.List;

public interface PedidoRepository {
    LiveData<List<Pedido>> getPedidos();
    Pedido getPedidoPorId(String id);
    List<Pedido> obtenerTodos();
    void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback);
    void cancelarReserva(String pedidoId, String canceladoPor, String motivo, Callback<Pedido> callback);
    void registrarAnticipo(String pedidoId, double monto, String metodo, Callback<Pedido> callback);
    void liquidarSaldo(String pedidoId, double monto, String metodo, Callback<Pago> callback);
    void reprogramarReserva(String pedidoId, long nuevoInicioMillis, Callback<Pedido> callback);
    void aprobarPedido(String pedidoId, Callback<Pedido> callback);
    void rechazarPedido(String pedidoId, String motivo, Callback<Pedido> callback);
    int liberarApartadosVencidos();
    List<Pedido> obtenerReservasActivasDeOperador(String operadorId, long desdeMillis, long hastaMillis);

    // Consultas síncronas para la interfaz
    ResultadoDisponibilidad evaluarDisponibilidad(String paqueteId, long inicioMillis, int duracionMin, String ignorarPedidoId);
    List<Long> consultarHorarios(String paqueteId, long unMomentoDelDia, int duracionMin, String ignorarPedidoId);
    List<Long> consultarAlternativas(String paqueteId, long desdeMillis, int duracionMin, int max);
    double calcularReembolsoPrevio(String pedidoId, String canceladoPor);
    void reasignarUnidades(Callback<Void> callback);
}
