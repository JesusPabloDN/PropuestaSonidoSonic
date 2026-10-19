package com.example.propuestasonidosonic.ui.cliente;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.PedidoClienteAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.data.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

public class ClientePedidosFragment extends Fragment implements PedidoClienteAdapter.OnPedidoClienteClickListener {

    private static final String KEY_MOSTRAR_ACTIVOS = "mostrar_activos";

    private MaterialButtonToggleGroup toggleGroupPedidos;
    private RecyclerView rvPedidosCliente;
    private View layoutEstadoVacio;
    private TextView tvVacioMensaje;
    private MaterialButton btnVerCatalogo;

    private PedidoClienteAdapter adapter;
    private PedidoRepository pedidoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private boolean mostrarActivos = true;
    private List<Pedido> todosLosPedidos = new ArrayList<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                actualizarLista();
                refreshHandler.postDelayed(this, 30_000L);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mostrarActivos = savedInstanceState.getBoolean(KEY_MOSTRAR_ACTIVOS, true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cliente_pedidos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer container = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = container.getPedidoRepository();
        sessionManager = container.getSessionManager();
        reloj = container.getReloj();

        toggleGroupPedidos = view.findViewById(R.id.toggleGroupPedidos);
        rvPedidosCliente = view.findViewById(R.id.rvPedidosCliente);
        layoutEstadoVacio = view.findViewById(R.id.layoutEstadoVacio);
        tvVacioMensaje = view.findViewById(R.id.tvVacioMensaje);
        btnVerCatalogo = view.findViewById(R.id.btnVerCatalogo);

        long ahora = reloj.ahora();
        adapter = new PedidoClienteAdapter(this, ahora);
        rvPedidosCliente.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPedidosCliente.setAdapter(adapter);

        toggleGroupPedidos.check(mostrarActivos ? R.id.btnTabActivos : R.id.btnTabHistorial);
        toggleGroupPedidos.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                mostrarActivos = (checkedId == R.id.btnTabActivos);
                actualizarLista();
            }
        });

        btnVerCatalogo.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.clienteHomeFragment, false)
                    .build();
            Navigation.findNavController(v).navigate(R.id.clienteHomeFragment, null, navOptions);
        });

        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            todosLosPedidos = pedidos != null ? pedidos : new ArrayList<>();
            actualizarLista();
        });

        sessionManager.getUsuarioActualLiveData().observe(getViewLifecycleOwner(), usuario -> {
            actualizarLista();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_MOSTRAR_ACTIVOS, mostrarActivos);
    }

    private void actualizarLista() {
        if (!isAdded()) return;

        Usuario usuarioActual = sessionManager.getUsuarioActual();
        String clienteId = usuarioActual != null ? usuarioActual.getId() : "";
        List<Pedido> pedidosCliente = PedidosClienteUtils.delCliente(todosLosPedidos, clienteId);

        long ahora = reloj.ahora();
        adapter.setAhora(ahora);

        List<Pedido> pedidosFiltrados;
        if (mostrarActivos) {
            pedidosFiltrados = PedidosClienteUtils.activos(pedidosCliente, ahora);
        } else {
            pedidosFiltrados = PedidosClienteUtils.historial(pedidosCliente, ahora);
        }

        adapter.submitList(pedidosFiltrados);

        if (pedidosFiltrados.isEmpty()) {
            layoutEstadoVacio.setVisibility(View.VISIBLE);
            rvPedidosCliente.setVisibility(View.GONE);
            tvVacioMensaje.setText(mostrarActivos
                    ? R.string.empty_pedidos_cliente_activos
                    : R.string.empty_pedidos_cliente_historial);
        } else {
            layoutEstadoVacio.setVisibility(View.GONE);
            rvPedidosCliente.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPedidoClick(Pedido pedido) {
        if (pedido == null || !isAdded()) return;
        Bundle args = new Bundle();
        args.putString("pedidoId", pedido.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_clientePedidos_to_detalle, args);
    }

    @Override
    public void onPagarAnticipoClick(Pedido pedido) {
        if (pedido == null || !isAdded()) return;
        Bundle args = new Bundle();
        args.putString("pedidoId", pedido.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_clientePedidos_to_pagoAnticipo, args);
    }
}
