package com.example.propuestasonidosonic.ui.cliente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.PaqueteClienteAdapter;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.ReservaClienteUtils;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Usuario;

import java.util.List;

public class ClienteHomeFragment extends Fragment {

    private TextView tvSaludoCliente;
    private RecyclerView rvPaquetesCliente;
    private View layoutEmptyCatalogo;
    private PaqueteClienteAdapter adapter;

    private PaqueteRepository paqueteRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cliente_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paqueteRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPaqueteRepository();
        sessionManager = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager();

        tvSaludoCliente = view.findViewById(R.id.tvSaludoCliente);
        rvPaquetesCliente = view.findViewById(R.id.rvPaquetesCliente);
        layoutEmptyCatalogo = view.findViewById(R.id.layoutEmptyCatalogo);

        // Saludo personalizado
        Usuario usuario = sessionManager.getUsuarioActual();
        String nombre = (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "Cliente";
        tvSaludoCliente.setText(getString(R.string.saludo_cliente, nombre));

        adapter = new PaqueteClienteAdapter(this::abrirDetallePaquete);
        rvPaquetesCliente.setAdapter(adapter);

        cargarPaquetes();
    }

    private void cargarPaquetes() {
        if (paqueteRepository != null) {
            paqueteRepository.getPaquetes().observe(getViewLifecycleOwner(), paquetes -> {
                List<Paquete> visibles = ReservaClienteUtils.paquetesVisibles(paquetes);
                adapter.setPaquetes(visibles);

                if (visibles.isEmpty()) {
                    layoutEmptyCatalogo.setVisibility(View.VISIBLE);
                    rvPaquetesCliente.setVisibility(View.GONE);
                } else {
                    layoutEmptyCatalogo.setVisibility(View.GONE);
                    rvPaquetesCliente.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void abrirDetallePaquete(Paquete paquete) {
        if (paquete == null) return;
        Bundle args = new Bundle();
        args.putString("paqueteId", paquete.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_clienteHome_to_paqueteDetalle, args);
    }
}
