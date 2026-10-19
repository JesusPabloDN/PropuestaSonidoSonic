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
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventoAsistenteAdapter extends RecyclerView.Adapter<EventoAsistenteAdapter.EventoViewHolder> {

    public interface OnEventoClickListener {
        void onEventoClick(Pedido pedido);
    }

    private final List<Pedido> pedidos = new ArrayList<>();
    private final Map<String, Evento> eventosPorId = new HashMap<>();
    private long ahora;
    private boolean esPestañaPasados;
    private final OnEventoClickListener listener;

    public EventoAsistenteAdapter(OnEventoClickListener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Pedido> nuevosPedidos, Map<String, Evento> eventos, long ahora, boolean esPestañaPasados) {
        this.pedidos.clear();
        if (nuevosPedidos != null) {
            this.pedidos.addAll(nuevosPedidos);
        }
        this.eventosPorId.clear();
        if (eventos != null) {
            this.eventosPorId.putAll(eventos);
        }
        this.ahora = ahora;
        this.esPestañaPasados = esPestañaPasados;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evento_asistente, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Pedido pedido = pedidos.get(position);
        Evento evento = (pedido.getEventoId() != null) ? eventosPorId.get(pedido.getEventoId()) : null;
        holder.bind(pedido, evento, ahora, esPestañaPasados, listener);
    }

    @Override
    public int getItemCount() {
        return pedidos.size();
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombreEvento;
        private final TextView tvBadgeHoy;
        private final TextView tvBadgeChecklist;
        private final TextView tvFechaEvento;
        private final TextView tvHorariosEvento;
        private final TextView tvDireccionEvento;
        private final TextView tvPaqueteEvento;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEvento = itemView.findViewById(R.id.tvNombreEvento);
            tvBadgeHoy = itemView.findViewById(R.id.tvBadgeHoy);
            tvBadgeChecklist = itemView.findViewById(R.id.tvBadgeChecklist);
            tvFechaEvento = itemView.findViewById(R.id.tvFechaEvento);
            tvHorariosEvento = itemView.findViewById(R.id.tvHorariosEvento);
            tvDireccionEvento = itemView.findViewById(R.id.tvDireccionEvento);
            tvPaqueteEvento = itemView.findViewById(R.id.tvPaqueteEvento);
        }

        public void bind(Pedido p, Evento ev, long ahora, boolean esPestañaPasados, OnEventoClickListener listener) {
            Context ctx = itemView.getContext();
            tvNombreEvento.setText(p.getNombreEvento());
            tvFechaEvento.setText(FormatUtils.fechaLarga(p.getInicioMillis()));

            String horaLlegada = FormatUtils.hora(AsistenteUtils.llegadaMillis(p));
            String horaIni = FormatUtils.hora(p.getInicioMillis());
            String horaFin = FormatUtils.hora(p.getFinMillis());
            tvHorariosEvento.setText(ctx.getString(R.string.horario_asistente_formato,
                    horaLlegada, horaIni, horaFin));

            tvDireccionEvento.setText(p.getDireccion());
            tvPaqueteEvento.setText(p.getPaqueteNombre());

            // Insignia "Hoy"
            if (AsistenteUtils.esHoy(p, ahora)) {
                tvBadgeHoy.setVisibility(View.VISIBLE);
                tvBadgeHoy.setText(R.string.badge_hoy);
                BadgeUtils.pintar(tvBadgeHoy,
                        ContextCompat.getColor(ctx, R.color.status_disponible),
                        ContextCompat.getColor(ctx, R.color.status_disponible_container));
            } else {
                tvBadgeHoy.setVisibility(View.GONE);
            }

            // Insignia de checklist (para pasados o si ya tiene checklist)
            boolean tieneChecklist = (ev != null && ev.getChecklist() != null && !ev.getChecklist().isEmpty());
            if (esPestañaPasados || tieneChecklist) {
                String estado = AsistenteUtils.estadoChecklist(ev != null ? ev.getChecklist() : null);
                int colorTxt, colorBg;
                if ("Completo".equals(estado)) {
                    colorTxt = ContextCompat.getColor(ctx, R.color.status_disponible);
                    colorBg = ContextCompat.getColor(ctx, R.color.status_disponible_container);
                } else if ("En evento".equals(estado)) {
                    colorTxt = ContextCompat.getColor(ctx, R.color.status_rentado);
                    colorBg = ContextCompat.getColor(ctx, R.color.status_rentado_container);
                } else if ("Pendiente".equals(estado)) {
                    colorTxt = ContextCompat.getColor(ctx, R.color.status_pendiente);
                    colorBg = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                } else {
                    colorTxt = ContextCompat.getColor(ctx, R.color.md_theme_light_onSurfaceVariant);
                    colorBg = ContextCompat.getColor(ctx, R.color.md_theme_light_surfaceVariant);
                }
                tvBadgeChecklist.setVisibility(View.VISIBLE);
                tvBadgeChecklist.setText(estado);
                BadgeUtils.pintar(tvBadgeChecklist, colorTxt, colorBg);
            } else {
                tvBadgeChecklist.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventoClick(p);
                }
            });
        }
    }
}
