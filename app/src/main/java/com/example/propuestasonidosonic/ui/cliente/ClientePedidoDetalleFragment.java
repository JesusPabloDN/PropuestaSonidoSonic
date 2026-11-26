package com.example.propuestasonidosonic.ui.cliente;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.ContactoUtils;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.CompartirRecibo;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TicketPdf;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.data.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class ClientePedidoDetalleFragment extends Fragment {

    private String pedidoId;

    private PedidoRepository pedidoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private View layoutNoDisponible;
    private View layoutContenido;
    private MaterialButton btnVolverAListaNoDisponible;

    // Sección Estado
    private TextView tvEstadoBadge;
    private TextView tvMensajeContextual;
    private TextView tvMotivoExtra;

    // Sección Tu Evento
    private TextView tvNombreEvento;
    private TextView tvFolio;
    private TextView tvFechaHorario;
    private TextView tvDireccion;
    private TextView tvPaquete;
    private TextView tvNotas;

    // Sección Dinero
    private TextView tvTotal;
    private TextView tvEstadoPago;
    private TextView tvDetallePago;
    private MaterialButton btnDescargarTicket;

    // Sección Acciones
    private MaterialButton btnCancelarReserva;
    private MaterialButton btnCambiarFecha;
    private MaterialButton btnContactar;

    private Pedido pedidoActual;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pedidoId = getArguments().getString("pedidoId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cliente_pedido_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer container = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = container.getPedidoRepository();
        sessionManager = container.getSessionManager();
        reloj = container.getReloj();

        layoutNoDisponible = view.findViewById(R.id.layoutNoDisponible);
        layoutContenido = view.findViewById(R.id.layoutContenido);
        btnVolverAListaNoDisponible = view.findViewById(R.id.btnVolverAListaNoDisponible);

        tvEstadoBadge = view.findViewById(R.id.tvEstadoBadge);
        tvMensajeContextual = view.findViewById(R.id.tvMensajeContextual);
        tvMotivoExtra = view.findViewById(R.id.tvMotivoExtra);

        tvNombreEvento = view.findViewById(R.id.tvNombreEvento);
        tvFolio = view.findViewById(R.id.tvFolio);
        tvFechaHorario = view.findViewById(R.id.tvFechaHorario);
        tvDireccion = view.findViewById(R.id.tvDireccion);
        tvPaquete = view.findViewById(R.id.tvPaquete);
        tvNotas = view.findViewById(R.id.tvNotas);

        tvTotal = view.findViewById(R.id.tvTotal);
        tvEstadoPago = view.findViewById(R.id.tvEstadoPago);
        tvDetallePago = view.findViewById(R.id.tvDetallePago);
        btnDescargarTicket = view.findViewById(R.id.btnDescargarTicket);

        btnCancelarReserva = view.findViewById(R.id.btnCancelarReserva);
        btnCambiarFecha = view.findViewById(R.id.btnCambiarFecha);
        btnContactar = view.findViewById(R.id.btnContactar);

        btnVolverAListaNoDisponible.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            pedidoActual = null;
            if (pedidos != null && pedidoId != null) {
                for (Pedido p : pedidos) {
                    if (pedidoId.equals(p.getId())) {
                        pedidoActual = p;
                        break;
                    }
                }
            }
            actualizarUI();
        });
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        Usuario usuarioActual = sessionManager.getUsuarioActual();
        if (pedidoActual == null || usuarioActual == null
                || !usuarioActual.getId().equals(pedidoActual.getClienteId())) {
            layoutNoDisponible.setVisibility(View.VISIBLE);
            layoutContenido.setVisibility(View.GONE);
            return;
        }

        layoutNoDisponible.setVisibility(View.GONE);
        layoutContenido.setVisibility(View.VISIBLE);

        Context ctx = requireContext();
        String estado = pedidoActual.getEstado();

        // 1. Estado
        BadgeUtils.pintarEstadoPedido(tvEstadoBadge, estado);
        tvEstadoBadge.setText(PedidosClienteUtils.etiquetaEstado(estado));

        switch (estado != null ? estado : "") {
            case Pedido.ESTADO_POR_APROBAR:
                tvMensajeContextual.setText(R.string.msg_contextual_por_aprobar);
                break;
            case Pedido.ESTADO_CONFIRMADO:
                tvMensajeContextual.setText(R.string.msg_contextual_confirmado);
                break;
            case Pedido.ESTADO_POR_COBRAR:
                tvMensajeContextual.setText(R.string.msg_contextual_por_cobrar);
                break;
            case Pedido.ESTADO_FINALIZADO:
                tvMensajeContextual.setText(R.string.msg_contextual_realizado);
                break;
            case Pedido.ESTADO_VENCIDO:
                tvMensajeContextual.setText(R.string.msg_contextual_vencido);
                break;
            case Pedido.ESTADO_CANCELADO:
                tvMensajeContextual.setText(R.string.msg_contextual_cancelado);
                break;
            case Pedido.ESTADO_RECHAZADO:
                tvMensajeContextual.setText(R.string.msg_contextual_rechazado);
                break;
            default:
                tvMensajeContextual.setText(estado);
                break;
        }

        if (Pedido.ESTADO_RECHAZADO.equals(estado)
                && pedidoActual.getMotivoRechazo() != null && !pedidoActual.getMotivoRechazo().isEmpty()) {
            tvMotivoExtra.setVisibility(View.VISIBLE);
            tvMotivoExtra.setText(getString(R.string.label_motivo_rechazo_detalle, pedidoActual.getMotivoRechazo()));
        } else if (Pedido.ESTADO_CANCELADO.equals(estado)
                && pedidoActual.getMotivoCancelacion() != null && !pedidoActual.getMotivoCancelacion().isEmpty()) {
            tvMotivoExtra.setVisibility(View.VISIBLE);
            tvMotivoExtra.setText(getString(R.string.label_motivo, pedidoActual.getMotivoCancelacion()));
        } else {
            tvMotivoExtra.setVisibility(View.GONE);
        }

        // 2. Tu Evento
        tvNombreEvento.setText(pedidoActual.getNombreEvento() != null ? pedidoActual.getNombreEvento() : "");
        tvFolio.setText(getString(R.string.formato_pedido_numero, pedidoActual.getNumero()));

        if (pedidoActual.getInicioMillis() > 0 && pedidoActual.getFinMillis() > 0) {
            String fecha = FormatUtils.fechaLarga(pedidoActual.getInicioMillis());
            String horario = FormatUtils.rangoHorario(pedidoActual.getInicioMillis(), pedidoActual.getFinMillis());
            tvFechaHorario.setText(fecha + " · " + horario);
            tvFechaHorario.setVisibility(View.VISIBLE);
        } else {
            tvFechaHorario.setVisibility(View.GONE);
        }

        String direccion = pedidoActual.getDireccion();
        if (direccion != null && !direccion.trim().isEmpty()) {
            tvDireccion.setText("Dirección: " + direccion);
            tvDireccion.setVisibility(View.VISIBLE);
        } else {
            tvDireccion.setText("Sin dirección registrada");
            tvDireccion.setVisibility(View.VISIBLE);
        }

        String paquete = pedidoActual.getPaqueteNombre();
        if (paquete != null && !paquete.trim().isEmpty()) {
            tvPaquete.setText("Paquete: " + paquete);
            tvPaquete.setVisibility(View.VISIBLE);
        } else {
            tvPaquete.setVisibility(View.GONE);
        }

        String notas = pedidoActual.getNotas();
        if (notas != null && !notas.trim().isEmpty()) {
            tvNotas.setText("Notas: " + notas);
            tvNotas.setVisibility(View.VISIBLE);
        } else {
            tvNotas.setVisibility(View.GONE);
        }

        // 3. Dinero: monto del paquete y si el negocio ya confirmó el pago
        tvTotal.setText(FormatUtils.formatoMoneda(pedidoActual.getTotal()));
        if (pedidoActual.isPagoConfirmado()) {
            tvEstadoPago.setText(R.string.estado_pago_pagado);
            tvEstadoPago.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDisponible));
            tvDetallePago.setText(getString(R.string.detalle_pago_cliente,
                    EstadosPedido.etiquetaMetodo(pedidoActual.getMetodoPago()),
                    FormatUtils.fechaHoraCorta(pedidoActual.getFechaPagoMillis())));
            btnDescargarTicket.setVisibility(View.VISIBLE);
            btnDescargarTicket.setOnClickListener(v -> compartirTicket(pedidoActual));
        } else {
            btnDescargarTicket.setVisibility(View.GONE);
            boolean debe = EstadosPedido.esAceptado(estado) || Pedido.ESTADO_POR_APROBAR.equals(estado);
            tvEstadoPago.setText(debe ? R.string.estado_pago_pendiente_corto : R.string.estado_pago_no_aplica_corto);
            tvEstadoPago.setTextColor(MaterialColors.getColor(tvEstadoPago,
                    com.google.android.material.R.attr.colorOnSurface));
            tvDetallePago.setText(debe ? R.string.aviso_pago_directo : R.string.aviso_pago_no_aplica);
        }

        // 4. Acciones
        long ahora = reloj.ahora();

        // Cancelar reserva
        if (PedidosClienteUtils.puedeCancelar(pedidoActual, ahora)) {
            btnCancelarReserva.setVisibility(View.VISIBLE);
            btnCancelarReserva.setOnClickListener(v -> mostrarDialogoCancelarReserva(pedidoActual));
        } else {
            btnCancelarReserva.setVisibility(View.GONE);
        }

        // Cambiar fecha
        btnCambiarFecha.setOnClickListener(v ->
                abrirWhatsApp(ContactoUtils.mensajeCambioFecha(pedidoActual.getNumero(), pedidoActual.getNombreEvento())));

        // Contactar
        btnContactar.setOnClickListener(v ->
                abrirWhatsApp(ContactoUtils.mensajeConsulta(pedidoActual.getNumero(), pedidoActual.getNombreEvento())));
    }

    private void mostrarDialogoCancelarReserva(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancelar_pedido_cliente, null);
        TextView tvMensaje = dialogView.findViewById(R.id.tvMensajeCancelacion);
        TextInputEditText etMotivo = dialogView.findViewById(R.id.etMotivoCancelacion);

        tvMensaje.setText(Pedido.ESTADO_POR_APROBAR.equals(pedido.getEstado())
                ? R.string.cancelar_solicitud_mensaje
                : R.string.cancelar_pedido_confirmado_mensaje);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_cancelar_reserva_titulo)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_btn_no_cancelar, null)
                .setPositiveButton(R.string.btn_confirmar_cancelacion, (dialog, which) -> {
                    String motivo = etMotivo.getText() != null ? etMotivo.getText().toString().trim() : "";
                    pedidoRepository.cancelarReserva(pedido.getId(), Pedido.CANCELA_CLIENTE, motivo, new Callback<Pedido>() {
                        @Override
                        public void onSuccess(Pedido result) {
                            if (!isAdded()) return;
                            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                                    R.string.toast_cancelado_exito, Snackbar.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
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

    private void compartirTicket(Pedido pedido) {
        if (!isAdded() || pedido == null) return;
        try {
            File pdf = TicketPdf.generar(requireContext(), pedido);
            CompartirRecibo.compartir(requireContext(), pdf, getString(R.string.titulo_compartir_ticket));
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.err_generar_recibo, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void abrirWhatsApp(String mensaje) {
        String url = ContactoUtils.urlWhatsApp(Config.NEGOCIO_WHATSAPP, mensaje);
        if (url.isEmpty()) {
            Snackbar.make(requireView(), R.string.err_abrir_whatsapp, Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.err_abrir_whatsapp, Snackbar.LENGTH_SHORT).show();
        }
    }
}
