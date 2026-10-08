package com.example.propuestasonidosonic;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LoginTabFragment extends Fragment {

    private EditText etUsuario;
    private EditText etContrasena;
    private Button btnIniciarSesion;

    public LoginTabFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        etUsuario = view.findViewById(R.id.etUsuario);
        etContrasena = view.findViewById(R.id.etContrasena);
        btnIniciarSesion = view.findViewById(R.id.btnIniciarSesion);

        btnIniciarSesion.setOnClickListener(v -> intentarIniciarSesion());

        return view;
    }

    private void intentarIniciarSesion() {
        String usuario = etUsuario.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        boolean valido = true;

        if (TextUtils.isEmpty(usuario)) {
            etUsuario.setError(getString(R.string.error_campo_vacio));
            valido = false;
        }

        if (TextUtils.isEmpty(contrasena)) {
            etContrasena.setError(getString(R.string.error_campo_vacio));
            valido = false;
        }

        if (valido) {
            // Navegar a HomeActivity
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else {
            Toast.makeText(getContext(), "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
