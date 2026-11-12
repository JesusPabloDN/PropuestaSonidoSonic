package com.example.propuestasonidosonic.ui.socio;

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
import androidx.viewpager2.widget.ViewPager2;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.ActividadRecienteUtils;
import com.example.propuestasonidosonic.domain.DashboardUtils;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.ui.comun.GraficaBarrasHorizontalView;
import com.example.propuestasonidosonic.ui.comun.GraficaBarrasView;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {

    private static final long INTERVALO_REFRESCO_MS = 60_000L;
    private static final long INTERVALO_CARRUSEL_MS = 6_000L;
    private static final int MESES_GRAFICA = 6;
    private static final String[] DIAS_SEMANA = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final Locale LOCALE_MX = new Locale("es", "MX");

    private PagoRepository pagoRepository;
    private PedidoRepository pedidoRepository;
    private EquipoRepository equipoRepository;

    // Carrusel de avisos
    private ViewPager2 vpAvisos;
    private TextView tvAvisosContador;
    private View btnAvisoAnterior;
    private View btnAvisoSiguiente;
    private AvisosDashboardAdapter avisosAdapter;

    // Este mes
    private TextView tvDashCobrado;
    private TextView tvDashReembolsos;
    private TextView tvDashNeto;
    private TextView tvDashPorCobrar;

    // Operación
    private TextView tvEventosSemanaTotal;
    private GraficaBarrasView graficaEventosSemana;
    private TextView tvDashApartadosPorPagar;
    private TextView tvDashEquiposPorAsignar;
    private TextView tvDashPorRevisar;

    // Ingresos y paquetes
    private TextView tvIngresosPeriodo;
    private GraficaBarrasView graficaIngresosMes;
    private GraficaBarrasHorizontalView graficaPaquetes;
    private TextView tvPaquetesVacio;

    // Lo más reciente
    private View rowRecientePedidos;
    private TextView tvRecientePedidosTitulo;
    private TextView tvRecientePedidosSubtitulo;
    private View rowRecienteAgenda;
    private TextView tvRecienteAgendaTitulo;
    private TextView tvRecienteAgendaSubtitulo;
    private View rowRecienteInventario;
    private TextView tvRecienteInventarioTitulo;
    private TextView tvRecienteInventarioSubtitulo;
    private View rowRecienteFinanzas;
    private TextView tvRecienteFinanzasTitulo;
    private TextView tvRecienteFinanzasSubtitulo;

    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Pedido> todosLosPedidos = new ArrayList<>();
    private List<Equipo> todosLosEquipos = new ArrayList<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            actualizarUI();
            refreshHandler.postDelayed(this, INTERVALO_REFRESCO_MS);
        }
    };
    private final Runnable carruselRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpAvisos != null && avisosAdapter != null && avisosAdapter.getItemCount() > 1) {
                int siguiente = (vpAvisos.getCurrentItem() + 1) % avisosAdapter.getItemCount();
                vpAvisos.setCurrentItem(siguiente, true);
            }
            refreshHandler.postDelayed(this, INTERVALO_CARRUSEL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pagoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPagoRepository();
        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();
        equipoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getEquipoRepository();

        vincularVistas(view);
        configurarCarrusel();
        configurarNavegacion();
        observarDatos();
    }

    private void vincularVistas(View view) {
        vpAvisos = view.findViewById(R.id.vpAvisos);
        tvAvisosContador = view.findViewById(R.id.tvAvisosContador);
        btnAvisoAnterior = view.findViewById(R.id.btnAvisoAnterior);
        btnAvisoSiguiente = view.findViewById(R.id.btnAvisoSiguiente);

        tvDashCobrado = view.findViewById(R.id.tvDashCobrado);
        tvDashReembolsos = view.findViewById(R.id.tvDashReembolsos);
        tvDashNeto = view.findViewById(R.id.tvDashNeto);
        tvDashPorCobrar = view.findViewById(R.id.tvDashPorCobrar);

        tvEventosSemanaTotal = view.findViewById(R.id.tvEventosSemanaTotal);
        graficaEventosSemana = view.findViewById(R.id.graficaEventosSemana);
        tvDashApartadosPorPagar = view.findViewById(R.id.tvDashApartadosPorPagar);
        tvDashEquiposPorAsignar = view.findViewById(R.id.tvDashEquiposPorAsignar);
        tvDashPorRevisar = view.findViewById(R.id.tvDashPorRevisar);

        tvIngresosPeriodo = view.findViewById(R.id.tvIngresosPeriodo);
        graficaIngresosMes = view.findViewById(R.id.graficaIngresosMes);
        graficaPaquetes = view.findViewById(R.id.graficaPaquetes);
        tvPaquetesVacio = view.findViewById(R.id.tvPaquetesVacio);

        rowRecientePedidos = view.findViewById(R.id.rowRecientePedidos);
        tvRecientePedidosTitulo = view.findViewById(R.id.tvRecientePedidosTitulo);
        tvRecientePedidosSubtitulo = view.findViewById(R.id.tvRecientePedidosSubtitulo);
        rowRecienteAgenda = view.findViewById(R.id.rowRecienteAgenda);
        tvRecienteAgendaTitulo = view.findViewById(R.id.tvRecienteAgendaTitulo);
        tvRecienteAgendaSubtitulo = view.findViewById(R.id.tvRecienteAgendaSubtitulo);
        rowRecienteInventario = view.findViewById(R.id.rowRecienteInventario);
        tvRecienteInventarioTitulo = view.findViewById(R.id.tvRecienteInventarioTitulo);
        tvRecienteInventarioSubtitulo = view.findViewById(R.id.tvRecienteInventarioSubtitulo);
        rowRecienteFinanzas = view.findViewById(R.id.rowRecienteFinanzas);
        tvRecienteFinanzasTitulo = view.findViewById(R.id.tvRecienteFinanzasTitulo);
        tvRecienteFinanzasSubtitulo = view.findViewById(R.id.tvRecienteFinanzasSubtitulo);
    }

    private void configurarCarrusel() {
        avisosAdapter = new AvisosDashboardAdapter(aviso -> {
            if (aviso.destino == R.id.pedidoDetalleFragment) {
                Navigation.findNavController(requireView()).navigate(aviso.destino, aviso.argumentos);
            } else {
                irAPestana(aviso.destino);
            }
        });
        vpAvisos.setAdapter(avisosAdapter);
        vpAvisos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarContadorAvisos();
            }
        });
        btnAvisoAnterior.setOnClickListener(v -> moverCarrusel(-1));
        btnAvisoSiguiente.setOnClickListener(v -> moverCarrusel(1));
    }

    private void moverCarrusel(int paso) {
        int total = avisosAdapter.getItemCount();
        if (total <= 1) return;
        int destino = (vpAvisos.getCurrentItem() + paso + total) % total;
        vpAvisos.setCurrentItem(destino, true);
        refreshHandler.removeCallbacks(carruselRunnable);
        refreshHandler.postDelayed(carruselRunnable, INTERVALO_CARRUSEL_MS);
    }

    private void actualizarContadorAvisos() {
        int total = avisosAdapter.getItemCount();
        tvAvisosContador.setText(getString(R.string.dash_avisos_contador, vpAvisos.getCurrentItem() + 1, Math.max(total, 1)));
        int visibilidad = total > 1 ? View.VISIBLE : View.INVISIBLE;
        btnAvisoAnterior.setVisibility(visibilidad);
        btnAvisoSiguiente.setVisibility(visibilidad);
    }

    private void irAPestana(int destino) {
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, false)
                .build();
        Navigation.findNavController(requireView()).navigate(destino, null, navOptions);
    }

    private void configurarNavegacion() {
        rowRecientePedidos.setOnClickListener(v -> irAPestana(R.id.nav_pedidos));
        rowRecienteAgenda.setOnClickListener(v -> irAPestana(R.id.nav_agenda));
        rowRecienteInventario.setOnClickListener(v -> irAPestana(R.id.nav_inventario));
        rowRecienteFinanzas.setOnClickListener(v -> irAPestana(R.id.nav_finanzas));

        requireView().findViewById(R.id.rowApartadosPorPagar).setOnClickListener(v -> irAPestana(R.id.nav_pedidos));
        requireView().findViewById(R.id.rowEquiposPorAsignar).setOnClickListener(v -> irAPestana(R.id.nav_pedidos));
        requireView().findViewById(R.id.rowPorRevisar).setOnClickListener(v -> irAPestana(R.id.nav_pedidos));

        requireView().findViewById(R.id.btnVerAgenda).setOnClickListener(v -> irAPestana(R.id.nav_agenda));
        requireView().findViewById(R.id.btnVerFinanzas).setOnClickListener(v -> irAPestana(R.id.nav_finanzas));
        requireView().findViewById(R.id.btnVerPaquetes).setOnClickListener(v -> irAPestana(R.id.nav_inventario));
    }

    private void observarDatos() {
        if (pagoRepository != null) {
            pagoRepository.getTodosLosPagos().observe(getViewLifecycleOwner(), pagos -> {
                todosLosPagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
                actualizarUI();
            });
        }
        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
                todosLosPedidos = pedidos != null ? new ArrayList<>(pedidos) : new ArrayList<>();
                actualizarUI();
            });
        }
        if (equipoRepository != null) {
            equipoRepository.getEquipos().observe(getViewLifecycleOwner(), equipos -> {
                todosLosEquipos = equipos != null ? new ArrayList<>(equipos) : new ArrayList<>();
                actualizarUI();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarUI();
        refreshHandler.postDelayed(refreshRunnable, INTERVALO_REFRESCO_MS);
        refreshHandler.postDelayed(carruselRunnable, INTERVALO_CARRUSEL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.removeCallbacks(carruselRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void actualizarUI() {
        if (!isAdded() || getView() == null) return;

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        int anio = cal.get(Calendar.YEAR);
        int mes0 = cal.get(Calendar.MONTH);
        long ahora = System.currentTimeMillis();

        actualizarAvisos(ahora);
        actualizarEsteMes(anio, mes0);
        actualizarOperacion(ahora);
        actualizarIngresosPorMes(anio, mes0);
        actualizarPaquetes(ahora);
        actualizarLoMasReciente(ahora);
    }

    // ---------- Carrusel de avisos ----------

    private void actualizarAvisos(long ahora) {
        List<AvisosDashboardAdapter.Aviso> avisos = new ArrayList<>();

        Pedido proximo = ActividadRecienteUtils.proximoEvento(todosLosPedidos, ahora);
        if (proximo != null) {
            long llegada = proximo.getBloqueoInicioMillis() > 0 ? proximo.getBloqueoInicioMillis() : proximo.getInicioMillis();
            Bundle args = new Bundle();
            args.putString("pedidoId", proximo.getId());
            avisos.add(new AvisosDashboardAdapter.Aviso(
                    R.drawable.ic_calendar_month_24,
                    getString(R.string.dash_aviso_proximo_evento),
                    proximo.getNombreEvento() != null ? proximo.getNombreEvento() : getString(R.string.nav_agenda),
                    getString(R.string.dash_aviso_proximo_detalle,
                            FormatUtils.formatoFecha(proximo.getInicioMillis()),
                            FormatUtils.rangoHorario(llegada, proximo.getFinMillis()),
                            proximo.getClienteNombre() != null ? proximo.getClienteNombre() : ""),
                    getString(R.string.dash_aviso_ver_pedido),
                    R.id.pedidoDetalleFragment,
                    args));
        }

        int porRevisar = DashboardUtils.contarEnRevision(todosLosPedidos);
        if (porRevisar > 0) {
            avisos.add(new AvisosDashboardAdapter.Aviso(
                    R.drawable.ic_info_24,
                    getString(R.string.dash_aviso_revision_etiqueta),
                    getResources().getQuantityString(R.plurals.dash_aviso_revision_titulo, porRevisar, porRevisar),
                    getString(R.string.dash_aviso_revision_detalle),
                    getString(R.string.dash_aviso_ir_pedidos),
                    R.id.nav_pedidos,
                    null));
        }

        List<Pedido> apartados = DashboardUtils.apartadosVigentes(todosLosPedidos, ahora);
        if (!apartados.isEmpty()) {
            long vence = apartados.get(0).getVencimientoApartadoMillis();
            avisos.add(new AvisosDashboardAdapter.Aviso(
                    R.drawable.ic_hourglass_24,
                    getString(R.string.dash_aviso_apartados_etiqueta),
                    getResources().getQuantityString(R.plurals.dash_aviso_apartados_titulo, apartados.size(), apartados.size()),
                    getString(R.string.dash_aviso_apartados_detalle,
                            FormatUtils.formatoFecha(vence), FormatUtils.hora(vence)),
                    getString(R.string.dash_aviso_ir_pedidos),
                    R.id.nav_pedidos,
                    null));
        }

        if (avisos.isEmpty()) {
            avisos.add(new AvisosDashboardAdapter.Aviso(
                    R.drawable.ic_check_circle_24,
                    getString(R.string.dash_aviso_al_dia_etiqueta),
                    getString(R.string.dash_aviso_al_dia_titulo),
                    getString(R.string.dash_aviso_al_dia_detalle),
                    null, 0, null));
        }

        if (avisosAdapter.actualizar(avisos) && vpAvisos.getCurrentItem() >= avisos.size()) {
            vpAvisos.setCurrentItem(0, false);
        }
        actualizarContadorAvisos();
    }

    // ---------- Este mes ----------

    private void actualizarEsteMes(int anio, int mes0) {
        double[] resumen = FinanzasUtils.resumenMes(todosLosPagos, todosLosPedidos, anio, mes0);
        tvDashCobrado.setText(FormatUtils.formatoMoneda(resumen[0]));
        tvDashReembolsos.setText(FormatUtils.formatoMoneda(resumen[1]));
        tvDashNeto.setText(FormatUtils.formatoMoneda(resumen[2]));
        if (resumen[2] < 0) {
            tvDashNeto.setTextColor(BadgeUtils.colorError(requireContext()));
        } else {
            tvDashNeto.setTextColor(MaterialColors.getColor(tvDashNeto, com.google.android.material.R.attr.colorOnSurface));
        }
        tvDashPorCobrar.setText(FormatUtils.formatoMoneda(resumen[3]));
    }

    // ---------- Operación ----------

    private void actualizarOperacion(long ahora) {
        int[] porDia = DashboardUtils.eventosPorDiaSemana(todosLosPedidos, ahora);
        float[] valores = new float[7];
        String[] textos = new String[7];
        int total = 0;
        for (int i = 0; i < 7; i++) {
            valores[i] = porDia[i];
            textos[i] = String.valueOf(porDia[i]);
            total += porDia[i];
        }
        graficaEventosSemana.setDatos(DIAS_SEMANA, valores, textos, DashboardUtils.indiceDiaSemana(ahora));
        tvEventosSemanaTotal.setText(getResources().getQuantityString(R.plurals.dash_eventos_semana_total, total, total));

        int[] contadores = DashboardUtils.contadores(todosLosPedidos, ahora);
        int apartadosPorPagar = contadores[2];
        int equiposPorAsignar = contadores[3];
        int porRevisar = DashboardUtils.contarEnRevision(todosLosPedidos);

        tvDashApartadosPorPagar.setText(String.valueOf(apartadosPorPagar));
        tvDashEquiposPorAsignar.setText(String.valueOf(equiposPorAsignar));
        tvDashPorRevisar.setText(String.valueOf(porRevisar));
        pintarContador(tvDashEquiposPorAsignar, equiposPorAsignar > 0);
        pintarContador(tvDashPorRevisar, porRevisar > 0);
        pintarContador(tvDashApartadosPorPagar, false);
    }

    private void pintarContador(TextView tv, boolean alerta) {
        int fondo = MaterialColors.getColor(tv, alerta
                ? com.google.android.material.R.attr.colorErrorContainer
                : com.google.android.material.R.attr.colorSecondaryContainer);
        int texto = MaterialColors.getColor(tv, alerta
                ? com.google.android.material.R.attr.colorOnErrorContainer
                : com.google.android.material.R.attr.colorOnSecondaryContainer);
        tv.getBackground().mutate().setTint(fondo);
        tv.setTextColor(texto);
    }

    // ---------- Ingresos netos por mes ----------

    private void actualizarIngresosPorMes(int anio, int mes0) {
        float[] valores = new float[MESES_GRAFICA];
        String[] etiquetas = new String[MESES_GRAFICA];
        String[] textos = new String[MESES_GRAFICA];

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), LOCALE_MX);
        cal.clear();
        cal.set(anio, mes0, 1);
        cal.add(Calendar.MONTH, -(MESES_GRAFICA - 1));
        SimpleDateFormat formatoMes = new SimpleDateFormat("MMM", LOCALE_MX);
        formatoMes.setTimeZone(TimeZone.getTimeZone(Config.ZONA_HORARIA));

        double total = 0;
        for (int i = 0; i < MESES_GRAFICA; i++) {
            int a = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH);
            double neto = FinanzasUtils.resumenMes(todosLosPagos, todosLosPedidos, a, m)[2];
            total += neto;
            valores[i] = (float) Math.max(0, neto);
            textos[i] = montoCompacto(neto);
            String mes = formatoMes.format(new Date(cal.getTimeInMillis())).replace(".", "");
            etiquetas[i] = mes.isEmpty() ? mes : Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
            cal.add(Calendar.MONTH, 1);
        }
        graficaIngresosMes.setDatos(etiquetas, valores, textos, MESES_GRAFICA - 1);
        tvIngresosPeriodo.setText(getString(R.string.dash_ingresos_periodo, FormatUtils.formatoMoneda(total)));
    }

    private String montoCompacto(double monto) {
        double abs = Math.abs(monto);
        String signo = monto < 0 ? "-" : "";
        if (abs >= 1_000_000) return String.format(LOCALE_MX, "%s$%.1fM", signo, abs / 1_000_000);
        if (abs >= 1_000) return String.format(LOCALE_MX, "%s$%.1fk", signo, abs / 1_000);
        return String.format(LOCALE_MX, "%s$%.0f", signo, abs);
    }

    // ---------- Paquetes más rentados ----------

    private void actualizarPaquetes(long ahora) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        cal.setTimeInMillis(ahora);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -(MESES_GRAFICA - 1));
        long desde = com.example.propuestasonidosonic.util.TiempoUtils.inicioDelDia(cal.getTimeInMillis());

        List<DashboardUtils.ConteoPaquete> top = DashboardUtils.paquetesMasRentados(todosLosPedidos, desde, ahora, 5);
        if (top.isEmpty()) {
            graficaPaquetes.setVisibility(View.GONE);
            tvPaquetesVacio.setVisibility(View.VISIBLE);
            return;
        }
        graficaPaquetes.setVisibility(View.VISIBLE);
        tvPaquetesVacio.setVisibility(View.GONE);

        String[] nombres = new String[top.size()];
        float[] valores = new float[top.size()];
        String[] textos = new String[top.size()];
        for (int i = 0; i < top.size(); i++) {
            nombres[i] = top.get(i).nombre;
            valores[i] = top.get(i).cantidad;
            textos[i] = getResources().getQuantityString(R.plurals.dash_paquetes_rentas, top.get(i).cantidad, top.get(i).cantidad);
        }
        graficaPaquetes.setDatos(nombres, valores, textos);
    }

    // ---------- Lo más reciente ----------

    private void actualizarLoMasReciente(long ahora) {
        Pedido ultimoPed = ActividadRecienteUtils.ultimoPedido(todosLosPedidos);
        if (ultimoPed == null) {
            tvRecientePedidosTitulo.setText(R.string.reciente_pedidos_vacio);
            tvRecientePedidosSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecientePedidosSubtitulo.setVisibility(View.VISIBLE);
            tvRecientePedidosTitulo.setText(getString(R.string.reciente_pedido_titulo_formato,
                    ultimoPed.getNumero(),
                    ultimoPed.getNombreEvento() != null ? ultimoPed.getNombreEvento() : ""));
            String estadoTexto = estadoPedidoTexto(ultimoPed.getEstado());
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultimoPed.getFechaCreacion());
            tvRecientePedidosSubtitulo.setText(getString(R.string.reciente_pedido_subtitulo_formato,
                    ultimoPed.getClienteNombre() != null ? ultimoPed.getClienteNombre() : "",
                    estadoTexto,
                    tiempoTexto));
        }

        Pedido proxEvento = ActividadRecienteUtils.proximoEvento(todosLosPedidos, ahora);
        if (proxEvento == null) {
            tvRecienteAgendaTitulo.setText(R.string.reciente_agenda_vacio);
            tvRecienteAgendaSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteAgendaSubtitulo.setVisibility(View.VISIBLE);
            tvRecienteAgendaTitulo.setText(proxEvento.getNombreEvento() != null ? proxEvento.getNombreEvento() : "");
            String fechaTexto = FormatUtils.formatoFecha(proxEvento.getInicioMillis());
            long llegadaOInicio = proxEvento.getBloqueoInicioMillis() > 0 ? proxEvento.getBloqueoInicioMillis() : proxEvento.getInicioMillis();
            String rangoHorario = FormatUtils.rangoHorario(llegadaOInicio, proxEvento.getFinMillis());
            tvRecienteAgendaSubtitulo.setText(getString(R.string.reciente_agenda_subtitulo_formato,
                    fechaTexto,
                    rangoHorario,
                    proxEvento.getClienteNombre() != null ? proxEvento.getClienteNombre() : ""));
        }

        Equipo ultimoEq = ActividadRecienteUtils.ultimoEquipoAgregado(todosLosEquipos);
        if (ultimoEq == null) {
            tvRecienteInventarioTitulo.setText(R.string.reciente_inventario_vacio);
            tvRecienteInventarioSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteInventarioSubtitulo.setVisibility(View.VISIBLE);
            tvRecienteInventarioTitulo.setText(getString(R.string.reciente_inventario_titulo_formato,
                    ultimoEq.getNombre(),
                    ultimoEq.getNumeroUnidad()));
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultimoEq.getFechaAlta());
            tvRecienteInventarioSubtitulo.setText(getString(R.string.reciente_inventario_subtitulo_formato,
                    ultimoEq.getCategoria() != null ? ultimoEq.getCategoria() : "",
                    tiempoTexto));
        }

        Pago ultPago = ActividadRecienteUtils.ultimoPago(todosLosPagos);
        if (ultPago == null) {
            tvRecienteFinanzasTitulo.setText(R.string.reciente_finanzas_vacio);
            tvRecienteFinanzasSubtitulo.setVisibility(View.GONE);
        } else {
            tvRecienteFinanzasSubtitulo.setVisibility(View.VISIBLE);
            tvRecienteFinanzasTitulo.setText(getString(R.string.reciente_finanzas_titulo_formato,
                    obtenerTipoPagoTexto(ultPago.getTipo()),
                    FormatUtils.formatoMoneda(ultPago.getMonto())));
            long numeroPedido = 0;
            if (ultPago.getPedidoId() != null) {
                for (Pedido p : todosLosPedidos) {
                    if (ultPago.getPedidoId().equals(p.getId())) {
                        numeroPedido = p.getNumero();
                        break;
                    }
                }
            }
            String tiempoTexto = FormatUtils.tiempoRelativoFormateado(requireContext(), ahora, ultPago.getFecha());
            tvRecienteFinanzasSubtitulo.setText(getString(R.string.reciente_finanzas_subtitulo_formato,
                    numeroPedido,
                    tiempoTexto));
        }
    }

    private String obtenerTipoPagoTexto(String tipo) {
        if (Pago.TIPO_ANTICIPO.equals(tipo)) return "Anticipo";
        if (Pago.TIPO_ABONO.equals(tipo)) return "Abono";
        if (Pago.TIPO_LIQUIDACION.equals(tipo)) return "Pago total";
        if (Pago.TIPO_REEMBOLSO.equals(tipo)) return "Reembolso";
        return tipo != null ? tipo : "Pago";
    }

    private String estadoPedidoTexto(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case Pedido.ESTADO_APARTADO:
                return getString(R.string.badge_apartado);
            case Pedido.ESTADO_EN_REVISION:
                return getString(R.string.badge_en_revision);
            case Pedido.ESTADO_CONFIRMADO:
                return getString(R.string.badge_confirmado);
            case Pedido.ESTADO_PAGADO:
                return getString(R.string.badge_pagado);
            case Pedido.ESTADO_COMPLETADO:
                return getString(R.string.badge_completado);
            case Pedido.ESTADO_CANCELADO:
                return getString(R.string.badge_cancelado);
            case Pedido.ESTADO_RECHAZADO:
                return getString(R.string.badge_rechazado);
            case Pedido.ESTADO_VENCIDO:
                return getString(R.string.badge_vencido);
            default:
                return estado;
        }
    }
}
