package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.ClienteAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ClientesFragment extends Fragment {

    private static final String KEY_QUERY = "key_consulta_busqueda";

    private TextInputEditText etBuscarCliente;
    private RecyclerView rvClientes;
    private LinearLayout layoutEmptyClientes;
    private TextView tvEmptyClientes;
    private ClienteAdapter adapter;

    private PedidoRepository pedidoRepository;
    private PagoRepository pagoRepository;
    private UsuarioRepository usuarioRepository;

    private List<Pedido> todosLosPedidos = new ArrayList<>();
    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Usuario> todosLosUsuarios = new ArrayList<>();

    private List<ClienteUtils.ResumenCliente> todosLosClientes = new ArrayList<>();
    private String consultaActual = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clientes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = appContainer.getPedidoRepository();
        pagoRepository = appContainer.getPagoRepository();
        usuarioRepository = appContainer.getUsuarioRepository();

        etBuscarCliente = view.findViewById(R.id.etBuscarCliente);
        rvClientes = view.findViewById(R.id.rvClientes);
        layoutEmptyClientes = view.findViewById(R.id.layoutEmptyClientes);
        tvEmptyClientes = view.findViewById(R.id.tvEmptyClientes);

        rvClientes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ClienteAdapter(cliente -> {
            if (!isAdded()) return;
            Bundle args = new Bundle();
            args.putString("clienteKey", cliente.getClave());
            NavHostFragment.findNavController(this).navigate(R.id.action_clientes_to_cliente, args);
        });
        rvClientes.setAdapter(adapter);

        if (savedInstanceState != null) {
            consultaActual = savedInstanceState.getString(KEY_QUERY, "");
        }

        etBuscarCliente.setText(consultaActual);
        etBuscarCliente.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                consultaActual = (s != null) ? s.toString() : "";
                filtrarYActualizarUI();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        observarDatos();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, consultaActual);
    }

    private void observarDatos() {
        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
                todosLosPedidos = pedidos != null ? new ArrayList<>(pedidos) : new ArrayList<>();
                recalcularClientes();
            });
        }

        if (pagoRepository != null) {
            pagoRepository.getTodosLosPagos().observe(getViewLifecycleOwner(), pagos -> {
                todosLosPagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
                recalcularClientes();
            });
        }

        if (usuarioRepository != null) {
            usuarioRepository.getUsuarios().observe(getViewLifecycleOwner(), usuarios -> {
                todosLosUsuarios = usuarios != null ? new ArrayList<>(usuarios) : new ArrayList<>();
                recalcularClientes();
            });
        }
    }

    private void recalcularClientes() {
        todosLosClientes = ClienteUtils.resumenes(todosLosPedidos, todosLosPagos, todosLosUsuarios);
        filtrarYActualizarUI();
    }

    private void filtrarYActualizarUI() {
        if (!isAdded()) return;

        List<ClienteUtils.ResumenCliente> filtrados = ClienteUtils.buscar(todosLosClientes, consultaActual);
        adapter.setClientes(filtrados);

        if (filtrados.isEmpty()) {
            rvClientes.setVisibility(View.GONE);
            layoutEmptyClientes.setVisibility(View.VISIBLE);
            if (todosLosClientes.isEmpty()) {
                tvEmptyClientes.setText(R.string.empty_clientes);
            } else {
                tvEmptyClientes.setText(R.string.empty_clientes_busqueda);
            }
        } else {
            rvClientes.setVisibility(View.VISIBLE);
            layoutEmptyClientes.setVisibility(View.GONE);
        }
    }
}
