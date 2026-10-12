package com.example.propuestasonidosonic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.ModeloInventario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponenteAdapter extends RecyclerView.Adapter<ComponenteAdapter.ComponenteViewHolder> {

    public interface OnComponenteActionListener {
        void onCantidadCambiada(ComponentePaquete componente, int nuevaCantidad);
        void onQuitarComponente(ComponentePaquete componente);
    }

    private final List<ComponentePaquete> componentes = new ArrayList<>();
    private final OnComponenteActionListener listener;
    private Map<String, ModeloInventario> modelosMap;

    public ComponenteAdapter(OnComponenteActionListener listener) {
        this.listener = listener;
    }

    public void setModelosMap(Map<String, ModeloInventario> modelosMap) {
        this.modelosMap = modelosMap;
        notifyDataSetChanged();
    }

    public void setComponentes(List<ComponentePaquete> nuevos) {
        this.componentes.clear();
        if (nuevos != null) {
            this.componentes.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ComponenteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_componente_paquete, parent, false);
        return new ComponenteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ComponenteViewHolder holder, int position) {
        holder.bind(componentes.get(position));
    }

    @Override
    public int getItemCount() {
        return componentes.size();
    }

    class ComponenteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvStock;
        private final ImageButton btnMenos;
        private final TextView tvCantidad;
        private final ImageButton btnMas;
        private final ImageButton btnQuitar;

        public ComponenteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvComponenteNombre);
            tvStock = itemView.findViewById(R.id.tvComponenteStock);
            btnMenos = itemView.findViewById(R.id.btnComponenteMenos);
            tvCantidad = itemView.findViewById(R.id.tvComponenteCantidad);
            btnMas = itemView.findViewById(R.id.btnComponenteMas);
            btnQuitar = itemView.findViewById(R.id.btnComponenteQuitar);
        }

        public void bind(ComponentePaquete componente) {
            Context context = itemView.getContext();
            tvNombre.setText(componente.getNombreModelo());
            tvCantidad.setText(String.valueOf(componente.getCantidad()));

            ModeloInventario mod = modelosMap != null ? modelosMap.get(componente.getModeloClave()) : null;
            int total = mod != null ? mod.getTotalUnidades() : componente.getCantidad();
            String cat = mod != null ? mod.getCategoria() : (componente.getCategoria() != null ? componente.getCategoria() : "");

            tvStock.setText(context.getString(R.string.componente_stock_formato, cat, total));

            // Menos: deshabilitado si está en 1
            btnMenos.setEnabled(componente.getCantidad() > 1);
            btnMenos.setAlpha(componente.getCantidad() > 1 ? 1.0f : 0.4f);

            // Mas: deshabilitado si está en el máximo disponible de inventario
            btnMas.setEnabled(componente.getCantidad() < total);
            btnMas.setAlpha(componente.getCantidad() < total ? 1.0f : 0.4f);

            btnMenos.setOnClickListener(v -> {
                if (listener != null && componente.getCantidad() > 1) {
                    listener.onCantidadCambiada(componente, componente.getCantidad() - 1);
                }
            });

            btnMas.setOnClickListener(v -> {
                if (listener != null && componente.getCantidad() < total) {
                    listener.onCantidadCambiada(componente, componente.getCantidad() + 1);
                }
            });

            btnQuitar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuitarComponente(componente);
                }
            });
        }
    }
}
