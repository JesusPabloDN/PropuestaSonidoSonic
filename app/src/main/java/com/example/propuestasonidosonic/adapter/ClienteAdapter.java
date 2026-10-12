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
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.util.BadgeUtils;
import com.example.propuestasonidosonic.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {

    public interface OnClienteClickListener {
        void onClienteClick(ClienteUtils.ResumenCliente cliente);
    }

    private final List<ClienteUtils.ResumenCliente> clientes = new ArrayList<>();
    private final OnClienteClickListener listener;

    public ClienteAdapter(OnClienteClickListener listener) {
        this.listener = listener;
    }

    public void setClientes(List<ClienteUtils.ResumenCliente> nuevosClientes) {
        clientes.clear();
        if (nuevosClientes != null) {
            clientes.addAll(nuevosClientes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        ClienteUtils.ResumenCliente cliente = clientes.get(position);
        holder.bind(cliente, listener);
    }

    @Override
    public int getItemCount() {
        return clientes.size();
    }

    static class ClienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvClienteNombre;
        private final TextView tvClienteTelefono;
        private final TextView tvClientePedidosPagado;
        private final TextView tvClienteSaldoBadge;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClienteNombre = itemView.findViewById(R.id.tvClienteNombre);
            tvClienteTelefono = itemView.findViewById(R.id.tvClienteTelefono);
            tvClientePedidosPagado = itemView.findViewById(R.id.tvClientePedidosPagado);
            tvClienteSaldoBadge = itemView.findViewById(R.id.tvClienteSaldoBadge);
        }

        public void bind(ClienteUtils.ResumenCliente cliente, OnClienteClickListener listener) {
            Context ctx = itemView.getContext();

            String nombre = cliente.getNombre();
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = "Sin nombre";
            }
            tvClienteNombre.setText(nombre);

            tvClienteTelefono.setText(ClienteUtils.formatoTelefono(cliente.getTelefono()));

            String pagadoStr = FormatUtils.formatoMoneda(cliente.getTotalPagadoNeto());
            tvClientePedidosPagado.setText(ctx.getString(R.string.formato_cliente_pedidos_pagado,
                    cliente.getPedidosValidos(), pagadoStr));

            if (cliente.getSaldoPendiente() > 0.005) {
                tvClienteSaldoBadge.setVisibility(View.VISIBLE);
                String saldoStr = FormatUtils.formatoMoneda(cliente.getSaldoPendiente());
                tvClienteSaldoBadge.setText(ctx.getString(R.string.formato_cliente_saldo, saldoStr));

                int colorTexto = ContextCompat.getColor(ctx, R.color.status_pendiente);
                int colorFondo = ContextCompat.getColor(ctx, R.color.status_pendiente_container);
                BadgeUtils.pintar(tvClienteSaldoBadge, colorTexto, colorFondo);
            } else {
                tvClienteSaldoBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClienteClick(cliente);
                }
            });
        }
    }
}
