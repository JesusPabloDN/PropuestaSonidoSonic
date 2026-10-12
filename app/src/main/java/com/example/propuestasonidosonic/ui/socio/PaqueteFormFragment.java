package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.ComponenteAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaqueteFormFragment extends Fragment implements ComponenteAdapter.OnComponenteActionListener {

    private static final String ARG_PAQUETE_ID = "paqueteId";

    private String paqueteId;
    private Paquete paqueteOriginal;
    private PaqueteFormViewModel viewModel;

    private PaqueteRepository paqueteRepository;
    private EquipoRepository equipoRepository;

    private TextInputLayout tilNombrePaquete;
    private TextInputEditText etNombrePaquete;
    private TextInputLayout tilDescripcionPaquete;
    private TextInputEditText etDescripcionPaquete;
    private RecyclerView rvComponentes;
    private TextView tvEmptyComponentes;
    private TextView tvErrorComponentes;
    private MaterialButton btnAgregarEquipo;
    private TextView tvPrecioSugerido;
    private MaterialButton btnUsarPrecioSugerido;
    private TextInputLayout tilPrecioPaquete;
    private TextInputEditText etPrecioPaquete;
    private TextInputLayout tilDuracionBase;
    private TextInputEditText etDuracionBase;
    private TextInputLayout tilPrecioHoraExtra;
    private TextInputEditText etPrecioHoraExtra;
    private MaterialButtonToggleGroup toggleOperadores;
    private TextInputLayout tilBufferMontaje;
    private TextInputEditText etBufferMontaje;
    private TextInputLayout tilBufferDesmontaje;
    private TextInputEditText etBufferDesmontaje;
    private MaterialSwitch swActivo;
    private MaterialButton btnGuardarPaquete;
    private MaterialButton btnEliminarPaquete;

    private ComponenteAdapter componenteAdapter;
    private double precioSugeridoCalculado = 0.0;
    private final Map<String, ModeloInventario> modelosMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer container = ((SonidoSonicApp) requireActivity().getApplication()).getAppContainer();
        paqueteRepository = container.getPaqueteRepository();
        equipoRepository = container.getEquipoRepository();

        if (getArguments() != null) {
            paqueteId = getArguments().getString(ARG_PAQUETE_ID);
        }

        viewModel = new ViewModelProvider(this).get(PaqueteFormViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paquete_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inicializarVistas(view);
        cargarModelosInventario();
        configurarRecyclerView();
        configurarListeners();

        if (paqueteId != null && !paqueteId.trim().isEmpty()) {
            paqueteOriginal = paqueteRepository.getPaquetePorId(paqueteId);
            if (paqueteOriginal == null) {
                Toast.makeText(requireContext(), "Paquete no encontrado", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                return;
            }
        }

        if (!viewModel.isInicializado()) {
            cargarDatosIniciales();
            viewModel.setInicializado(true);
        } else {
            actualizarListaComponentes();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarTituloBarra();
    }

    private void actualizarTituloBarra() {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) {
                if (paqueteId != null && !paqueteId.trim().isEmpty()) {
                    ab.setTitle(R.string.paquete_form_titulo_editar);
                } else {
                    ab.setTitle(R.string.paquete_form_titulo_nuevo);
                }
            }
        }
    }

    private void inicializarVistas(View view) {
        tilNombrePaquete = view.findViewById(R.id.tilNombrePaquete);
        etNombrePaquete = view.findViewById(R.id.etNombrePaquete);
        tilDescripcionPaquete = view.findViewById(R.id.tilDescripcionPaquete);
        etDescripcionPaquete = view.findViewById(R.id.etDescripcionPaquete);
        rvComponentes = view.findViewById(R.id.rvComponentes);
        tvEmptyComponentes = view.findViewById(R.id.tvEmptyComponentes);
        tvErrorComponentes = view.findViewById(R.id.tvErrorComponentes);
        btnAgregarEquipo = view.findViewById(R.id.btnAgregarEquipo);
        tvPrecioSugerido = view.findViewById(R.id.tvPrecioSugerido);
        btnUsarPrecioSugerido = view.findViewById(R.id.btnUsarPrecioSugerido);
        tilPrecioPaquete = view.findViewById(R.id.tilPrecioPaquete);
        etPrecioPaquete = view.findViewById(R.id.etPrecioPaquete);
        tilDuracionBase = view.findViewById(R.id.tilDuracionBase);
        etDuracionBase = view.findViewById(R.id.etDuracionBase);
        tilPrecioHoraExtra = view.findViewById(R.id.tilPrecioHoraExtra);
        etPrecioHoraExtra = view.findViewById(R.id.etPrecioHoraExtra);
        toggleOperadores = view.findViewById(R.id.toggleOperadores);
        tilBufferMontaje = view.findViewById(R.id.tilBufferMontaje);
        etBufferMontaje = view.findViewById(R.id.etBufferMontaje);
        tilBufferDesmontaje = view.findViewById(R.id.tilBufferDesmontaje);
        etBufferDesmontaje = view.findViewById(R.id.etBufferDesmontaje);
        swActivo = view.findViewById(R.id.swActivo);
        btnGuardarPaquete = view.findViewById(R.id.btnGuardarPaquete);
        btnEliminarPaquete = view.findViewById(R.id.btnEliminarPaquete);
    }

    private void cargarModelosInventario() {
        modelosMap.clear();
        List<ModeloInventario> modelos = equipoRepository.obtenerModelos();
        for (ModeloInventario m : modelos) {
            modelosMap.put(m.getClave(), m);
        }
    }

    private void configurarRecyclerView() {
        componenteAdapter = new ComponenteAdapter(this);
        componenteAdapter.setModelosMap(modelosMap);
        rvComponentes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComponentes.setAdapter(componenteAdapter);
    }

    private void configurarListeners() {
        btnAgregarEquipo.setOnClickListener(v -> mostrarDialogoAgregarModelo());

        btnUsarPrecioSugerido.setOnClickListener(v -> {
            etPrecioPaquete.setText(String.format(java.util.Locale.ROOT, "%.2f", precioSugeridoCalculado));
            tilPrecioPaquete.setError(null);
        });

        btnGuardarPaquete.setOnClickListener(v -> procesarGuardado());

        btnEliminarPaquete.setOnClickListener(v -> confirmarEliminacion());
    }

    private void cargarDatosIniciales() {
        if (paqueteOriginal != null) {
            etNombrePaquete.setText(paqueteOriginal.getNombre());
            etDescripcionPaquete.setText(paqueteOriginal.getDescripcion());
            etPrecioPaquete.setText(String.format(java.util.Locale.ROOT, "%.2f", paqueteOriginal.getPrecio()));
            etDuracionBase.setText(String.valueOf(paqueteOriginal.getDuracionBaseHoras()));
            etPrecioHoraExtra.setText(String.format(java.util.Locale.ROOT, "%.2f", paqueteOriginal.getPrecioHoraExtra()));

            if (paqueteOriginal.getOperadoresRequeridos() == 2) {
                toggleOperadores.check(R.id.btnOperadores2);
            } else {
                toggleOperadores.check(R.id.btnOperadores1);
            }

            etBufferMontaje.setText(String.valueOf(paqueteOriginal.getBufferMontajeMin()));
            etBufferDesmontaje.setText(String.valueOf(paqueteOriginal.getBufferDesmontajeMin()));
            swActivo.setChecked(paqueteOriginal.isActivo());
            btnEliminarPaquete.setVisibility(View.VISIBLE);

            viewModel.setComponentes(paqueteOriginal.getComponentes());
        } else {
            etDuracionBase.setText(String.valueOf(Config.DURACION_BASE_HORAS_DEFAULT));
            etPrecioHoraExtra.setText("0");
            toggleOperadores.check(R.id.btnOperadores1);
            etBufferMontaje.setText(String.valueOf(Config.BUFFER_MONTAJE_MIN_DEFAULT));
            etBufferDesmontaje.setText(String.valueOf(Config.BUFFER_DESMONTAJE_MIN_DEFAULT));
            swActivo.setChecked(true);
            btnEliminarPaquete.setVisibility(View.GONE);
        }
        actualizarListaComponentes();
    }

    private void actualizarListaComponentes() {
        List<ComponentePaquete> comps = viewModel.getComponentes();
        componenteAdapter.setComponentes(comps);

        if (comps.isEmpty()) {
            tvEmptyComponentes.setVisibility(View.VISIBLE);
            rvComponentes.setVisibility(View.GONE);
        } else {
            tvEmptyComponentes.setVisibility(View.GONE);
            rvComponentes.setVisibility(View.VISIBLE);
            tvErrorComponentes.setVisibility(View.GONE);
        }

        // Recalcular precio sugerido
        double suma = 0.0;
        for (ComponentePaquete c : comps) {
            ModeloInventario mod = modelosMap.get(c.getModeloClave());
            if (mod != null) {
                suma += mod.getPrecioUnitario() * c.getCantidad();
            }
        }
        precioSugeridoCalculado = suma;
        tvPrecioSugerido.setText(getString(R.string.precio_sugerido_formato, FormatUtils.formatoMoneda(suma)));
    }

    private void mostrarDialogoAgregarModelo() {
        List<ModeloInventario> todos = equipoRepository.obtenerModelos();
        if (todos.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_agregar_equipo_titulo)
                    .setMessage(R.string.dialog_sin_modelos)
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        // Filtrar los que no están ya en el paquete
        List<String> clavesActuales = new ArrayList<>();
        for (ComponentePaquete c : viewModel.getComponentes()) {
            clavesActuales.add(c.getModeloClave());
        }

        List<ModeloInventario> disponibles = new ArrayList<>();
        for (ModeloInventario m : todos) {
            if (!clavesActuales.contains(m.getClave())) {
                disponibles.add(m);
            }
        }

        if (disponibles.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los modelos disponibles ya han sido agregados", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] opciones = new String[disponibles.size()];
        for (int i = 0; i < disponibles.size(); i++) {
            ModeloInventario m = disponibles.get(i);
            opciones[i] = getString(R.string.modelo_opcion_formato,
                    m.getNombreModelo(), m.getCategoria(), m.getUnidadesDisponibles(), m.getTotalUnidades());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_agregar_equipo_titulo)
                .setItems(opciones, (dialog, which) -> {
                    ModeloInventario seleccionado = disponibles.get(which);
                    ComponentePaquete nuevo = new ComponentePaquete(
                            seleccionado.getClave(),
                            seleccionado.getNombreModelo(),
                            seleccionado.getCategoria(),
                            1
                    );
                    viewModel.agregarComponente(nuevo);
                    actualizarListaComponentes();
                })
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    @Override
    public void onCantidadCambiada(ComponentePaquete componente, int nuevaCantidad) {
        componente.setCantidad(nuevaCantidad);
        actualizarListaComponentes();
    }

    @Override
    public void onQuitarComponente(ComponentePaquete componente) {
        viewModel.removerComponente(componente);
        actualizarListaComponentes();
    }

    private void procesarGuardado() {
        limpiarErrores();

        String nombre = etNombrePaquete.getText() != null ? etNombrePaquete.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nombre)) {
            tilNombrePaquete.setError(getString(R.string.error_nombre_paquete_requerido));
            return;
        }

        List<ComponentePaquete> componentes = viewModel.getComponentes();
        if (componentes.isEmpty()) {
            tvErrorComponentes.setText(R.string.error_sin_componentes);
            tvErrorComponentes.setVisibility(View.VISIBLE);
            return;
        }

        String precioStr = etPrecioPaquete.getText() != null ? etPrecioPaquete.getText().toString().trim() : "";
        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio <= 0) {
                tilPrecioPaquete.setError(getString(R.string.error_precio_paquete_invalido));
                return;
            }
        } catch (NumberFormatException e) {
            tilPrecioPaquete.setError(getString(R.string.error_precio_paquete_invalido));
            return;
        }

        String duracionStr = etDuracionBase.getText() != null ? etDuracionBase.getText().toString().trim() : "";
        int duracion;
        try {
            duracion = Integer.parseInt(duracionStr);
            if (duracion < 1 || duracion > 24) {
                tilDuracionBase.setError(getString(R.string.error_duracion_invalida));
                return;
            }
        } catch (NumberFormatException e) {
            tilDuracionBase.setError(getString(R.string.error_duracion_invalida));
            return;
        }

        String horaExtraStr = etPrecioHoraExtra.getText() != null ? etPrecioHoraExtra.getText().toString().trim() : "";
        double precioHoraExtra;
        try {
            precioHoraExtra = Double.parseDouble(horaExtraStr);
            if (precioHoraExtra < 0) {
                tilPrecioHoraExtra.setError(getString(R.string.error_precio_hora_extra_invalido));
                return;
            }
        } catch (NumberFormatException e) {
            tilPrecioHoraExtra.setError(getString(R.string.error_precio_hora_extra_invalido));
            return;
        }

        int operadores = (toggleOperadores.getCheckedButtonId() == R.id.btnOperadores2) ? 2 : 1;

        String montajeStr = etBufferMontaje.getText() != null ? etBufferMontaje.getText().toString().trim() : "";
        int bufferMontaje;
        try {
            bufferMontaje = Integer.parseInt(montajeStr);
            if (bufferMontaje < 0 || bufferMontaje > 480) {
                tilBufferMontaje.setError(getString(R.string.error_buffer_invalido));
                return;
            }
        } catch (NumberFormatException e) {
            tilBufferMontaje.setError(getString(R.string.error_buffer_invalido));
            return;
        }

        String desmontajeStr = etBufferDesmontaje.getText() != null ? etBufferDesmontaje.getText().toString().trim() : "";
        int bufferDesmontaje;
        try {
            bufferDesmontaje = Integer.parseInt(desmontajeStr);
            if (bufferDesmontaje < 0 || bufferDesmontaje > 480) {
                tilBufferDesmontaje.setError(getString(R.string.error_buffer_invalido));
                return;
            }
        } catch (NumberFormatException e) {
            tilBufferDesmontaje.setError(getString(R.string.error_buffer_invalido));
            return;
        }

        boolean activo = swActivo.isChecked();
        String descripcion = etDescripcionPaquete.getText() != null ? etDescripcionPaquete.getText().toString().trim() : "";

        Paquete p = paqueteOriginal != null ? new Paquete(paqueteOriginal) : new Paquete();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setDuracionBaseHoras(duracion);
        p.setPrecioHoraExtra(precioHoraExtra);
        p.setOperadoresRequeridos(operadores);
        p.setBufferMontajeMin(bufferMontaje);
        p.setBufferDesmontajeMin(bufferDesmontaje);
        p.setActivo(activo);
        p.setComponentes(componentes);

        btnGuardarPaquete.setEnabled(false);

        paqueteRepository.guardarPaquete(p, new Callback<Paquete>() {
            @Override
            public void onSuccess(Paquete result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.msg_paquete_guardado, Toast.LENGTH_SHORT).show();
                ocultarTeclado();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                btnGuardarPaquete.setEnabled(true);
                if (error != null && error.contains("nombre")) {
                    tilNombrePaquete.setError(error);
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("No se pudo guardar")
                        .setMessage(error)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });
    }

    private void limpiarErrores() {
        tilNombrePaquete.setError(null);
        tvErrorComponentes.setVisibility(View.GONE);
        tilPrecioPaquete.setError(null);
        tilDuracionBase.setError(null);
        tilPrecioHoraExtra.setError(null);
        tilBufferMontaje.setError(null);
        tilBufferDesmontaje.setError(null);
    }

    private void confirmarEliminacion() {
        if (paqueteOriginal == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_eliminar_paquete_titulo)
                .setMessage(R.string.dialog_eliminar_paquete_mensaje)
                .setPositiveButton(R.string.btn_eliminar, (dialog, which) -> {
                    paqueteRepository.eliminarPaquete(paqueteOriginal.getId(), new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), R.string.msg_paquete_eliminado, Toast.LENGTH_SHORT).show();
                            ocultarTeclado();
                            getParentFragmentManager().popBackStack();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Error al eliminar")
                                    .setMessage(error)
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    });
                })
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    private void ocultarTeclado() {
        View view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
