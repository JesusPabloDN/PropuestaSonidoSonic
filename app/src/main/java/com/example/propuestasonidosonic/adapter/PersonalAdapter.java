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
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PersonalAdapter extends RecyclerView.Adapter<PersonalAdapter.PersonalViewHolder> {

    public interface OnPersonalClickListener {
        void onPersonalClick(Usuario usuario);
    }

    private final List<Usuario> usuarios = new ArrayList<>();
    private final OnPersonalClickListener listener;

    public PersonalAdapter(OnPersonalClickListener listener) {
        this.listener = listener;
    }

    public void setUsuarios(List<Usuario> lista) {
        this.usuarios.clear();
        if (lista != null) {
            for (Usuario u : lista) {
                if (Usuario.ROL_SOCIO.equals(u.getRol()) || Usuario.ROL_ASISTENTE.equals(u.getRol())) {
                    this.usuarios.add(u);
                }
            }
        }
        // Socios primero, luego Asistentes; dentro de cada grupo alfabético sin distinguir mayúsculas
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
    public PersonalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_personal, parent, false);
        return new PersonalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonalViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario, listener);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    static class PersonalViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvRolBadge;
        private final TextView tvEstadoBadge;
        private final TextView tvCorreo;
        private final TextView tvTelefono;

        public PersonalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvRolBadge = itemView.findViewById(R.id.tvRolBadge);
            tvEstadoBadge = itemView.findViewById(R.id.tvEstadoBadge);
            tvCorreo = itemView.findViewById(R.id.tvCorreo);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
        }

        public void bind(Usuario usuario, OnPersonalClickListener listener) {
            Context context = itemView.getContext();
            tvNombre.setText(usuario.getNombre());
            tvCorreo.setText(usuario.getCorreo());
            tvTelefono.setText(usuario.getTelefono());

            // Badge Rol
            if (Usuario.ROL_SOCIO.equals(usuario.getRol())) {
                tvRolBadge.setText(R.string.badge_rol_socio);
                int txtColor = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorOnPrimaryContainer);
                int bgCol = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorPrimaryContainer);
                BadgeUtils.pintar(tvRolBadge, txtColor, bgCol);
            } else {
                tvRolBadge.setText(R.string.badge_rol_asistente);
                int txtColor = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorOnSecondaryContainer);
                int bgCol = MaterialColors.getColor(tvRolBadge, com.google.android.material.R.attr.colorSecondaryContainer);
                BadgeUtils.pintar(tvRolBadge, txtColor, bgCol);
            }

            // Badge Estado
            if (usuario.isCuentaActiva()) {
                tvEstadoBadge.setText(R.string.badge_cuenta_activa);
                int txtColor = ContextCompat.getColor(context, R.color.status_disponible);
                int bgCol = ContextCompat.getColor(context, R.color.status_disponible_container);
                BadgeUtils.pintar(tvEstadoBadge, txtColor, bgCol);
            } else {
                tvEstadoBadge.setText(R.string.badge_cuenta_inactiva);
                int txtColor = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorOnSurfaceVariant);
                int bgCol = MaterialColors.getColor(tvEstadoBadge, com.google.android.material.R.attr.colorSurfaceVariant);
                BadgeUtils.pintar(tvEstadoBadge, txtColor, bgCol);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPersonalClick(usuario);
                }
            });
        }
    }
}
