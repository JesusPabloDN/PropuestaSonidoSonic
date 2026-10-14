package com.example.propuestasonidosonic.data.memory;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PasarelaPago;
import com.example.propuestasonidosonic.domain.TarjetaUtils;

import java.util.UUID;

public class PasarelaSimulada implements PasarelaPago {

    @Override
    public void cobrar(double monto, TarjetaDatos tarjeta, Callback<ResultadoPago> callback) {
        if (callback == null) return;
        if (tarjeta == null) {
            callback.onError("Datos de tarjeta nulos");
            return;
        }

        String digitos = TarjetaUtils.soloDigitos(tarjeta.getNumero());
        String ultimos4 = digitos.length() >= 4 ? digitos.substring(digitos.length() - 4) : digitos;

        if ("4000000000000002".equals(digitos)) {
            callback.onSuccess(new ResultadoPago(false, null, "La tarjeta fue rechazada por el banco", ultimos4));
        } else if ("4000000000009995".equals(digitos)) {
            callback.onSuccess(new ResultadoPago(false, null, "Fondos insuficientes", ultimos4));
        } else {
            String hex8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String referencia = "SIM-" + hex8;
            callback.onSuccess(new ResultadoPago(true, referencia, null, ultimos4));
        }
    }
}
