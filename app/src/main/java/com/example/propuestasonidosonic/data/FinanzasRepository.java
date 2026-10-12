package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.MovimientoFinanciero;
import java.util.List;

public interface FinanzasRepository {
    LiveData<List<MovimientoFinanciero>> getMovimientos();
    void registrarMovimiento(MovimientoFinanciero movimiento, Callback<MovimientoFinanciero> callback);
    void eliminarMovimiento(String id, Callback<Void> callback);
}
