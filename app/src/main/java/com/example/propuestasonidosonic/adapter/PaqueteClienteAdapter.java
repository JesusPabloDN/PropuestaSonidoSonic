package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PaqueteClienteAdapter extends RecyclerView.Adapter<PaqueteClienteAdapter.ViewHolder> {

    public interface OnPaqueteClickListener {
        void onPaqueteClick(Paquete paquete);
    }

    private final List<Paquete> paquetes = new ArrayList<>();
    private final OnPaqueteClickListener listener;

    public PaqueteClienteAdapter(OnPaqueteClickListener listener) {
        this.listener = listener;
    }

    public void setPaquetes(List<Paquete> nuevos) {
        paquetes.clear();
        if (nuevos != null) {
            paquetes.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paquete_cliente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(paquetes.get(position));
    }

    @Override
    public int getItemCount() {
        return paquetes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivFoto;
        private final TextView tvNombre;
        private final TextView tvDescripcion;
        private final TextView tvPrecioDuracion;
        private final Chip chipEquipos;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoPaquete);
            tvNombre = itemView.findViewById(R.id.tvNombrePaquete);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionPaquete);
            tvPrecioDuracion = itemView.findViewById(R.id.tvPrecioDuracionPaquete);
            chipEquipos = itemView.findViewById(R.id.chipEquiposPaquete);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPaqueteClick(paquetes.get(pos));
                }
            });
        }

        void bind(Paquete paquete) {
            Context context = itemView.getContext();
            tvNombre.setText(paquete.getNombre());
            String desc = paquete.getDescripcion();
            if (desc != null && !desc.trim().isEmpty()) {
                tvDescripcion.setText(desc.trim());
                tvDescripcion.setVisibility(View.VISIBLE);
            } else {
                tvDescripcion.setVisibility(View.GONE);
            }

            int totalPiezas = 0;
            String catPrincipal = CategoriaEquipo.BOCINAS;
            if (paquete.getComponentes() != null && !paquete.getComponentes().isEmpty()) {
                for (ComponentePaquete c : paquete.getComponentes()) {
                    totalPiezas += c.getCantidad();
                }
                catPrincipal = paquete.getComponentes().get(0).getCategoria();
            }

            tvPrecioDuracion.setText(context.getString(R.string.paquete_precio_desde_formato,
                    FormatUtils.formatoMoneda(paquete.getPrecio()), paquete.getDuracionBaseHoras()));
            chipEquipos.setText(context.getString(R.string.paquete_equipos_incluidos_formato, totalPiezas));

            int fallbackDrawable = CategoriaEquipo.getImagenPorDefecto(catPrincipal);
            if (paquete.getFotoUri() != null && !paquete.getFotoUri().trim().isEmpty()) {
                Glide.with(context)
                        .load(Uri.parse(paquete.getFotoUri()))
                        .centerCrop()
                        .error(fallbackDrawable)
                        .into(ivFoto);
            } else {
                Glide.with(context)
                        .load(fallbackDrawable)
                        .centerCrop()
                        .into(ivFoto);
            }
        }
    }
}
