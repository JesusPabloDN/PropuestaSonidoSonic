package com.example.propuestasonidosonic.ui.comun;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.PerfilUtils;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.PasswordValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PerfilFragment extends Fragment {

    private static final String KEY_NOMBRE_EDITADO = "key_nombre_editado";
    private static final String KEY_TELEFONO_EDITADO = "key_telefono_editado";

    private SessionManager sessionManager;
    private UsuarioRepository usuarioRepository;
    private Usuario usuarioActual;

    private TextView tvPerfilAvatar;
    private TextView tvPerfilCuentaCreada;
    private TextInputLayout tilPerfilNombre;
    private TextInputEditText etPerfilNombre;
    private TextInputLayout tilPerfilTelefono;
    private TextInputEditText etPerfilTelefono;
    private TextInputEditText etPerfilCorreo;
    private TextInputEditText etPerfilRol;
    private MaterialButton btnGuardarPerfil;

    private TextInputLayout tilPerfilPasswordActual;
    private TextInputEditText etPerfilPasswordActual;
    private TextInputLayout tilPerfilPasswordNueva;
    private TextInputEditText etPerfilPasswordNueva;
    private TextInputLayout tilPerfilPasswordConfirmar;
    private TextInputEditText etPerfilPasswordConfirmar;
    private MaterialButton btnCambiarContrasena;

    private String nombreOriginal = "";
    private String telefonoOriginal = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager();
        usuarioRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getUsuarioRepository();

        usuarioActual = sessionManager.getUsuarioActual();

        tvPerfilAvatar = view.findViewById(R.id.tvPerfilAvatar);
        tvPerfilCuentaCreada = view.findViewById(R.id.tvPerfilCuentaCreada);
        tilPerfilNombre = view.findViewById(R.id.tilPerfilNombre);
        etPerfilNombre = view.findViewById(R.id.etPerfilNombre);
        tilPerfilTelefono = view.findViewById(R.id.tilPerfilTelefono);
        etPerfilTelefono = view.findViewById(R.id.etPerfilTelefono);
        etPerfilCorreo = view.findViewById(R.id.etPerfilCorreo);
        etPerfilRol = view.findViewById(R.id.etPerfilRol);
        btnGuardarPerfil = view.findViewById(R.id.btnGuardarPerfil);

        tilPerfilPasswordActual = view.findViewById(R.id.tilPerfilPasswordActual);
        etPerfilPasswordActual = view.findViewById(R.id.etPerfilPasswordActual);
        tilPerfilPasswordNueva = view.findViewById(R.id.tilPerfilPasswordNueva);
        etPerfilPasswordNueva = view.findViewById(R.id.etPerfilPasswordNueva);
        tilPerfilPasswordConfirmar = view.findViewById(R.id.tilPerfilPasswordConfirmar);
        etPerfilPasswordConfirmar = view.findViewById(R.id.etPerfilPasswordConfirmar);
        btnCambiarContrasena = view.findViewById(R.id.btnCambiarContrasena);

        poblarDatosUsuario();

        if (savedInstanceState != null) {
            String savedNombre = savedInstanceState.getString(KEY_NOMBRE_EDITADO);
            if (savedNombre != null) {
                etPerfilNombre.setText(savedNombre);
            }
            String savedTelefono = savedInstanceState.getString(KEY_TELEFONO_EDITADO);
            if (savedTelefono != null) {
                etPerfilTelefono.setText(savedTelefono);
            }
        }

        configurarValidacionPerfil();
        configurarCambioContrasena();
    }

    private void poblarDatosUsuario() {
        if (usuarioActual == null) return;

        nombreOriginal = usuarioActual.getNombre() != null ? usuarioActual.getNombre() : "";
        telefonoOriginal = usuarioActual.getTelefono() != null ? usuarioActual.getTelefono() : "";

        tvPerfilAvatar.setText(PerfilUtils.iniciales(nombreOriginal));
        etPerfilNombre.setText(nombreOriginal);
        etPerfilTelefono.setText(telefonoOriginal);
        etPerfilCorreo.setText(usuarioActual.getCorreo());

        String rolTexto;
        if (Usuario.ROL_SOCIO.equals(usuarioActual.getRol())) {
            rolTexto = getString(R.string.drawer_header_rol_socio);
        } else if (Usuario.ROL_CLIENTE.equals(usuarioActual.getRol())) {
            rolTexto = getString(R.string.drawer_header_rol_cliente);
        } else if (Usuario.ROL_ASISTENTE.equals(usuarioActual.getRol())) {
            rolTexto = getString(R.string.drawer_header_rol_asistente);
        } else {
            rolTexto = usuarioActual.getRol();
        }
        etPerfilRol.setText(rolTexto);

        if (usuarioActual.getFechaAlta() > 0) {
            tvPerfilCuentaCreada.setText(getString(R.string.label_cuenta_creada,
                    FormatUtils.formatoFecha(usuarioActual.getFechaAlta())));
        } else {
            tvPerfilCuentaCreada.setVisibility(View.GONE);
        }
    }

    private void configurarValidacionPerfil() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPerfilNombre.setError(null);
                tilPerfilTelefono.setError(null);
                validarCambiosPerfil();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPerfilNombre.addTextChangedListener(watcher);
        etPerfilTelefono.addTextChangedListener(watcher);

        btnGuardarPerfil.setOnClickListener(v -> guardarPerfil());
    }

    private void validarCambiosPerfil() {
        String nuevoNombre = etPerfilNombre.getText() != null ? etPerfilNombre.getText().toString().trim() : "";
        String nuevoTelefono = etPerfilTelefono.getText() != null ? etPerfilTelefono.getText().toString().trim() : "";

        boolean cambio = !nuevoNombre.equals(nombreOriginal.trim()) || !nuevoTelefono.equals(telefonoOriginal.trim());
        boolean valido = !nuevoNombre.isEmpty() && nuevoNombre.length() <= 80 && nuevoTelefono.replaceAll("\\D", "").length() >= 10;

        btnGuardarPerfil.setEnabled(cambio && valido);
    }

    private void guardarPerfil() {
        if (usuarioActual == null) return;

        String nuevoNombre = etPerfilNombre.getText() != null ? etPerfilNombre.getText().toString().trim() : "";
        String nuevoTelefono = etPerfilTelefono.getText() != null ? etPerfilTelefono.getText().toString().trim() : "";

        boolean error = false;
        if (nuevoNombre.isEmpty()) {
            tilPerfilNombre.setError(getString(R.string.error_campo_obligatorio));
            error = true;
        } else if (nuevoNombre.length() > 80) {
            tilPerfilNombre.setError(getString(R.string.error_nombre_personal_invalido));
            error = true;
        }

        String digitos = nuevoTelefono.replaceAll("\\D", "");
        if (digitos.length() < 10) {
            tilPerfilTelefono.setError(getString(R.string.error_telefono_personal_invalido));
            error = true;
        }

        if (error) return;

        btnGuardarPerfil.setEnabled(false);
        usuarioRepository.actualizarPerfil(usuarioActual.getId(), nuevoNombre, nuevoTelefono, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                if (!isAdded()) return;
                usuarioActual = result;
                nombreOriginal = result.getNombre();
                telefonoOriginal = result.getTelefono();
                tvPerfilAvatar.setText(PerfilUtils.iniciales(nombreOriginal));
                btnGuardarPerfil.setEnabled(false);
                Snackbar.make(requireView(), R.string.snackbar_perfil_actualizado, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensajeError) {
                if (!isAdded()) return;
                btnGuardarPerfil.setEnabled(true);
                Snackbar.make(requireView(), mensajeError != null ? mensajeError : "Error al actualizar perfil", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarCambioContrasena() {
        TextWatcher passWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPerfilPasswordActual.setError(null);
                tilPerfilPasswordNueva.setError(null);
                tilPerfilPasswordConfirmar.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPerfilPasswordActual.addTextChangedListener(passWatcher);
        etPerfilPasswordNueva.addTextChangedListener(passWatcher);
        etPerfilPasswordConfirmar.addTextChangedListener(passWatcher);

        btnCambiarContrasena.setOnClickListener(v -> cambiarContrasena());
    }

    private void cambiarContrasena() {
        if (usuarioActual == null) return;

        String passActual = etPerfilPasswordActual.getText() != null ? etPerfilPasswordActual.getText().toString() : "";
        String passNueva = etPerfilPasswordNueva.getText() != null ? etPerfilPasswordNueva.getText().toString() : "";
        String passConfirmar = etPerfilPasswordConfirmar.getText() != null ? etPerfilPasswordConfirmar.getText().toString() : "";

        boolean error = false;
        if (passActual.isEmpty()) {
            tilPerfilPasswordActual.setError(getString(R.string.error_campo_obligatorio));
            error = true;
        }

        if (passNueva.isEmpty()) {
            tilPerfilPasswordNueva.setError(getString(R.string.error_campo_obligatorio));
            error = true;
        } else if (!PasswordValidator.isValid(passNueva)) {
            tilPerfilPasswordNueva.setError(getString(R.string.error_contrasena_debil));
            error = true;
        } else if (passNueva.equals(passActual)) {
            tilPerfilPasswordNueva.setError(getString(R.string.error_contrasena_misma));
            error = true;
        }

        if (passConfirmar.isEmpty()) {
            tilPerfilPasswordConfirmar.setError(getString(R.string.error_campo_obligatorio));
            error = true;
        } else if (!passNueva.equals(passConfirmar)) {
            tilPerfilPasswordConfirmar.setError(getString(R.string.error_contrasenas_no_coinciden));
            error = true;
        }

        if (error) return;

        btnCambiarContrasena.setEnabled(false);
        usuarioRepository.cambiarContrasena(usuarioActual.getId(), passActual, passNueva, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded()) return;
                btnCambiarContrasena.setEnabled(true);
                etPerfilPasswordActual.setText("");
                etPerfilPasswordNueva.setText("");
                etPerfilPasswordConfirmar.setText("");
                Snackbar.make(requireView(), R.string.snackbar_contrasena_actualizada, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensajeError) {
                if (!isAdded()) return;
                btnCambiarContrasena.setEnabled(true);
                if (mensajeError != null && mensajeError.contains("actual")) {
                    tilPerfilPasswordActual.setError(mensajeError);
                } else if (mensajeError != null && mensajeError.contains("seguridad")) {
                    tilPerfilPasswordNueva.setError(mensajeError);
                } else {
                    Snackbar.make(requireView(), mensajeError != null ? mensajeError : "Error al cambiar contraseña", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etPerfilNombre != null && etPerfilNombre.getText() != null) {
            outState.putString(KEY_NOMBRE_EDITADO, etPerfilNombre.getText().toString());
        }
        if (etPerfilTelefono != null && etPerfilTelefono.getText() != null) {
            outState.putString(KEY_TELEFONO_EDITADO, etPerfilTelefono.getText().toString());
        }
    }
}
