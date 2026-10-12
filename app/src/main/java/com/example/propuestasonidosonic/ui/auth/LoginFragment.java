package com.example.propuestasonidosonic.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends Fragment {

    private static final String KEY_ROL_SELECCIONADO = "key_rol_seleccionado";
    private static final int TAB_INDEX_SOCIO = 0;
    private static final int TAB_INDEX_CLIENTE = 1;
    private static final int TAB_INDEX_ASISTENTE = 2;

    private TabLayout tabLayoutRol;
    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private TextInputLayout tilContrasena;
    private TextInputEditText etContrasena;
    private MaterialButton btnIniciarSesion;
    private TextView tvIrARegistro;

    private UsuarioRepository usuarioRepository;
    private SessionManager sessionManager;
    private int rolSeleccionadoIndex = TAB_INDEX_CLIENTE; // Cliente por defecto

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        usuarioRepository = appContainer.getUsuarioRepository();
        sessionManager = appContainer.getSessionManager();

        if (savedInstanceState != null) {
            rolSeleccionadoIndex = savedInstanceState.getInt(KEY_ROL_SELECCIONADO, TAB_INDEX_CLIENTE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayoutRol = view.findViewById(R.id.tabLayoutRol);
        tilCorreo = view.findViewById(R.id.tilCorreo);
        etCorreo = view.findViewById(R.id.etCorreo);
        tilContrasena = view.findViewById(R.id.tilContrasena);
        etContrasena = view.findViewById(R.id.etContrasena);
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion);
        tvIrARegistro = view.findViewById(R.id.tvIrARegistro);

        configurarTabs();
        configurarLimpiezaErrores();

        // Prellenar credenciales fijas de Socio en la primera apertura si aún no hay texto
        if (savedInstanceState == null && (etCorreo.getText() == null || etCorreo.getText().toString().isEmpty())) {
            etCorreo.setText(Config.CUENTA_SOCIO_CORREO);
            etContrasena.setText(Config.CUENTA_SOCIO_CONTRASENA);
        }

        btnIniciarSesion.setOnClickListener(v -> intentarLogin());

        // Acción Done del teclado en el campo de contraseña
        etContrasena.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin();
                return true;
            }
            return false;
        });

        tvIrARegistro.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_login_to_register);
        });
    }

    private void configurarTabs() {
        tabLayoutRol.removeAllTabs();
        tabLayoutRol.addTab(tabLayoutRol.newTab().setText(R.string.tab_socio));
        tabLayoutRol.addTab(tabLayoutRol.newTab().setText(R.string.tab_cliente));
        tabLayoutRol.addTab(tabLayoutRol.newTab().setText(R.string.tab_asistente));

        // Seleccionar la pestaña activa (por defecto Cliente = 1)
        TabLayout.Tab tabActual = tabLayoutRol.getTabAt(rolSeleccionadoIndex);
        if (tabActual != null) {
            tabActual.select();
        }
        actualizarVisibilidadRegistro(rolSeleccionadoIndex);

        tabLayoutRol.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                rolSeleccionadoIndex = tab.getPosition();
                actualizarVisibilidadRegistro(rolSeleccionadoIndex);
                // Limpiar errores sin borrar textos
                tilCorreo.setError(null);
                tilContrasena.setError(null);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void actualizarVisibilidadRegistro(int position) {
        if (position == TAB_INDEX_CLIENTE) {
            tvIrARegistro.setVisibility(View.VISIBLE);
        } else {
            tvIrARegistro.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ROL_SELECCIONADO, rolSeleccionadoIndex);
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

    private String obtenerRolEsperadoSegunTab() {
        switch (rolSeleccionadoIndex) {
            case TAB_INDEX_SOCIO:
                return Usuario.ROL_SOCIO;
            case TAB_INDEX_ASISTENTE:
                return Usuario.ROL_ASISTENTE;
            case TAB_INDEX_CLIENTE:
            default:
                return Usuario.ROL_CLIENTE;
        }
    }

    private void intentarLogin() {
        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String contrasena = etContrasena.getText() != null ? etContrasena.getText().toString().trim() : "";
        String rolEsperado = obtenerRolEsperadoSegunTab();

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
                        tilCorreo.setError(getString(R.string.error_rol_no_coincide_asistente));
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
                        navController.navigate(R.id.action_login_to_asistente_graph);
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
