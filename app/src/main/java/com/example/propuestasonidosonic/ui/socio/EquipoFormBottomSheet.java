package com.example.propuestasonidosonic.ui.socio;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class EquipoFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_EQUIPO_ID = "arg_equipo_id";
    private static final String KEY_FOTO_URI = "key_foto_uri";
    private static final String KEY_NOMBRE = "key_nombre";
    private static final String KEY_CATEGORIA = "key_categoria";
    private static final String KEY_PRECIO = "key_precio";
    private static final String KEY_ESTADO = "key_estado";
    private static final String KEY_CANTIDAD = "key_cantidad";

    private final List<String> opcionesEstado = Arrays.asList(
            Equipo.ESTADO_DISPONIBLE,
            Equipo.ESTADO_EN_REPARACION,
            Equipo.ESTADO_DANADO
    );

    private String equipoId;
    private Equipo equipoOriginal;
    private String fotoUriActual;

    private TextView tvTituloDialog;
    private ShapeableImageView ivFotoPreview;
    private MaterialButton btnElegirFoto;
    private MaterialButton btnQuitarFoto;
    private TextInputLayout tilNombreEquipo;
    private TextInputEditText etNombreEquipo;
    private Spinner spCategoria;
    private TextInputLayout tilPrecioRenta;
    private TextInputEditText etPrecioRenta;
    private Spinner spEstadoFisico;
    private TextInputLayout tilCantidad;
    private TextInputEditText etCantidad;
    private MaterialButton btnGuardar;
    private MaterialButton btnEliminar;
    private MaterialButton btnCancelar;

    private EquipoRepository equipoRepository;
    private EventoRepository eventoRepository;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    fotoUriActual = uri.toString();
                    actualizarPreviewFoto();
                }
            });

    public static EquipoFormBottomSheet newInstance(@Nullable String equipoId) {
        EquipoFormBottomSheet fragment = new EquipoFormBottomSheet();
        if (equipoId != null) {
            Bundle args = new Bundle();
            args.putString(ARG_EQUIPO_ID, equipoId);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer container = ((SonidoSonicApp) requireActivity().getApplication()).getAppContainer();
        equipoRepository = container.getEquipoRepository();
        eventoRepository = container.getEventoRepository();

        if (getArguments() != null) {
            equipoId = getArguments().getString(ARG_EQUIPO_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_equipo_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inicializarVistas(view);
        configurarSpinners();
        configurarListeners();

        if (savedInstanceState != null) {
            restaurarEstado(savedInstanceState);
        } else if (equipoId != null) {
            cargarDatosEquipo();
        } else {
            configurarModoCreacion();
        }
    }

    private void inicializarVistas(View view) {
        tvTituloDialog = view.findViewById(R.id.tvTituloDialog);
        ivFotoPreview = view.findViewById(R.id.ivFotoPreview);
        btnElegirFoto = view.findViewById(R.id.btnElegirFoto);
        btnQuitarFoto = view.findViewById(R.id.btnQuitarFoto);
        tilNombreEquipo = view.findViewById(R.id.tilNombreEquipo);
        etNombreEquipo = view.findViewById(R.id.etNombreEquipo);
        spCategoria = view.findViewById(R.id.spCategoriaDialog);
        tilPrecioRenta = view.findViewById(R.id.tilPrecioRenta);
        etPrecioRenta = view.findViewById(R.id.etPrecioRenta);
        spEstadoFisico = view.findViewById(R.id.spEstadoFisicoDialog);
        tilCantidad = view.findViewById(R.id.tilCantidad);
        etCantidad = view.findViewById(R.id.etCantidad);
        btnGuardar = view.findViewById(R.id.btnGuardarEquipo);
        btnEliminar = view.findViewById(R.id.btnEliminarEquipo);
        btnCancelar = view.findViewById(R.id.btnCancelarEquipo);
    }

    private void configurarSpinners() {
        // Spinner Categoría
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoriaEquipo.TODAS_LAS_CATEGORIAS);
        spCategoria.setAdapter(adapterCat);

        // Spinner Estado Físico
        List<String> nombresEstados = Arrays.asList(
                getString(R.string.estado_disponible),
                getString(R.string.estado_en_reparacion),
                getString(R.string.estado_danado)
        );
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, nombresEstados);
        spEstadoFisico.setAdapter(adapterEstado);
    }

    private void configurarListeners() {
        btnElegirFoto.setOnClickListener(v -> {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnQuitarFoto.setOnClickListener(v -> {
            fotoUriActual = null;
            actualizarPreviewFoto();
        });

        spCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (fotoUriActual == null) {
                    actualizarPreviewFoto();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancelar.setOnClickListener(v -> dismiss());

        btnGuardar.setOnClickListener(v -> procesarGuardado());

        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }

    private void configurarModoCreacion() {
        tvTituloDialog.setText(R.string.dialog_nuevo_equipo_titulo);
        tilCantidad.setVisibility(View.VISIBLE);
        btnEliminar.setVisibility(View.GONE);
        actualizarPreviewFoto();
    }

    private void cargarDatosEquipo() {
        equipoRepository.getEquipoPorId(equipoId, new Callback<Equipo>() {
            @Override
            public void onSuccess(Equipo e) {
                if (!isAdded()) return;
                equipoOriginal = e;
                tvTituloDialog.setText(getString(R.string.dialog_editar_equipo_titulo));
                tilCantidad.setVisibility(View.GONE);
                btnEliminar.setVisibility(View.VISIBLE);

                etNombreEquipo.setText(e.getNombre());
                etPrecioRenta.setText(String.valueOf(e.getPrecioRenta()));
                fotoUriActual = e.getFotoUri();

                int catPos = CategoriaEquipo.TODAS_LAS_CATEGORIAS.indexOf(e.getCategoria());
                if (catPos >= 0) spCategoria.setSelection(catPos);

                int estadoPos = opcionesEstado.indexOf(e.getEstadoFisico());
                if (estadoPos >= 0) spEstadoFisico.setSelection(estadoPos);

                actualizarPreviewFoto();
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }
        });
    }

    private void actualizarPreviewFoto() {
        if (!isAdded()) return;

        String catSeleccionada = (String) spCategoria.getSelectedItem();
        int fallback = CategoriaEquipo.getImagenPorDefecto(catSeleccionada);

        if (fotoUriActual != null && !fotoUriActual.trim().isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(fotoUriActual))
                    .placeholder(fallback)
                    .error(fallback)
                    .centerCrop()
                    .into(ivFotoPreview);
            btnQuitarFoto.setVisibility(View.VISIBLE);
        } else {
            Glide.with(this)
                    .load(fallback)
                    .centerCrop()
                    .into(ivFotoPreview);
            btnQuitarFoto.setVisibility(View.GONE);
        }
    }

    private void procesarGuardado() {
        tilNombreEquipo.setError(null);
        tilPrecioRenta.setError(null);
        tilCantidad.setError(null);

        String nombre = etNombreEquipo.getText() != null ? etNombreEquipo.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nombre)) {
            tilNombreEquipo.setError(getString(R.string.error_nombre_requerido));
            return;
        }

        String precioStr = etPrecioRenta.getText() != null ? etPrecioRenta.getText().toString().trim() : "";
        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio < 0) {
                tilPrecioRenta.setError(getString(R.string.error_precio_invalido));
                return;
            }
        } catch (NumberFormatException e) {
            tilPrecioRenta.setError(getString(R.string.error_precio_invalido));
            return;
        }

        String categoria = (String) spCategoria.getSelectedItem();
        int estadoIndex = spEstadoFisico.getSelectedItemPosition();
        String estadoFisico = (estadoIndex >= 0 && estadoIndex < opcionesEstado.size())
                ? opcionesEstado.get(estadoIndex) : Equipo.ESTADO_DISPONIBLE;

        if (equipoOriginal == null) {
            // Modo creación
            String cantStr = etCantidad.getText() != null ? etCantidad.getText().toString().trim() : "1";
            int cantidad;
            try {
                cantidad = Integer.parseInt(cantStr);
                if (cantidad < 1 || cantidad > 50) {
                    tilCantidad.setError(getString(R.string.error_cantidad_invalida));
                    return;
                }
            } catch (NumberFormatException e) {
                tilCantidad.setError(getString(R.string.error_cantidad_invalida));
                return;
            }

            guardarNuevasUnidades(nombre, categoria, precio, estadoFisico, cantidad, fotoUriActual);
        } else {
            // Modo edición
            // Verificar si cambia a En reparación o Dañado teniendo reservas futuras
            boolean cambiaAEstadoNoDisponible = !Equipo.ESTADO_DISPONIBLE.equals(estadoFisico) &&
                    Equipo.ESTADO_DISPONIBLE.equals(equipoOriginal.getEstadoFisico());

            if (cambiaAEstadoNoDisponible) {
                verificarReservasFuturasYGuardar(nombre, categoria, precio, estadoFisico, fotoUriActual);
            } else {
                guardarEdicion(nombre, categoria, precio, estadoFisico, fotoUriActual);
            }
        }
    }

    private void guardarNuevasUnidades(String nombre, String categoria, double precio,
                                       String estadoFisico, int cantidad, String fotoUri) {
        equipoRepository.agregarUnidades(nombre, categoria, precio, estadoFisico, cantidad, fotoUri,
                new Callback<List<Equipo>>() {
                    @Override
                    public void onSuccess(List<Equipo> result) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    cantidad > 1 ? cantidad + " unidades creadas exitosamente" : "Unidad creada exitosamente",
                                    Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void verificarReservasFuturasYGuardar(String nombre, String categoria, double precio,
                                                  String estadoFisico, String fotoUri) {
        List<Evento> reservasFuturas = eventoRepository.reservasFuturas(equipoOriginal.getId(), System.currentTimeMillis());
        if (!reservasFuturas.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Evento ev : reservasFuturas) {
                sb.append("• ").append(ev.getNombre()).append(" (")
                        .append(FormatUtils.formatoFecha(ev.getInicioMillis())).append(")\n");
            }

            String estadoNombre = Equipo.ESTADO_EN_REPARACION.equals(estadoFisico)
                    ? getString(R.string.estado_en_reparacion) : getString(R.string.estado_danado);

            String mensaje = getString(R.string.dialog_advertencia_reservas_mensaje, sb.toString().trim(), estadoNombre);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_advertencia_reservas_titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Continuar y cambiar", (dialog, which) -> {
                        guardarEdicion(nombre, categoria, precio, estadoFisico, fotoUri);
                    })
                    .setNegativeButton(R.string.dialog_cancelar, null)
                    .show();
        } else {
            guardarEdicion(nombre, categoria, precio, estadoFisico, fotoUri);
        }
    }

    private void guardarEdicion(String nombre, String categoria, double precio,
                                String estadoFisico, String fotoUri) {
        equipoOriginal.setNombre(nombre);
        equipoOriginal.setCategoria(categoria);
        equipoOriginal.setPrecioRenta(precio);
        equipoOriginal.setEstadoFisico(estadoFisico);
        equipoOriginal.setFotoUri(fotoUri);

        equipoRepository.actualizar(equipoOriginal, new Callback<Equipo>() {
            @Override
            public void onSuccess(Equipo result) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Unidad actualizada", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmarEliminacion() {
        if (equipoOriginal == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_eliminar_equipo_titulo)
                .setMessage(R.string.dialog_eliminar_equipo_mensaje)
                .setPositiveButton(R.string.btn_eliminar, (dialog, which) -> {
                    equipoRepository.eliminar(equipoOriginal.getId(), new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Unidad eliminada", Toast.LENGTH_SHORT).show();
                                dismiss();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (isAdded()) {
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("No se puede eliminar")
                                        .setMessage(error)
                                        .setPositiveButton("Aceptar", null)
                                        .show();
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FOTO_URI, fotoUriActual);
        if (etNombreEquipo != null) {
            outState.putString(KEY_NOMBRE, etNombreEquipo.getText() != null ? etNombreEquipo.getText().toString() : "");
        }
        if (spCategoria != null) {
            outState.putInt(KEY_CATEGORIA, spCategoria.getSelectedItemPosition());
        }
        if (etPrecioRenta != null) {
            outState.putString(KEY_PRECIO, etPrecioRenta.getText() != null ? etPrecioRenta.getText().toString() : "");
        }
        if (spEstadoFisico != null) {
            outState.putInt(KEY_ESTADO, spEstadoFisico.getSelectedItemPosition());
        }
        if (etCantidad != null) {
            outState.putString(KEY_CANTIDAD, etCantidad.getText() != null ? etCantidad.getText().toString() : "");
        }
    }

    private void restaurarEstado(Bundle state) {
        fotoUriActual = state.getString(KEY_FOTO_URI);
        String nombre = state.getString(KEY_NOMBRE);
        if (nombre != null) etNombreEquipo.setText(nombre);
        int catPos = state.getInt(KEY_CATEGORIA, 0);
        spCategoria.setSelection(catPos);
        String precio = state.getString(KEY_PRECIO);
        if (precio != null) etPrecioRenta.setText(precio);
        int estadoPos = state.getInt(KEY_ESTADO, 0);
        spEstadoFisico.setSelection(estadoPos);
        String cantidad = state.getString(KEY_CANTIDAD);
        if (cantidad != null) etCantidad.setText(cantidad);

        if (equipoId != null) {
            tvTituloDialog.setText(R.string.dialog_editar_equipo_titulo);
            tilCantidad.setVisibility(View.GONE);
            btnEliminar.setVisibility(View.VISIBLE);
            equipoRepository.getEquipoPorId(equipoId, new Callback<Equipo>() {
                @Override
                public void onSuccess(Equipo e) {
                    equipoOriginal = e;
                }
                @Override
                public void onError(String error) {}
            });
        } else {
            configurarModoCreacion();
        }
        actualizarPreviewFoto();
    }
}
