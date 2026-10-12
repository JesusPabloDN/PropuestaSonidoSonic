package com.example.propuestasonidosonic.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.PasswordValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterFragment extends Fragment {

    private TextInputLayout tilNombre;
    private TextInputEditText etNombre;
    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private TextInputLayout tilTelefono;
    private TextInputEditText etTelefono;
    private TextInputLayout tilContrasena;
    private TextInputEditText etContrasena;
    private TextInputLayout tilConfirmarContrasena;
    private TextInputEditText etConfirmarContrasena;
    private MaterialButton btnRegistrarse;
    private TextView tvIrALogin;

    private UsuarioRepository usuarioRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        usuarioRepository = appContainer.getUsuarioRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilNombre = view.findViewById(R.id.tilNombre);
        etNombre = view.findViewById(R.id.etNombre);
        tilCorreo = view.findViewById(R.id.tilCorreo);
        etCorreo = view.findViewById(R.id.etCorreo);
        tilTelefono = view.findViewById(R.id.tilTelefono);
        etTelefono = view.findViewById(R.id.etTelefono);
        tilContrasena = view.findViewById(R.id.tilContrasena);
        etContrasena = view.findViewById(R.id.etContrasena);
        tilConfirmarContrasena = view.findViewById(R.id.tilConfirmarContrasena);
        etConfirmarContrasena = view.findViewById(R.id.etConfirmarContrasena);
        btnRegistrarse = view.findViewById(R.id.btnRegistrarse);
        tvIrALogin = view.findViewById(R.id.tvIrALogin);

        configurarLimpiezaErrores();

        btnRegistrarse.setOnClickListener(v -> intentarRegistro());
        tvIrALogin.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.popBackStack();
        });
    }

    private void configurarLimpiezaErrores() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilNombre.setError(null);
                tilCorreo.setError(null);
                tilTelefono.setError(null);
                tilContrasena.setError(null);
                tilConfirmarContrasena.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etNombre.addTextChangedListener(watcher);
        etCorreo.addTextChangedListener(watcher);
        etTelefono.addTextChangedListener(watcher);
        etContrasena.addTextChangedListener(watcher);
        etConfirmarContrasena.addTextChangedListener(watcher);
    }

    private void intentarRegistro() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
        String contrasena = etContrasena.getText() != null ? etContrasena.getText().toString().trim() : "";
        String confirmarContrasena = etConfirmarContrasena.getText() != null ? etConfirmarContrasena.getText().toString().trim() : "";

        boolean valido = true;

        if (TextUtils.isEmpty(nombre)) {
            tilNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }

        if (TextUtils.isEmpty(correo)) {
            tilCorreo.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.setError(getString(R.string.error_correo_invalido));
            valido = false;
        }

        if (TextUtils.isEmpty(telefono)) {
            tilTelefono.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (telefono.length() < 7) {
            tilTelefono.setError(getString(R.string.error_telefono_invalido));
            valido = false;
        }

        if (TextUtils.isEmpty(contrasena)) {
            tilContrasena.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (!PasswordValidator.isValid(contrasena)) {
            tilContrasena.setError(getString(R.string.error_contrasena_invalida));
            valido = false;
        }

        if (TextUtils.isEmpty(confirmarContrasena)) {
            tilConfirmarContrasena.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (!contrasena.equals(confirmarContrasena)) {
            tilConfirmarContrasena.setError(getString(R.string.error_contrasenas_no_coinciden));
            valido = false;
        }

        if (!valido) {
            return;
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setTelefono(telefono);
        nuevoUsuario.setRol(Usuario.ROL_CLIENTE);
        nuevoUsuario.setContrasena(contrasena);
        nuevoUsuario.setCuentaActiva(true);
        nuevoUsuario.setFechaAlta(System.currentTimeMillis());

        usuarioRepository.registrarUsuario(nuevoUsuario, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.registro_exitoso, Toast.LENGTH_LONG).show();
                }
                if (getView() != null) {
                    Navigation.findNavController(getView()).popBackStack();
                }
            }

            @Override
            public void onError(String mensaje) {
                tilCorreo.setError(getString(R.string.error_correo_ya_registrado));
            }
        });
    }
}
