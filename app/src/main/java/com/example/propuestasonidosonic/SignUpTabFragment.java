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

public class SignUpTabFragment extends Fragment {

    private EditText etNuevoUsuario;
    private EditText etContrasenaRegistro;
    private EditText etConfirmarContrasena;
    private Button btnRegistrarse;

    public SignUpTabFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        etNuevoUsuario = view.findViewById(R.id.etNuevoUsuario);
        etContrasenaRegistro = view.findViewById(R.id.etContrasenaRegistro);
        etConfirmarContrasena = view.findViewById(R.id.etConfirmarContrasena);
        btnRegistrarse = view.findViewById(R.id.btnRegistrarse);

        btnRegistrarse.setOnClickListener(v -> intentarRegistrarse());

        return view;
    }

    private void intentarRegistrarse() {
        String nuevoUsuario = etNuevoUsuario.getText().toString().trim();
        String contrasena = etContrasenaRegistro.getText().toString().trim();
        String confirmarContrasena = etConfirmarContrasena.getText().toString().trim();

        boolean valido = true;

        if (TextUtils.isEmpty(nuevoUsuario)) {
            etNuevoUsuario.setError(getString(R.string.error_campo_vacio));
            valido = false;
        }

        if (TextUtils.isEmpty(contrasena)) {
            etContrasenaRegistro.setError(getString(R.string.error_campo_vacio));
            valido = false;
        }

        if (TextUtils.isEmpty(confirmarContrasena)) {
            etConfirmarContrasena.setError(getString(R.string.error_campo_vacio));
            valido = false;
        }

        if (valido && !contrasena.equals(confirmarContrasena)) {
            etConfirmarContrasena.setError(getString(R.string.error_contrasenas_no_coinciden));
            Toast.makeText(getContext(), getString(R.string.error_contrasenas_no_coinciden),
                    Toast.LENGTH_SHORT).show();
            valido = false;
        }

        if (valido) {
            // Navegar a HomeActivity
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else if (TextUtils.isEmpty(nuevoUsuario) || TextUtils.isEmpty(contrasena)
                || TextUtils.isEmpty(confirmarContrasena)) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
