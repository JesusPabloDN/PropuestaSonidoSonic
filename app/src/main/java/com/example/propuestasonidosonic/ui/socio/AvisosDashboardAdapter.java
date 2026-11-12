package com.example.propuestasonidosonic.ui.socio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;

import java.util.ArrayList;
import java.util.List;

public class AvisosDashboardAdapter extends RecyclerView.Adapter<AvisosDashboardAdapter.AvisoViewHolder> {

    public static class Aviso {
        public final int icono;
        public final String etiqueta;
        public final String titulo;
        public final String detalle;
        public final String accion;
        public final int destino;
        public final Bundle argumentos;

        public Aviso(int icono, String etiqueta, String titulo, String detalle, String accion, int destino, Bundle argumentos) {
            this.icono = icono;
            this.etiqueta = etiqueta;
            this.titulo = titulo;
            this.detalle = detalle;
            this.accion = accion;
            this.destino = destino;
            this.argumentos = argumentos;
        }

        String clave() {
            return etiqueta + "|" + titulo + "|" + detalle;
        }
    }

    public interface OnAvisoClickListener {
        void onAvisoClick(Aviso aviso);
    }

    private final List<Aviso> avisos = new ArrayList<>();
    private final OnAvisoClickListener listener;

    public AvisosDashboardAdapter(OnAvisoClickListener listener) {
        this.listener = listener;
    }

    public boolean actualizar(List<Aviso> nuevos) {
        StringBuilder antes = new StringBuilder();
        for (Aviso a : avisos) antes.append(a.clave()).append('#');
        StringBuilder despues = new StringBuilder();
        for (Aviso a : nuevos) despues.append(a.clave()).append('#');
        if (antes.toString().equals(despues.toString())) return false;
        avisos.clear();
        avisos.addAll(nuevos);
        notifyDataSetChanged();
        return true;
    }

    @NonNull
    @Override
    public AvisoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_aviso_dashboard, parent, false);
        return new AvisoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AvisoViewHolder holder, int position) {
        Aviso aviso = avisos.get(position);
        holder.ivIcono.setImageResource(aviso.icono);
        holder.tvEtiqueta.setText(aviso.etiqueta);
        holder.tvTitulo.setText(aviso.titulo);
        holder.tvDetalle.setText(aviso.detalle);
        if (aviso.accion != null && aviso.destino != 0) {
            holder.tvAccion.setVisibility(View.VISIBLE);
            holder.tvAccion.setText(aviso.accion);
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAvisoClick(aviso);
            });
        } else {
            holder.tvAccion.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return avisos.size();
    }

    static class AvisoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcono;
        final TextView tvEtiqueta;
        final TextView tvTitulo;
        final TextView tvDetalle;
        final TextView tvAccion;

        AvisoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.ivAvisoIcono);
            tvEtiqueta = itemView.findViewById(R.id.tvAvisoEtiqueta);
            tvTitulo = itemView.findViewById(R.id.tvAvisoTitulo);
            tvDetalle = itemView.findViewById(R.id.tvAvisoDetalle);
            tvAccion = itemView.findViewById(R.id.tvAvisoAccion);
        }
    }
}
