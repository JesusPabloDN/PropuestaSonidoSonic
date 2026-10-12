package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.EquipoAdapter;
import com.example.propuestasonidosonic.adapter.PaqueteAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventarioFragment extends Fragment implements EquipoAdapter.OnEquipoClickListener,
        EquipoAdapter.EstadoHelper, PaqueteAdapter.OnPaqueteClickListener {

    private static final String KEY_SEARCH_QUERY = "key_search_query";
    private static final String KEY_SELECTED_CAT_POS = "key_selected_cat_pos";
    private static final String KEY_TAB_POS = "key_tab_pos";

    private TabLayout tabsInventario;
    private MaterialCardView cardBarraBusqueda;
    private EditText etBuscarNombre;
    private Spinner spCategoriaFiltro;

    private FrameLayout frameListaEquipos;
    private RecyclerView rvEquipos;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMensaje;

    private FrameLayout frameListaPaquetes;
    private RecyclerView rvPaquetes;
    private LinearLayout layoutEmptyPaquetes;

    private FloatingActionButton fabNuevoEquipo;

    private EquipoAdapter equipoAdapter;
    private PaqueteAdapter paqueteAdapter;

    private EquipoRepository equipoRepository;
    private EventoRepository eventoRepository;
    private PaqueteRepository paqueteRepository;

    private final List<Equipo> listaCompleta = new ArrayList<>();
    private final List<String> categoriasFiltro = new ArrayList<>();
    private final List<Paquete> paquetesLista = new ArrayList<>();

    private int tabSeleccionadaPos = 0;
    private String queryPendiente = "";
    private int catPosPendiente = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer appContainer = ((SonidoSonicApp) requireActivity().getApplication()).getAppContainer();
        equipoRepository = appContainer.getEquipoRepository();
        eventoRepository = appContainer.getEventoRepository();
        paqueteRepository = appContainer.getPaqueteRepository();

        tabsInventario = view.findViewById(R.id.tabsInventario);
        cardBarraBusqueda = view.findViewById(R.id.cardBarraBusqueda);
        etBuscarNombre = view.findViewById(R.id.etBuscarNombre);
        spCategoriaFiltro = view.findViewById(R.id.spCategoriaFiltro);

        frameListaEquipos = view.findViewById(R.id.frameListaEquipos);
        rvEquipos = view.findViewById(R.id.rvEquipos);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyMensaje = view.findViewById(R.id.tvEmptyMensaje);

        frameListaPaquetes = view.findViewById(R.id.frameListaPaquetes);
        rvPaquetes = view.findViewById(R.id.rvPaquetes);
        layoutEmptyPaquetes = view.findViewById(R.id.layoutEmptyPaquetes);

        fabNuevoEquipo = view.findViewById(R.id.fabNuevoEquipo);

        configurarTabs();
        configurarRecyclerViewEquipos();
        configurarRecyclerViewPaquetes();
        configurarFiltroCategorias();
        configurarBusqueda();
        configurarFab();

        if (savedInstanceState != null) {
            tabSeleccionadaPos = savedInstanceState.getInt(KEY_TAB_POS, 0);
            queryPendiente = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            catPosPendiente = savedInstanceState.getInt(KEY_SELECTED_CAT_POS, 0);

            etBuscarNombre.setText(queryPendiente);
            if (catPosPendiente >= 0 && catPosPendiente < categoriasFiltro.size()) {
                spCategoriaFiltro.setSelection(catPosPendiente);
            }
        }

        seleccionarTab(tabSeleccionadaPos);
        observarDatos();
    }

    private void configurarTabs() {
        tabsInventario.removeAllTabs();
        tabsInventario.addTab(tabsInventario.newTab().setText(R.string.tab_inventario_equipo));
        tabsInventario.addTab(tabsInventario.newTab().setText(R.string.tab_inventario_paquetes));

        tabsInventario.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabSeleccionadaPos = tab.getPosition();
                aplicarCambioTab(tabSeleccionadaPos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void seleccionarTab(int pos) {
        TabLayout.Tab tab = tabsInventario.getTabAt(pos);
        if (tab != null) {
            tab.select();
        }
        aplicarCambioTab(pos);
    }

    private void aplicarCambioTab(int pos) {
        if (pos == 0) {
            // Pestaña Equipo
            cardBarraBusqueda.setVisibility(View.VISIBLE);
            frameListaEquipos.setVisibility(View.VISIBLE);
            frameListaPaquetes.setVisibility(View.GONE);
            fabNuevoEquipo.setContentDescription(getString(R.string.dialog_nuevo_equipo_titulo));
        } else {
            // Pestaña Paquetes
            cardBarraBusqueda.setVisibility(View.GONE);
            frameListaEquipos.setVisibility(View.GONE);
            frameListaPaquetes.setVisibility(View.VISIBLE);
            fabNuevoEquipo.setContentDescription(getString(R.string.paquete_form_titulo_nuevo));
        }
    }

    private void configurarRecyclerViewEquipos() {
        equipoAdapter = new EquipoAdapter(this, this);
        rvEquipos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEquipos.setAdapter(equipoAdapter);
    }

    private void configurarRecyclerViewPaquetes() {
        paqueteAdapter = new PaqueteAdapter(this);
        rvPaquetes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPaquetes.setAdapter(paqueteAdapter);
    }

    private void configurarFiltroCategorias() {
        categoriasFiltro.clear();
        categoriasFiltro.add(getString(R.string.categoria_todas));
        categoriasFiltro.addAll(CategoriaEquipo.TODAS_LAS_CATEGORIAS);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categoriasFiltro);
        spCategoriaFiltro.setAdapter(spinnerAdapter);

        spCategoriaFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltroEquipos();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarBusqueda() {
        etBuscarNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltroEquipos();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarFab() {
        fabNuevoEquipo.setOnClickListener(v -> {
            if (tabSeleccionadaPos == 0) {
                EquipoFormBottomSheet dialog = EquipoFormBottomSheet.newInstance(null);
                dialog.show(getChildFragmentManager(), "EquipoFormNuevo");
            } else {
                Navigation.findNavController(requireView()).navigate(R.id.action_inventario_to_paqueteForm);
            }
        });
    }

    private void observarDatos() {
        equipoRepository.getEquipos().observe(getViewLifecycleOwner(), equipos -> {
            listaCompleta.clear();
            if (equipos != null) {
                listaCompleta.addAll(equipos);
            }
            aplicarFiltroEquipos();
            actualizarModelosEnPaquetes();
        });

        paqueteRepository.getPaquetes().observe(getViewLifecycleOwner(), paquetes -> {
            paquetesLista.clear();
            if (paquetes != null) {
                paquetesLista.addAll(paquetes);
            }
            paqueteAdapter.setPaquetes(paquetesLista);
            actualizarEstadoVacioPaquetes();
        });
    }

    private void actualizarModelosEnPaquetes() {
        List<ModeloInventario> modelos = equipoRepository.obtenerModelos();
        Map<String, ModeloInventario> map = new HashMap<>();
        for (ModeloInventario m : modelos) {
            map.put(m.getClave(), m);
        }
        paqueteAdapter.setModelosMap(map);
    }

    private void actualizarEstadoVacioPaquetes() {
        if (paquetesLista.isEmpty()) {
            rvPaquetes.setVisibility(View.GONE);
            layoutEmptyPaquetes.setVisibility(View.VISIBLE);
        } else {
            rvPaquetes.setVisibility(View.VISIBLE);
            layoutEmptyPaquetes.setVisibility(View.GONE);
        }
    }

    private void aplicarFiltroEquipos() {
        String query = etBuscarNombre.getText() != null ? FormatUtils.quitarAcentos(etBuscarNombre.getText().toString()) : "";
        int selectedPos = spCategoriaFiltro.getSelectedItemPosition();
        String categoriaSeleccionada = (selectedPos >= 0 && selectedPos < categoriasFiltro.size())
                ? categoriasFiltro.get(selectedPos) : getString(R.string.categoria_todas);

        boolean filtrarPorCategoria = !categoriaSeleccionada.equalsIgnoreCase(getString(R.string.categoria_todas));

        List<Equipo> filtrados = new ArrayList<>();
        for (Equipo e : listaCompleta) {
            if (filtrarPorCategoria && !categoriaSeleccionada.equalsIgnoreCase(e.getCategoria())) {
                continue;
            }
            if (!query.isEmpty()) {
                String nombreNormalizado = FormatUtils.quitarAcentos(e.getNombre());
                if (!nombreNormalizado.contains(query)) {
                    continue;
                }
            }
            filtrados.add(e);
        }

        filtrados.sort((e1, e2) -> {
            int comp = e1.getNombre().compareToIgnoreCase(e2.getNombre());
            if (comp != 0) return comp;
            return Integer.compare(e1.getNumeroUnidad(), e2.getNumeroUnidad());
        });

        equipoAdapter.setEquipos(filtrados);

        if (listaCompleta.isEmpty()) {
            rvEquipos.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMensaje.setText(R.string.empty_inventario);
        } else if (filtrados.isEmpty()) {
            rvEquipos.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMensaje.setText(R.string.empty_inventario_sin_resultados);
        } else {
            rvEquipos.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEquipoClick(Equipo equipo) {
        EquipoFormBottomSheet dialog = EquipoFormBottomSheet.newInstance(equipo.getId());
        dialog.show(getChildFragmentManager(), "EquipoFormEditar");
    }

    @Override
    public void onPaqueteClick(Paquete paquete) {
        Bundle args = new Bundle();
        args.putString("paqueteId", paquete.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_inventario_to_paqueteForm, args);
    }

    @Override
    public String obtenerEstadoEfectivo(Equipo equipo) {
        return equipoRepository.estadoEfectivo(equipo, System.currentTimeMillis());
    }

    @Override
    public Evento obtenerProximaReserva(String equipoId) {
        return equipoRepository.proximaReserva(equipoId, System.currentTimeMillis());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB_POS, tabSeleccionadaPos);
        if (etBuscarNombre != null && etBuscarNombre.getText() != null) {
            outState.putString(KEY_SEARCH_QUERY, etBuscarNombre.getText().toString());
        }
        if (spCategoriaFiltro != null) {
            outState.putInt(KEY_SELECTED_CAT_POS, spCategoriaFiltro.getSelectedItemPosition());
        }
    }
}
