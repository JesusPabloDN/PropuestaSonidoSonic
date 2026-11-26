package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder> {

    public static final int TAB_POR_APROBAR = 0;
    public static final int TAB_CONFIRMADOS = 1;
    public static final int TAB_POR_COBRAR = 2;
    public static final int TAB_HISTORIAL = 3;

    public interface OnPedidoClickListener {
        void onPedidoClick(Pedido pedido);
    }

    private final List<Pedido> lista = new ArrayList<>();
    private final OnPedidoClickListener listener;
    private int tabActual = TAB_POR_APROBAR;

    public PedidoAdapter(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Pedido> nuevos, int tabActual) {
        this.tabActual = tabActual;
        this.lista.clear();
        if (nuevos != null) {
            this.lista.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        holder.bind(lista.get(position), tabActual, listener);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreEvento;
        private final TextView tvFolio;
        private final TextView tvEstadoBadge;
        private final TextView tvFechaHorario;
        private final TextView tvPaquete;
        private final TextView tvCliente;
        private final TextView tvLineaDinero;
        private final TextView tvVencimiento;

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEvento = itemView.findViewById(R.id.tvNombreEvento);
            tvFolio = itemView.findViewById(R.id.tvFolio);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvFechaHorario = itemView.findViewById(R.id.tvFechaHorario);
            tvPaquete = itemView.findViewById(R.id.tvPaquete);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvLineaDinero = itemView.findViewById(R.id.tvLineaDinero);
            tvVencimiento = itemView.findViewById(R.id.tvVencimiento);
        }

        public void bind(Pedido pedido, int tabActual, OnPedidoClickListener listener) {
            Context ctx = itemView.getContext();

            tvNombreEvento.setText(pedido.getNombreEvento());
            tvFolio.setText(ctx.getString(R.string.folio_formato, pedido.getNumero()));

            // Insignia de estado
            BadgeUtils.pintarEstadoPedido(tvEstadoBadge, pedido.getEstado());

            // Fecha y Horario
            String fechaStr = FormatUtils.formatoFechaCompleta(pedido.getInicioMillis()).replace(".", "");
            String rangoStr = FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis());
            tvFechaHorario.setText(fechaStr + " · " + rangoStr);

            // Paquete y Cliente
            tvPaquete.setText(pedido.getPaqueteNombre() != null ? pedido.getPaqueteNombre() : "");
            tvCliente.setText(pedido.getClienteNombre() != null ? pedido.getClienteNombre() : "");

            // Línea de dinero y vencimiento según la pestaña
            String monto = FormatUtils.formatoMoneda(pedido.getTotal());
            if (tabActual == TAB_POR_APROBAR) {
                tvLineaDinero.setText(ctx.getString(R.string.linea_monto_a_pagar, monto));

                // La solicitud vence cuando empieza el evento sin haber sido aprobada
                long restanteMs = pedido.getInicioMillis() - System.currentTimeMillis();
                tvVencimiento.setVisibility(View.VISIBLE);
                if (restanteMs <= 0) {
                    tvVencimiento.setText(R.string.badge_vencido);
                    tvVencimiento.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDanado));
                } else {
                    tvVencimiento.setText(ctx.getString(R.string.vence_en_formato, FormatUtils.tiempoRestante(restanteMs)));
                    if (restanteMs < 24 * 3600_000L) {
                        tvVencimiento.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDanado));
                    } else {
                        tvVencimiento.setTextColor(MaterialColors.getColor(tvVencimiento,
                                com.google.android.material.R.attr.colorOnSurfaceVariant));
                    }
                }
            } else if (tabActual == TAB_POR_COBRAR) {
                tvLineaDinero.setText(ctx.getString(R.string.linea_por_cobrar, monto));
                tvVencimiento.setVisibility(View.GONE);
            } else {
                tvLineaDinero.setText(ctx.getString(pedido.isPagoConfirmado()
                        ? R.string.linea_total_pagado : R.string.linea_total_pendiente, monto));
                tvVencimiento.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPedidoClick(pedido);
                }
            });
        }
    }
}
