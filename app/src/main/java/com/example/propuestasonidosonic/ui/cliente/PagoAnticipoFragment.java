package com.example.propuestasonidosonic.ui.cliente;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PasarelaPago;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.ReservaClienteUtils;
import com.example.propuestasonidosonic.domain.TarjetaUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PagoAnticipoFragment extends Fragment {

    private String pedidoId;
    private Pedido pedidoActual;

    private PedidoRepository pedidoRepository;
    private PasarelaPago pasarelaPago;
    private SessionManager sessionManager;

    // Vistas principales
    private View layoutFormularioPago;
    private View layoutResultadoPago;
    private View layoutReservaNoDisponible;

    // Formulario
    private TextView tvResumenPagoEvento;
    private TextView tvResumenPagoFechaHorario;
    private TextView tvResumenPagoTotal;
    private TextView tvResumenPagoAnticipo;
    private TextView tvCuentaRegresiva;
    private TextInputLayout tilTitular;
    private TextInputEditText etTitular;
    private TextInputLayout tilNumeroTarjeta;
    private TextInputEditText etNumeroTarjeta;
    private TextInputLayout tilVencimiento;
    private TextInputEditText etVencimiento;
    private TextInputLayout tilCvv;
    private TextInputEditText etCvv;
    private TextView tvErrorPago;
    private ProgressBar progressBarPago;
    private MaterialButton btnPagar;

    // Resultado
    private ImageView ivIconoResultado;
    private TextView tvTituloResultado;
    private TextView tvNumeroPedidoResultado;
    private TextView tvDetalleEventoResultado;
    private TextView tvMontoPagadoResultado;
    private TextView tvMensajeSaldoResultado;
    private MaterialButton btnVolverCatalogoResultado;

    // No disponible
    private MaterialButton btnVolverCatalogoNoDisponible;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isUpdatingCardNumber = false;
    private boolean isUpdatingExpiry = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pago_anticipo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        pasarelaPago = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPasarelaPago();
        sessionManager = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager();

        if (getArguments() != null) {
            pedidoId = getArguments().getString("pedidoId");
        }

        vincularVistas(view);
        configurarBackPressed();

        if (!cargarPedido()) {
            mostrarNoDisponible();
            return;
        }

        configurarFormulario();
        iniciarCuentaRegresiva();
    }

    private void vincularVistas(View view) {
        layoutFormularioPago = view.findViewById(R.id.layoutFormularioPago);
        layoutResultadoPago = view.findViewById(R.id.layoutResultadoPago);
        layoutReservaNoDisponible = view.findViewById(R.id.layoutReservaNoDisponible);

        tvResumenPagoEvento = view.findViewById(R.id.tvResumenPagoEvento);
        tvResumenPagoFechaHorario = view.findViewById(R.id.tvResumenPagoFechaHorario);
        tvResumenPagoTotal = view.findViewById(R.id.tvResumenPagoTotal);
        tvResumenPagoAnticipo = view.findViewById(R.id.tvResumenPagoAnticipo);
        tvCuentaRegresiva = view.findViewById(R.id.tvCuentaRegresiva);

        tilTitular = view.findViewById(R.id.tilTitular);
        etTitular = view.findViewById(R.id.etTitular);
        tilNumeroTarjeta = view.findViewById(R.id.tilNumeroTarjeta);
        etNumeroTarjeta = view.findViewById(R.id.etNumeroTarjeta);
        tilVencimiento = view.findViewById(R.id.tilVencimiento);
        etVencimiento = view.findViewById(R.id.etVencimiento);
        tilCvv = view.findViewById(R.id.tilCvv);
        etCvv = view.findViewById(R.id.etCvv);
        tvErrorPago = view.findViewById(R.id.tvErrorPago);
        progressBarPago = view.findViewById(R.id.progressBarPago);
        btnPagar = view.findViewById(R.id.btnPagar);

        ivIconoResultado = view.findViewById(R.id.ivIconoResultado);
        tvTituloResultado = view.findViewById(R.id.tvTituloResultado);
        tvNumeroPedidoResultado = view.findViewById(R.id.tvNumeroPedidoResultado);
        tvDetalleEventoResultado = view.findViewById(R.id.tvDetalleEventoResultado);
        tvMontoPagadoResultado = view.findViewById(R.id.tvMontoPagadoResultado);
        tvMensajeSaldoResultado = view.findViewById(R.id.tvMensajeSaldoResultado);
        btnVolverCatalogoResultado = view.findViewById(R.id.btnVolverCatalogoResultado);

        btnVolverCatalogoNoDisponible = view.findViewById(R.id.btnVolverCatalogoNoDisponible);

        btnVolverCatalogoResultado.setOnClickListener(v -> volverAlCatalogo());
        btnVolverCatalogoNoDisponible.setOnClickListener(v -> volverAlCatalogo());
    }

    private boolean cargarPedido() {
        if (pedidoId == null || pedidoRepository == null) return false;
        pedidoActual = pedidoRepository.getPedidoPorId(pedidoId);
        if (pedidoActual == null) return false;

        Usuario usuario = sessionManager.getUsuarioActual();
        if (usuario == null || !usuario.getId().equals(pedidoActual.getClienteId())) {
            return false;
        }

        long ahora = System.currentTimeMillis();
        // Si ya está confirmado o en revisión, mostrar resultado
        if (Pedido.ESTADO_CONFIRMADO.equals(pedidoActual.getEstado())
                || Pedido.ESTADO_EN_REVISION.equals(pedidoActual.getEstado())
                || Pedido.ESTADO_PAGADO.equals(pedidoActual.getEstado())) {
            mostrarResultado(pedidoActual);
            return true;
        }

        // Si está apartado pero ya venció
        if (!Pedido.ESTADO_APARTADO.equals(pedidoActual.getEstado())
                || pedidoActual.getVencimientoApartadoMillis() <= ahora) {
            return false;
        }

        return true;
    }

    private void configurarFormulario() {
        tvResumenPagoEvento.setText(pedidoActual.getNombreEvento());
        tvResumenPagoFechaHorario.setText(FormatUtils.fechaHoraCorta(pedidoActual.getInicioMillis()));
        tvResumenPagoTotal.setText("Total del pedido: " + FormatUtils.formatoMoneda(pedidoActual.getTotal()));
        tvResumenPagoAnticipo.setText(getString(R.string.label_anticipo_a_pagar,
                FormatUtils.formatoMoneda(pedidoActual.getAnticipoRequerido())));
        btnPagar.setText(getString(R.string.btn_pagar_formato,
                FormatUtils.formatoMoneda(pedidoActual.getAnticipoRequerido())));

        configurarWatchersTarjeta();
        btnPagar.setOnClickListener(v -> procesarPago());
    }

    private void configurarWatchersTarjeta() {
        etTitular.addTextChangedListener(new SimpleTextWatcher(s -> validarTarjeta()));

        etNumeroTarjeta.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (isUpdatingCardNumber) return;
            isUpdatingCardNumber = true;

            String formateado = TarjetaUtils.formatearNumero(s);
            etNumeroTarjeta.setText(formateado);
            etNumeroTarjeta.setSelection(formateado.length());

            String marca = TarjetaUtils.marca(s);
            if (!"DESCONOCIDA".equals(marca)) {
                tilNumeroTarjeta.setHelperText(marca);
            } else {
                tilNumeroTarjeta.setHelperText(" ");
            }

            isUpdatingCardNumber = false;
            validarTarjeta();
        }));

        etVencimiento.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (isUpdatingExpiry) return;
            isUpdatingExpiry = true;

            String limpio = TarjetaUtils.soloDigitos(s);
            StringBuilder sb = new StringBuilder();
            if (limpio.length() >= 2) {
                sb.append(limpio.substring(0, 2)).append('/');
                if (limpio.length() > 2) {
                    sb.append(limpio.substring(2, Math.min(4, limpio.length())));
                }
            } else {
                sb.append(limpio);
            }
            etVencimiento.setText(sb.toString());
            etVencimiento.setSelection(sb.length());

            isUpdatingExpiry = false;
            validarTarjeta();
        }));

        etCvv.addTextChangedListener(new SimpleTextWatcher(s -> validarTarjeta()));
    }

    private void validarTarjeta() {
        String titular = etTitular.getText() != null ? etTitular.getText().toString().trim() : "";
        String numero = etNumeroTarjeta.getText() != null ? etNumeroTarjeta.getText().toString() : "";
        String vencimiento = etVencimiento.getText() != null ? etVencimiento.getText().toString() : "";
        String cvv = etCvv.getText() != null ? etCvv.getText().toString().trim() : "";

        boolean titularValido = TarjetaUtils.titularValido(titular);
        boolean numeroValido = TarjetaUtils.numeroValido(numero);

        boolean vencValido = false;
        String digitosVenc = TarjetaUtils.soloDigitos(vencimiento);
        if (digitosVenc.length() == 4) {
            try {
                int mes = Integer.parseInt(digitosVenc.substring(0, 2));
                int anio = Integer.parseInt(digitosVenc.substring(2, 4));
                long ahora = System.currentTimeMillis();
                vencValido = TarjetaUtils.vencimientoValido(mes, anio, ahora);
            } catch (Exception ignored) {}
        }

        String marca = TarjetaUtils.marca(numero);
        boolean cvvValido = TarjetaUtils.cvvValido(cvv, marca);

        boolean todoValido = titularValido && numeroValido && vencValido && cvvValido;
        btnPagar.setEnabled(todoValido);
    }

    private void procesarPago() {
        btnPagar.setEnabled(false);
        progressBarPago.setVisibility(View.VISIBLE);
        tvErrorPago.setVisibility(View.GONE);

        String titular = etTitular.getText() != null ? etTitular.getText().toString().trim() : "";
        String numero = etNumeroTarjeta.getText() != null ? etNumeroTarjeta.getText().toString() : "";
        String digitosVenc = TarjetaUtils.soloDigitos(etVencimiento.getText() != null ? etVencimiento.getText().toString() : "");
        int mes = Integer.parseInt(digitosVenc.substring(0, 2));
        int anio = Integer.parseInt(digitosVenc.substring(2, 4));
        String cvv = etCvv.getText() != null ? etCvv.getText().toString().trim() : "";

        PasarelaPago.TarjetaDatos tarjetaDatos = new PasarelaPago.TarjetaDatos(titular, numero, mes, anio, cvv);

        // Simulamos progreso de ~1s
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;

            pasarelaPago.cobrar(pedidoActual.getAnticipoRequerido(), tarjetaDatos, new Callback<PasarelaPago.ResultadoPago>() {
                @Override
                public void onSuccess(PasarelaPago.ResultadoPago resultado) {
                    if (!isAdded()) return;

                    if (!resultado.isAprobado()) {
                        progressBarPago.setVisibility(View.GONE);
                        tvErrorPago.setText(resultado.getMotivoRechazo() != null ? resultado.getMotivoRechazo() : "Pago rechazado");
                        tvErrorPago.setVisibility(View.VISIBLE);
                        validarTarjeta();
                    } else {
                        // Pago aprobado por la pasarela -> registrar anticipo en repositorio
                        pedidoRepository.registrarAnticipo(pedidoActual.getId(),
                                pedidoActual.getAnticipoRequerido(),
                                Pago.METODO_TARJETA,
                                new Callback<Pedido>() {
                                    @Override
                                    public void onSuccess(Pedido result) {
                                        if (!isAdded()) return;
                                        limpiarDatosTarjeta();
                                        detenerCuentaRegresiva();
                                        mostrarResultado(result);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        if (!isAdded()) return;
                                        progressBarPago.setVisibility(View.GONE);
                                        tvErrorPago.setText(error);
                                        tvErrorPago.setVisibility(View.VISIBLE);
                                        btnPagar.setEnabled(true);
                                    }
                                });
                    }
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    progressBarPago.setVisibility(View.GONE);
                    tvErrorPago.setText(error);
                    tvErrorPago.setVisibility(View.VISIBLE);
                    btnPagar.setEnabled(true);
                }
            });
        }, 800);
    }

    private void mostrarResultado(Pedido pedido) {
        layoutFormularioPago.setVisibility(View.GONE);
        layoutReservaNoDisponible.setVisibility(View.GONE);
        layoutResultadoPago.setVisibility(View.VISIBLE);

        tvNumeroPedidoResultado.setText(getString(R.string.formato_pedido_numero, pedido.getNumero()));
        tvDetalleEventoResultado.setText(pedido.getNombreEvento() + "\n" + FormatUtils.fechaHoraCorta(pedido.getInicioMillis()));
        tvMontoPagadoResultado.setText("Monto pagado: " + FormatUtils.formatoMoneda(pedido.getAnticipoPagado()));

        double saldoRestante = pedido.getTotal() - pedido.getAnticipoPagado();

        if (Pedido.ESTADO_EN_REVISION.equals(pedido.getEstado())) {
            ivIconoResultado.setImageResource(R.drawable.ic_hourglass_24);
            tvTituloResultado.setText(R.string.titulo_pago_recibido_revision);
            tvMensajeSaldoResultado.setText(R.string.aviso_revision_urgente_cliente);
        } else {
            ivIconoResultado.setImageResource(R.drawable.ic_check_circle_24);
            tvTituloResultado.setText(R.string.titulo_reserva_confirmada);
            tvMensajeSaldoResultado.setText(getString(R.string.formato_saldo_pendiente_aviso, FormatUtils.formatoMoneda(saldoRestante)));
        }
    }

    private void mostrarNoDisponible() {
        detenerCuentaRegresiva();
        layoutFormularioPago.setVisibility(View.GONE);
        layoutResultadoPago.setVisibility(View.GONE);
        layoutReservaNoDisponible.setVisibility(View.VISIBLE);
    }

    private void volverAlCatalogo() {
        limpiarDatosTarjeta();
        detenerCuentaRegresiva();
        Navigation.findNavController(requireView()).popBackStack(R.id.clienteHomeFragment, false);
    }

    private void iniciarCuentaRegresiva() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || pedidoActual == null) return;
                long ahora = System.currentTimeMillis();
                long segs = ReservaClienteUtils.segundosRestantes(pedidoActual.getVencimientoApartadoMillis(), ahora);
                if (segs <= 0) {
                    mostrarNoDisponible();
                } else {
                    tvCuentaRegresiva.setText(getString(R.string.formato_cuenta_regresiva,
                            ReservaClienteUtils.formatoCuentaRegresiva(segs)));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void detenerCuentaRegresiva() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void limpiarDatosTarjeta() {
        if (etTitular != null) etTitular.setText("");
        if (etNumeroTarjeta != null) etNumeroTarjeta.setText("");
        if (etVencimiento != null) etVencimiento.setText("");
        if (etCvv != null) etCvv.setText("");
    }

    private void configurarBackPressed() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutResultadoPago.getVisibility() == View.VISIBLE
                        || layoutReservaNoDisponible.getVisibility() == View.VISIBLE) {
                    volverAlCatalogo();
                } else {
                    limpiarDatosTarjeta();
                    detenerCuentaRegresiva();
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        detenerCuentaRegresiva();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (layoutFormularioPago.getVisibility() == View.VISIBLE && pedidoActual != null) {
            iniciarCuentaRegresiva();
        }
    }

    @Override
    public void onDestroyView() {
        detenerCuentaRegresiva();
        limpiarDatosTarjeta();
        super.onDestroyView();
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> consumer;
        SimpleTextWatcher(java.util.function.Consumer<String> consumer) { this.consumer = consumer; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            consumer.accept(s != null ? s.toString() : "");
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
