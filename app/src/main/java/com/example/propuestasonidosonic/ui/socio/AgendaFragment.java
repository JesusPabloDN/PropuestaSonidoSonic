package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.CalendarioAdapter;
import com.example.propuestasonidosonic.adapter.EventoAgendaAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.AgendaUtils;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AgendaFragment extends Fragment implements CalendarioAdapter.OnDiaClickListener, EventoAgendaAdapter.OnEventoClickListener {

    private static final String KEY_ANIO_MOSTRADO = "key_anio_mostrado";
    private static final String KEY_MES_MOSTRADO = "key_mes_mostrado";
    private static final String KEY_DIA_SELECCIONADO = "key_dia_seleccionado";
    private static final String KEY_VISTA_ACTUAL = "key_vista_actual";

    private static final int VISTA_DIA = 0;
    private static final int VISTA_MES = 1;
    private static final long INTERVALO_REFRESCO_MS = 60_000L;

    private ImageButton btnMesAnterior;
    private TextView tvMes;
    private ImageButton btnMesSiguiente;
    private MaterialButton btnHoy;
    private RecyclerView rvCalendario;
    private MaterialButtonToggleGroup toggleVista;
    private TextView tvTituloLista;
    private RecyclerView rvEventosAgenda;
    private LinearLayout layoutEmptyAgenda;
    private TextView tvEmptyAgendaMensaje;

    private CalendarioAdapter calendarioAdapter;
    private EventoAgendaAdapter eventosAdapter;
    private LinearLayoutManager layoutManagerEventos;

    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;

    private int anioMostrado;
    private int mesMostrado0;
    private long diaSeleccionadoMillis;
    private int vistaActual = VISTA_DIA;

    private final List<Pedido> listaPedidosCache = new ArrayList<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                if (pedidoRepository != null) {
                    pedidoRepository.liberarApartadosVencidos();
                }
                actualizarTodo();
                refreshHandler.postDelayed(this, INTERVALO_REFRESCO_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = appContainer.getPedidoRepository();
        usuarioRepository = appContainer.getUsuarioRepository();

        Calendar calHoy = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        int hoyAnio = calHoy.get(Calendar.YEAR);
        int hoyMes0 = calHoy.get(Calendar.MONTH);
        long hoyMillis = TiempoUtils.inicioDelDia(calHoy.getTimeInMillis());

        if (savedInstanceState != null) {
            anioMostrado = savedInstanceState.getInt(KEY_ANIO_MOSTRADO, hoyAnio);
            mesMostrado0 = savedInstanceState.getInt(KEY_MES_MOSTRADO, hoyMes0);
            diaSeleccionadoMillis = savedInstanceState.getLong(KEY_DIA_SELECCIONADO, hoyMillis);
            vistaActual = savedInstanceState.getInt(KEY_VISTA_ACTUAL, VISTA_DIA);
        } else {
            anioMostrado = hoyAnio;
            mesMostrado0 = hoyMes0;
            diaSeleccionadoMillis = hoyMillis;
            vistaActual = VISTA_DIA;
        }

        vincularVistas(view);
        configurarControlesMes();
        configurarCalendario();
        configurarToggleVista();
        configurarListaEventos();
        observarPedidos();
    }

    private void vincularVistas(View view) {
        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        tvMes = view.findViewById(R.id.tvMes);
        btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        btnHoy = view.findViewById(R.id.btnHoy);
        rvCalendario = view.findViewById(R.id.rvCalendario);
        toggleVista = view.findViewById(R.id.toggleVista);
        tvTituloLista = view.findViewById(R.id.tvTituloLista);
        rvEventosAgenda = view.findViewById(R.id.rvEventosAgenda);
        layoutEmptyAgenda = view.findViewById(R.id.layoutEmptyAgenda);
        tvEmptyAgendaMensaje = view.findViewById(R.id.tvEmptyAgendaMensaje);
    }

    private void configurarControlesMes() {
        btnMesAnterior.setOnClickListener(v -> cambiarMes(-1));
        btnMesSiguiente.setOnClickListener(v -> cambiarMes(1));
        btnHoy.setOnClickListener(v -> irAHoy());
    }

    private void cambiarMes(int deltaMeses) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.clear();
        cal.set(anioMostrado, mesMostrado0, 1);
        cal.add(Calendar.MONTH, deltaMeses);
        anioMostrado = cal.get(Calendar.YEAR);
        mesMostrado0 = cal.get(Calendar.MONTH);

        Calendar hoyCal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        int hoyAnio = hoyCal.get(Calendar.YEAR);
        int hoyMes0 = hoyCal.get(Calendar.MONTH);

        if (anioMostrado == hoyAnio && mesMostrado0 == hoyMes0) {
            diaSeleccionadoMillis = TiempoUtils.inicioDelDia(hoyCal.getTimeInMillis());
        } else {
            diaSeleccionadoMillis = TiempoUtils.construir(anioMostrado, mesMostrado0, 1, 0);
        }

        actualizarTodo();
    }

    private void irAHoy() {
        Calendar hoyCal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        anioMostrado = hoyCal.get(Calendar.YEAR);
        mesMostrado0 = hoyCal.get(Calendar.MONTH);
        diaSeleccionadoMillis = TiempoUtils.inicioDelDia(hoyCal.getTimeInMillis());
        actualizarTodo();
    }

    private void configurarCalendario() {
        calendarioAdapter = new CalendarioAdapter(this);
        rvCalendario.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvCalendario.setAdapter(calendarioAdapter);
    }

    private void configurarToggleVista() {
        if (vistaActual == VISTA_MES) {
            toggleVista.check(R.id.btnVistaMes);
        } else {
            toggleVista.check(R.id.btnVistaDia);
        }

        toggleVista.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                vistaActual = (checkedId == R.id.btnVistaMes) ? VISTA_MES : VISTA_DIA;
                actualizarListaEventos();
            }
        });
    }

    private void configurarListaEventos() {
        eventosAdapter = new EventoAgendaAdapter(usuarioRepository, this);
        layoutManagerEventos = new LinearLayoutManager(requireContext());
        rvEventosAgenda.setLayoutManager(layoutManagerEventos);
        rvEventosAgenda.setAdapter(eventosAdapter);
    }

    private void observarPedidos() {
        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
            listaPedidosCache.clear();
            if (pedidos != null) {
                listaPedidosCache.addAll(pedidos);
            }
            actualizarTodo();
        });
    }

    private void actualizarTodo() {
        if (!isAdded()) return;

        // Título del mes
        tvMes.setText(FormatUtils.mesAnio(anioMostrado, mesMostrado0));

        // Celdas del mes
        List<Long> celdas = AgendaUtils.celdasDelMes(anioMostrado, mesMostrado0);

        // Marcadores por día
        long ahora = System.currentTimeMillis();
        Map<String, List<Pedido>> agrupados = AgendaUtils.agruparPorDia(listaPedidosCache, ahora);
        Map<String, int[]> marcadores = new HashMap<>();
        for (Map.Entry<String, List<Pedido>> entry : agrupados.entrySet()) {
            marcadores.put(entry.getKey(), AgendaUtils.marcadoresDia(entry.getValue()));
        }

        calendarioAdapter.setDatos(celdas, mesMostrado0, diaSeleccionadoMillis, marcadores);
        actualizarListaEventos();
    }

    private void actualizarListaEventos() {
        if (!isAdded()) return;

        long ahora = System.currentTimeMillis();

        if (vistaActual == VISTA_DIA) {
            tvTituloLista.setText(FormatUtils.fechaLarga(diaSeleccionadoMillis));

            Map<String, List<Pedido>> agrupados = AgendaUtils.agruparPorDia(listaPedidosCache, ahora);
            String claveDia = TiempoUtils.claveDia(diaSeleccionadoMillis);
            List<Pedido> pedidosDia = agrupados.get(claveDia);

            if (pedidosDia == null || pedidosDia.isEmpty()) {
                rvEventosAgenda.setVisibility(View.GONE);
                layoutEmptyAgenda.setVisibility(View.VISIBLE);
                tvEmptyAgendaMensaje.setText(R.string.empty_agenda_dia);
                eventosAdapter.setEventosDelDia(null);
            } else {
                layoutEmptyAgenda.setVisibility(View.GONE);
                rvEventosAgenda.setVisibility(View.VISIBLE);
                eventosAdapter.setEventosDelDia(pedidosDia);
            }
        } else {
            // Modo Mes
            List<Pedido> pedidosMes = AgendaUtils.delMes(listaPedidosCache, anioMostrado, mesMostrado0, ahora);
            int totalEventos = pedidosMes.size();
            String mesNombre = FormatUtils.mesAnio(anioMostrado, mesMostrado0);
            String textoContador = getResources().getQuantityString(R.plurals.contador_eventos, totalEventos, totalEventos);
            tvTituloLista.setText(getString(R.string.titulo_agenda_mes_formato, mesNombre, textoContador));

            if (pedidosMes.isEmpty()) {
                rvEventosAgenda.setVisibility(View.GONE);
                layoutEmptyAgenda.setVisibility(View.VISIBLE);
                tvEmptyAgendaMensaje.setText(R.string.empty_agenda_mes);
                eventosAdapter.setEventosDelMesAgrupados(null);
            } else {
                layoutEmptyAgenda.setVisibility(View.GONE);
                rvEventosAgenda.setVisibility(View.VISIBLE);
                Map<String, List<Pedido>> mapaMesAgrupado = AgendaUtils.agruparPorDia(pedidosMes, ahora);
                eventosAdapter.setEventosDelMesAgrupados(mapaMesAgrupado);
            }
        }
    }

    @Override
    public void onDiaClick(long diaMillis) {
        diaSeleccionadoMillis = diaMillis;
        calendarioAdapter.setDiaSeleccionado(diaMillis);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.setTimeInMillis(diaMillis);
        int anioCelda = cal.get(Calendar.YEAR);
        int mesCelda0 = cal.get(Calendar.MONTH);

        // Si es de otro mes, cambia también el mes mostrado
        if (anioCelda != anioMostrado || mesCelda0 != mesMostrado0) {
            anioMostrado = anioCelda;
            mesMostrado0 = mesCelda0;
            actualizarTodo();
            return;
        }

        if (vistaActual == VISTA_DIA) {
            actualizarListaEventos();
        } else {
            // Modo Mes: hacer scroll al encabezado de ese día si tiene eventos
            String claveDia = TiempoUtils.claveDia(diaMillis);
            int pos = eventosAdapter.obtenerPosicionDia(claveDia);
            if (pos != -1 && layoutManagerEventos != null) {
                layoutManagerEventos.scrollToPositionWithOffset(pos, 0);
            }
        }
    }

    @Override
    public void onEventoClick(Pedido pedido) {
        if (pedido == null || !isAdded()) return;
        Bundle args = new Bundle();
        args.putString("pedidoId", pedido.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_agenda_to_detalle, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pedidoRepository != null) {
            pedidoRepository.liberarApartadosVencidos();
        }
        actualizarTodo();
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
        outState.putInt(KEY_ANIO_MOSTRADO, anioMostrado);
        outState.putInt(KEY_MES_MOSTRADO, mesMostrado0);
        outState.putLong(KEY_DIA_SELECCIONADO, diaSeleccionadoMillis);
        outState.putInt(KEY_VISTA_ACTUAL, vistaActual);
    }
}
