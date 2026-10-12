package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EquipoAdapter extends RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder> {

    public interface OnEquipoClickListener {
        void onEquipoClick(Equipo equipo);
    }

    public interface EstadoHelper {
        String obtenerEstadoEfectivo(Equipo equipo);
        Evento obtenerProximaReserva(String equipoId);
    }

    private final List<Equipo> equipos = new ArrayList<>();
    private final OnEquipoClickListener listener;
    private final EstadoHelper estadoHelper;

    public EquipoAdapter(OnEquipoClickListener listener, EstadoHelper estadoHelper) {
        this.listener = listener;
        this.estadoHelper = estadoHelper;
    }

    public void setEquipos(List<Equipo> nuevosEquipos) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return equipos.size();
            }

            @Override
            public int getNewListSize() {
                return nuevosEquipos.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(equipos.get(oldItemPosition).getId(), nuevosEquipos.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Equipo oldE = equipos.get(oldItemPosition);
                Equipo newE = nuevosEquipos.get(newItemPosition);
                return Objects.equals(oldE.getNombre(), newE.getNombre()) &&
                        oldE.getNumeroUnidad() == newE.getNumeroUnidad() &&
                        Objects.equals(oldE.getCategoria(), newE.getCategoria()) &&
                        Objects.equals(oldE.getEstadoFisico(), newE.getEstadoFisico()) &&
                        Double.compare(oldE.getPrecioRenta(), newE.getPrecioRenta()) == 0 &&
                        Objects.equals(oldE.getFotoUri(), newE.getFotoUri());
            }
        });
        equipos.clear();
        equipos.addAll(nuevosEquipos);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public EquipoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipo, parent, false);
        return new EquipoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipoViewHolder holder, int position) {
        holder.bind(equipos.get(position));
    }

    @Override
    public int getItemCount() {
        return equipos.size();
    }

    class EquipoViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView ivFoto;
        private final TextView tvNombreUnidad;
        private final TextView tvEstadoBadge;
        private final TextView tvCategoriaPrecio;
        private final TextView tvProximaReserva;

        public EquipoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivEquipoFoto);
            tvNombreUnidad = itemView.findViewById(R.id.tvNombreUnidad);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvCategoriaPrecio = itemView.findViewById(R.id.tvCategoriaPrecio);
            tvProximaReserva = itemView.findViewById(R.id.tvProximaReserva);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEquipoClick(equipos.get(pos));
                }
            });
        }

        public void bind(Equipo equipo) {
            Context context = itemView.getContext();

            // Nombre y número de unidad
            String nombreConUnidad = equipo.getNombre() + " (" + equipo.getNumeroUnidad() + ")";
            tvNombreUnidad.setText(nombreConUnidad);

            // Categoría y precio de renta por evento
            String catPrecio = equipo.getCategoria() + " · " + FormatUtils.formatoMoneda(equipo.getPrecioRenta());
            tvCategoriaPrecio.setText(catPrecio);

            // Foto: fotoUri (Glide) o drawable por defecto de su categoría
            int fallbackDrawable = CategoriaEquipo.getImagenPorDefecto(equipo.getCategoria());
            if (equipo.getFotoUri() != null && !equipo.getFotoUri().trim().isEmpty()) {
                Glide.with(context)
                        .load(Uri.parse(equipo.getFotoUri()))
                        .placeholder(fallbackDrawable)
                        .error(fallbackDrawable)
                        .centerCrop()
                        .into(ivFoto);
            } else {
                Glide.with(context)
                        .load(fallbackDrawable)
                        .centerCrop()
                        .into(ivFoto);
            }

            // Estado efectivo (deriva Rentado si hay evento hoy no cancelado)
            String estado = estadoHelper != null ? estadoHelper.obtenerEstadoEfectivo(equipo) : equipo.getEstadoFisico();
            configurarBadgeEstado(context, estado);

            // Próxima reserva futura
            Evento proxima = estadoHelper != null ? estadoHelper.obtenerProximaReserva(equipo.getId()) : null;
            if (proxima != null) {
                String textoReserva = String.format("Próxima reserva: %s · %s",
                        FormatUtils.formatoFecha(proxima.getInicioMillis()),
                        proxima.getNombre());
                tvProximaReserva.setText(textoReserva);
                tvProximaReserva.setVisibility(View.VISIBLE);
            } else {
                tvProximaReserva.setVisibility(View.GONE);
            }
        }

        private void configurarBadgeEstado(Context context, String estado) {
            int textColor;
            int containerColor;
            String texto;

            if (Equipo.ESTADO_RENTADO.equals(estado)) {
                textColor = ContextCompat.getColor(context, R.color.status_rentado);
                containerColor = ContextCompat.getColor(context, R.color.status_rentado_container);
                texto = context.getString(R.string.estado_rentado);
            } else if (Equipo.ESTADO_EN_REPARACION.equals(estado)) {
                textColor = ContextCompat.getColor(context, R.color.status_en_reparacion);
                containerColor = ContextCompat.getColor(context, R.color.status_en_reparacion_container);
                texto = context.getString(R.string.estado_en_reparacion);
            } else if (Equipo.ESTADO_DANADO.equals(estado)) {
                textColor = ContextCompat.getColor(context, R.color.status_danado);
                containerColor = ContextCompat.getColor(context, R.color.status_danado_container);
                texto = context.getString(R.string.estado_danado);
            } else {
                textColor = ContextCompat.getColor(context, R.color.status_disponible);
                containerColor = ContextCompat.getColor(context, R.color.status_disponible_container);
                texto = context.getString(R.string.estado_disponible);
            }

            tvEstadoBadge.setText(texto);
            com.example.propuestasonidosonic.util.BadgeUtils.pintar(tvEstadoBadge, textColor, containerColor);
        }
    }
}
