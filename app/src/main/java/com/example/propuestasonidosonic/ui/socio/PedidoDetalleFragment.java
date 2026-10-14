package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.ui.comun.SelectorHorarioHelper;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.domain.ReciboUtils;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.CompartirRecibo;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.ReciboPdf;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PedidoDetalleFragment extends Fragment {

    private String pedidoId;
    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;
    private EquipoRepository equipoRepository;
    private PagoRepository pagoRepository;

    private MaterialCardView cardRevision;
    private TextView tvRevisionTitulo;
    private TextView tvRevisionMotivo;
    private MaterialButton btnRechazarPedido;
    private MaterialButton btnAprobarPedido;

    private TextView tvNombreEvento;
    private TextView tvFolio;
    private TextView tvEstadoBadge;
    private TextView tvLiquidadoBadge;
    private TextView tvVencimiento;

    private TextView tvClienteNombre;
    private TextView tvClienteTelefono;
    private TextView tvClienteDireccion;
    private TextView tvClienteOrigen;
    private TextView tvClienteNotas;

    private TextView tvPaqueteNombre;
    private TextView tvComponentesLista;

    private TextView tvHorarioFecha;
    private TextView tvHorarioEvento;
    private TextView tvHorarioMontaje;
    private TextView tvHorarioDesmontaje;

    private TextView tvPersonalAsignado;

    private MaterialCardView cardEquipoAsignado;
    private TextView tvAvisoAsignacionIncompleta;
    private TextView tvEquipoAsignadoLista;

    private TextView tvTotal;
    private TextView tvAnticipoRequerido;
    private TextView tvAnticipoPagado;
    private TextView tvSaldoPendiente;
    private TextView tvReembolsoMonto;

    private MaterialCardView cardCancelacion;
    private TextView tvCancelacionTitulo;
    private TextView tvCanceladoPor;
    private TextView tvMotivoCancelacion;

    private TextView tvHistorialVacio;
    private android.widget.LinearLayout layoutPagosContainer;

    private View layoutAcciones;
    private MaterialButton btnCobrarSaldo;
    private MaterialButton btnRegistrarAnticipo;
    private MaterialButton btnReprogramar;
    private MaterialButton btnCancelar;

    private List<Pago> ultimosPagos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pedido_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        usuarioRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getUsuarioRepository();
        equipoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getEquipoRepository();
        pagoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPagoRepository();

        if (getArguments() != null) {
            pedidoId = getArguments().getString("pedidoId");
        }

        if (pedidoId == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        vincularVistas(view);
        cargarPedido();
        observarCambios();
    }

    private void vincularVistas(View view) {
        tvNombreEvento = view.findViewById(R.id.tvNombreEvento);
        tvFolio = view.findViewById(R.id.tvFolio);
        tvEstadoBadge = view.findViewById(R.id.tvEstadoBadge);
        tvLiquidadoBadge = view.findViewById(R.id.tvLiquidadoBadge);
        tvVencimiento = view.findViewById(R.id.tvVencimiento);

        tvClienteNombre = view.findViewById(R.id.tvClienteNombre);
        tvClienteTelefono = view.findViewById(R.id.tvClienteTelefono);
        tvClienteDireccion = view.findViewById(R.id.tvClienteDireccion);
        tvClienteOrigen = view.findViewById(R.id.tvClienteOrigen);
        tvClienteNotas = view.findViewById(R.id.tvClienteNotas);

        tvPaqueteNombre = view.findViewById(R.id.tvPaqueteNombre);
        tvComponentesLista = view.findViewById(R.id.tvComponentesLista);

        tvHorarioFecha = view.findViewById(R.id.tvHorarioFecha);
        tvHorarioEvento = view.findViewById(R.id.tvHorarioEvento);
        tvHorarioMontaje = view.findViewById(R.id.tvHorarioMontaje);
        tvHorarioDesmontaje = view.findViewById(R.id.tvHorarioDesmontaje);

        tvPersonalAsignado = view.findViewById(R.id.tvPersonalAsignado);

        cardEquipoAsignado = view.findViewById(R.id.cardEquipoAsignado);
        tvAvisoAsignacionIncompleta = view.findViewById(R.id.tvAvisoAsignacionIncompleta);
        tvEquipoAsignadoLista = view.findViewById(R.id.tvEquipoAsignadoLista);

        tvTotal = view.findViewById(R.id.tvTotal);
        tvAnticipoRequerido = view.findViewById(R.id.tvAnticipoRequerido);
        tvAnticipoPagado = view.findViewById(R.id.tvAnticipoPagado);
        tvSaldoPendiente = view.findViewById(R.id.tvSaldoPendiente);
        tvReembolsoMonto = view.findViewById(R.id.tvReembolsoMonto);

        tvHistorialVacio = view.findViewById(R.id.tvHistorialVacio);
        layoutPagosContainer = view.findViewById(R.id.layoutPagosContainer);

        cardRevision = view.findViewById(R.id.cardRevision);
        tvRevisionTitulo = view.findViewById(R.id.tvRevisionTitulo);
        tvRevisionMotivo = view.findViewById(R.id.tvRevisionMotivo);
        btnRechazarPedido = view.findViewById(R.id.btnRechazarPedido);
        btnAprobarPedido = view.findViewById(R.id.btnAprobarPedido);

        cardCancelacion = view.findViewById(R.id.cardCancelacion);
        tvCancelacionTitulo = view.findViewById(R.id.tvCancelacionTitulo);
        tvCanceladoPor = view.findViewById(R.id.tvCanceladoPor);
        tvMotivoCancelacion = view.findViewById(R.id.tvMotivoCancelacion);

        layoutAcciones = view.findViewById(R.id.layoutAcciones);
        btnCobrarSaldo = view.findViewById(R.id.btnCobrarSaldo);
        btnRegistrarAnticipo = view.findViewById(R.id.btnRegistrarAnticipo);
        btnReprogramar = view.findViewById(R.id.btnReprogramar);
        btnCancelar = view.findViewById(R.id.btnCancelar);
    }

    private void observarCambios() {
        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            cargarPedido();
        });
        if (pagoRepository != null) {
            pagoRepository.getPagosPorPedido(pedidoId).observe(getViewLifecycleOwner(), pagos -> {
                ultimosPagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
                Pedido p = pedidoRepository.getPedidoPorId(pedidoId);
                if (p != null) {
                    actualizarHistorialPagos(p, ultimosPagos);
                }
            });
        }
    }

    private void cargarPedido() {
        if (!isAdded()) return;

        Pedido pedido = pedidoRepository.getPedidoPorId(pedidoId);
        if (pedido == null) {
            Toast.makeText(requireContext(), "Pedido no encontrado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        Context ctx = requireContext();

        // 0. Alerta de Revisión
        if (Pedido.ESTADO_EN_REVISION.equals(pedido.getEstado())) {
            cardRevision.setVisibility(View.VISIBLE);
            int colorAmbarContainer = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
            int colorAmbarTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
            cardRevision.setCardBackgroundColor(colorAmbarContainer);
            cardRevision.setStrokeColor(colorAmbarTexto);
            tvRevisionTitulo.setTextColor(colorAmbarTexto);

            String motivoLegible = getString(R.string.motivo_revision_urgente);
            if (pedido.getMotivosRevision() != null && !pedido.getMotivosRevision().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String m : pedido.getMotivosRevision()) {
                    if (sb.length() > 0) sb.append(", ");
                    if (Pedido.MOTIVO_URGENTE.equals(m)) {
                        sb.append(getString(R.string.motivo_revision_urgente));
                    } else {
                        sb.append(m);
                    }
                }
                motivoLegible = sb.toString();
            }
            tvRevisionMotivo.setText(motivoLegible);

            btnAprobarPedido.setOnClickListener(v -> mostrarDialogoAprobar(pedido));
            btnRechazarPedido.setOnClickListener(v -> mostrarDialogoRechazar(pedido));
        } else {
            cardRevision.setVisibility(View.GONE);
        }

        // 1. Encabezado
        tvNombreEvento.setText(pedido.getNombreEvento());
        tvFolio.setText(ctx.getString(R.string.folio_formato, pedido.getNumero()));
        pintarBadgeEstado(ctx, pedido.getEstado());

        if (pedido.estaLiquidado() && !Pedido.ESTADO_CANCELADO.equals(pedido.getEstado())) {
            tvLiquidadoBadge.setVisibility(View.VISIBLE);
            BadgeUtils.pintar(tvLiquidadoBadge,
                    ContextCompat.getColor(ctx, R.color.status_disponible),
                    ContextCompat.getColor(ctx, R.color.status_disponible_container));
        } else {
            tvLiquidadoBadge.setVisibility(View.GONE);
        }

        if (Pedido.ESTADO_APARTADO.equals(pedido.getEstado())) {
            long restanteMs = pedido.getVencimientoApartadoMillis() - System.currentTimeMillis();
            tvVencimiento.setVisibility(View.VISIBLE);
            tvVencimiento.setText(ctx.getString(R.string.vence_en_formato, FormatUtils.tiempoRestante(restanteMs)));
            if (restanteMs < 2 * 3600_000L) {
                tvVencimiento.setTextColor(ContextCompat.getColor(ctx, R.color.status_danado));
            } else {
                tvVencimiento.setTextColor(MaterialColors.getColor(tvVencimiento,
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
            }
        } else {
            tvVencimiento.setVisibility(View.GONE);
        }

        // 2. Cliente
        tvClienteNombre.setText(pedido.getClienteNombre());
        tvClienteNombre.setOnClickListener(v -> {
            if (!isAdded()) return;
            String clave = ClienteUtils.clave(pedido.getClienteId(), pedido.getClienteTelefono(), pedido.getClienteNombre());
            Bundle args = new Bundle();
            args.putString("clienteKey", clave);
            Navigation.findNavController(v).navigate(R.id.action_detalle_to_cliente, args);
        });
        tvClienteTelefono.setText("Tel: " + pedido.getClienteTelefono());
        tvClienteDireccion.setText("Dirección: " + pedido.getDireccion());
        tvClienteOrigen.setText(getString(R.string.label_origen_valor, pedido.getCanalOrigen()));

        if (pedido.getNotas() != null && !pedido.getNotas().trim().isEmpty()) {
            tvClienteNotas.setVisibility(View.VISIBLE);
            tvClienteNotas.setText("Notas: " + pedido.getNotas().trim());
        } else {
            tvClienteNotas.setVisibility(View.GONE);
        }

        // 3. Paquete y Equipo
        tvPaqueteNombre.setText(pedido.getPaqueteNombre() != null ? pedido.getPaqueteNombre() : "");
        StringBuilder sbComp = new StringBuilder();
        if (pedido.getComponentes() != null && !pedido.getComponentes().isEmpty()) {
            for (ComponentePaquete cp : pedido.getComponentes()) {
                if (sbComp.length() > 0) sbComp.append("\n");
                sbComp.append("• ").append(cp.getCantidad()).append(" × ").append(cp.getNombreModelo());
            }
        } else {
            sbComp.append("Sin componentes registrados");
        }
        tvComponentesLista.setText(sbComp.toString());

        // 4. Horario
        tvHorarioFecha.setText(FormatUtils.fechaLarga(pedido.getInicioMillis()));
        int durMin = (int) ((pedido.getFinMillis() - pedido.getInicioMillis()) / 60_000L);
        int durHoras = durMin / 60;
        int durResto = durMin % 60;
        String durTexto = durResto > 0 ? durHoras + " h " + durResto + " min" : durHoras + " h";
        tvHorarioEvento.setText(getString(R.string.label_horario_evento,
                FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis()), durTexto));

        long bloqIni = pedido.getBloqueoInicioMillis() > 0 ? pedido.getBloqueoInicioMillis() : pedido.getInicioMillis();
        long bloqFin = pedido.getBloqueoFinMillis() > 0 ? pedido.getBloqueoFinMillis() : pedido.getFinMillis();
        tvHorarioMontaje.setText(getString(R.string.label_resumen_montaje,
                FormatUtils.rangoHorario(bloqIni, pedido.getInicioMillis()).split("–")[0]));
        tvHorarioDesmontaje.setText(getString(R.string.label_resumen_desmontaje,
                FormatUtils.rangoHorario(pedido.getFinMillis(), bloqFin).split("–")[1]));

        // 5. Personal Asignado
        StringBuilder sbOp = new StringBuilder();
        if (pedido.getOperadorIds() != null && !pedido.getOperadorIds().isEmpty()) {
            for (String opId : pedido.getOperadorIds()) {
                Usuario u = usuarioRepository.getUsuarioPorId(opId);
                if (u != null) {
                    if (sbOp.length() > 0) sbOp.append(", ");
                    String rolLabel = Usuario.ROL_SOCIO.equals(u.getRol()) ? "Socio" : "Asistente";
                    sbOp.append(u.getNombre()).append(" (").append(rolLabel).append(")");
                }
            }
        }
        tvPersonalAsignado.setText(sbOp.length() > 0 ? sbOp.toString() : "Ninguno asignado");

        // 5b. Equipo Asignado
        boolean tieneEvento = pedido.getEventoId() != null
                || Pedido.ESTADO_CONFIRMADO.equals(pedido.getEstado())
                || Pedido.ESTADO_PAGADO.equals(pedido.getEstado())
                || Pedido.ESTADO_EN_REVISION.equals(pedido.getEstado())
                || Pedido.ESTADO_COMPLETADO.equals(pedido.getEstado());

        if (!tieneEvento) {
            cardEquipoAsignado.setVisibility(View.GONE);
        } else {
            cardEquipoAsignado.setVisibility(View.VISIBLE);
            tvAvisoAsignacionIncompleta.setVisibility(pedido.isAsignacionIncompleta() ? View.VISIBLE : View.GONE);

            Map<String, Equipo> equiposMap = new HashMap<>();
            if (equipoRepository != null) {
                for (Equipo eq : equipoRepository.listarTodos()) {
                    if (eq != null) equiposMap.put(eq.getId(), eq);
                }
            }

            Map<String, List<Integer>> porModelo = new LinkedHashMap<>();
            int eliminadas = 0;
            if (pedido.getEquipoIds() != null) {
                for (String eqId : pedido.getEquipoIds()) {
                    Equipo eq = equiposMap.get(eqId);
                    if (eq != null) {
                        String nombre = eq.getNombre() != null ? eq.getNombre().trim() : "Equipo";
                        if (!porModelo.containsKey(nombre)) {
                            porModelo.put(nombre, new ArrayList<>());
                        }
                        porModelo.get(nombre).add(eq.getNumeroUnidad());
                    } else {
                        eliminadas++;
                    }
                }
            }

            StringBuilder sbEquipo = new StringBuilder();
            for (Map.Entry<String, List<Integer>> entry : porModelo.entrySet()) {
                List<Integer> nums = entry.getValue();
                Collections.sort(nums);
                StringBuilder numsSb = new StringBuilder();
                for (int i = 0; i < nums.size(); i++) {
                    if (i > 0) numsSb.append(", ");
                    numsSb.append(nums.get(i));
                }
                if (sbEquipo.length() > 0) sbEquipo.append("\n");
                String formato = nums.size() == 1
                        ? ctx.getString(R.string.formato_modelo_unidad_singular, entry.getKey(), numsSb.toString())
                        : ctx.getString(R.string.formato_modelo_unidades, entry.getKey(), numsSb.toString());
                sbEquipo.append("• ").append(formato);
            }

            for (int i = 0; i < eliminadas; i++) {
                if (sbEquipo.length() > 0) sbEquipo.append("\n");
                sbEquipo.append("• ").append(ctx.getString(R.string.unidad_eliminada));
            }

            if (sbEquipo.length() == 0) {
                sbEquipo.append("Sin equipo asignado");
            }
            tvEquipoAsignadoLista.setText(sbEquipo.toString());
        }

        // 6. Dinero
        tvTotal.setText("Total: " + FormatUtils.formatoMoneda(pedido.getTotal()));
        tvAnticipoRequerido.setText(getString(R.string.label_resumen_anticipo,
                FormatUtils.formatoMoneda(pedido.getAnticipoRequerido())));

        if (pedido.getAnticipoPagado() > 0) {
            String metodo = pedido.getMetodoAnticipo() != null ? pedido.getMetodoAnticipo() : "Efectivo";
            String fechaPago = FormatUtils.formatoFecha(pedido.getFechaAnticipo());
            tvAnticipoPagado.setText(getString(R.string.label_anticipo_cubierto,
                    FormatUtils.formatoMoneda(pedido.getAnticipoPagado()), metodo, fechaPago));
        } else {
            tvAnticipoPagado.setText(R.string.label_sin_anticipo);
        }

        double saldo = pedido.getSaldoPendiente();
        tvSaldoPendiente.setText(getString(R.string.label_saldo_pendiente, FormatUtils.formatoMoneda(saldo)));

        if (pedido.getReembolsoMonto() > 0) {
            tvReembolsoMonto.setVisibility(View.VISIBLE);
            tvReembolsoMonto.setText(getString(R.string.label_reembolso_simulado,
                    FormatUtils.formatoMoneda(pedido.getReembolsoMonto())));
        } else {
            tvReembolsoMonto.setVisibility(View.GONE);
        }

        // Historial de pagos
        actualizarHistorialPagos(pedido, ultimosPagos);

        // 7. Cancelación y Rechazo
        if (Pedido.ESTADO_CANCELADO.equals(pedido.getEstado())) {
            cardCancelacion.setVisibility(View.VISIBLE);
            if (tvCancelacionTitulo != null) tvCancelacionTitulo.setText(R.string.seccion_cancelacion);
            tvCanceladoPor.setVisibility(View.VISIBLE);
            tvCanceladoPor.setText(getString(R.string.label_cancelado_por,
                    pedido.getCanceladoPor() != null ? pedido.getCanceladoPor() : "Socio"));
            tvMotivoCancelacion.setText(getString(R.string.label_motivo,
                    pedido.getMotivoCancelacion() != null && !pedido.getMotivoCancelacion().isEmpty()
                            ? pedido.getMotivoCancelacion() : "Sin motivo registrado"));
        } else if (Pedido.ESTADO_RECHAZADO.equals(pedido.getEstado())) {
            cardCancelacion.setVisibility(View.VISIBLE);
            if (tvCancelacionTitulo != null) tvCancelacionTitulo.setText("Rechazo");
            tvCanceladoPor.setVisibility(View.GONE);
            tvMotivoCancelacion.setText(getString(R.string.label_motivo,
                    pedido.getMotivoRechazo() != null && !pedido.getMotivoRechazo().isEmpty()
                            ? pedido.getMotivoRechazo() : "Sin motivo registrado"));
        } else {
            cardCancelacion.setVisibility(View.GONE);
        }

        // 8. Botones según estado
        configurarBotonesAccion(pedido);
    }

    private void mostrarDialogoAprobar(Pedido pedido) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_aprobar_pedido_titulo)
                .setMessage(R.string.dialog_aprobar_pedido_mensaje)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .setPositiveButton(R.string.btn_aprobar, (dialog, which) -> {
                    pedidoRepository.aprobarPedido(pedido.getId(), new Callback<Pedido>() {
                        @Override
                        public void onSuccess(Pedido result) {
                            if (!isAdded()) return;
                            Snackbar.make(requireView(), R.string.snackbar_pedido_aprobado, Snackbar.LENGTH_SHORT).show();
                            cargarPedido();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void mostrarDialogoRechazar(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rechazar_pedido, null);
        TextInputLayout tilMotivo = dialogView.findViewById(R.id.tilMotivoRechazo);
        TextInputEditText etMotivo = dialogView.findViewById(R.id.etMotivoRechazo);
        TextView tvAvisoReembolso = dialogView.findViewById(R.id.tvAvisoReembolso);

        double reembolso = pedido.getAnticipoPagado();
        tvAvisoReembolso.setText(getString(R.string.aviso_reembolso_rechazo, FormatUtils.formatoMoneda(reembolso)));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_rechazar_pedido_titulo)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .setPositiveButton(R.string.btn_rechazar, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnConfirmar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirmar.setOnClickListener(v -> {
                String motivo = etMotivo.getText() != null ? etMotivo.getText().toString().trim() : "";
                if (motivo.length() < 3) {
                    tilMotivo.setError(getString(R.string.err_motivo_minimo_3));
                    return;
                }
                tilMotivo.setError(null);
                dialog.dismiss();

                pedidoRepository.rechazarPedido(pedido.getId(), motivo, new Callback<Pedido>() {
                    @Override
                    public void onSuccess(Pedido result) {
                        if (!isAdded()) return;
                        Snackbar.make(requireView(), R.string.snackbar_pedido_rechazado, Snackbar.LENGTH_SHORT).show();
                        cargarPedido();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                    }
                });
            });
        });
        dialog.show();
    }

    private void actualizarHistorialPagos(Pedido pedido, List<Pago> pagos) {
        if (!isAdded()) return;
        List<Pago> lista = new ArrayList<>(pagos != null ? pagos : Collections.emptyList());
        Collections.sort(lista, (p1, p2) -> Long.compare(p1.getFecha(), p2.getFecha()));

        if (lista.isEmpty()) {
            tvHistorialVacio.setVisibility(View.VISIBLE);
            layoutPagosContainer.removeAllViews();
            return;
        }

        tvHistorialVacio.setVisibility(View.GONE);
        layoutPagosContainer.removeAllViews();

        Context ctx = requireContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Pago pago : lista) {
            View itemView = inflater.inflate(R.layout.item_pago, layoutPagosContainer, false);
            TextView tvPagoFolio = itemView.findViewById(R.id.tvPagoFolio);
            TextView tvPagoTipo = itemView.findViewById(R.id.tvPagoTipo);
            TextView tvPagoFechaMetodo = itemView.findViewById(R.id.tvPagoFechaMetodo);
            TextView tvPagoMonto = itemView.findViewById(R.id.tvPagoMonto);
            MaterialButton btnRecibo = itemView.findViewById(R.id.btnRecibo);

            tvPagoFolio.setText(pago.getFolio() != null ? pago.getFolio() : "");
            tvPagoTipo.setText(ReciboUtils.tituloRecibo(pago.getTipo()).replace("Recibo de ", "").replace("Comprobante de ", ""));
            String metodo = pago.getMetodo() != null ? pago.getMetodo() : "";
            tvPagoFechaMetodo.setText(FormatUtils.fechaHoraCorta(pago.getFecha()) + " · " + metodo);

            boolean esReembolso = Pago.TIPO_REEMBOLSO.equals(pago.getTipo());
            if (esReembolso) {
                tvPagoMonto.setText("− " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvPagoMonto.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(ctx));
            } else {
                tvPagoMonto.setText("+ " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvPagoMonto.setTextColor(ContextCompat.getColor(ctx, R.color.status_disponible));
            }

            btnRecibo.setOnClickListener(v -> compartirReciboPdf(pedido, pago, lista));

            layoutPagosContainer.addView(itemView);
        }
    }

    private void configurarBotonesAccion(Pedido pedido) {
        String estado = pedido.getEstado();
        boolean puedeCobrar = (Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_PAGADO.equals(estado)
                || Pedido.ESTADO_EN_REVISION.equals(estado)
                || Pedido.ESTADO_COMPLETADO.equals(estado))
                && pedido.getSaldoPendiente() > 0.005;

        if (Pedido.ESTADO_APARTADO.equals(estado)) {
            layoutAcciones.setVisibility(View.VISIBLE);
            btnCobrarSaldo.setVisibility(View.GONE);
            btnRegistrarAnticipo.setVisibility(View.VISIBLE);
            btnReprogramar.setVisibility(View.VISIBLE);
            btnCancelar.setVisibility(View.VISIBLE);
            btnCancelar.setText(R.string.btn_cancelar_apartado);

            btnRegistrarAnticipo.setOnClickListener(v -> mostrarDialogoRegistrarAnticipo(pedido));
            btnReprogramar.setOnClickListener(v -> mostrarDialogoReprogramar(pedido));
            btnCancelar.setOnClickListener(v -> mostrarDialogoCancelar(pedido));
        } else if (Pedido.ESTADO_EN_REVISION.equals(estado)) {
            layoutAcciones.setVisibility(View.VISIBLE);
            btnRegistrarAnticipo.setVisibility(View.GONE);
            btnReprogramar.setVisibility(View.GONE); // En revisión se oculta reprogramar
            btnCobrarSaldo.setVisibility(puedeCobrar ? View.VISIBLE : View.GONE);
            if (puedeCobrar) {
                btnCobrarSaldo.setOnClickListener(v -> mostrarDialogoCobrarSaldo(pedido));
            }
            btnCancelar.setVisibility(View.VISIBLE);
            btnCancelar.setText(R.string.btn_cancelar_pedido);
            btnCancelar.setOnClickListener(v -> mostrarDialogoCancelar(pedido));
        } else if (Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_PAGADO.equals(estado)) {
            layoutAcciones.setVisibility(View.VISIBLE);
            btnRegistrarAnticipo.setVisibility(View.GONE);
            btnCobrarSaldo.setVisibility(puedeCobrar ? View.VISIBLE : View.GONE);
            if (puedeCobrar) {
                btnCobrarSaldo.setOnClickListener(v -> mostrarDialogoCobrarSaldo(pedido));
            }
            btnReprogramar.setVisibility(View.VISIBLE);
            btnCancelar.setVisibility(View.VISIBLE);
            btnCancelar.setText(R.string.btn_cancelar_pedido);

            btnReprogramar.setOnClickListener(v -> mostrarDialogoReprogramar(pedido));
            btnCancelar.setOnClickListener(v -> mostrarDialogoCancelar(pedido));
        } else if (Pedido.ESTADO_COMPLETADO.equals(estado)) {
            btnRegistrarAnticipo.setVisibility(View.GONE);
            btnReprogramar.setVisibility(View.GONE);
            btnCancelar.setVisibility(View.GONE);
            if (puedeCobrar) {
                layoutAcciones.setVisibility(View.VISIBLE);
                btnCobrarSaldo.setVisibility(View.VISIBLE);
                btnCobrarSaldo.setOnClickListener(v -> mostrarDialogoCobrarSaldo(pedido));
            } else {
                layoutAcciones.setVisibility(View.GONE);
                btnCobrarSaldo.setVisibility(View.GONE);
            }
        } else {
            layoutAcciones.setVisibility(View.GONE);
            btnCobrarSaldo.setVisibility(View.GONE);
            btnRegistrarAnticipo.setVisibility(View.GONE);
            btnReprogramar.setVisibility(View.GONE);
            btnCancelar.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoCobrarSaldo(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cobrar_saldo, null);
        TextInputLayout tilMonto = dialogView.findViewById(R.id.tilMontoCobro);
        TextInputEditText etMonto = dialogView.findViewById(R.id.etMontoCobro);
        MaterialButtonToggleGroup toggleMetodo = dialogView.findViewById(R.id.toggleMetodoPagoCobro);

        double saldo = pedido.getSaldoPendiente();
        etMonto.setText(String.format(java.util.Locale.US, "%.2f", saldo));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_cobrar_saldo_titulo)
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(R.string.btn_confirmar_cobro, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String strMonto = etMonto.getText() != null ? etMonto.getText().toString().trim() : "";
                double monto;
                try {
                    monto = Double.parseDouble(strMonto);
                } catch (NumberFormatException e) {
                    tilMonto.setError(getString(R.string.err_monto_invalido));
                    return;
                }

                if (monto <= 0) {
                    tilMonto.setError(getString(R.string.err_monto_minimo));
                    return;
                }

                if (monto > saldo + 0.005) {
                    tilMonto.setError(getString(R.string.err_monto_mayor_saldo));
                    return;
                }

                tilMonto.setError(null);

                String metodo = Pedido.METODO_EFECTIVO;
                int checkedId = toggleMetodo.getCheckedButtonId();
                if (checkedId == R.id.btnMetodoTransferenciaCobro) {
                    metodo = Pedido.METODO_TRANSFERENCIA;
                } else if (checkedId == R.id.btnMetodoTarjetaCobro) {
                    metodo = Pedido.METODO_TARJETA;
                }

                pedidoRepository.liquidarSaldo(pedido.getId(), monto, metodo, new Callback<Pago>() {
                    @Override
                    public void onSuccess(Pago result) {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        cargarPedido();
                        Snackbar.make(requireView(), R.string.msg_pago_registrado, Snackbar.LENGTH_LONG)
                                .setAction(R.string.btn_recibo, snackView -> {
                                    Pedido pActual = pedidoRepository.getPedidoPorId(pedido.getId());
                                    compartirReciboPdf(pActual != null ? pActual : pedido, result,
                                            ultimosPagos != null && !ultimosPagos.isEmpty() ? ultimosPagos : Collections.singletonList(result));
                                })
                                .show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        tilMonto.setError(error != null ? error : "Error al procesar el pago");
                    }
                });
            });
        });

        dialog.show();
    }

    private void compartirReciboPdf(Pedido pedido, Pago pago, List<Pago> pagosDelPedido) {
        try {
            File pdf = ReciboPdf.generar(requireContext(), pedido, pago, pagosDelPedido);
            CompartirRecibo.compartir(requireContext(), pdf, "Compartir " + ReciboUtils.tituloRecibo(pago.getTipo()));
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.err_generar_recibo, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoRegistrarAnticipo(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_registrar_anticipo, null);
        TextInputLayout tilMonto = dialogView.findViewById(R.id.tilMontoAnticipo);
        TextInputEditText etMonto = dialogView.findViewById(R.id.etMontoAnticipo);
        MaterialButtonToggleGroup toggleMetodo = dialogView.findViewById(R.id.toggleMetodoPago);

        double pendiente = Math.max(0.0, pedido.getAnticipoRequerido() - pedido.getAnticipoPagado());
        etMonto.setText(String.format(java.util.Locale.US, "%.2f", pendiente));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_anticipo_titulo)
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(R.string.btn_confirmar_anticipo, (dialog, which) -> {
                    String strMonto = etMonto.getText() != null ? etMonto.getText().toString().trim() : "";
                    double monto;
                    try {
                        monto = Double.parseDouble(strMonto);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), R.string.err_monto_invalido, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String metodo = Pedido.METODO_EFECTIVO;
                    int checkedId = toggleMetodo.getCheckedButtonId();
                    if (checkedId == R.id.btnMetodoTransferencia) {
                        metodo = Pedido.METODO_TRANSFERENCIA;
                    } else if (checkedId == R.id.btnMetodoTarjeta) {
                        metodo = Pedido.METODO_TARJETA;
                    }

                    pedidoRepository.registrarAnticipo(pedido.getId(), monto, metodo, new Callback<Pedido>() {
                        @Override
                        public void onSuccess(Pedido result) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Anticipo registrado. Pedido confirmado.", Toast.LENGTH_SHORT).show();
                            cargarPedido();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Error al registrar anticipo")
                                    .setMessage(error)
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void mostrarDialogoReprogramar(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reprogramar_pedido, null);
        MaterialButton btnFecha = dialogView.findViewById(R.id.btnSeleccionarFechaReprog);
        ChipGroup chipHorarios = dialogView.findViewById(R.id.chipGroupHorariosReprog);
        View sinHorarios = dialogView.findViewById(R.id.layoutSinHorariosReprog);
        ChipGroup chipAlternativas = dialogView.findViewById(R.id.chipGroupAlternativasReprog);

        final Long[] nuevoInicioHolder = new Long[]{null};

        SelectorHorarioHelper helper = new SelectorHorarioHelper(
                requireContext(),
                getParentFragmentManager(),
                pedidoRepository,
                btnFecha,
                chipHorarios,
                sinHorarios,
                chipAlternativas,
                new SelectorHorarioHelper.OnHorarioSeleccionadoListener() {
                    @Override
                    public void onHorarioSeleccionado(long inicioMillis) {
                        nuevoInicioHolder[0] = inicioMillis;
                    }

                    @Override
                    public void onHorarioLimpiado() {
                        nuevoInicioHolder[0] = null;
                    }
                }
        );

        int durMin = (int) ((pedido.getFinMillis() - pedido.getInicioMillis()) / 60_000L);
        helper.setConfiguracion(pedido.getPaqueteId(), durMin, pedido.getId());
        helper.setFechaSeleccionada(pedido.getInicioMillis(), null);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_reprogramar_titulo)
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(R.string.btn_confirmar_reprogramacion, (dialog, which) -> {
                    if (nuevoInicioHolder[0] == null) {
                        Toast.makeText(requireContext(), R.string.err_horario_no_seleccionado, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pedidoRepository.reprogramarReserva(pedido.getId(), nuevoInicioHolder[0], new Callback<Pedido>() {
                        @Override
                        public void onSuccess(Pedido result) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), R.string.toast_reprogramado_exito, Toast.LENGTH_SHORT).show();
                            cargarPedido();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("No se pudo reprogramar")
                                    .setMessage(error)
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void mostrarDialogoCancelar(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancelar_pedido, null);
        MaterialButtonToggleGroup toggleQuien = dialogView.findViewById(R.id.toggleQuienCancela);
        TextView tvReembolso = dialogView.findViewById(R.id.tvReembolsoCalculado);
        TextInputEditText etMotivo = dialogView.findViewById(R.id.etMotivoCancelacion);

        final String[] quienCancelaHolder = new String[]{Pedido.CANCELA_CLIENTE};

        Runnable actualizarReembolso = () -> {
            double reembolso = pedidoRepository.calcularReembolsoPrevio(pedido.getId(), quienCancelaHolder[0]);
            if (reembolso > 0) {
                tvReembolso.setText(getString(R.string.label_reembolso_calculado, FormatUtils.formatoMoneda(reembolso)));
            } else {
                tvReembolso.setText(R.string.label_sin_reembolso);
            }
        };

        actualizarReembolso.run();

        toggleQuien.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnCancelaSocios) {
                quienCancelaHolder[0] = Pedido.CANCELA_SOCIO;
            } else {
                quienCancelaHolder[0] = Pedido.CANCELA_CLIENTE;
            }
            actualizarReembolso.run();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_cancelar_titulo)
                .setView(dialogView)
                .setNegativeButton("No cancelar", null)
                .setPositiveButton(R.string.btn_confirmar_cancelacion, (dialog, which) -> {
                    String motivo = (etMotivo.getText() != null) ? etMotivo.getText().toString().trim() : "";
                    pedidoRepository.cancelarReserva(pedido.getId(), quienCancelaHolder[0], motivo, new Callback<Pedido>() {
                        @Override
                        public void onSuccess(Pedido result) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), R.string.toast_cancelado_exito, Toast.LENGTH_SHORT).show();
                            cargarPedido();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Error al cancelar")
                                    .setMessage(error)
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void pintarBadgeEstado(Context ctx, String estado) {
        if (estado == null) estado = Pedido.ESTADO_APARTADO;

        int colorTexto;
        int colorFondo;
        int textoRes;

        switch (estado) {
            case Pedido.ESTADO_APARTADO:
                textoRes = R.string.badge_apartado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                break;
            case Pedido.ESTADO_EN_REVISION:
                textoRes = R.string.badge_en_revision;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                break;
            case Pedido.ESTADO_CONFIRMADO:
                textoRes = R.string.badge_confirmado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                break;
            case Pedido.ESTADO_PAGADO:
                textoRes = R.string.badge_pagado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                break;
            case Pedido.ESTADO_COMPLETADO:
                textoRes = R.string.badge_completado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_rentado);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_rentado_container);
                break;
            case Pedido.ESTADO_CANCELADO:
                textoRes = R.string.badge_cancelado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                break;
            case Pedido.ESTADO_RECHAZADO:
                textoRes = R.string.badge_rechazado;
                colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                break;
            case Pedido.ESTADO_VENCIDO:
            default:
                textoRes = R.string.badge_vencido;
                colorTexto = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorOnSurfaceVariant);
                colorFondo = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorSurfaceVariant);
                break;
        }

        tvEstadoBadge.setText(textoRes);
        BadgeUtils.pintar(tvEstadoBadge, colorTexto, colorFondo);
    }
}
