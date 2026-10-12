package com.example.propuestasonidosonic.ui.socio;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.PasswordValidator;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class PersonalFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USUARIO_ID = "arg_usuario_id";

    private String usuarioIdEdicion;
    private Usuario usuarioOriginal;

    private UsuarioRepository usuarioRepository;
    private PedidoRepository pedidoRepository;
    private SessionManager sessionManager;

    private TextView tvTituloDialog;
    private TextView tvLabelRol;
    private MaterialButtonToggleGroup toggleRol;
    private MaterialButton btnRolSocio;
    private MaterialButton btnRolAsistente;
    private TextView tvAvisoNoEditable;

    private TextInputLayout tilNombre;
    private TextInputEditText etNombre;
    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private TextInputLayout tilTelefono;
    private TextInputEditText etTelefono;

    private MaterialSwitch switchCuentaActiva;

    private TextInputLayout tilContrasena;
    private TextInputEditText etContrasena;
    private TextInputLayout tilConfirmarContrasena;
    private TextInputEditText etConfirmarContrasena;

    private MaterialButton btnCancelar;
    private MaterialButton btnGuardar;

    public static PersonalFormBottomSheet nuevo() {
        return new PersonalFormBottomSheet();
    }

    public static PersonalFormBottomSheet editar(String usuarioId) {
        PersonalFormBottomSheet sheet = new PersonalFormBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USUARIO_ID, usuarioId);
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheetInternal = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_personal_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        usuarioRepository = appContainer.getUsuarioRepository();
        pedidoRepository = appContainer.getPedidoRepository();
        sessionManager = appContainer.getSessionManager();

        if (getArguments() != null) {
            usuarioIdEdicion = getArguments().getString(ARG_USUARIO_ID);
        }

        vincularVistas(view);
        configurarModo();

        btnCancelar.setOnClickListener(v -> dismiss());
        btnGuardar.setOnClickListener(v -> procesarGuardado());
    }

    private void vincularVistas(View view) {
        tvTituloDialog = view.findViewById(R.id.tvTituloDialog);
        tvLabelRol = view.findViewById(R.id.tvLabelRol);
        toggleRol = view.findViewById(R.id.toggleRol);
        btnRolSocio = view.findViewById(R.id.btnRolSocio);
        btnRolAsistente = view.findViewById(R.id.btnRolAsistente);
        tvAvisoNoEditable = view.findViewById(R.id.tvAvisoNoEditable);

        tilNombre = view.findViewById(R.id.tilNombre);
        etNombre = view.findViewById(R.id.etNombre);
        tilCorreo = view.findViewById(R.id.tilCorreo);
        etCorreo = view.findViewById(R.id.etCorreo);
        tilTelefono = view.findViewById(R.id.tilTelefono);
        etTelefono = view.findViewById(R.id.etTelefono);

        switchCuentaActiva = view.findViewById(R.id.switchCuentaActiva);

        tilContrasena = view.findViewById(R.id.tilContrasena);
        etContrasena = view.findViewById(R.id.etContrasena);
        tilConfirmarContrasena = view.findViewById(R.id.tilConfirmarContrasena);
        etConfirmarContrasena = view.findViewById(R.id.etConfirmarContrasena);

        btnCancelar = view.findViewById(R.id.btnCancelar);
        btnGuardar = view.findViewById(R.id.btnGuardar);
    }

    private void configurarModo() {
        if (usuarioIdEdicion == null) {
            // Modo Nuevo
            tvTituloDialog.setText(R.string.dialog_nuevo_personal_titulo);
            toggleRol.check(R.id.btnRolAsistente);
            switchCuentaActiva.setVisibility(View.GONE);
            tvAvisoNoEditable.setVisibility(View.GONE);
            tilContrasena.setHint(getString(R.string.hint_contrasena_temporal));
            tilConfirmarContrasena.setVisibility(View.VISIBLE);
        } else {
            // Modo Edición
            tvTituloDialog.setText(R.string.dialog_editar_personal_titulo);
            btnRolSocio.setEnabled(false);
            btnRolAsistente.setEnabled(false);
            tvAvisoNoEditable.setVisibility(View.VISIBLE);
            tilCorreo.setEnabled(false);
            etCorreo.setEnabled(false);
            switchCuentaActiva.setVisibility(View.VISIBLE);
            tilContrasena.setHint(getString(R.string.hint_nueva_contrasena_opcional));
            tilContrasena.setHelperText(null);

            cargarUsuarioParaEdicion();
        }
    }

    private void cargarUsuarioParaEdicion() {
        usuarioRepository.getUsuarioPorId(usuarioIdEdicion, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                if (!isAdded()) return;
                usuarioOriginal = usuario;
                if (Usuario.ROL_SOCIO.equals(usuario.getRol())) {
                    toggleRol.check(R.id.btnRolSocio);
                } else {
                    toggleRol.check(R.id.btnRolAsistente);
                }
                etNombre.setText(usuario.getNombre());
                etCorreo.setText(usuario.getCorreo());
                etTelefono.setText(usuario.getTelefono());
                switchCuentaActiva.setChecked(usuario.isCuentaActiva());
            }

            @Override
            public void onError(String mensaje) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilCorreo.setError(null);
        tilTelefono.setError(null);
        tilContrasena.setError(null);
        tilConfirmarContrasena.setError(null);
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

    private void procesarGuardado() {
        limpiarErrores();
        if (usuarioIdEdicion == null) {
            guardarNuevoPersonal();
        } else {
            guardarEdicionPersonal();
        }
    }

    private void guardarNuevoPersonal() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
        String contrasena = etContrasena.getText() != null ? etContrasena.getText().toString() : "";
        String confirmar = etConfirmarContrasena.getText() != null ? etConfirmarContrasena.getText().toString() : "";

        String rol = (toggleRol.getCheckedButtonId() == R.id.btnRolSocio)
                ? Usuario.ROL_SOCIO : Usuario.ROL_ASISTENTE;

        boolean valido = true;
        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }

        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }

        if (telefono.replaceAll("\\D", "").length() < 10) {
            tilTelefono.setError(getString(R.string.error_telefono_invalido));
            valido = false;
        }

        if (!PasswordValidator.isValid(contrasena)) {
            tilContrasena.setError(getString(R.string.error_contrasena_invalida));
            valido = false;
        }

        if (!contrasena.equals(confirmar)) {
            tilConfirmarContrasena.setError(getString(R.string.error_contrasenas_no_coinciden));
            valido = false;
        }

        if (!valido) return;

        btnGuardar.setEnabled(false);
        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setTelefono(telefono);
        nuevo.setRol(rol);

        usuarioRepository.crearPersonal(nuevo, contrasena, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                ocultarTeclado();
                Toast.makeText(requireContext(), R.string.msg_personal_creado, Toast.LENGTH_LONG).show();
                dismiss();
            }

            @Override
            public void onError(String mensaje) {
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                if (mensaje != null && mensaje.toLowerCase().contains("correo")) {
                    tilCorreo.setError(mensaje);
                } else if (mensaje != null && mensaje.toLowerCase().contains("contraseña")) {
                    tilContrasena.setError(mensaje);
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.dialog_nuevo_personal_titulo)
                            .setMessage(mensaje)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        });
    }

    private void guardarEdicionPersonal() {
        if (usuarioOriginal == null) return;

        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
        String nuevaContrasena = etContrasena.getText() != null ? etContrasena.getText().toString() : "";
        String confirmar = etConfirmarContrasena.getText() != null ? etConfirmarContrasena.getText().toString() : "";
        boolean nuevaCuentaActiva = switchCuentaActiva.isChecked();

        boolean valido = true;
        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }

        if (!telefono.isEmpty() && telefono.replaceAll("\\D", "").length() < 10) {
            tilTelefono.setError(getString(R.string.error_telefono_invalido));
            valido = false;
        }

        if (!nuevaContrasena.isEmpty()) {
            if (!PasswordValidator.isValid(nuevaContrasena)) {
                tilContrasena.setError(getString(R.string.error_contrasena_invalida));
                valido = false;
            }
            if (!nuevaContrasena.equals(confirmar)) {
                tilConfirmarContrasena.setError(getString(R.string.error_contrasenas_no_coinciden));
                valido = false;
            }
        }

        if (!valido) return;

        // Comprobar si se está desactivando una cuenta activa
        if (usuarioOriginal.isCuentaActiva() && !nuevaCuentaActiva) {
            long ahora = System.currentTimeMillis();
            List<Pedido> reservasFuturas = pedidoRepository.obtenerReservasActivasDeOperador(
                    usuarioOriginal.getId(), ahora, Long.MAX_VALUE);

            if (!reservasFuturas.isEmpty()) {
                String mensaje = String.format(getString(R.string.dialog_desactivar_con_reservas_formato),
                        usuarioOriginal.getNombre(), reservasFuturas.size());
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_desactivar_titulo)
                        .setMessage(mensaje)
                        .setNegativeButton(R.string.dialog_cancelar, null)
                        .setPositiveButton(R.string.btn_desactivar, (dialog, which) -> {
                            ejecutarActualizacion(nombre, telefono, false, nuevaContrasena);
                        })
                        .show();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_desactivar_titulo)
                        .setMessage(R.string.dialog_desactivar_sin_reservas)
                        .setNegativeButton(R.string.dialog_cancelar, null)
                        .setPositiveButton(R.string.btn_desactivar, (dialog, which) -> {
                            ejecutarActualizacion(nombre, telefono, false, nuevaContrasena);
                        })
                        .show();
            }
        } else {
            ejecutarActualizacion(nombre, telefono, nuevaCuentaActiva, nuevaContrasena);
        }
    }

    private void ejecutarActualizacion(String nombre, String telefono, Boolean cuentaActiva, String nuevaContrasena) {
        btnGuardar.setEnabled(false);
        String actorId = sessionManager != null && sessionManager.getUsuarioActual() != null
                ? sessionManager.getUsuarioActual().getId() : null;

        usuarioRepository.actualizarPersonal(actorId, usuarioOriginal.getId(), nombre, telefono,
                cuentaActiva, nuevaContrasena, new Callback<Usuario>() {
                    @Override
                    public void onSuccess(Usuario result) {
                        if (!isAdded()) return;
                        btnGuardar.setEnabled(true);
                        ocultarTeclado();
                        Toast.makeText(requireContext(), R.string.msg_personal_actualizado, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (!isAdded()) return;
                        btnGuardar.setEnabled(true);
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.dialog_editar_personal_titulo)
                                .setMessage(mensaje)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });
    }
}
