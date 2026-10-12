package com.example.propuestasonidosonic.ui.socio;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.domain.FinanzasUtils;
import com.example.propuestasonidosonic.domain.ReciboUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FinanzasFragment extends Fragment {

    private static final String KEY_ANIO = "anio";
    private static final String KEY_MES0 = "mes0";
    private static final String KEY_FILTRO = "filtro";

    private PagoRepository pagoRepository;
    private PedidoRepository pedidoRepository;

    private int anio;
    private int mes0;
    private int filtroActual = FinanzasUtils.FILTRO_TODOS;

    private ImageButton btnMesAnterior;
    private TextView tvMesFinanzas;
    private ImageButton btnMesSiguiente;
    private MaterialButton btnMesActual;

    private TextView tvKpiCobrado;
    private TextView tvKpiReembolsos;
    private TextView tvKpiNeto;
    private TextView tvKpiPorCobrar;

    private MaterialButtonToggleGroup toggleFiltroFinanzas;
    private TextView tvEmptyMovimientos;
    private LinearLayout layoutMovimientosContainer;

    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Pedido> todosLosPedidos = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
        anio = cal.get(Calendar.YEAR);
        mes0 = cal.get(Calendar.MONTH);

        if (savedInstanceState != null) {
            anio = savedInstanceState.getInt(KEY_ANIO, anio);
            mes0 = savedInstanceState.getInt(KEY_MES0, mes0);
            filtroActual = savedInstanceState.getInt(KEY_FILTRO, filtroActual);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finanzas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pagoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPagoRepository();
        pedidoRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPedidoRepository();

        vincularVistas(view);
        configurarEventos();
        observarDatos();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ANIO, anio);
        outState.putInt(KEY_MES0, mes0);
        outState.putInt(KEY_FILTRO, filtroActual);
    }

    private void vincularVistas(View view) {
        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        tvMesFinanzas = view.findViewById(R.id.tvMesFinanzas);
        btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        btnMesActual = view.findViewById(R.id.btnMesActual);

        tvKpiCobrado = view.findViewById(R.id.tvKpiCobrado);
        tvKpiReembolsos = view.findViewById(R.id.tvKpiReembolsos);
        tvKpiNeto = view.findViewById(R.id.tvKpiNeto);
        tvKpiPorCobrar = view.findViewById(R.id.tvKpiPorCobrar);

        toggleFiltroFinanzas = view.findViewById(R.id.toggleFiltroFinanzas);
        tvEmptyMovimientos = view.findViewById(R.id.tvEmptyMovimientos);
        layoutMovimientosContainer = view.findViewById(R.id.layoutMovimientosContainer);

        // Configurar selección del toggle según filtroActual
        if (filtroActual == FinanzasUtils.FILTRO_INGRESOS) {
            toggleFiltroFinanzas.check(R.id.btnFiltroIngresos);
        } else if (filtroActual == FinanzasUtils.FILTRO_REEMBOLSOS) {
            toggleFiltroFinanzas.check(R.id.btnFiltroReembolsos);
        } else {
            toggleFiltroFinanzas.check(R.id.btnFiltroTodos);
        }
    }

    private void configurarEventos() {
        btnMesAnterior.setOnClickListener(v -> {
            mes0--;
            if (mes0 < 0) {
                mes0 = 11;
                anio--;
            }
            actualizarUI();
        });

        btnMesSiguiente.setOnClickListener(v -> {
            mes0++;
            if (mes0 > 11) {
                mes0 = 0;
                anio++;
            }
            actualizarUI();
        });

        btnMesActual.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(Config.ZONA_HORARIA), Locale.ROOT);
            anio = cal.get(Calendar.YEAR);
            mes0 = cal.get(Calendar.MONTH);
            actualizarUI();
        });

        toggleFiltroFinanzas.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFiltroIngresos) {
                filtroActual = FinanzasUtils.FILTRO_INGRESOS;
            } else if (checkedId == R.id.btnFiltroReembolsos) {
                filtroActual = FinanzasUtils.FILTRO_REEMBOLSOS;
            } else {
                filtroActual = FinanzasUtils.FILTRO_TODOS;
            }
            actualizarListaMovimientos();
        });
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
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        tvMesFinanzas.setText(FormatUtils.mesAnio(anio, mes0));

        // Recalcular KPIs con FinanzasUtils
        double[] resumen = FinanzasUtils.resumenMes(todosLosPagos, todosLosPedidos, anio, mes0);
        double cobrado = resumen[0];
        double reembolsos = resumen[1];
        double neto = resumen[2];
        double porCobrar = resumen[3];

        tvKpiCobrado.setText(FormatUtils.formatoMoneda(cobrado));
        tvKpiReembolsos.setText(FormatUtils.formatoMoneda(reembolsos));
        tvKpiNeto.setText(FormatUtils.formatoMoneda(neto));
        if (neto < 0) {
            tvKpiNeto.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(requireContext()));
        } else {
            tvKpiNeto.setTextColor(MaterialColors.getColor(tvKpiNeto, com.google.android.material.R.attr.colorOnSurface));
        }
        tvKpiPorCobrar.setText(FormatUtils.formatoMoneda(porCobrar));

        actualizarListaMovimientos();
    }

    private void actualizarListaMovimientos() {
        if (!isAdded()) return;

        List<Pago> movimientos = FinanzasUtils.movimientosDelMes(todosLosPagos, anio, mes0, filtroActual);
        layoutMovimientosContainer.removeAllViews();

        if (movimientos.isEmpty()) {
            tvEmptyMovimientos.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMovimientos.setVisibility(View.GONE);

        // Mapa de pedidos por ID para resolver evento y cliente
        Map<String, Pedido> mapaPedidos = new HashMap<>();
        for (Pedido p : todosLosPedidos) {
            if (p != null) {
                mapaPedidos.put(p.getId(), p);
            }
        }

        Context ctx = requireContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Pago pago : movimientos) {
            View itemView = inflater.inflate(R.layout.item_movimiento_finanzas, layoutMovimientosContainer, false);
            TextView tvFolio = itemView.findViewById(R.id.tvMovimientoFolio);
            TextView tvTipo = itemView.findViewById(R.id.tvMovimientoTipo);
            TextView tvEventoCliente = itemView.findViewById(R.id.tvMovimientoEventoCliente);
            TextView tvFechaMetodo = itemView.findViewById(R.id.tvMovimientoFechaMetodo);
            TextView tvMonto = itemView.findViewById(R.id.tvMovimientoMonto);

            tvFolio.setText(pago.getFolio() != null ? pago.getFolio() : "");
            tvTipo.setText(ReciboUtils.tituloRecibo(pago.getTipo()).replace("Recibo de ", "").replace("Comprobante de ", ""));

            Pedido ped = mapaPedidos.get(pago.getPedidoId());
            if (ped != null) {
                String nomEvento = ped.getNombreEvento() != null ? ped.getNombreEvento() : "";
                String nomCliente = ped.getClienteNombre() != null ? ped.getClienteNombre() : "";
                if (!nomEvento.isEmpty() && !nomCliente.isEmpty()) {
                    tvEventoCliente.setText(nomEvento + " · " + nomCliente);
                } else if (!nomEvento.isEmpty()) {
                    tvEventoCliente.setText(nomEvento);
                } else {
                    tvEventoCliente.setText(nomCliente);
                }
            } else {
                tvEventoCliente.setText("Pedido " + (pago.getPedidoId() != null ? pago.getPedidoId() : ""));
            }

            String metodo = pago.getMetodo() != null ? pago.getMetodo() : "";
            tvFechaMetodo.setText(FormatUtils.fechaHoraCorta(pago.getFecha()) + " · " + metodo);

            boolean esReembolso = Pago.TIPO_REEMBOLSO.equals(pago.getTipo());
            if (esReembolso) {
                tvMonto.setText("− " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvMonto.setTextColor(com.example.propuestasonidosonic.util.BadgeUtils.colorError(ctx));
            } else {
                tvMonto.setText("+ " + FormatUtils.formatoMoneda(pago.getMonto()));
                tvMonto.setTextColor(ContextCompat.getColor(ctx, R.color.status_disponible));
            }

            itemView.setOnClickListener(v -> {
                if (pago.getPedidoId() != null) {
                    Bundle args = new Bundle();
                    args.putString("pedidoId", pago.getPedidoId());
                    Navigation.findNavController(requireView()).navigate(R.id.action_finanzas_to_detalle, args);
                }
            });

            layoutMovimientosContainer.addView(itemView);
        }
    }
}
