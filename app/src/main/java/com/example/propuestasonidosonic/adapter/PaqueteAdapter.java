package com.example.propuestasonidosonic.adapter;

import android.content.Context;
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
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PaqueteAdapter extends RecyclerView.Adapter<PaqueteAdapter.PaqueteViewHolder> {

    public interface OnPaqueteClickListener {
        void onPaqueteClick(Paquete paquete);
    }

    private final List<Paquete> paquetes = new ArrayList<>();
    private final OnPaqueteClickListener listener;
    private Map<String, ModeloInventario> modelosMap;

    public PaqueteAdapter(OnPaqueteClickListener listener) {
        this.listener = listener;
    }

    public void setModelosMap(Map<String, ModeloInventario> modelosMap) {
        this.modelosMap = modelosMap;
        notifyDataSetChanged();
    }

    public void setPaquetes(List<Paquete> nuevos) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return paquetes.size();
            }

            @Override
            public int getNewListSize() {
                return nuevos.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(paquetes.get(oldItemPosition).getId(), nuevos.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Paquete o = paquetes.get(oldItemPosition);
                Paquete n = nuevos.get(newItemPosition);
                return Objects.equals(o.getNombre(), n.getNombre()) &&
                        Double.compare(o.getPrecio(), n.getPrecio()) == 0 &&
                        o.isActivo() == n.isActivo() &&
                        o.getDuracionBaseHoras() == n.getDuracionBaseHoras() &&
                        o.getOperadoresRequeridos() == n.getOperadoresRequeridos() &&
                        Objects.equals(o.getComponentes(), n.getComponentes());
            }
        });
        paquetes.clear();
        for (Paquete p : nuevos) {
            paquetes.add(new Paquete(p));
        }
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PaqueteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paquete, parent, false);
        return new PaqueteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PaqueteViewHolder holder, int position) {
        holder.bind(paquetes.get(position));
    }

    @Override
    public int getItemCount() {
        return paquetes.size();
    }

    class PaqueteViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView ivImagen;
        private final TextView tvNombre;
        private final TextView tvEstado;
        private final TextView tvResumen;
        private final TextView tvPrecio;
        private final TextView tvDetalle;
        private final TextView tvComponentes;
        private final TextView tvAlerta;

        public PaqueteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen = itemView.findViewById(R.id.ivPaqueteImagen);
            tvNombre = itemView.findViewById(R.id.tvPaqueteNombre);
            tvEstado = itemView.findViewById(R.id.tvPaqueteEstado);
            tvResumen = itemView.findViewById(R.id.tvPaqueteResumen);
            tvPrecio = itemView.findViewById(R.id.tvPaquetePrecio);
            tvDetalle = itemView.findViewById(R.id.tvPaqueteDetalle);
            tvComponentes = itemView.findViewById(R.id.tvPaqueteComponentes);
            tvAlerta = itemView.findViewById(R.id.tvPaqueteAlerta);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPaqueteClick(paquetes.get(pos));
                }
            });
        }

        public void bind(Paquete paquete) {
            Context context = itemView.getContext();

            tvNombre.setText(paquete.getNombre());

            // Estado Activo / Inactivo
            if (paquete.isActivo()) {
                tvEstado.setText(R.string.badge_paquete_activo);
                int txtColor = ContextCompat.getColor(context, R.color.status_disponible);
                int bgCol = ContextCompat.getColor(context, R.color.status_disponible_container);
                BadgeUtils.pintar(tvEstado, txtColor, bgCol);
            } else {
                tvEstado.setText(R.string.badge_paquete_inactivo);
                int txtColor = MaterialColors.getColor(tvEstado, com.google.android.material.R.attr.colorOnSurfaceVariant);
                int bgCol = MaterialColors.getColor(tvEstado, com.google.android.material.R.attr.colorSurfaceVariant);
                BadgeUtils.pintar(tvEstado, txtColor, bgCol);
            }

            // Totales de piezas y tipos
            List<ComponentePaquete> componentes = paquete.getComponentes() != null ? paquete.getComponentes() : new ArrayList<>();
            int totalPiezas = 0;
            int tipos = componentes.size();
            StringBuilder compResumen = new StringBuilder();

            String categoriaMayorPrecio = null;
            double mayorPrecio = -1.0;
            String modeloFaltante = null;

            for (int i = 0; i < componentes.size(); i++) {
                ComponentePaquete c = componentes.get(i);
                totalPiezas += c.getCantidad();

                if (i > 0) compResumen.append(", ");
                compResumen.append(c.getCantidad()).append("× ").append(c.getNombreModelo());

                ModeloInventario mod = modelosMap != null ? modelosMap.get(c.getModeloClave()) : null;
                if (mod != null) {
                    if (mod.getPrecioUnitario() > mayorPrecio) {
                        mayorPrecio = mod.getPrecioUnitario();
                        categoriaMayorPrecio = mod.getCategoria();
                    }
                    if (c.getCantidad() > mod.getTotalUnidades() && modeloFaltante == null) {
                        modeloFaltante = c.getNombreModelo();
                    }
                } else if (modeloFaltante == null) {
                    modeloFaltante = c.getNombreModelo();
                }
            }

            tvResumen.setText(context.getString(R.string.paquete_resumen_formato, totalPiezas, tipos));
            tvPrecio.setText(context.getString(R.string.paquete_precio_formato, FormatUtils.formatoMoneda(paquete.getPrecio())));
            String operTxt = context.getString(R.string.operadores_n_formato, paquete.getOperadoresRequeridos());
            tvDetalle.setText(context.getString(R.string.paquete_detalle_formato, paquete.getDuracionBaseHoras(), operTxt));
            tvComponentes.setText(compResumen.toString());

            // Imagen = categoría del componente de mayor precio
            int drawableId = CategoriaEquipo.getImagenPorDefecto(categoriaMayorPrecio);
            Glide.with(context)
                    .load(drawableId)
                    .centerCrop()
                    .into(ivImagen);

            // Regla de Stock insuficiente
            if (modeloFaltante != null) {
                tvAlerta.setText(context.getString(R.string.alerta_stock_insuficiente_formato, modeloFaltante));
                int txtColor = ContextCompat.getColor(context, R.color.status_pendiente);
                int bgCol = ContextCompat.getColor(context, R.color.status_pendiente_container);
                BadgeUtils.pintar(tvAlerta, txtColor, bgCol);
                tvAlerta.setVisibility(View.VISIBLE);
            } else {
                tvAlerta.setVisibility(View.GONE);
            }
        }
    }
}
