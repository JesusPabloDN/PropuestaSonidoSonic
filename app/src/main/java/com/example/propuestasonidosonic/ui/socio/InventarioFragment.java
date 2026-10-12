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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.EquipoAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class InventarioFragment extends Fragment implements EquipoAdapter.OnEquipoClickListener, EquipoAdapter.EstadoHelper {

    private static final String KEY_SEARCH_QUERY = "key_search_query";
    private static final String KEY_SELECTED_CAT_POS = "key_selected_cat_pos";

    private EditText etBuscarNombre;
    private Spinner spCategoriaFiltro;
    private RecyclerView rvEquipos;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMensaje;
    private FloatingActionButton fabNuevoEquipo;

    private EquipoAdapter adapter;
    private EquipoRepository equipoRepository;
    private EventoRepository eventoRepository;

    private final List<Equipo> listaCompleta = new ArrayList<>();
    private final List<String> categoriasFiltro = new ArrayList<>();

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

        etBuscarNombre = view.findViewById(R.id.etBuscarNombre);
        spCategoriaFiltro = view.findViewById(R.id.spCategoriaFiltro);
        rvEquipos = view.findViewById(R.id.rvEquipos);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyMensaje = view.findViewById(R.id.tvEmptyMensaje);
        fabNuevoEquipo = view.findViewById(R.id.fabNuevoEquipo);

        configurarRecyclerView();
        configurarFiltroCategorias();
        configurarBusqueda();
        configurarFab();

        if (savedInstanceState != null) {
            queryPendiente = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            catPosPendiente = savedInstanceState.getInt(KEY_SELECTED_CAT_POS, 0);

            etBuscarNombre.setText(queryPendiente);
            if (catPosPendiente >= 0 && catPosPendiente < categoriasFiltro.size()) {
                spCategoriaFiltro.setSelection(catPosPendiente);
            }
        }

        observarDatos();
    }

    private void configurarRecyclerView() {
        adapter = new EquipoAdapter(this, this);
        rvEquipos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEquipos.setAdapter(adapter);
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
                aplicarFiltro();
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
                aplicarFiltro();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarFab() {
        fabNuevoEquipo.setOnClickListener(v -> {
            EquipoFormBottomSheet dialog = EquipoFormBottomSheet.newInstance(null);
            dialog.show(getChildFragmentManager(), "EquipoFormNuevo");
        });
    }

    private void observarDatos() {
        equipoRepository.getEquipos().observe(getViewLifecycleOwner(), equipos -> {
            listaCompleta.clear();
            if (equipos != null) {
                listaCompleta.addAll(equipos);
            }
            aplicarFiltro();
        });
    }

    private void aplicarFiltro() {
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

        // Ordenar por nombre y luego por número de unidad
        filtrados.sort((e1, e2) -> {
            int comp = e1.getNombre().compareToIgnoreCase(e2.getNombre());
            if (comp != 0) return comp;
            return Integer.compare(e1.getNumeroUnidad(), e2.getNumeroUnidad());
        });

        adapter.setEquipos(filtrados);

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
        if (etBuscarNombre != null && etBuscarNombre.getText() != null) {
            outState.putString(KEY_SEARCH_QUERY, etBuscarNombre.getText().toString());
        }
        if (spCategoriaFiltro != null) {
            outState.putInt(KEY_SELECTED_CAT_POS, spCategoriaFiltro.getSelectedItemPosition());
        }
    }
}
