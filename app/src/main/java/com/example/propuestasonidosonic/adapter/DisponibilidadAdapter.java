package com.example.propuestasonidosonic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DisponibilidadAdapter extends RecyclerView.Adapter<DisponibilidadAdapter.DisponibilidadViewHolder> {

    public interface OnDisponibilidadToggleListener {
        void onDisponibilidadToggle(Usuario usuario, boolean nuevoEstado, MaterialSwitch switchView);
    }

    private final List<Usuario> usuarios = new ArrayList<>();
    private DiaOperativo diaOperativo;
    private boolean esDiaPasado = false;
    private final OnDisponibilidadToggleListener listener;

    public DisponibilidadAdapter(OnDisponibilidadToggleListener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Usuario> lista, DiaOperativo diaOperativo, boolean esDiaPasado) {
        this.usuarios.clear();
        this.diaOperativo = diaOperativo;
        this.esDiaPasado = esDiaPasado;
        if (lista != null) {
            for (Usuario u : lista) {
                // Solo personal activo (Socio o Asistente)
                if (u.isCuentaActiva() && (Usuario.ROL_SOCIO.equals(u.getRol()) || Usuario.ROL_ASISTENTE.equals(u.getRol()))) {
                    this.usuarios.add(u);
                }
            }
        }
        Collections.sort(this.usuarios, new Comparator<Usuario>() {
            @Override
            public int compare(Usuario o1, Usuario o2) {
                int r1 = Usuario.ROL_SOCIO.equals(o1.getRol()) ? 0 : 1;
                int r2 = Usuario.ROL_SOCIO.equals(o2.getRol()) ? 0 : 1;
                if (r1 != r2) {
                    return Integer.compare(r1, r2);
                }
                String n1 = o1.getNombre() != null ? o1.getNombre() : "";
                String n2 = o2.getNombre() != null ? o2.getNombre() : "";
                return n1.compareToIgnoreCase(n2);
            }
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DisponibilidadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_disponibilidad_operador, parent, false);
        return new DisponibilidadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DisponibilidadViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario, diaOperativo, esDiaPasado, listener);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    static class DisponibilidadViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvRolBadge;
        private final MaterialSwitch switchDisponibilidad;

        public DisponibilidadViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvRolBadge = itemView.findViewById(R.id.tvRolBadge);
            switchDisponibilidad = itemView.findViewById(R.id.switchDisponibilidad);
        }

        public void bind(Usuario usuario, DiaOperativo diaOp, boolean esDiaPasado, OnDisponibilidadToggleListener listener) {
            tvNombre.setText(usuario.getNombre());

            // Badge Rol
            boolean esSocio = Usuario.ROL_SOCIO.equals(usuario.getRol());
            if (esSocio) {
                tvRolBadge.setText(R.string.badge_rol_socio);
                int txtColor = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorOnPrimaryContainer);
                int bgCol = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorPrimaryContainer);
                BadgeUtils.pintar(tvRolBadge, txtColor, bgCol);
                switchDisponibilidad.setText(R.string.label_disponible_este_dia);
            } else {
                tvRolBadge.setText(R.string.badge_rol_asistente);
                int txtColor = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorOnSecondaryContainer);
                int bgCol = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorSecondaryContainer);
                BadgeUtils.pintar(tvRolBadge, txtColor, bgCol);
                switchDisponibilidad.setText(R.string.label_trabaja_este_dia);
            }

            // Determinar si está activo para el día
            Set<String> sociosNoDisp = (diaOp != null && diaOp.getSociosNoDisponiblesIds() != null)
                    ? new HashSet<>(diaOp.getSociosNoDisponiblesIds()) : Collections.emptySet();
            Set<String> asistDisp = (diaOp != null && diaOp.getAsistentesDisponiblesIds() != null)
                    ? new HashSet<>(diaOp.getAsistentesDisponiblesIds()) : Collections.emptySet();

            boolean estaActivo;
            if (esSocio) {
                // Opt-out: activo salvo que esté en sociosNoDisponiblesIds
                estaActivo = !sociosNoDisp.contains(usuario.getId());
            } else {
                // Opt-in: activo solo si está en asistentesDisponiblesIds
                estaActivo = asistDisp.contains(usuario.getId());
            }

            // Evitar disparar listener al setear programáticamente
            switchDisponibilidad.setOnCheckedChangeListener(null);
            switchDisponibilidad.setChecked(estaActivo);
            switchDisponibilidad.setEnabled(!esDiaPasado);

            switchDisponibilidad.setOnClickListener(v -> {
                boolean nuevoEstado = switchDisponibilidad.isChecked();
                if (listener != null) {
                    listener.onDisponibilidadToggle(usuario, nuevoEstado, switchDisponibilidad);
                }
            });
        }
    }
}
