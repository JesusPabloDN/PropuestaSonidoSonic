package com.example.propuestasonidosonic.ui.asistente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.CalendarioAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.AgendaUtils;
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.domain.DisponibilidadAsistenteUtils;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class AsistenteDisponibilidadFragment extends Fragment {

    private static final String KEY_ANIO = "key_anio";
    private static final String KEY_MES0 = "key_mes0";

    private DiaOperativoRepository diaOperativoRepository;
    private PedidoRepository pedidoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private TextView tvMesAnio;
    private ImageButton btnMesAnterior;
    private ImageButton btnMesSiguiente;
    private MaterialButton btnHoy;
    private RecyclerView rvCalendarioDisponibilidad;
    private TextView tvDiasDisponiblesMes;
    private MaterialButton btnMarcarFinesDeSemana;

    private CalendarioAdapter adapter;

    private int anioActual;
    private int mesActual0;

    private List<Pedido> todosLosPedidosCache = new ArrayList<>();
    private final Set<String> fechasDisponibles = new HashSet<>();
    private final Set<String> fechasConEvento = new HashSet<>();

    private static final Locale LOCALE_MX = new Locale("es", "MX");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        diaOperativoRepository = appContainer.getDiaOperativoRepository();
        pedidoRepository = appContainer.getPedidoRepository();
        sessionManager = appContainer.getSessionManager();
        reloj = appContainer.getReloj();

        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.setTimeInMillis(ahora);

        if (savedInstanceState != null) {
            anioActual = savedInstanceState.getInt(KEY_ANIO, cal.get(Calendar.YEAR));
            mesActual0 = savedInstanceState.getInt(KEY_MES0, cal.get(Calendar.MONTH));
        } else {
            anioActual = cal.get(Calendar.YEAR);
            mesActual0 = cal.get(Calendar.MONTH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asistente_disponibilidad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMesAnio = view.findViewById(R.id.tvMesAnio);
        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        btnHoy = view.findViewById(R.id.btnHoy);
        rvCalendarioDisponibilidad = view.findViewById(R.id.rvCalendarioDisponibilidad);
        tvDiasDisponiblesMes = view.findViewById(R.id.tvDiasDisponiblesMes);
        btnMarcarFinesDeSemana = view.findViewById(R.id.btnMarcarFinesDeSemana);

        adapter = new CalendarioAdapter(this::onDiaClick);
        rvCalendarioDisponibilidad.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvCalendarioDisponibilidad.setAdapter(adapter);

        btnMesAnterior.setOnClickListener(v -> cambiarMes(-1));
        btnMesSiguiente.setOnClickListener(v -> cambiarMes(1));
        btnHoy.setOnClickListener(v -> irAHoy());

        btnMarcarFinesDeSemana.setOnClickListener(v -> marcarFinesDeSemana());

        actualizarTituloMes();

        diaOperativoRepository.getDias().observe(getViewLifecycleOwner(), dias -> {
            recalcularDisponibilidadYEventos();
        });

        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            todosLosPedidosCache = (pedidos != null) ? pedidos : new ArrayList<>();
            recalcularDisponibilidadYEventos();
        });
    }

    private void cambiarMes(int delta) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(anioActual, mesActual0, 1);
        cal.add(Calendar.MONTH, delta);
        anioActual = cal.get(Calendar.YEAR);
        mesActual0 = cal.get(Calendar.MONTH);
        actualizarTituloMes();
        recalcularDisponibilidadYEventos();
    }

    private void irAHoy() {
        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.setTimeInMillis(ahora);
        anioActual = cal.get(Calendar.YEAR);
        mesActual0 = cal.get(Calendar.MONTH);
        actualizarTituloMes();
        recalcularDisponibilidadYEventos();
    }

    private void actualizarTituloMes() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(anioActual, mesActual0, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM 'de' yyyy", LOCALE_MX);
        sdf.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));
        tvMesAnio.setText(sdf.format(new Date(cal.getTimeInMillis())));
    }

    private void recalcularDisponibilidadYEventos() {
        if (!isAdded()) return;

        Usuario usuario = sessionManager.getUsuarioActual();
        String asistenteId = (usuario != null) ? usuario.getId() : null;

        fechasDisponibles.clear();
        fechasConEvento.clear();

        if (asistenteId != null) {
            Map<String, DiaOperativo> diasMap = diaOperativoRepository.obtenerTodosPorClave();
            for (Map.Entry<String, DiaOperativo> entry : diasMap.entrySet()) {
                DiaOperativo d = entry.getValue();
                if (d != null && d.getAsistentesDisponiblesIds() != null
                        && d.getAsistentesDisponiblesIds().contains(asistenteId)) {
                    fechasDisponibles.add(entry.getKey());
                }
            }

            List<Pedido> misEventos = AsistenteUtils.eventosDelAsistente(todosLosPedidosCache, asistenteId);
            for (Pedido p : misEventos) {
                if (p != null) {
                    fechasConEvento.add(TiempoUtils.claveDia(p.getInicioMillis()));
                }
            }
        }

        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();
        List<Long> celdas = AgendaUtils.celdasDelMes(anioActual, mesActual0);
        adapter.setModoDisponibilidad(celdas, mesActual0, fechasDisponibles, fechasConEvento, ahora);

        // Conteo de días disponibles en este mes calendario
        int disponiblesEsteMes = 0;
        List<String> clavesMes = DisponibilidadAsistenteUtils.clavesDelMes(anioActual, mesActual0);
        for (String c : clavesMes) {
            if (fechasDisponibles.contains(c)) {
                disponiblesEsteMes++;
            }
        }
        tvDiasDisponiblesMes.setText(getString(R.string.dias_disponibles_mes_formato, disponiblesEsteMes));
    }

    private void onDiaClick(long diaMillis) {
        Usuario usuario = sessionManager.getUsuarioActual();
        if (usuario == null) return;
        String asistenteId = usuario.getId();

        String fechaClave = TiempoUtils.claveDia(diaMillis);
        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();

        if (!DisponibilidadAsistenteUtils.puedeMarcar(fechaClave, ahora)) {
            return;
        }

        boolean estaDisponible = fechasDisponibles.contains(fechaClave);
        if (estaDisponible) {
            // Quiere desmarcar
            List<Pedido> misEventos = AsistenteUtils.eventosDelAsistente(todosLosPedidosCache, asistenteId);
            if (!DisponibilidadAsistenteUtils.puedeDesmarcar(fechaClave, ahora, misEventos)) {
                Snackbar.make(requireView(), R.string.aviso_desmarcar_evento, Snackbar.LENGTH_LONG).show();
                return;
            }
            diaOperativoRepository.establecerDisponibilidadAsistente(fechaClave, asistenteId, false, null);
        } else {
            // Quiere marcar
            diaOperativoRepository.establecerDisponibilidadAsistente(fechaClave, asistenteId, true, null);
        }
    }

    private void marcarFinesDeSemana() {
        Usuario usuario = sessionManager.getUsuarioActual();
        if (usuario == null) return;
        String asistenteId = usuario.getId();

        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();
        List<String> finesDeSemana = DisponibilidadAsistenteUtils.clavesFinesDeSemana(anioActual, mesActual0, ahora);

        List<String> recienMarcados = new ArrayList<>();
        for (String clave : finesDeSemana) {
            if (!fechasDisponibles.contains(clave)) {
                recienMarcados.add(clave);
            }
        }

        if (recienMarcados.isEmpty()) {
            Snackbar.make(requireView(), R.string.snackbar_dias_marcados_confirmacion, Snackbar.LENGTH_SHORT).show();
            return;
        }

        diaOperativoRepository.establecerDisponibilidadAsistenteVarios(recienMarcados, asistenteId, true, null);

        String mensaje = getString(R.string.snackbar_dias_marcados_confirmacion, recienMarcados.size());
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_deshacer, v -> {
                    List<Pedido> misEventos = AsistenteUtils.eventosDelAsistente(todosLosPedidosCache, asistenteId);
                    List<String> desmarcar = new ArrayList<>();
                    for (String clave : recienMarcados) {
                        if (DisponibilidadAsistenteUtils.puedeDesmarcar(clave, ahora, misEventos)) {
                            desmarcar.add(clave);
                        }
                    }
                    if (!desmarcar.isEmpty()) {
                        diaOperativoRepository.establecerDisponibilidadAsistenteVarios(desmarcar, asistenteId, false, null);
                    }
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ANIO, anioActual);
        outState.putInt(KEY_MES0, mesActual0);
    }
}
