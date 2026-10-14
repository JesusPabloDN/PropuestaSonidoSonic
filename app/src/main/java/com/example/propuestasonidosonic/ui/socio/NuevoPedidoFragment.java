package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.ui.comun.SelectorHorarioHelper;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NuevoPedidoFragment extends Fragment implements SelectorHorarioHelper.OnHorarioSeleccionadoListener {

    private NuevoPedidoViewModel viewModel;
    private PaqueteRepository paqueteRepository;
    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;

    private TextView tvAvisoSinPaquetes;
    private TextInputLayout tilPaquete;
    private MaterialAutoCompleteTextView actvPaquete;
    private TextInputLayout tilDuracion;
    private MaterialAutoCompleteTextView actvDuracion;
    private MaterialButton btnSeleccionarFecha;
    private ChipGroup chipGroupHorarios;
    private View layoutSinHorarios;
    private ChipGroup chipGroupAlternativas;

    private MaterialCardView cardResumenEnVivo;
    private TextView tvResumenFin;
    private TextView tvResumenMontaje;
    private TextView tvResumenDesmontaje;
    private TextView tvResumenPersonal;
    private TextView tvResumenTotal;
    private TextView tvResumenAnticipo;

    private TextInputLayout tilNombreEvento;
    private TextInputEditText etNombreEvento;
    private TextInputLayout tilNombreCliente;
    private MaterialAutoCompleteTextView etNombreCliente;
    private TextInputLayout tilTelefonoCliente;
    private TextInputEditText etTelefonoCliente;
    private TextInputLayout tilDireccion;
    private TextInputEditText etDireccion;
    private TextInputLayout tilNotas;
    private TextInputEditText etNotas;

    private MaterialButtonToggleGroup toggleOrigen;
    private TextInputLayout tilRetenerHoras;
    private MaterialAutoCompleteTextView actvRetenerHoras;
    private MaterialButton btnApartar;

    private SelectorHorarioHelper selectorHorarioHelper;
    private final List<Paquete> paquetesActivos = new ArrayList<>();
    private final List<Integer> opcionesDuracionMin = new ArrayList<>();
    private final List<String> etiquetasDuracion = new ArrayList<>();
    private final List<ClienteUtils.ResumenCliente> clientesSugerencias = new ArrayList<>();
    private String clienteIdSeleccionado = null;
    private PagoRepository pagoRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_pedido, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NuevoPedidoViewModel.class);
        paqueteRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPaqueteRepository();
        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        usuarioRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getUsuarioRepository();
        pagoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPagoRepository();

        vincularVistas(view);
        configurarSelectorHorario();
        configurarDuraciones();
        configurarHorasApartado();
        configurarOrigenToggle();
        configurarAutocompletarCliente();
        configurarTextWatchers();
        cargarPaquetes();
        configurarBotonApartar();
    }

    private void vincularVistas(View view) {
        tvAvisoSinPaquetes = view.findViewById(R.id.tvAvisoSinPaquetes);
        tilPaquete = view.findViewById(R.id.tilPaquete);
        actvPaquete = view.findViewById(R.id.actvPaquete);
        tilDuracion = view.findViewById(R.id.tilDuracion);
        actvDuracion = view.findViewById(R.id.actvDuracion);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        chipGroupHorarios = view.findViewById(R.id.chipGroupHorarios);
        layoutSinHorarios = view.findViewById(R.id.layoutSinHorarios);
        chipGroupAlternativas = view.findViewById(R.id.chipGroupAlternativas);

        cardResumenEnVivo = view.findViewById(R.id.cardResumenEnVivo);
        tvResumenFin = view.findViewById(R.id.tvResumenFin);
        tvResumenMontaje = view.findViewById(R.id.tvResumenMontaje);
        tvResumenDesmontaje = view.findViewById(R.id.tvResumenDesmontaje);
        tvResumenPersonal = view.findViewById(R.id.tvResumenPersonal);
        tvResumenTotal = view.findViewById(R.id.tvResumenTotal);
        tvResumenAnticipo = view.findViewById(R.id.tvResumenAnticipo);

        tilNombreEvento = view.findViewById(R.id.tilNombreEvento);
        etNombreEvento = view.findViewById(R.id.etNombreEvento);
        tilNombreCliente = view.findViewById(R.id.tilNombreCliente);
        etNombreCliente = view.findViewById(R.id.etNombreCliente);
        tilTelefonoCliente = view.findViewById(R.id.tilTelefonoCliente);
        etTelefonoCliente = view.findViewById(R.id.etTelefonoCliente);
        tilDireccion = view.findViewById(R.id.tilDireccion);
        etDireccion = view.findViewById(R.id.etDireccion);
        tilNotas = view.findViewById(R.id.tilNotas);
        etNotas = view.findViewById(R.id.etNotas);

        toggleOrigen = view.findViewById(R.id.toggleOrigen);
        tilRetenerHoras = view.findViewById(R.id.tilRetenerHoras);
        actvRetenerHoras = view.findViewById(R.id.actvRetenerHoras);
        btnApartar = view.findViewById(R.id.btnApartar);
    }

    private void configurarSelectorHorario() {
        selectorHorarioHelper = new SelectorHorarioHelper(
                requireContext(),
                getParentFragmentManager(),
                pedidoRepository,
                btnSeleccionarFecha,
                chipGroupHorarios,
                layoutSinHorarios,
                chipGroupAlternativas,
                this
        );

        if (viewModel.getFechaMillis() > 0) {
            selectorHorarioHelper.setFechaSeleccionada(viewModel.getFechaMillis(), viewModel.getHorarioInicioMillis());
        }
    }

    private void configurarDuraciones() {
        opcionesDuracionMin.clear();
        etiquetasDuracion.clear();
        for (int min = Config.DURACION_MIN_MIN; min <= Config.DURACION_MAX_MIN; min += Config.PASO_DURACION_MIN) {
            opcionesDuracionMin.add(min);
            int h = min / 60;
            int m = min % 60;
            String texto = (m > 0) ? h + " h " + m + " min" : h + " h";
            etiquetasDuracion.add(texto);
        }

        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, etiquetasDuracion);
        actvDuracion.setAdapter(durAdapter);

        // Preseleccionar duración inicial
        int index = opcionesDuracionMin.indexOf(viewModel.getDuracionMin());
        if (index >= 0) {
            actvDuracion.setText(etiquetasDuracion.get(index), false);
        } else {
            actvDuracion.setText(etiquetasDuracion.get(0), false);
            viewModel.setDuracionMin(opcionesDuracionMin.get(0));
        }

        actvDuracion.setOnItemClickListener((parent, v, position, id) -> {
            int elegida = opcionesDuracionMin.get(position);
            viewModel.setDuracionMin(elegida);
            actualizarConfiguracionSelector();
        });
    }

    private void configurarHorasApartado() {
        List<String> etiquetas = new ArrayList<>();
        int defaultPos = 3; // 24 horas por defecto
        for (int i = 0; i < Config.APARTADO_MANUAL_OPCIONES_HORAS.length; i++) {
            int h = Config.APARTADO_MANUAL_OPCIONES_HORAS[i];
            etiquetas.add(getString(R.string.horas_formato, h));
            if (h == viewModel.getHorasApartado()) {
                defaultPos = i;
            }
        }

        ArrayAdapter<String> horasAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, etiquetas);
        actvRetenerHoras.setAdapter(horasAdapter);
        actvRetenerHoras.setText(etiquetas.get(defaultPos), false);

        actvRetenerHoras.setOnItemClickListener((parent, v, position, id) -> {
            viewModel.setHorasApartado(Config.APARTADO_MANUAL_OPCIONES_HORAS[position]);
        });
    }

    private void configurarOrigenToggle() {
        if (Pedido.ORIGEN_WHATSAPP.equals(viewModel.getOrigen())) {
            toggleOrigen.check(R.id.btnOrigenWhatsapp);
        } else if (Pedido.ORIGEN_PRESENCIAL.equals(viewModel.getOrigen())) {
            toggleOrigen.check(R.id.btnOrigenPresencial);
        } else {
            toggleOrigen.check(R.id.btnOrigenLlamada);
        }

        toggleOrigen.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnOrigenLlamada) {
                viewModel.setOrigen(Pedido.ORIGEN_LLAMADA);
            } else if (checkedId == R.id.btnOrigenWhatsapp) {
                viewModel.setOrigen(Pedido.ORIGEN_WHATSAPP);
            } else if (checkedId == R.id.btnOrigenPresencial) {
                viewModel.setOrigen(Pedido.ORIGEN_PRESENCIAL);
            }
        });
    }

    private void configurarAutocompletarCliente() {
        etNombreCliente.setThreshold(2);

        final List<Pedido>[] pedidosHolder = new List[]{new ArrayList<Pedido>()};
        final List<Pago>[] pagosHolder = new List[]{new ArrayList<Pago>()};
        final List<Usuario>[] usuariosHolder = new List[]{new ArrayList<Usuario>()};

        Runnable actualizarSugerencias = () -> {
            clientesSugerencias.clear();
            clientesSugerencias.addAll(ClienteUtils.resumenes(pedidosHolder[0], pagosHolder[0], usuariosHolder[0]));
            List<String> etiquetas = new ArrayList<>();
            for (ClienteUtils.ResumenCliente c : clientesSugerencias) {
                String tel = (c.getTelefono() != null && !c.getTelefono().isEmpty()) ? c.getTelefono() : "";
                etiquetas.add(c.getNombre() + " · " + tel);
            }
            if (isAdded()) {
                ArrayAdapter<String> sugerenciasAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, etiquetas);
                etNombreCliente.setAdapter(sugerenciasAdapter);
            }
        };

        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), p -> {
                pedidosHolder[0] = p != null ? new ArrayList<>(p) : new ArrayList<>();
                actualizarSugerencias.run();
            });
        }
        if (pagoRepository != null) {
            pagoRepository.getTodosLosPagos().observe(getViewLifecycleOwner(), pag -> {
                pagosHolder[0] = pag != null ? new ArrayList<>(pag) : new ArrayList<>();
                actualizarSugerencias.run();
            });
        }
        if (usuarioRepository != null) {
            usuarioRepository.getUsuarios().observe(getViewLifecycleOwner(), u -> {
                usuariosHolder[0] = u != null ? new ArrayList<>(u) : new ArrayList<>();
                actualizarSugerencias.run();
            });
        }

        etNombreCliente.setOnItemClickListener((parent, v, position, id) -> {
            String itemText = (String) parent.getItemAtPosition(position);
            for (ClienteUtils.ResumenCliente c : clientesSugerencias) {
                String tel = (c.getTelefono() != null && !c.getTelefono().isEmpty()) ? c.getTelefono() : "";
                if ((c.getNombre() + " · " + tel).equals(itemText)) {
                    etNombreCliente.setText(c.getNombre(), false);
                    etTelefonoCliente.setText(c.getTelefono());
                    clienteIdSeleccionado = c.getClienteId();
                    viewModel.setNombreCliente(c.getNombre());
                    viewModel.setTelefonoCliente(c.getTelefono());
                    break;
                }
            }
        });
    }

    private void configurarTextWatchers() {
        etNombreEvento.setText(viewModel.getNombreEvento());
        etNombreCliente.setText(viewModel.getNombreCliente());
        etTelefonoCliente.setText(viewModel.getTelefonoCliente());
        etDireccion.setText(viewModel.getDireccion());
        etNotas.setText(viewModel.getNotas());

        etNombreEvento.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setNombreEvento(s);
            tilNombreEvento.setError(null);
        }));
        etNombreCliente.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setNombreCliente(s);
            tilNombreCliente.setError(null);
            if (clienteIdSeleccionado != null) {
                boolean coincide = false;
                for (ClienteUtils.ResumenCliente c : clientesSugerencias) {
                    if (c.getClienteId() != null && c.getClienteId().equals(clienteIdSeleccionado)
                            && c.getNombre().equalsIgnoreCase(s)) {
                        coincide = true;
                        break;
                    }
                }
                if (!coincide) {
                    clienteIdSeleccionado = null;
                }
            }
        }));
        etTelefonoCliente.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setTelefonoCliente(s);
            tilTelefonoCliente.setError(null);
        }));
        etDireccion.addTextChangedListener(new SimpleWatcher(s -> {
            viewModel.setDireccion(s);
            tilDireccion.setError(null);
        }));
        etNotas.addTextChangedListener(new SimpleWatcher(viewModel::setNotas));
    }

    private void cargarPaquetes() {
        paqueteRepository.getPaquetes().observe(getViewLifecycleOwner(), paquetes -> {
            paquetesActivos.clear();
            List<String> items = new ArrayList<>();

            if (paquetes != null) {
                for (Paquete p : paquetes) {
                    if (p.isActivo()) {
                        paquetesActivos.add(p);
                        items.add(p.getNombre() + " · " + FormatUtils.formatoMoneda(p.getPrecioBase()));
                    }
                }
            }

            if (paquetesActivos.isEmpty()) {
                tvAvisoSinPaquetes.setVisibility(View.VISIBLE);
                actvPaquete.setEnabled(false);
                btnApartar.setEnabled(false);
                return;
            }

            tvAvisoSinPaquetes.setVisibility(View.GONE);
            actvPaquete.setEnabled(true);
            btnApartar.setEnabled(true);

            ArrayAdapter<String> paqAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, items);
            actvPaquete.setAdapter(paqAdapter);

            // Restaurar paquete seleccionado si existe
            if (viewModel.getPaqueteSeleccionado() != null) {
                for (int i = 0; i < paquetesActivos.size(); i++) {
                    if (paquetesActivos.get(i).getId().equals(viewModel.getPaqueteSeleccionado().getId())) {
                        actvPaquete.setText(items.get(i), false);
                        break;
                    }
                }
            }

            actvPaquete.setOnItemClickListener((parent, v, position, id) -> {
                Paquete elegido = paquetesActivos.get(position);
                viewModel.setPaqueteSeleccionado(elegido);
                tilPaquete.setError(null);

                // Preseleccionar duración base
                int baseMin = elegido.getDuracionBaseHoras() * 60;
                int posDur = opcionesDuracionMin.indexOf(baseMin);
                if (posDur >= 0) {
                    viewModel.setDuracionMin(baseMin);
                    actvDuracion.setText(etiquetasDuracion.get(posDur), false);
                }

                actualizarConfiguracionSelector();
            });

            actualizarConfiguracionSelector();
        });
    }

    private void actualizarConfiguracionSelector() {
        if (viewModel.getPaqueteSeleccionado() != null) {
            selectorHorarioHelper.setConfiguracion(
                    viewModel.getPaqueteSeleccionado().getId(),
                    viewModel.getDuracionMin(),
                    null
            );
        }
    }

    @Override
    public void onHorarioSeleccionado(long inicioMillis) {
        viewModel.setHorarioInicioMillis(inicioMillis);
        viewModel.setFechaMillis(TiempoUtils.inicioDelDia(inicioMillis));
        actualizarResumenEnVivo(inicioMillis);
    }

    @Override
    public void onHorarioLimpiado() {
        viewModel.setHorarioInicioMillis(null);
        cardResumenEnVivo.setVisibility(View.GONE);
    }

    private void actualizarResumenEnVivo(long inicioMillis) {
        Paquete paquete = viewModel.getPaqueteSeleccionado();
        if (paquete == null) return;

        int duracionMin = viewModel.getDuracionMin();
        long finMillis = TiempoUtils.sumarMinutos(inicioMillis, duracionMin);

        ResultadoDisponibilidad eval = pedidoRepository.evaluarDisponibilidad(
                paquete.getId(), inicioMillis, duracionMin, null);

        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", new Locale("es", "MX"));
        sdfHora.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));

        tvResumenFin.setText(getString(R.string.label_resumen_fin, sdfHora.format(new Date(finMillis))));
        tvResumenMontaje.setText(getString(R.string.label_resumen_montaje,
                sdfHora.format(new Date(eval.getBloqueoInicio()))));
        tvResumenDesmontaje.setText(getString(R.string.label_resumen_desmontaje,
                sdfHora.format(new Date(eval.getBloqueoFin()))));

        // Nombres de personal asignado
        List<String> opIds = eval.getOperadorIds();
        StringBuilder sbPersonal = new StringBuilder();
        if (opIds != null && !opIds.isEmpty()) {
            for (String opId : opIds) {
                Usuario u = usuarioRepository.getUsuarioPorId(opId);
                if (u != null) {
                    if (sbPersonal.length() > 0) sbPersonal.append(", ");
                    String rolLabel = Usuario.ROL_SOCIO.equals(u.getRol()) ? "Socio" : "Asistente";
                    sbPersonal.append(u.getNombre()).append(" (").append(rolLabel).append(")");
                }
            }
        }
        tvResumenPersonal.setText(getString(R.string.label_resumen_personal,
                sbPersonal.length() > 0 ? sbPersonal.toString() : "Pendiente"));

        double total = MotorDisponibilidad.calcularTotal(paquete, duracionMin);
        double anticipo = MotorDisponibilidad.calcularAnticipo(total);

        tvResumenTotal.setText(getString(R.string.label_resumen_total, FormatUtils.formatoMoneda(total)));
        tvResumenAnticipo.setText(getString(R.string.label_resumen_anticipo, FormatUtils.formatoMoneda(anticipo)));

        cardResumenEnVivo.setVisibility(View.VISIBLE);
    }

    private void configurarBotonApartar() {
        btnApartar.setOnClickListener(v -> validarYCrearApartado());
    }

    private void validarYCrearApartado() {
        boolean valido = true;

        if (viewModel.getPaqueteSeleccionado() == null) {
            tilPaquete.setError(getString(R.string.err_campo_requerido));
            valido = false;
        }

        if (viewModel.getHorarioInicioMillis() == null) {
            Toast.makeText(requireContext(), R.string.err_horario_no_seleccionado, Toast.LENGTH_SHORT).show();
            valido = false;
        }

        String nomEvento = (etNombreEvento.getText() != null) ? etNombreEvento.getText().toString().trim() : "";
        if (nomEvento.isEmpty() || nomEvento.length() > 80) {
            tilNombreEvento.setError(getString(R.string.err_nombre_evento_invalido));
            valido = false;
        }

        String nomCliente = (etNombreCliente.getText() != null) ? etNombreCliente.getText().toString().trim() : "";
        if (nomCliente.isEmpty()) {
            tilNombreCliente.setError(getString(R.string.err_nombre_cliente_invalido));
            valido = false;
        }

        String tel = (etTelefonoCliente.getText() != null) ? etTelefonoCliente.getText().toString().trim() : "";
        String digitosTel = tel.replaceAll("\\D", "");
        if (digitosTel.length() < 10) {
            tilTelefonoCliente.setError(getString(R.string.err_telefono_invalido));
            valido = false;
        }

        String dir = (etDireccion.getText() != null) ? etDireccion.getText().toString().trim() : "";
        if (dir.length() < 10) {
            tilDireccion.setError(getString(R.string.err_direccion_corta));
            valido = false;
        }

        if (!valido) return;

        // Ocultar teclado
        View focusedView = requireActivity().getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }

        btnApartar.setEnabled(false);

        String notas = (etNotas.getText() != null) ? etNotas.getText().toString().trim() : "";

        SolicitudReserva solicitud = new SolicitudReserva(
                viewModel.getPaqueteSeleccionado().getId(),
                viewModel.getHorarioInicioMillis(),
                viewModel.getDuracionMin(),
                viewModel.getOrigen(),
                nomEvento,
                clienteIdSeleccionado, // clienteId si corresponde a un Usuario; si no, null
                nomCliente,
                tel,
                dir,
                notas,
                viewModel.getHorasApartado()
        );

        pedidoRepository.crearReserva(solicitud, new Callback<Pedido>() {
            @Override
            public void onSuccess(Pedido result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.toast_apartado_creado, Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("pedidoId", result.getId());
                Navigation.findNavController(requireView()).navigate(R.id.action_nuevoPedido_to_detalle, args);
            }

            @Override
            public void onError(String mensaje) {
                if (!isAdded()) return;
                btnApartar.setEnabled(true);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("No se pudo crear el apartado")
                        .setMessage(mensaje)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });
    }

    private static class SimpleWatcher implements TextWatcher {
        interface OnChange { void onChange(String text); }
        private final OnChange onChange;
        SimpleWatcher(OnChange onChange) { this.onChange = onChange; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChange.onChange(s != null ? s.toString() : "");
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
