package com.example.propuestasonidosonic.data.memory;

import com.example.propuestasonidosonic.model.Pedido;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Adapta los pedidos guardados con el esquema anterior (anticipos, apartados y reembolsos)
 * al esquema actual. Si devuelve true, el documento se vuelve a guardar completo y así
 * Firestore deja de tener los campos que ya no se usan.
 */
final class MigracionPedidos {

    // Estados del esquema anterior
    private static final String LEGADO_APARTADO = "APARTADO";
    private static final String LEGADO_PAGADO = "PAGADO";
    private static final String LEGADO_EN_REVISION = "EN_REVISION";
    private static final String LEGADO_COMPLETADO = "COMPLETADO";

    // Campos que ya no forman parte del modelo
    private static final String[] CAMPOS_OBSOLETOS = {
            "anticipoRequerido", "anticipoPagado", "metodoAnticipo", "fechaAnticipo",
            "reembolsoMonto", "vencimientoApartadoMillis", "requiereRevision",
            "cobrado", "fechaCobro"
    };

    private MigracionPedidos() {}

    static boolean normalizar(Pedido p, DocumentSnapshot doc) {
        if (p == null) return false;
        boolean cambio = false;

        if (doc != null) {
            for (String campo : CAMPOS_OBSOLETOS) {
                if (doc.contains(campo)) {
                    cambio = true;
                    break;
                }
            }

            // Un pedido que ya estaba liquidado se conserva como pago confirmado
            if (!p.isPagoConfirmado() && Boolean.TRUE.equals(doc.getBoolean("cobrado"))) {
                p.setPagoConfirmado(true);
                Long fecha = doc.getLong("fechaCobro");
                p.setFechaPagoMillis(fecha != null && fecha > 0 ? fecha : p.getFechaActualizacion());
                String metodo = doc.getString("metodoAnticipo");
                p.setMetodoPago(Pedido.METODO_TRANSFERENCIA.equals(metodo)
                        ? Pedido.METODO_TRANSFERENCIA : Pedido.METODO_EFECTIVO);
                cambio = true;
            }
        }

        String estado = p.getEstado();
        if (estado == null || LEGADO_APARTADO.equals(estado)) {
            // Las solicitudes de la app esperan aprobación; las registradas por un socio quedan confirmadas
            p.setEstado(Pedido.ORIGEN_APP.equals(p.getCanalOrigen())
                    ? Pedido.ESTADO_POR_APROBAR : Pedido.ESTADO_CONFIRMADO);
            cambio = true;
        } else if (LEGADO_EN_REVISION.equals(estado)) {
            p.setEstado(Pedido.ESTADO_POR_APROBAR);
            cambio = true;
        } else if (LEGADO_PAGADO.equals(estado)) {
            p.setEstado(Pedido.ESTADO_CONFIRMADO);
            cambio = true;
        } else if (LEGADO_COMPLETADO.equals(estado)) {
            p.setEstado(p.isPagoConfirmado() ? Pedido.ESTADO_FINALIZADO : Pedido.ESTADO_POR_COBRAR);
            cambio = true;
        }
        return cambio;
    }
}
