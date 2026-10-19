package com.example.propuestasonidosonic.ui.cliente;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.ContactoUtils;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.domain.ReciboUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.CompartirRecibo;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.ReciboPdf;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.data.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClientePedidoDetalleFragment extends Fragment {

    private String pedidoId;

    private PedidoRepository pedidoRepository;
    private PagoRepository pagoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private View layoutNoDisponible;
    private View layoutContenido;
    private MaterialButton btnVolverAListaNoDisponible;

    // Sección Estado
    private TextView tvEstadoBadge;
    private TextView tvMensajeContextual;
    private TextView tvMotivoExtra;
    private TextView tvReembolsoEstado;

    // Sección Tu Evento
    private TextView tvNombreEvento;
    private TextView tvFolio;
    private TextView tvFechaHorario;
    private TextView tvDireccion;
    private TextView tvPaquete;
    private TextView tvNotas;

    // Sección Dinero
    private TextView tvTotal;
    private TextView tvAnticipoPagado;
    private TextView tvSaldoPendiente;
    private TextView tvAvisoSaldo;
    private View layoutReembolsoDinero;
    private TextView tvReembolsoMonto;

    // Sección Pagos
    private TextView tvHistorialVacio;
    private LinearLayout layoutPagosContainer;

    // Sección Acciones
    private LinearLayout layoutPagarAnticipo;
    private TextView tvCuentaRegresiva;
    private MaterialButton btnPagarAnticipo;
    private MaterialButton btnCancelarReserva;
    private MaterialButton btnCambiarFecha;
    private MaterialButton btnContactar;

    private Pedido pedidoActual;
    private List<Pago> pagosActuales = new ArrayList<>();

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable runnableTimer;

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
        pagoRepository = container.getPagoRepository();
        sessionManager = container.getSessionManager();
        reloj = container.getReloj();

        layoutNoDisponible = view.findViewById(R.id.layoutNoDisponible);
        layoutContenido = view.findViewById(R.id.layoutContenido);
        btnVolverAListaNoDisponible = view.findViewById(R.id.btnVolverAListaNoDisponible);

        tvEstadoBadge = view.findViewById(R.id.tvEstadoBadge);
        tvMensajeContextual = view.findViewById(R.id.tvMensajeContextual);
        tvMotivoExtra = view.findViewById(R.id.tvMotivoExtra);
        tvReembolsoEstado = view.findViewById(R.id.tvReembolsoEstado);

        tvNombreEvento = view.findViewById(R.id.tvNombreEvento);
        tvFolio = view.findViewById(R.id.tvFolio);
        tvFechaHorario = view.findViewById(R.id.tvFechaHorario);
        tvDireccion = view.findViewById(R.id.tvDireccion);
        tvPaquete = view.findViewById(R.id.tvPaquete);
        tvNotas = view.findViewById(R.id.tvNotas);

        tvTotal = view.findViewById(R.id.tvTotal);
        tvAnticipoPagado = view.findViewById(R.id.tvAnticipoPagado);
        tvSaldoPendiente = view.findViewById(R.id.tvSaldoPendiente);
        tvAvisoSaldo = view.findViewById(R.id.tvAvisoSaldo);
        layoutReembolsoDinero = view.findViewById(R.id.layoutReembolsoDinero);
        tvReembolsoMonto = view.findViewById(R.id.tvReembolsoMonto);

        tvHistorialVacio = view.findViewById(R.id.tvHistorialVacio);
        layoutPagosContainer = view.findViewById(R.id.layoutPagosContainer);

        layoutPagarAnticipo = view.findViewById(R.id.layoutPagarAnticipo);
        tvCuentaRegresiva = view.findViewById(R.id.tvCuentaRegresiva);
        btnPagarAnticipo = view.findViewById(R.id.btnPagarAnticipo);
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

        if (pedidoId != null) {
            pagoRepository.getPagosPorPedido(pedidoId).observe(getViewLifecycleOwner(), pagos -> {
                pagosActuales.clear();
                if (pagos != null) {
                    pagosActuales.addAll(pagos);
                }
                actualizarSeccionPagos();
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        detenerCuentaRegresiva();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detenerCuentaRegresiva();
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        Usuario usuarioActual = sessionManager.getUsuarioActual();
        if (pedidoActual == null || usuarioActual == null
                || !usuarioActual.getId().equals(pedidoActual.getClienteId())) {
            layoutNoDisponible.setVisibility(View.VISIBLE);
            layoutContenido.setVisibility(View.GONE);
            detenerCuentaRegresiva();
            return;
        }

        layoutNoDisponible.setVisibility(View.GONE);
        layoutContenido.setVisibility(View.VISIBLE);

        Context ctx = requireContext();
        String estado = pedidoActual.getEstado();

        // 1. Estado
        tvEstadoBadge.setText(PedidosClienteUtils.etiquetaEstado(estado));
        pintarBadge(ctx, estado);

        switch (estado) {
            case Pedido.ESTADO_APARTADO:
                tvMensajeContextual.setText(R.string.msg_contextual_apartado);
                break;
            case Pedido.ESTADO_EN_REVISION:
                tvMensajeContextual.setText(R.string.msg_contextual_revision);
                break;
            case Pedido.ESTADO_CONFIRMADO:
            case Pedido.ESTADO_PAGADO:
                tvMensajeContextual.setText(R.string.msg_contextual_confirmado);
                break;
            case Pedido.ESTADO_COMPLETADO:
                tvMensajeContextual.setText(R.string.msg_contextual_realizado);
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

        if (pedidoActual.getReembolsoMonto() > 0) {
            tvReembolsoEstado.setVisibility(View.VISIBLE);
            tvReembolsoEstado.setText(getString(R.string.formato_reembolso_monto,
                    FormatUtils.formatoMoneda(pedidoActual.getReembolsoMonto())));
        } else {
            tvReembolsoEstado.setVisibility(View.GONE);
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

        // 3. Dinero
        tvTotal.setText(FormatUtils.formatoMoneda(pedidoActual.getTotal()));
        tvAnticipoPagado.setText(FormatUtils.formatoMoneda(pedidoActual.getAnticipoPagado()));
        tvSaldoPendiente.setText(FormatUtils.formatoMoneda(pedidoActual.getSaldoPendiente()));

        boolean esActivo = (Pedido.ESTADO_CONFIRMADO.equals(estado)
                || Pedido.ESTADO_PAGADO.equals(estado)
                || Pedido.ESTADO_EN_REVISION.equals(estado));
        if (esActivo && pedidoActual.getSaldoPendiente() > 0) {
            tvAvisoSaldo.setVisibility(View.VISIBLE);
        } else {
            tvAvisoSaldo.setVisibility(View.GONE);
        }

        if (pedidoActual.getReembolsoMonto() > 0) {
            layoutReembolsoDinero.setVisibility(View.VISIBLE);
            tvReembolsoMonto.setText(FormatUtils.formatoMoneda(pedidoActual.getReembolsoMonto()));
        } else {
            layoutReembolsoDinero.setVisibility(View.GONE);
        }

        actualizarSeccionPagos();

        // 5. Acciones
        long ahora = reloj.ahora();

        // Pagar anticipo
        if (PedidosClienteUtils.puedePagarAnticipo(pedidoActual, ahora)) {
            layoutPagarAnticipo.setVisibility(View.VISIBLE);
            iniciarCuentaRegresiva(pedidoActual.getVencimientoApartadoMillis());
            btnPagarAnticipo.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putString("pedidoId", pedidoActual.getId());
                Navigation.findNavController(v).navigate(R.id.action_clientePedidoDetalle_to_pagoAnticipo, b);
            });
        } else {
            layoutPagarAnticipo.setVisibility(View.GONE);
            detenerCuentaRegresiva();
        }

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

    private void actualizarSeccionPagos() {
        if (!isAdded() || layoutPagosContainer == null) return;

        if (pagosActuales.isEmpty()) {
            tvHistorialVacio.setVisibility(View.VISIBLE);
            layoutPagosContainer.removeAllViews();
            return;
        }

        tvHistorialVacio.setVisibility(View.GONE);
        layoutPagosContainer.removeAllViews();

        Context ctx = requireContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Pago pago : pagosActuales) {
            View itemView = inflater.inflate(R.layout.item_pago, layoutPagosContainer, false);
            TextView tvPagoFolio = itemView.findViewById(R.id.tvPagoFolio);
            TextView tvPagoTipo = itemView.findViewById(R.id.tvPagoTipo);
            TextView tvPagoFechaMetodo = itemView.findViewById(R.id.tvPagoFechaMetodo);
            TextView tvPagoMonto = itemView.findViewById(R.id.tvPagoMonto);
            MaterialButton btnRecibo = itemView.findViewById(R.id.btnRecibo);

            tvPagoFolio.setText(pago.getFolio() != null ? pago.getFolio() : "");
            tvPagoTipo.setText(ReciboUtils.tituloRecibo(pago.getTipo())
                    .replace("Recibo de ", "")
                    .replace("Comprobante de ", ""));

            String metodo = pago.getMetodo() != null ? pago.getMetodo() : "";
            tvPagoFechaMetodo.setText(FormatUtils.fechaHoraCorta(pago.getFecha()) + " · " + metodo);

            boolean esReembolso = Pago.TIPO_REEMBOLSO.equals(pago.getTipo());
            if (esReembolso) {
                tvPagoMonto.setText("− " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvPagoMonto.setTextColor(BadgeUtils.colorError(ctx));
            } else {
                tvPagoMonto.setText("+ " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvPagoMonto.setTextColor(ContextCompat.getColor(ctx, R.color.status_disponible));
            }

            btnRecibo.setOnClickListener(v -> compartirReciboPdf(pedidoActual, pago, pagosActuales));

            layoutPagosContainer.addView(itemView);
        }
    }

    private void iniciarCuentaRegresiva(long vencimientoMillis) {
        detenerCuentaRegresiva();
        runnableTimer = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                long ahora = reloj.ahora();
                long diffMs = vencimientoMillis - ahora;
                if (diffMs <= 0) {
                    tvCuentaRegresiva.setText(R.string.formato_vencida);
                    layoutPagarAnticipo.setVisibility(View.GONE);
                    return;
                }
                long min = diffMs / 60000L;
                long seg = (diffMs % 60000L) / 1000L;
                tvCuentaRegresiva.setText(getString(R.string.formato_cuenta_regresiva,
                        String.format(Locale.ROOT, "%02d:%02d", min, seg)));
                timerHandler.postDelayed(this, 1000L);
            }
        };
        timerHandler.post(runnableTimer);
    }

    private void detenerCuentaRegresiva() {
        if (runnableTimer != null) {
            timerHandler.removeCallbacks(runnableTimer);
            runnableTimer = null;
        }
    }

    private void mostrarDialogoCancelarReserva(Pedido pedido) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancelar_pedido_cliente, null);
        TextView tvMensajeReembolso = dialogView.findViewById(R.id.tvMensajeReembolso);
        TextView tvDetallePolitica = dialogView.findViewById(R.id.tvDetallePolitica);
        TextInputEditText etMotivo = dialogView.findViewById(R.id.etMotivoCancelacion);

        double pagado = pedido.getAnticipoPagado();
        if (pagado > 0) {
            double reembolso = pedidoRepository.calcularReembolsoPrevio(pedido.getId(), Pedido.CANCELA_CLIENTE);
            int porcentaje = (int) Math.round((reembolso / pagado) * 100.0);
            tvMensajeReembolso.setText(getString(R.string.cancelar_reembolso_formato,
                    FormatUtils.formatoMoneda(reembolso), porcentaje));

            if (Pedido.ESTADO_EN_REVISION.equals(pedido.getEstado())) {
                tvDetallePolitica.setText(R.string.aviso_reembolso_revision);
            } else {
                long horas = (pedido.getInicioMillis() - reloj.ahora()) / 3600_000L;
                if (horas >= Config.REEMBOLSO_TOTAL_DIAS * 24L) {
                    tvDetallePolitica.setText(R.string.cancelacion_politica_100);
                } else if (horas >= Config.REEMBOLSO_PARCIAL_HORAS) {
                    tvDetallePolitica.setText(R.string.cancelacion_politica_50);
                } else {
                    tvDetallePolitica.setText(R.string.cancelacion_politica_0);
                }
            }
        } else {
            tvMensajeReembolso.setText(R.string.cancelar_liberar_sin_pago);
            tvDetallePolitica.setVisibility(View.GONE);
        }

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

    private void compartirReciboPdf(Pedido pedido, Pago pago, List<Pago> pagosDelPedido) {
        try {
            File pdf = ReciboPdf.generar(requireContext(), pedido, pago, pagosDelPedido);
            CompartirRecibo.compartir(requireContext(), pdf, "Compartir " + ReciboUtils.tituloRecibo(pago.getTipo()));
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

    private void pintarBadge(Context ctx, String estado) {
        if (estado == null) estado = "";
        int colorTexto;
        int colorFondo;

        switch (estado) {
            case Pedido.ESTADO_APARTADO:
            case Pedido.ESTADO_EN_REVISION:
                colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                break;
            case Pedido.ESTADO_CONFIRMADO:
            case Pedido.ESTADO_PAGADO:
                colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                break;
            case Pedido.ESTADO_COMPLETADO:
                colorTexto = ContextCompat.getColor(ctx, R.color.status_rentado);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_rentado_container);
                break;
            case Pedido.ESTADO_CANCELADO:
            case Pedido.ESTADO_RECHAZADO:
                colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                break;
            default:
                colorTexto = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorOnSurfaceVariant);
                colorFondo = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorSurfaceVariant);
                break;
        }

        BadgeUtils.pintar(tvEstadoBadge, colorTexto, colorFondo);
    }
}
