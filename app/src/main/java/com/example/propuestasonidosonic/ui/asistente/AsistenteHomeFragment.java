package com.example.propuestasonidosonic.ui.asistente;

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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.EventoAsistenteAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Reloj;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsistenteHomeFragment extends Fragment {

    private static final String KEY_TAB_SELECCIONADA = "key_tab_seleccionada";
    private static final int TAB_PROXIMOS = 0;
    private static final int TAB_PASADOS = 1;

    private int tabSeleccionada = TAB_PROXIMOS;

    private PedidoRepository pedidoRepository;
    private EventoRepository eventoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private TextView tvSaludoAsistente;
    private MaterialButtonToggleGroup btnGroupAsistenteEventos;
    private RecyclerView rvEventosAsistente;
    private LinearLayout layoutEmptyAsistente;
    private TextView tvEmptyMensaje;

    private EventoAsistenteAdapter adapter;

    private List<Pedido> todosLosPedidosCache = new ArrayList<>();
    private final Map<String, Evento> eventosCache = new HashMap<>();

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            actualizarLista();
            autoRefreshHandler.postDelayed(this, 60_000L);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = appContainer.getPedidoRepository();
        eventoRepository = appContainer.getEventoRepository();
        sessionManager = appContainer.getSessionManager();
        reloj = appContainer.getReloj();

        if (savedInstanceState != null) {
            tabSeleccionada = savedInstanceState.getInt(KEY_TAB_SELECCIONADA, TAB_PROXIMOS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asistente_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSaludoAsistente = view.findViewById(R.id.tvSaludoAsistente);
        btnGroupAsistenteEventos = view.findViewById(R.id.btnGroupAsistenteEventos);
        rvEventosAsistente = view.findViewById(R.id.rvEventosAsistente);
        layoutEmptyAsistente = view.findViewById(R.id.layoutEmptyAsistente);
        tvEmptyMensaje = view.findViewById(R.id.tvEmptyMensaje);

        Usuario usuario = sessionManager.getUsuarioActual();
        String nombre = (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "";
        tvSaludoAsistente.setText(getString(R.string.saludo_asistente_formato, nombre));

        adapter = new EventoAsistenteAdapter(pedido -> {
            Bundle args = new Bundle();
            args.putString("pedidoId", pedido.getId());
            NavController navController = NavHostFragment.findNavController(AsistenteHomeFragment.this);
            navController.navigate(R.id.action_asistenteHome_to_detalle, args);
        });

        rvEventosAsistente.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEventosAsistente.setAdapter(adapter);

        if (tabSeleccionada == TAB_PROXIMOS) {
            btnGroupAsistenteEventos.check(R.id.btnProximos);
        } else {
            btnGroupAsistenteEventos.check(R.id.btnPasados);
        }

        btnGroupAsistenteEventos.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnProximos) {
                    tabSeleccionada = TAB_PROXIMOS;
                } else if (checkedId == R.id.btnPasados) {
                    tabSeleccionada = TAB_PASADOS;
                }
                actualizarLista();
            }
        });

        sessionManager.getUsuarioActualLiveData().observe(getViewLifecycleOwner(), u -> {
            String n = (u != null && u.getNombre() != null) ? u.getNombre() : "";
            tvSaludoAsistente.setText(getString(R.string.saludo_asistente_formato, n));
            actualizarLista();
        });

        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            todosLosPedidosCache = pedidos != null ? pedidos : new ArrayList<>();
            actualizarLista();
        });

        eventoRepository.getEventos().observe(getViewLifecycleOwner(), eventos -> {
            eventosCache.clear();
            if (eventos != null) {
                for (Evento ev : eventos) {
                    if (ev != null && ev.getId() != null) {
                        eventosCache.put(ev.getId(), ev);
                    }
                }
            }
            actualizarLista();
        });
    }

    private void actualizarLista() {
        if (!isAdded()) return;

        Usuario usuario = sessionManager.getUsuarioActual();
        String asistenteId = usuario != null ? usuario.getId() : null;

        List<Pedido> misPedidos = AsistenteUtils.eventosDelAsistente(todosLosPedidosCache, asistenteId);
        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();

        List<Pedido> listaMostrar;
        boolean esPasados = (tabSeleccionada == TAB_PASADOS);

        if (esPasados) {
            listaMostrar = AsistenteUtils.pasados(misPedidos, ahora);
        } else {
            listaMostrar = AsistenteUtils.proximos(misPedidos, ahora);
        }

        adapter.setDatos(listaMostrar, eventosCache, ahora, esPasados);

        if (listaMostrar.isEmpty()) {
            rvEventosAsistente.setVisibility(View.GONE);
            layoutEmptyAsistente.setVisibility(View.VISIBLE);
            tvEmptyMensaje.setText(esPasados ? R.string.empty_asistente_pasados : R.string.empty_asistente_proximos);
        } else {
            rvEventosAsistente.setVisibility(View.VISIBLE);
            layoutEmptyAsistente.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 60_000L);
        actualizarLista();
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB_SELECCIONADA, tabSeleccionada);
    }
}
