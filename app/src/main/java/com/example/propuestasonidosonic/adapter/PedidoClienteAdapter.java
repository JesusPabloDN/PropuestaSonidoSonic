package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.domain.PedidosClienteUtils;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class PedidoClienteAdapter extends RecyclerView.Adapter<PedidoClienteAdapter.ViewHolder> {

    public interface OnPedidoClienteClickListener {
        void onPedidoClick(Pedido pedido);
        void onPagarAnticipoClick(Pedido pedido);
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
        private final TextView tvSaldoPendiente;
        private final LinearLayout layoutApartado;
        private final TextView tvVenceEn;
        private final MaterialButton btnPagarAnticipo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEvento = itemView.findViewById(R.id.tvNombreEvento);
            tvFolio = itemView.findViewById(R.id.tvFolio);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvFechaHorario = itemView.findViewById(R.id.tvFechaHorario);
            tvPaquete = itemView.findViewById(R.id.tvPaquete);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvSaldoPendiente = itemView.findViewById(R.id.tvSaldoPendiente);
            layoutApartado = itemView.findViewById(R.id.layoutApartado);
            tvVenceEn = itemView.findViewById(R.id.tvVenceEn);
            btnPagarAnticipo = itemView.findViewById(R.id.btnPagarAnticipo);
        }

        public void bind(Pedido pedido, OnPedidoClienteClickListener listener, long ahora) {
            Context ctx = itemView.getContext();

            tvNombreEvento.setText(pedido.getNombreEvento() != null ? pedido.getNombreEvento() : "");
            tvFolio.setText("#" + pedido.getNumero());

            // Estado y Badge
            String estado = pedido.getEstado();
            String etiqueta = PedidosClienteUtils.etiquetaEstado(estado);
            tvEstadoBadge.setText(etiqueta);
            pintarBadge(ctx, estado);

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

            // Saldo por pagar si aplica
            boolean esConfirmadoORevision = Pedido.ESTADO_CONFIRMADO.equals(estado)
                    || Pedido.ESTADO_PAGADO.equals(estado)
                    || Pedido.ESTADO_EN_REVISION.equals(estado);
            if (esConfirmadoORevision && pedido.getSaldoPendiente() > 0) {
                tvSaldoPendiente.setVisibility(View.VISIBLE);
                tvSaldoPendiente.setText(ctx.getString(R.string.saldo_por_pagar_formato,
                        FormatUtils.formatoMoneda(pedido.getSaldoPendiente())));
            } else {
                tvSaldoPendiente.setVisibility(View.GONE);
            }

            // Apartado info (vence en y pagar anticipo)
            if (PedidosClienteUtils.puedePagarAnticipo(pedido, ahora)) {
                layoutApartado.setVisibility(View.VISIBLE);
                tvVenceEn.setText(PedidosClienteUtils.textoVencimiento(pedido.getVencimientoApartadoMillis(), ahora));
                tvVenceEn.setTextColor(ContextCompat.getColor(ctx, R.color.status_pendiente));
                btnPagarAnticipo.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPagarAnticipoClick(pedido);
                    }
                });
            } else {
                layoutApartado.setVisibility(View.GONE);
            }

            // Clic en la tarjeta
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPedidoClick(pedido);
                }
            });
        }

        private void pintarBadge(Context ctx, String estado) {
            if (estado == null) estado = "";
            int colorTexto;
            int colorFondo;

            switch (estado) {
                case Pedido.ESTADO_APARTADO:
                case Pedido.ESTADO_EN_REVISION:
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                    break;
                case Pedido.ESTADO_CONFIRMADO:
                case Pedido.ESTADO_PAGADO:
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_disponible);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                    break;
                case Pedido.ESTADO_COMPLETADO:
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_rentado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_rentado_container);
                    break;
                case Pedido.ESTADO_CANCELADO:
                case Pedido.ESTADO_RECHAZADO:
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                    break;
                default:
                    colorTexto = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    colorFondo = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorSurfaceVariant);
                    break;
            }

            BadgeUtils.pintar(tvEstadoBadge, colorTexto, colorFondo);
        }
    }
}
