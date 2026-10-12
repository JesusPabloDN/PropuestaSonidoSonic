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

    public static final int TAB_POR_PAGAR = 0;
    public static final int TAB_CONFIRMADOS = 1;
    public static final int TAB_HISTORIAL = 2;

    public interface OnPedidoClickListener {
        void onPedidoClick(Pedido pedido);
    }

    private final List<Pedido> lista = new ArrayList<>();
    private final OnPedidoClickListener listener;
    private int tabActual = TAB_POR_PAGAR;

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
            pintarBadgeEstado(ctx, pedido.getEstado());

            // Fecha y Horario
            String fechaStr = FormatUtils.formatoFechaCompleta(pedido.getInicioMillis()).replace(".", "");
            String rangoStr = FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis());
            tvFechaHorario.setText(fechaStr + " · " + rangoStr);

            // Paquete y Cliente
            tvPaquete.setText(pedido.getPaqueteNombre() != null ? pedido.getPaqueteNombre() : "");
            tvCliente.setText(pedido.getClienteNombre() != null ? pedido.getClienteNombre() : "");

            // Línea de dinero y vencimiento según la pestaña
            if (tabActual == TAB_POR_PAGAR) {
                double anticipoPendiente = Math.max(0.0, pedido.getAnticipoRequerido() - pedido.getAnticipoPagado());
                tvLineaDinero.setText(ctx.getString(R.string.anticipo_pendiente_formato,
                        FormatUtils.formatoMoneda(anticipoPendiente)));

                long restanteMs = pedido.getVencimientoApartadoMillis() - System.currentTimeMillis();
                tvVencimiento.setVisibility(View.VISIBLE);
                String restanteTexto = FormatUtils.tiempoRestante(restanteMs);
                if (restanteMs <= 0) {
                    tvVencimiento.setText(R.string.badge_vencido);
                    tvVencimiento.setTextColor(ContextCompat.getColor(ctx, R.color.status_danado));
                } else {
                    tvVencimiento.setText(ctx.getString(R.string.vence_en_formato, restanteTexto));
                    if (restanteMs < 2 * 3600_000L) {
                        tvVencimiento.setTextColor(ContextCompat.getColor(ctx, R.color.status_danado));
                    } else {
                        tvVencimiento.setTextColor(MaterialColors.getColor(tvVencimiento,
                                com.google.android.material.R.attr.colorOnSurfaceVariant));
                    }
                }
            } else if (tabActual == TAB_CONFIRMADOS) {
                double saldo = Math.max(0.0, pedido.getTotal() - pedido.getAnticipoPagado());
                tvLineaDinero.setText(ctx.getString(R.string.anticipo_pagado_saldo_formato,
                        FormatUtils.formatoMoneda(saldo)));
                tvVencimiento.setVisibility(View.GONE);
            } else {
                tvLineaDinero.setText("Total: " + FormatUtils.formatoMoneda(pedido.getTotal()));
                tvVencimiento.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPedidoClick(pedido);
                }
            });
        }

        private void pintarBadgeEstado(Context ctx, String estado) {
            if (estado == null) estado = Pedido.ESTADO_APARTADO;

            int colorTexto;
            int colorFondo;
            int textoRes;

            switch (estado) {
                case Pedido.ESTADO_APARTADO:
                    textoRes = R.string.badge_apartado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                    break;
                case Pedido.ESTADO_EN_REVISION:
                    textoRes = R.string.badge_en_revision;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                    break;
                case Pedido.ESTADO_CONFIRMADO:
                    textoRes = R.string.badge_confirmado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                    break;
                case Pedido.ESTADO_PAGADO:
                    textoRes = R.string.badge_pagado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                    break;
                case Pedido.ESTADO_COMPLETADO:
                    textoRes = R.string.badge_completado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_rentado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_rentado_container);
                    break;
                case Pedido.ESTADO_CANCELADO:
                    textoRes = R.string.badge_cancelado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                    break;
                case Pedido.ESTADO_RECHAZADO:
                    textoRes = R.string.badge_rechazado;
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                    break;
                case Pedido.ESTADO_VENCIDO:
                default:
                    textoRes = R.string.badge_vencido;
                    colorTexto = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    colorFondo = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorSurfaceVariant);
                    break;
            }

            tvEstadoBadge.setText(textoRes);
            BadgeUtils.pintar(tvEstadoBadge, colorTexto, colorFondo);
        }
    }
}
