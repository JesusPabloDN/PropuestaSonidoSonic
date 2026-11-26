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
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.CompartirRecibo;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TicketPdf;
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

import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.Evento;

public class PedidoDetalleFragment extends Fragment {

    private String pedidoId;
    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;
    private EquipoRepository equipoRepository;
    private SessionManager sessionManager;
    private EventoRepository eventoRepository;

    private TextView tvChecklistResumen;
    private TextView tvChecklistObservacionesTitulo;
    private TextView tvChecklistObservacionesLista;

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
    private TextView tvEstadoPago;
    private TextView tvDetallePago;
    private MaterialButton btnVerTicket;

    private MaterialCardView cardCancelacion;
    private TextView tvCancelacionTitulo;
    private TextView tvCanceladoPor;
    private TextView tvMotivoCancelacion;

    private View layoutAcciones;
    private MaterialButton btnConfirmarPago;
    private MaterialButton btnReprogramar;
    private MaterialButton btnCancelar;

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
        sessionManager = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager();
        eventoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getEventoRepository();

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
        tvChecklistResumen = view.findViewById(R.id.tvChecklistResumen);
        tvChecklistObservacionesTitulo = view.findViewById(R.id.tvChecklistObservacionesTitulo);
        tvChecklistObservacionesLista = view.findViewById(R.id.tvChecklistObservacionesLista);

        tvTotal = view.findViewById(R.id.tvTotal);
        tvEstadoPago = view.findViewById(R.id.tvEstadoPago);
        tvDetallePago = view.findViewById(R.id.tvDetallePago);
        btnVerTicket = view.findViewById(R.id.btnVerTicket);

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
        btnConfirmarPago = view.findViewById(R.id.btnConfirmarPago);
        btnReprogramar = view.findViewById(R.id.btnReprogramar);
        btnCancelar = view.findViewById(R.id.btnCancelar);
    }

    private void observarCambios() {
        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            cargarPedido();
        });
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

        // 0. Solicitud por aprobar
        if (Pedido.ESTADO_POR_APROBAR.equals(pedido.getEstado())) {
            cardRevision.setVisibility(View.VISIBLE);
            // Tarjeta neutra del tema; el acento ambar va solo en el titulo (rol de acento sobre superficie)
            tvRevisionTitulo.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoPendiente));

            StringBuilder motivo = new StringBuilder(getString(R.string.msg_solicitud_por_aprobar));
            if (pedido.getMotivosRevision() != null) {
                for (String m : pedido.getMotivosRevision()) {
                    motivo.append("\n• ").append(Pedido.MOTIVO_URGENTE.equals(m)
                            ? getString(R.string.motivo_revision_urgente) : m);
                }
            }
            tvRevisionMotivo.setText(motivo.toString());

            btnAprobarPedido.setOnClickListener(v -> mostrarDialogoAprobar(pedido));
            btnRechazarPedido.setOnClickListener(v -> mostrarDialogoRechazar(pedido));
        } else {
            cardRevision.setVisibility(View.GONE);
        }

        // 1. Encabezado
        tvNombreEvento.setText(pedido.getNombreEvento());
        tvFolio.setText(ctx.getString(R.string.folio_formato, pedido.getNumero()));
        BadgeUtils.pintarEstadoPedido(tvEstadoBadge, pedido.getEstado());

        if (pedido.isPagoConfirmado()) {
            tvLiquidadoBadge.setVisibility(View.VISIBLE);
            BadgeUtils.pintarPagado(tvLiquidadoBadge);
        } else {
            tvLiquidadoBadge.setVisibility(View.GONE);
        }

        if (Pedido.ESTADO_POR_APROBAR.equals(pedido.getEstado())) {
            // La solicitud vence si nadie la aprueba antes de que empiece el evento
            long restanteMs = pedido.getInicioMillis() - System.currentTimeMillis();
            tvVencimiento.setVisibility(View.VISIBLE);
            tvVencimiento.setText(ctx.getString(R.string.vence_en_formato, FormatUtils.tiempoRestante(restanteMs)));
            if (restanteMs < 24 * 3600_000L) {
                tvVencimiento.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDanado));
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
                || EstadosPedido.ocupaRecursos(pedido.getEstado())
                || EstadosPedido.esRealizado(pedido.getEstado());

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

            if (pedido.getEventoId() != null && eventoRepository != null) {
                eventoRepository.getEventoPorId(pedido.getEventoId(), new Callback<Evento>() {
                    @Override
                    public void onSuccess(Evento evento) {
                        if (!isAdded() || evento == null) return;
                        List<ChecklistItem> cl = evento.getChecklist();
                        if (cl != null && !cl.isEmpty()) {
                            int[] res = AsistenteUtils.resumenChecklist(cl);
                            tvChecklistResumen.setText(getString(R.string.checklist_resumen_socio_formato,
                                    res[1], res[0], res[2], res[0]));
                            tvChecklistResumen.setVisibility(View.VISIBLE);

                            if (res[3] > 0) {
                                StringBuilder sbObs = new StringBuilder();
                                for (ChecklistItem it : cl) {
                                    if (it != null && it.getObservacion() != null && !it.getObservacion().trim().isEmpty()) {
                                        if (sbObs.length() > 0) sbObs.append("\n");
                                        sbObs.append("• ").append(it.getNombre()).append(": ").append(it.getObservacion().trim());
                                    }
                                }
                                tvChecklistObservacionesLista.setText(sbObs.toString());
                                tvChecklistObservacionesTitulo.setVisibility(View.VISIBLE);
                                tvChecklistObservacionesLista.setVisibility(View.VISIBLE);
                            } else {
                                tvChecklistObservacionesTitulo.setVisibility(View.GONE);
                                tvChecklistObservacionesLista.setVisibility(View.GONE);
                            }
                        } else {
                            tvChecklistResumen.setVisibility(View.GONE);
                            tvChecklistObservacionesTitulo.setVisibility(View.GONE);
                            tvChecklistObservacionesLista.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        tvChecklistResumen.setVisibility(View.GONE);
                        tvChecklistObservacionesTitulo.setVisibility(View.GONE);
                        tvChecklistObservacionesLista.setVisibility(View.GONE);
                    }
                });
            } else {
                tvChecklistResumen.setVisibility(View.GONE);
                tvChecklistObservacionesTitulo.setVisibility(View.GONE);
                tvChecklistObservacionesLista.setVisibility(View.GONE);
            }
        }

        // 6. Dinero: lo que el cliente debe pagar y si un socio ya confirmó el pago
        tvTotal.setText(getString(R.string.label_monto_a_pagar, FormatUtils.formatoMoneda(pedido.getTotal())));
        if (pedido.isPagoConfirmado()) {
            tvEstadoPago.setText(R.string.estado_pago_recibido);
            tvEstadoPago.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDisponible));
            String quien = pedido.getPagoConfirmadoPorNombre() != null && !pedido.getPagoConfirmadoPorNombre().isEmpty()
                    ? pedido.getPagoConfirmadoPorNombre() : getString(R.string.rol_socio);
            tvDetallePago.setText(getString(R.string.detalle_pago_confirmado,
                    EstadosPedido.etiquetaMetodo(pedido.getMetodoPago()),
                    FormatUtils.fechaHoraCorta(pedido.getFechaPagoMillis()),
                    quien));
            tvDetallePago.setVisibility(View.VISIBLE);
            btnVerTicket.setVisibility(View.VISIBLE);
            btnVerTicket.setOnClickListener(v -> compartirTicket(pedidoRepository.getPedidoPorId(pedidoId)));
        } else {
            btnVerTicket.setVisibility(View.GONE);
            if (EstadosPedido.esAceptado(pedido.getEstado())) {
                tvEstadoPago.setText(R.string.estado_pago_pendiente);
                tvEstadoPago.setTextColor(Pedido.ESTADO_POR_COBRAR.equals(pedido.getEstado())
                        ? BadgeUtils.colorError(ctx)
                        : MaterialColors.getColor(tvEstadoPago, com.google.android.material.R.attr.colorOnSurface));
                tvDetallePago.setText(Pedido.ESTADO_POR_COBRAR.equals(pedido.getEstado())
                        ? R.string.detalle_pago_por_cobrar : R.string.detalle_pago_pendiente);
                tvDetallePago.setVisibility(View.VISIBLE);
            } else if (Pedido.ESTADO_POR_APROBAR.equals(pedido.getEstado())) {
                tvEstadoPago.setText(R.string.estado_pago_sin_aprobar);
                tvEstadoPago.setTextColor(MaterialColors.getColor(tvEstadoPago, com.google.android.material.R.attr.colorOnSurfaceVariant));
                tvDetallePago.setVisibility(View.GONE);
            } else {
                tvEstadoPago.setText(R.string.estado_pago_no_aplica);
                tvEstadoPago.setTextColor(MaterialColors.getColor(tvEstadoPago, com.google.android.material.R.attr.colorOnSurfaceVariant));
                tvDetallePago.setVisibility(View.GONE);
            }
        }

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

    private void configurarBotonesAccion(Pedido pedido) {
        long ahora = System.currentTimeMillis();
        boolean confirmarPago = EstadosPedido.puedeConfirmarPago(pedido);
        boolean reprogramar = EstadosPedido.puedeReprogramarse(pedido, ahora);
        // Una solicitud por aprobar se rechaza desde su tarjeta; cancelar aplica a pedidos confirmados
        boolean cancelar = Pedido.ESTADO_CONFIRMADO.equals(pedido.getEstado())
                && EstadosPedido.puedeCancelarse(pedido, ahora);

        btnConfirmarPago.setVisibility(confirmarPago ? View.VISIBLE : View.GONE);
        btnReprogramar.setVisibility(reprogramar ? View.VISIBLE : View.GONE);
        btnCancelar.setVisibility(cancelar ? View.VISIBLE : View.GONE);
        layoutAcciones.setVisibility(confirmarPago || reprogramar || cancelar ? View.VISIBLE : View.GONE);

        if (confirmarPago) {
            btnConfirmarPago.setOnClickListener(v -> mostrarDialogoConfirmarPago(pedido));
        }
        if (reprogramar) {
            btnReprogramar.setOnClickListener(v -> mostrarDialogoReprogramar(pedido));
        }
        if (cancelar) {
            btnCancelar.setText(R.string.btn_cancelar_pedido);
            btnCancelar.setOnClickListener(v -> mostrarDialogoCancelar(pedido));
        }
    }

    private void mostrarDialogoConfirmarPago(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirmar_pago, null);
        TextView tvMonto = dialogView.findViewById(R.id.tvMontoConfirmar);
        TextView tvConcepto = dialogView.findViewById(R.id.tvConceptoConfirmar);
        TextView tvError = dialogView.findViewById(R.id.tvErrorMetodo);
        MaterialButtonToggleGroup toggleMetodo = dialogView.findViewById(R.id.toggleMetodoPago);

        tvMonto.setText(FormatUtils.formatoMoneda(pedido.getTotal()));
        String paquete = pedido.getPaqueteNombre() != null ? pedido.getPaqueteNombre() : "";
        String evento = pedido.getNombreEvento() != null ? pedido.getNombreEvento() : "";
        tvConcepto.setText(paquete.isEmpty() ? evento : paquete + " · " + evento);
        toggleMetodo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) tvError.setVisibility(View.GONE);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirmar_pago_titulo)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .setPositiveButton(R.string.btn_confirmar_pago_corto, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int checkedId = toggleMetodo.getCheckedButtonId();
            String metodo;
            if (checkedId == R.id.btnMetodoEfectivo) {
                metodo = Pedido.METODO_EFECTIVO;
            } else if (checkedId == R.id.btnMetodoTransferencia) {
                metodo = Pedido.METODO_TRANSFERENCIA;
            } else {
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            Usuario socio = sessionManager != null ? sessionManager.getUsuarioActual() : null;
            String socioId = socio != null ? socio.getId() : null;
            String socioNombre = socio != null ? socio.getNombre() : null;

            pedidoRepository.confirmarPago(pedido.getId(), metodo, socioId, socioNombre, new Callback<Pedido>() {
                @Override
                public void onSuccess(Pedido result) {
                    if (!isAdded()) return;
                    dialog.dismiss();
                    cargarPedido();
                    Snackbar.make(requireView(), R.string.msg_pago_confirmado, Snackbar.LENGTH_LONG)
                            .setAction(R.string.btn_ticket, snackView -> compartirTicket(result))
                            .show();
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    tvError.setText(error != null ? error : getString(R.string.err_confirmar_pago));
                    tvError.setVisibility(View.VISIBLE);
                }
            });
        }));

        dialog.show();
    }

    private void compartirTicket(Pedido pedido) {
        if (!isAdded() || pedido == null) return;
        try {
            File pdf = TicketPdf.generar(requireContext(), pedido);
            CompartirRecibo.compartir(requireContext(), pdf, getString(R.string.titulo_compartir_ticket));
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.err_generar_recibo, Snackbar.LENGTH_SHORT).show();
        }
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
        TextInputEditText etMotivo = dialogView.findViewById(R.id.etMotivoCancelacion);

        final String[] quienCancelaHolder = new String[]{Pedido.CANCELA_CLIENTE};

        toggleQuien.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnCancelaSocios) {
                quienCancelaHolder[0] = Pedido.CANCELA_SOCIO;
            } else {
                quienCancelaHolder[0] = Pedido.CANCELA_CLIENTE;
            }
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
}
