package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.DisponibilidadAdapter;
import com.example.propuestasonidosonic.adapter.PersonalAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class UsuariosFragment extends Fragment {

    private static final String KEY_SELECTED_TAB = "key_selected_tab";
    private static final String KEY_SELECTED_DATE = "key_selected_date";

    private TabLayout tabsUsuarios;
    private View framePersonal;
    private View frameDisponibilidad;

    private RecyclerView rvPersonal;
    private FloatingActionButton fabNuevoPersonal;
    private PersonalAdapter personalAdapter;

    private ImageButton btnDiaAnterior;
    private TextView tvFechaSeleccionada;
    private ImageButton btnDiaSiguiente;
    private TextView tvAvisoDiaPasado;
    private TextView tvResumenPersonalDia;
    private RecyclerView rvDisponibilidad;
    private TextView tvEmptyDisponibilidad;
    private DisponibilidadAdapter disponibilidadAdapter;

    private UsuarioRepository usuarioRepository;
    private DiaOperativoRepository diaOperativoRepository;
    private PedidoRepository pedidoRepository;

    private int tabSeleccionada = 0;
    private long diaSeleccionadoMillis;

    private List<Usuario> usuariosCache = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        usuarioRepository = appContainer.getUsuarioRepository();
        diaOperativoRepository = appContainer.getDiaOperativoRepository();
        pedidoRepository = appContainer.getPedidoRepository();

        if (savedInstanceState != null) {
            tabSeleccionada = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            diaSeleccionadoMillis = savedInstanceState.getLong(KEY_SELECTED_DATE,
                    TiempoUtils.inicioDelDia(System.currentTimeMillis()));
        } else {
            diaSeleccionadoMillis = TiempoUtils.inicioDelDia(System.currentTimeMillis());
        }

        vincularVistas(view);
        configurarTabs();
        configurarPersonal();
        configurarDisponibilidad();
        observarDatos();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, tabSeleccionada);
        outState.putLong(KEY_SELECTED_DATE, diaSeleccionadoMillis);
    }

    private void vincularVistas(View view) {
        tabsUsuarios = view.findViewById(R.id.tabsUsuarios);
        framePersonal = view.findViewById(R.id.framePersonal);
        frameDisponibilidad = view.findViewById(R.id.frameDisponibilidad);

        rvPersonal = view.findViewById(R.id.rvPersonal);
        fabNuevoPersonal = view.findViewById(R.id.fabNuevoPersonal);

        btnDiaAnterior = view.findViewById(R.id.btnDiaAnterior);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
        btnDiaSiguiente = view.findViewById(R.id.btnDiaSiguiente);
        tvAvisoDiaPasado = view.findViewById(R.id.tvAvisoDiaPasado);
        tvResumenPersonalDia = view.findViewById(R.id.tvResumenPersonalDia);
        rvDisponibilidad = view.findViewById(R.id.rvDisponibilidad);
        tvEmptyDisponibilidad = view.findViewById(R.id.tvEmptyDisponibilidad);
    }

    private void configurarTabs() {
        TabLayout.Tab tab = tabsUsuarios.getTabAt(tabSeleccionada);
        if (tab != null) {
            tab.select();
        }
        actualizarVisibilidadPestanas();

        tabsUsuarios.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabSeleccionada = tab.getPosition();
                actualizarVisibilidadPestanas();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void actualizarVisibilidadPestanas() {
        if (tabSeleccionada == 0) {
            framePersonal.setVisibility(View.VISIBLE);
            frameDisponibilidad.setVisibility(View.GONE);
        } else {
            framePersonal.setVisibility(View.GONE);
            frameDisponibilidad.setVisibility(View.VISIBLE);
            actualizarVistaDisponibilidad();
        }
    }

    private void configurarPersonal() {
        personalAdapter = new PersonalAdapter(usuario -> {
            PersonalFormBottomSheet.editar(usuario.getId())
                    .show(getChildFragmentManager(), "editar_personal");
        });
        rvPersonal.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersonal.setAdapter(personalAdapter);

        fabNuevoPersonal.setOnClickListener(v -> {
            PersonalFormBottomSheet.nuevo()
                    .show(getChildFragmentManager(), "nuevo_personal");
        });
    }

    private void configurarDisponibilidad() {
        disponibilidadAdapter = new DisponibilidadAdapter((usuario, nuevoEstado, switchView) -> {
            gestionarCambioDisponibilidad(usuario, nuevoEstado, switchView);
        });
        rvDisponibilidad.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDisponibilidad.setAdapter(disponibilidadAdapter);

        btnDiaAnterior.setOnClickListener(v -> {
            diaSeleccionadoMillis = TiempoUtils.sumarMinutos(diaSeleccionadoMillis, -24 * 60);
            actualizarVistaDisponibilidad();
        });

        btnDiaSiguiente.setOnClickListener(v -> {
            diaSeleccionadoMillis = TiempoUtils.sumarMinutos(diaSeleccionadoMillis, 24 * 60);
            actualizarVistaDisponibilidad();
        });

        tvFechaSeleccionada.setOnClickListener(v -> mostrarDatePicker());
    }

    private void mostrarDatePicker() {
        // MaterialDatePicker trabaja en UTC
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.btn_elegir_fecha)
                .setSelection(diaSeleccionadoMillis)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                // Convertir medianoche UTC a día local
                Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
                calUtc.setTimeInMillis(selection);
                int anio = calUtc.get(Calendar.YEAR);
                int mes0 = calUtc.get(Calendar.MONTH);
                int dia = calUtc.get(Calendar.DAY_OF_MONTH);
                diaSeleccionadoMillis = TiempoUtils.construir(anio, mes0, dia, 0);
                actualizarVistaDisponibilidad();
            }
        });

        picker.show(getChildFragmentManager(), "date_picker");
    }

    private void observarDatos() {
        usuarioRepository.getUsuarios().observe(getViewLifecycleOwner(), lista -> {
            usuariosCache = lista != null ? lista : new ArrayList<>();
            personalAdapter.setUsuarios(usuariosCache);
            if (tabSeleccionada == 1) {
                actualizarVistaDisponibilidad();
            }
        });

        diaOperativoRepository.getDias().observe(getViewLifecycleOwner(), dias -> {
            if (tabSeleccionada == 1) {
                actualizarVistaDisponibilidad();
            }
        });
    }

    private void actualizarVistaDisponibilidad() {
        tvFechaSeleccionada.setText(FormatUtils.fechaLarga(diaSeleccionadoMillis));

        long hoyInicio = TiempoUtils.inicioDelDia(System.currentTimeMillis());
        boolean esDiaPasado = diaSeleccionadoMillis < hoyInicio;
        tvAvisoDiaPasado.setVisibility(esDiaPasado ? View.VISIBLE : View.GONE);

        String fechaClave = TiempoUtils.claveDia(diaSeleccionadoMillis);
        DiaOperativo diaOp = diaOperativoRepository.obtenerDia(fechaClave);

        disponibilidadAdapter.setDatos(usuariosCache, diaOp, esDiaPasado);

        // Contar y mostrar resumen de personal disponible ese día
        Set<String> sociosNoDisp = (diaOp != null && diaOp.getSociosNoDisponiblesIds() != null)
                ? new HashSet<>(diaOp.getSociosNoDisponiblesIds()) : Collections.emptySet();
        Set<String> asistDisp = (diaOp != null && diaOp.getAsistentesDisponiblesIds() != null)
                ? new HashSet<>(diaOp.getAsistentesDisponiblesIds()) : Collections.emptySet();

        int totalDisponibles = 0;
        int asistentesActivosTotales = 0;
        for (Usuario u : usuariosCache) {
            if (u.isCuentaActiva()) {
                if (Usuario.ROL_SOCIO.equals(u.getRol())) {
                    if (!sociosNoDisp.contains(u.getId())) {
                        totalDisponibles++;
                    }
                } else if (Usuario.ROL_ASISTENTE.equals(u.getRol())) {
                    asistentesActivosTotales++;
                    if (asistDisp.contains(u.getId())) {
                        totalDisponibles++;
                    }
                }
            }
        }

        tvResumenPersonalDia.setText(String.format(getString(R.string.resumen_personal_dia_formato), totalDisponibles));

        // Empty state de asistentes
        tvEmptyDisponibilidad.setVisibility(asistentesActivosTotales == 0 ? View.VISIBLE : View.GONE);
    }

    private void gestionarCambioDisponibilidad(Usuario usuario, boolean nuevoEstado, MaterialSwitch switchView) {
        boolean esSocio = Usuario.ROL_SOCIO.equals(usuario.getRol());

        // Si se está quitando a alguien (apagando el switch):
        if (!nuevoEstado) {
            long inicioDia = diaSeleccionadoMillis;
            long finDia = TiempoUtils.sumarMinutos(inicioDia, 24 * 60);
            List<Pedido> reservasDia = pedidoRepository.obtenerReservasActivasDeOperador(
                    usuario.getId(), inicioDia, finDia);

            if (!reservasDia.isEmpty()) {
                String mensaje = String.format(getString(R.string.dialog_quitar_dia_con_reservas_formato),
                        usuario.getNombre(), reservasDia.size());
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_desactivar_titulo)
                        .setMessage(mensaje)
                        .setNegativeButton(R.string.dialog_cancelar, (dialog, which) -> {
                            // Revertir switch
                            switchView.setChecked(true);
                        })
                        .setPositiveButton(R.string.btn_quitar, (dialog, which) -> {
                            aplicarCambioDisponibilidad(usuario, false);
                        })
                        .setOnCancelListener(dialog -> {
                            switchView.setChecked(true);
                        })
                        .show();
                return;
            }
        }

        aplicarCambioDisponibilidad(usuario, nuevoEstado);
    }

    private void aplicarCambioDisponibilidad(Usuario usuario, boolean nuevoEstado) {
        String fechaClave = TiempoUtils.claveDia(diaSeleccionadoMillis);
        DiaOperativo diaOp = diaOperativoRepository.obtenerDia(fechaClave);
        if (diaOp == null) {
            diaOp = new DiaOperativo(fechaClave, new ArrayList<>(), new ArrayList<>());
        }

        List<String> asistList = diaOp.getAsistentesDisponiblesIds() != null
                ? new ArrayList<>(diaOp.getAsistentesDisponiblesIds()) : new ArrayList<>();
        List<String> sociosNoList = diaOp.getSociosNoDisponiblesIds() != null
                ? new ArrayList<>(diaOp.getSociosNoDisponiblesIds()) : new ArrayList<>();

        boolean esSocio = Usuario.ROL_SOCIO.equals(usuario.getRol());

        if (esSocio) {
            // Socio activo = NO estar en sociosNoDisponiblesIds
            if (nuevoEstado) {
                sociosNoList.remove(usuario.getId());
            } else {
                if (!sociosNoList.contains(usuario.getId())) {
                    sociosNoList.add(usuario.getId());
                }
            }
        } else {
            // Asistente activo = SÍ estar en asistentesDisponiblesIds
            if (nuevoEstado) {
                if (!asistList.contains(usuario.getId())) {
                    asistList.add(usuario.getId());
                }
            } else {
                asistList.remove(usuario.getId());
            }
        }

        diaOp.setAsistentesDisponiblesIds(asistList);
        diaOp.setSociosNoDisponiblesIds(sociosNoList);

        diaOperativoRepository.guardarDia(diaOp, null);
    }
}
