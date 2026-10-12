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
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventoAgendaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TIPO_HEADER = 0;
    public static final int TIPO_EVENTO = 1;

    public interface OnEventoClickListener {
        void onEventoClick(Pedido pedido);
    }

    public static class ElementoAgenda {
        public final int tipo;
        public final String claveDia;
        public final String textoHeader;
        public final Pedido pedido;

        public static ElementoAgenda header(String claveDia, String textoHeader) {
            return new ElementoAgenda(TIPO_HEADER, claveDia, textoHeader, null);
        }

        public static ElementoAgenda evento(Pedido pedido) {
            String clave = pedido != null ? TiempoUtils.claveDia(pedido.getInicioMillis()) : "";
            return new ElementoAgenda(TIPO_EVENTO, clave, null, pedido);
        }

        private ElementoAgenda(int tipo, String claveDia, String textoHeader, Pedido pedido) {
            this.tipo = tipo;
            this.claveDia = claveDia;
            this.textoHeader = textoHeader;
            this.pedido = pedido;
        }
    }

    private final List<ElementoAgenda> elementos = new ArrayList<>();
    private final UsuarioRepository usuarioRepository;
    private final OnEventoClickListener listener;

    public EventoAgendaAdapter(UsuarioRepository usuarioRepository, OnEventoClickListener listener) {
        this.usuarioRepository = usuarioRepository;
        this.listener = listener;
    }

    public void setElementos(List<ElementoAgenda> nuevos) {
        this.elementos.clear();
        if (nuevos != null) {
            this.elementos.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    public void setEventosDelDia(List<Pedido> pedidosDia) {
        this.elementos.clear();
        if (pedidosDia != null) {
            for (Pedido p : pedidosDia) {
                this.elementos.add(ElementoAgenda.evento(p));
            }
        }
        notifyDataSetChanged();
    }

    public void setEventosDelMesAgrupados(Map<String, List<Pedido>> mapaDiaPedidos) {
        this.elementos.clear();
        if (mapaDiaPedidos != null) {
            for (Map.Entry<String, List<Pedido>> entry : mapaDiaPedidos.entrySet()) {
                List<Pedido> lista = entry.getValue();
                if (lista != null && !lista.isEmpty()) {
                    long primerInicio = lista.get(0).getInicioMillis();
                    String headerTexto = FormatUtils.fechaEncabezadoMes(primerInicio);
                    this.elementos.add(ElementoAgenda.header(entry.getKey(), headerTexto));
                    for (Pedido p : lista) {
                        this.elementos.add(ElementoAgenda.evento(p));
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public int obtenerPosicionDia(String claveDia) {
        if (claveDia == null) return -1;
        for (int i = 0; i < elementos.size(); i++) {
            ElementoAgenda e = elementos.get(i);
            if (claveDia.equals(e.claveDia)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return elementos.get(position).tipo;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_HEADER) {
            View view = inflater.inflate(R.layout.item_header_fecha_agenda, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_evento_agenda, parent, false);
            return new EventoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ElementoAgenda item = elementos.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.textoHeader);
        } else if (holder instanceof EventoViewHolder) {
            ((EventoViewHolder) holder).bind(item.pedido, usuarioRepository, listener);
        }
    }

    @Override
    public int getItemCount() {
        return elementos.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeaderFecha;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderFecha = itemView.findViewById(R.id.tvHeaderFecha);
        }

        public void bind(String texto) {
            tvHeaderFecha.setText(texto != null ? texto : "");
        }
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRangoHorario;
        private final TextView tvNombreEvento;
        private final TextView tvEstadoBadge;
        private final TextView tvPaqueteCliente;
        private final TextView tvPersonalAsignado;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRangoHorario = itemView.findViewById(R.id.tvRangoHorario);
            tvNombreEvento = itemView.findViewById(R.id.tvNombreEvento);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvPaqueteCliente = itemView.findViewById(R.id.tvPaqueteCliente);
            tvPersonalAsignado = itemView.findViewById(R.id.tvPersonalAsignado);
        }

        public void bind(Pedido pedido, UsuarioRepository usuarioRepo, OnEventoClickListener listener) {
            if (pedido == null) return;
            Context ctx = itemView.getContext();

            // Rango de horas
            tvRangoHorario.setText(FormatUtils.rangoHorario(pedido.getInicioMillis(), pedido.getFinMillis()));

            // Nombre del evento
            tvNombreEvento.setText(pedido.getNombreEvento() != null ? pedido.getNombreEvento() : "");

            // Badge de estado
            pintarBadgeEstado(ctx, pedido.getEstado());

            // Paquete · Cliente
            String paquete = pedido.getPaqueteNombre() != null ? pedido.getPaqueteNombre() : "";
            String cliente = pedido.getClienteNombre() != null ? pedido.getClienteNombre() : "";
            String paqueteCliente = paquete.isEmpty() ? cliente : (cliente.isEmpty() ? paquete : paquete + " · " + cliente);
            tvPaqueteCliente.setText(paqueteCliente);

            // Personal asignado (nombres desde UsuarioRepository)
            List<String> opIds = pedido.getOperadorIds();
            if (opIds != null && !opIds.isEmpty() && usuarioRepo != null) {
                StringBuilder sb = new StringBuilder("Personal: ");
                for (int i = 0; i < opIds.size(); i++) {
                    String uid = opIds.get(i);
                    Usuario u = usuarioRepo.getUsuarioPorId(uid);
                    if (u != null) {
                        sb.append(u.getNombre());
                        if (Usuario.ROL_SOCIO.equals(u.getRol())) {
                            sb.append(" (Socio)");
                        } else if (Usuario.ROL_ASISTENTE.equals(u.getRol())) {
                            sb.append(" (Asistente)");
                        }
                    } else {
                        sb.append(uid);
                    }
                    if (i < opIds.size() - 1) sb.append(", ");
                }
                tvPersonalAsignado.setText(sb.toString());
                tvPersonalAsignado.setVisibility(View.VISIBLE);
            } else {
                tvPersonalAsignado.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventoClick(pedido);
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
                    colorTexto = ContextCompat.getColor(ctx, R.color.status_danado);
                    colorFondo = ContextCompat.getColor(ctx, R.color.status_danado_container);
                    break;
            }

            tvEstadoBadge.setText(textoRes);
            BadgeUtils.pintar(tvEstadoBadge, colorTexto, colorFondo);
        }
    }
}
