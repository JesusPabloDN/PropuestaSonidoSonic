package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class PedidoClienteAdapter extends RecyclerView.Adapter<PedidoClienteAdapter.ViewHolder> {

    public interface OnPedidoClienteClickListener {
        void onPedidoClick(Pedido pedido);
    }

    private final List<Pedido> items = new ArrayList<>();
    private final OnPedidoClienteClickListener listener;
    private long ahora;

    public PedidoClienteAdapter(OnPedidoClienteClickListener listener, long ahora) {
        this.listener = listener;
        this.ahora = ahora;
    }

    public void setAhora(long ahora) {
        this.ahora = ahora;
    }

    public void submitList(List<Pedido> nuevos) {
        items.clear();
        if (nuevos != null) {
            items.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido_cliente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pedido pedido = items.get(position);
        holder.bind(pedido, listener, ahora);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreEvento;
        private final TextView tvFolio;
        private final TextView tvEstadoBadge;
        private final TextView tvFechaHorario;
        private final TextView tvPaquete;
        private final TextView tvTotal;
        private final TextView tvPagoInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEvento = itemView.findViewById(R.id.tvNombreEvento);
            tvFolio = itemView.findViewById(R.id.tvFolio);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvFechaHorario = itemView.findViewById(R.id.tvFechaHorario);
            tvPaquete = itemView.findViewById(R.id.tvPaquete);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvPagoInfo = itemView.findViewById(R.id.tvPagoInfo);
        }

        public void bind(Pedido pedido, OnPedidoClienteClickListener listener, long ahora) {
            Context ctx = itemView.getContext();

            tvNombreEvento.setText(pedido.getNombreEvento() != null ? pedido.getNombreEvento() : "");
            tvFolio.setText("#" + pedido.getNumero());

            // Estado y Badge
            String estado = pedido.getEstado();
            String etiqueta = PedidosClienteUtils.etiquetaEstado(estado);
            BadgeUtils.pintarEstadoPedido(tvEstadoBadge, estado);
            tvEstadoBadge.setText(etiqueta);

            // Fecha y Horario
            if (pedido.getInicioMillis() > 0 && pedido.getFinMillis() > 0) {
                String fecha = FormatUtils.formatoFecha(pedido.getInicioMillis());
                String horario = FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis());
                tvFechaHorario.setText(fecha + " · " + horario);
                tvFechaHorario.setVisibility(View.VISIBLE);
            } else {
                tvFechaHorario.setVisibility(View.GONE);
            }

            // Paquete
            if (pedido.getPaqueteNombre() != null && !pedido.getPaqueteNombre().isEmpty()) {
                tvPaquete.setText(pedido.getPaqueteNombre());
                tvPaquete.setVisibility(View.VISIBLE);
            } else {
                tvPaquete.setVisibility(View.GONE);
            }

            // Total
            tvTotal.setText(FormatUtils.formatoMoneda(pedido.getTotal()));

            // Estado del pago: el cliente paga directamente al negocio
            if (pedido.isPagoConfirmado()) {
                tvPagoInfo.setVisibility(View.VISIBLE);
                tvPagoInfo.setText(R.string.estado_pago_pagado);
                tvPagoInfo.setTextColor(BadgeUtils.colorAtributo(ctx, R.attr.colorEstadoDisponible));
            } else if (EstadosPedido.esAceptado(estado)) {
                tvPagoInfo.setVisibility(View.VISIBLE);
                tvPagoInfo.setText(R.string.estado_pago_pendiente_corto);
                tvPagoInfo.setTextColor(MaterialColors.getColor(tvPagoInfo,
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                tvPagoInfo.setVisibility(View.GONE);
            }

            // Clic en la tarjeta
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPedidoClick(pedido);
                }
            });
        }
    }
}
