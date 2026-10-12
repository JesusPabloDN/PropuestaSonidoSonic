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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends Fragment {

    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private TextInputLayout tilContrasena;
    private TextInputEditText etContrasena;
    private MaterialButton btnLoginSocio;
    private MaterialButton btnLoginCliente;
    private MaterialButton btnLoginTecnico;
    private TextView tvIrARegistro;

    private UsuarioRepository usuarioRepository;
    private SessionManager sessionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        usuarioRepository = appContainer.getUsuarioRepository();
        sessionManager = appContainer.getSessionManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilCorreo = view.findViewById(R.id.tilCorreo);
        etCorreo = view.findViewById(R.id.etCorreo);
        tilContrasena = view.findViewById(R.id.tilContrasena);
        etContrasena = view.findViewById(R.id.etContrasena);
        btnLoginSocio = view.findViewById(R.id.btnLoginSocio);
        btnLoginCliente = view.findViewById(R.id.btnLoginCliente);
        btnLoginTecnico = view.findViewById(R.id.btnLoginTecnico);
        tvIrARegistro = view.findViewById(R.id.tvIrARegistro);

        // Prellenar credenciales fijas de Socio para facilitar pruebas inmediatas
        if (savedInstanceState == null) {
            etCorreo.setText(Config.CUENTA_SOCIO_CORREO);
            etContrasena.setText(Config.CUENTA_SOCIO_CONTRASENA);
        }

        configurarLimpiezaErrores();

        btnLoginSocio.setOnClickListener(v -> intentarLogin(Usuario.ROL_SOCIO));
        btnLoginCliente.setOnClickListener(v -> intentarLogin(Usuario.ROL_CLIENTE));
        btnLoginTecnico.setOnClickListener(v -> intentarLogin(Usuario.ROL_TECNICO));

        tvIrARegistro.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_login_to_register);
        });
    }

    private void configurarLimpiezaErrores() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilCorreo.setError(null);
                tilContrasena.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etCorreo.addTextChangedListener(watcher);
        etContrasena.addTextChangedListener(watcher);
    }

    private void intentarLogin(String rolEsperado) {
        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String contrasena = etContrasena.getText() != null ? etContrasena.getText().toString().trim() : "";

        boolean valido = true;
        if (TextUtils.isEmpty(correo)) {
            tilCorreo.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.setError(getString(R.string.error_correo_invalido));
            valido = false;
        }

        if (TextUtils.isEmpty(contrasena)) {
            tilContrasena.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }

        if (!valido) {
            return;
        }

        usuarioRepository.autenticar(correo, contrasena, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                if (!usuario.isCuentaActiva()) {
                    tilCorreo.setError(getString(R.string.error_cuenta_inactiva));
                    return;
                }

                if (!rolEsperado.equals(usuario.getRol())) {
                    if (Usuario.ROL_SOCIO.equals(rolEsperado)) {
                        tilCorreo.setError(getString(R.string.error_rol_no_coincide_socio));
                    } else if (Usuario.ROL_CLIENTE.equals(rolEsperado)) {
                        tilCorreo.setError(getString(R.string.error_rol_no_coincide_cliente));
                    } else {
                        tilCorreo.setError(getString(R.string.error_rol_no_coincide_tecnico));
                    }
                    return;
                }

                // Iniciar sesión y navegar al grafo correspondiente
                sessionManager.iniciarSesion(usuario);
                if (getView() != null) {
                    NavController navController = Navigation.findNavController(getView());
                    if (Usuario.ROL_SOCIO.equals(usuario.getRol())) {
                        navController.navigate(R.id.action_login_to_socio_graph);
                    } else if (Usuario.ROL_CLIENTE.equals(usuario.getRol())) {
                        navController.navigate(R.id.action_login_to_cliente_graph);
                    } else {
                        navController.navigate(R.id.action_login_to_tecnico_graph);
                    }
                }
            }

            @Override
            public void onError(String mensaje) {
                tilContrasena.setError(getString(R.string.error_credenciales_invalidas));
            }
        });
    }
}
