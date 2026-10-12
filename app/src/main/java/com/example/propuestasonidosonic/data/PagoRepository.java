package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.Pago;
import java.util.List;

public interface PagoRepository {
    LiveData<List<Pago>> getPagosPorPedido(String pedidoId);
    LiveData<List<Pago>> getTodosLosPagos();
    void registrarPago(Pago pago, Callback<Pago> callback);
}
