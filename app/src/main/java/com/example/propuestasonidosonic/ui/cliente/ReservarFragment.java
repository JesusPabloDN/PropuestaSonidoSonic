package com.example.propuestasonidosonic.ui.cliente;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ReservaClienteUtils;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.ui.comun.SelectorHorarioHelper;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ReservarFragment extends Fragment implements SelectorHorarioHelper.OnHorarioSeleccionadoListener {

    private ReservarViewModel viewModel;
    private PaqueteRepository paqueteRepository;
    private PedidoRepository pedidoRepository;
    private SessionManager sessionManager;

    private TextView tvResumenPaqueteNombre;
    private TextView tvResumenPaqueteDetalle;
    private TextInputLayout tilNombreEvento;
    private TextInputEditText etNombreEvento;
    private MaterialButton btnSeleccionarFecha;
    private ChipGroup chipGroupHorarios;
    private View layoutSinHorarios;
    private TextInputLayout tilDuracion;
    private AutoCompleteTextView actvDuracion;
    private TextInputLayout tilTelefono;
    private TextInputEditText etTelefono;
    private TextInputLayout tilDireccion;
    private TextInputEditText etDireccion;
    private TextInputLayout tilNotas;
    private TextInputEditText etNotas;

    private TextView tvTotalResumen;
    private TextView tvAnticipoResumen;
    private TextView tvAvisoUrgente;
    private MaterialButton btnApartarYPagar;

    private SelectorHorarioHelper selectorHorarioHelper;
    private final List<Integer> opcionesDuracionMin = new ArrayList<>();
    private final List<String> etiquetasDuracion = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReservarViewModel.class);
        paqueteRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPaqueteRepository();
        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        sessionManager = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager();

        if (getArguments() != null && viewModel.getPaqueteId() == null) {
            String pId = getArguments().getString("paqueteId");
            viewModel.setPaqueteId(pId);
        }

        vincularVistas(view);
        configurarPaquete();
        configurarDuraciones();
        configurarSelectorHorario();
        configurarCamposCliente();
        configurarTextWatchers();
        configurarBotonApartar();
        actualizarResumen();
    }

    private void vincularVistas(View view) {
        tvResumenPaqueteNombre = view.findViewById(R.id.tvResumenPaqueteNombre);
        tvResumenPaqueteDetalle = view.findViewById(R.id.tvResumenPaqueteDetalle);
        tilNombreEvento = view.findViewById(R.id.tilNombreEvento);
        etNombreEvento = view.findViewById(R.id.etNombreEvento);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        chipGroupHorarios = view.findViewById(R.id.chipGroupHorarios);
        layoutSinHorarios = view.findViewById(R.id.layoutSinHorarios);
        tilDuracion = view.findViewById(R.id.tilDuracion);
        actvDuracion = view.findViewById(R.id.actvDuracion);
        tilTelefono = view.findViewById(R.id.tilTelefono);
        etTelefono = view.findViewById(R.id.etTelefono);
        tilDireccion = view.findViewById(R.id.tilDireccion);
        etDireccion = view.findViewById(R.id.etDireccion);
        tilNotas = view.findViewById(R.id.tilNotas);
        etNotas = view.findViewById(R.id.etNotas);

        tvTotalResumen = view.findViewById(R.id.tvTotalResumen);
        tvAnticipoResumen = view.findViewById(R.id.tvAnticipoResumen);
        tvAvisoUrgente = view.findViewById(R.id.tvAvisoUrgente);
        btnApartarYPagar = view.findViewById(R.id.btnApartarYPagar);
    }

    private void configurarPaquete() {
        if (viewModel.getPaqueteId() == null) return;
        Paquete paquete = paqueteRepository.getPaquetePorId(viewModel.getPaqueteId());
        if (paquete == null) {
            Toast.makeText(requireContext(), "Paquete no encontrado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }
        viewModel.setPaquete(paquete);

        tvResumenPaqueteNombre.setText(paquete.getNombre());
        tvResumenPaqueteDetalle.setText(getString(R.string.paquete_precio_desde_formato,
                FormatUtils.formatoMoneda(paquete.getPrecio()), paquete.getDuracionBaseHoras()));

        // Preseleccionar duración inicial si es primera carga
        if (viewModel.getDuracionMin() < paquete.getDuracionBaseHoras() * 60) {
            viewModel.setDuracionMin(paquete.getDuracionBaseHoras() * 60);
        }
    }

    private void configurarDuraciones() {
        opcionesDuracionMin.clear();
        etiquetasDuracion.clear();
        int baseMin = viewModel.getPaquete() != null ? viewModel.getPaquete().getDuracionBaseHoras() * 60 : Config.DURACION_MIN_MIN;

        for (int min = baseMin; min <= Config.DURACION_MAX_MIN; min += Config.PASO_DURACION_MIN) {
            opcionesDuracionMin.add(min);
            int h = min / 60;
            int m = min % 60;
            String texto = (m > 0) ? h + " h " + m + " min" : h + " h";
            etiquetasDuracion.add(texto);
        }

        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, etiquetasDuracion);
        actvDuracion.setAdapter(durAdapter);

        int pos = opcionesDuracionMin.indexOf(viewModel.getDuracionMin());
        if (pos >= 0) {
            actvDuracion.setText(etiquetasDuracion.get(pos), false);
        } else if (!etiquetasDuracion.isEmpty()) {
            actvDuracion.setText(etiquetasDuracion.get(0), false);
            viewModel.setDuracionMin(opcionesDuracionMin.get(0));
        }

        actvDuracion.setOnItemClickListener((parent, v, position, id) -> {
            int elegida = opcionesDuracionMin.get(position);
            viewModel.setDuracionMin(elegida);
            actualizarConfiguracionSelector();
            actualizarResumen();
            validarFormulario();
        });
    }

    private void configurarSelectorHorario() {
        selectorHorarioHelper = new SelectorHorarioHelper(
                requireContext(),
                getParentFragmentManager(),
                pedidoRepository,
                btnSeleccionarFecha,
                chipGroupHorarios,
                layoutSinHorarios,
                null, // Cliente no ve chips de alternativas
                this
        );

        if (viewModel.getFechaMillis() > 0) {
            selectorHorarioHelper.setFechaSeleccionada(viewModel.getFechaMillis(), viewModel.getHorarioInicioMillis());
        }
        actualizarConfiguracionSelector();
    }

    private void actualizarConfiguracionSelector() {
        if (selectorHorarioHelper != null && viewModel.getPaquete() != null) {
            selectorHorarioHelper.setConfiguracion(
                    viewModel.getPaquete().getId(),
                    viewModel.getDuracionMin(),
                    null
            );
        }
    }

    private void configurarCamposCliente() {
        Usuario usuario = sessionManager.getUsuarioActual();
        if (usuario != null && viewModel.getTelefono().isEmpty()) {
            String tel = usuario.getTelefono() != null ? usuario.getTelefono() : "";
            viewModel.setTelefono(tel);
        }
    }

    private void configurarTextWatchers() {
        etNombreEvento.setText(viewModel.getNombreEvento());
        etTelefono.setText(viewModel.getTelefono());
        etDireccion.setText(viewModel.getDireccion());
        etNotas.setText(viewModel.getNotas());

        etNombreEvento.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setNombreEvento(s);
            validarFormulario();
        }));
        etTelefono.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setTelefono(s);
            validarFormulario();
        }));
        etDireccion.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setDireccion(s);
            validarFormulario();
        }));
        etNotas.addTextChangedListener(new SimpleWatcher(viewModel::setNotas));
    }

    @Override
    public void onHorarioSeleccionado(long inicioMillis) {
        viewModel.setHorarioInicioMillis(inicioMillis);
        viewModel.setFechaMillis(TiempoUtils.inicioDelDia(inicioMillis));
        actualizarResumen();
        validarFormulario();
    }

    @Override
    public void onHorarioLimpiado() {
        viewModel.setHorarioInicioMillis(null);
        actualizarResumen();
        validarFormulario();
    }

    private void actualizarResumen() {
        Paquete paquete = viewModel.getPaquete();
        if (paquete == null) return;

        double total = MotorDisponibilidad.calcularTotal(paquete, viewModel.getDuracionMin());
        double anticipo = MotorDisponibilidad.calcularAnticipo(total);

        tvTotalResumen.setText(getString(R.string.label_resumen_total, FormatUtils.formatoMoneda(total)));
        tvAnticipoResumen.setText(getString(R.string.paquete_detalle_anticipo_formato, FormatUtils.formatoMoneda(anticipo)));

        Long horarioInicio = viewModel.getHorarioInicioMillis();
        long ahora = System.currentTimeMillis();
        if (horarioInicio != null && ReservaClienteUtils.esUrgente(horarioInicio, ahora)) {
            tvAvisoUrgente.setVisibility(View.VISIBLE);
        } else {
            tvAvisoUrgente.setVisibility(View.GONE);
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;

        String nomEvento = (etNombreEvento.getText() != null) ? etNombreEvento.getText().toString().trim() : "";
        if (nomEvento.isEmpty() || nomEvento.length() > 80) {
            valido = false;
        }

        if (viewModel.getHorarioInicioMillis() == null) {
            valido = false;
        }

        String tel = (etTelefono.getText() != null) ? etTelefono.getText().toString().trim() : "";
        String digitosTel = tel.replaceAll("\\D", "");
        if (digitosTel.length() < 10) {
            valido = false;
        }

        String dir = (etDireccion.getText() != null) ? etDireccion.getText().toString().trim() : "";
        if (dir.length() < 10) {
            valido = false;
        }

        btnApartarYPagar.setEnabled(valido);
        return valido;
    }

    private void configurarBotonApartar() {
        btnApartarYPagar.setOnClickListener(v -> apartarYPagar());
    }

    private void apartarYPagar() {
        if (!validarFormulario()) return;

        btnApartarYPagar.setEnabled(false);

        Usuario usuario = sessionManager.getUsuarioActual();
        String clienteId = usuario != null ? usuario.getId() : "";
        String clienteNombre = usuario != null ? usuario.getNombre() : "";
        String nomEvento = etNombreEvento.getText().toString().trim();
        String tel = etTelefono.getText().toString().trim();
        String dir = etDireccion.getText().toString().trim();
        String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";

        SolicitudReserva solicitud = new SolicitudReserva(
                viewModel.getPaqueteId(),
                viewModel.getHorarioInicioMillis(),
                viewModel.getDuracionMin(),
                Pedido.ORIGEN_APP,
                nomEvento,
                clienteId,
                clienteNombre,
                tel,
                dir,
                notas,
                null // horasApartado null para app (30 min automático)
        );

        pedidoRepository.crearReserva(solicitud, new Callback<Pedido>() {
            @Override
            public void onSuccess(Pedido result) {
                if (!isAdded()) return;
                Bundle args = new Bundle();
                args.putString("pedidoId", result.getId());
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.reservarFragment, true)
                        .build();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_reservar_to_pagoAnticipo, args, navOptions);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                btnApartarYPagar.setEnabled(true);
                String mensaje = ReservaClienteUtils.mensajeAmigable(error);
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private static class SimpleWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> consumer;
        SimpleWatcher(java.util.function.Consumer<String> consumer) { this.consumer = consumer; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            consumer.accept(s != null ? s.toString() : "");
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
