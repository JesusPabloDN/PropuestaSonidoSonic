package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.PedidoAdapter;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.model.Pedido;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PedidosFragment extends Fragment implements PedidoAdapter.OnPedidoClickListener {

    private static final String KEY_SELECTED_TAB = "key_selected_tab_pedidos";
    private static final long INTERVALO_REFRESCO_MS = 30_000L;

    private TabLayout tabsPedidos;
    private RecyclerView rvPedidos;
    private LinearLayout layoutEmptyPedidos;
    private TextView tvEmptyMensaje;
    private FloatingActionButton fabNuevoPedido;

    private PedidoAdapter adapter;
    private PedidoRepository pedidoRepository;

    private int tabSeleccionada = 0;
    private final List<Pedido> todosLosPedidos = new ArrayList<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && pedidoRepository != null) {
                pedidoRepository.liberarApartadosVencidos();
                aplicarFiltroYActualizar();
                refreshHandler.postDelayed(this, INTERVALO_REFRESCO_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pedidos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();

        if (savedInstanceState != null) {
            tabSeleccionada = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
        }

        vincularVistas(view);
        configurarTabs();
        configurarRecyclerView();
        configurarFab();
        observarPedidos();
    }

    private void vincularVistas(View view) {
        tabsPedidos = view.findViewById(R.id.tabsPedidos);
        rvPedidos = view.findViewById(R.id.rvPedidos);
        layoutEmptyPedidos = view.findViewById(R.id.layoutEmptyPedidos);
        tvEmptyMensaje = view.findViewById(R.id.tvEmptyMensaje);
        fabNuevoPedido = view.findViewById(R.id.fabNuevoPedido);
    }

    private void configurarTabs() {
        tabsPedidos.removeAllTabs();
        tabsPedidos.addTab(tabsPedidos.newTab().setText(R.string.tab_pedidos_por_pagar));
        tabsPedidos.addTab(tabsPedidos.newTab().setText(R.string.tab_pedidos_confirmados));
        tabsPedidos.addTab(tabsPedidos.newTab().setText(R.string.tab_pedidos_historial));

        TabLayout.Tab tab = tabsPedidos.getTabAt(tabSeleccionada);
        if (tab != null) {
            tab.select();
        }

        tabsPedidos.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabSeleccionada = tab.getPosition();
                aplicarFiltroYActualizar();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void configurarRecyclerView() {
        adapter = new PedidoAdapter(this);
        rvPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPedidos.setAdapter(adapter);
    }

    private void configurarFab() {
        fabNuevoPedido.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_pedidos_to_nuevoPedido);
        });
    }

    private void observarPedidos() {
        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            todosLosPedidos.clear();
            if (pedidos != null) {
                todosLosPedidos.addAll(pedidos);
            }
            aplicarFiltroYActualizar();
        });
    }

    private void aplicarFiltroYActualizar() {
        if (!isAdded()) return;

        List<Pedido> filtrados = new ArrayList<>();

        if (tabSeleccionada == PedidoAdapter.TAB_POR_PAGAR) {
            tvEmptyMensaje.setText(R.string.empty_pedidos_por_pagar);
            for (Pedido p : todosLosPedidos) {
                if (Pedido.ESTADO_APARTADO.equals(p.getEstado())) {
                    filtrados.add(p);
                }
            }
            // Ordenar por vencimiento (el que vence primero arriba)
            Collections.sort(filtrados, new Comparator<Pedido>() {
                @Override
                public int compare(Pedido o1, Pedido o2) {
                    return Long.compare(o1.getVencimientoApartadoMillis(), o2.getVencimientoApartadoMillis());
                }
            });
        } else if (tabSeleccionada == PedidoAdapter.TAB_CONFIRMADOS) {
            tvEmptyMensaje.setText(R.string.empty_pedidos_confirmados);
            for (Pedido p : todosLosPedidos) {
                String st = p.getEstado();
                if (Pedido.ESTADO_CONFIRMADO.equals(st)
                        || Pedido.ESTADO_PAGADO.equals(st)
                        || Pedido.ESTADO_EN_REVISION.equals(st)) {
                    filtrados.add(p);
                }
            }
            // Ordenar por fecha del evento (el más próximo arriba)
            Collections.sort(filtrados, new Comparator<Pedido>() {
                @Override
                public int compare(Pedido o1, Pedido o2) {
                    return Long.compare(o1.getInicioMillis(), o2.getInicioMillis());
                }
            });
        } else {
            tvEmptyMensaje.setText(R.string.empty_pedidos_historial);
            for (Pedido p : todosLosPedidos) {
                String st = p.getEstado();
                if (Pedido.ESTADO_COMPLETADO.equals(st)
                        || Pedido.ESTADO_CANCELADO.equals(st)
                        || Pedido.ESTADO_RECHAZADO.equals(st)
                        || Pedido.ESTADO_VENCIDO.equals(st)) {
                    filtrados.add(p);
                }
            }
            // Ordenar por fecha de actualización (el más reciente arriba)
            Collections.sort(filtrados, new Comparator<Pedido>() {
                @Override
                public int compare(Pedido o1, Pedido o2) {
                    return Long.compare(o2.getFechaActualizacion(), o1.getFechaActualizacion());
                }
            });
        }

        if (filtrados.isEmpty()) {
            rvPedidos.setVisibility(View.GONE);
            layoutEmptyPedidos.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyPedidos.setVisibility(View.GONE);
            rvPedidos.setVisibility(View.VISIBLE);
        }

        adapter.setDatos(filtrados, tabSeleccionada);
    }

    @Override
    public void onPedidoClick(Pedido pedido) {
        if (pedido == null || !isAdded()) return;
        Bundle args = new Bundle();
        args.putString("pedidoId", pedido.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_pedidos_to_detalle, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pedidoRepository != null) {
            pedidoRepository.liberarApartadosVencidos();
            aplicarFiltroYActualizar();
        }
        refreshHandler.postDelayed(refreshRunnable, INTERVALO_REFRESCO_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, tabSeleccionada);
    }
}
