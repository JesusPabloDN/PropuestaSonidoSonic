package com.example.propuestasonidosonic.ui.socio;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.adapter.PedidoAdapter;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ClienteUtils;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ClienteDetalleFragment extends Fragment {

    private String clienteKey = "";

    private TextView tvClienteDetalleNombre;
    private TextView tvClienteDetalleTelefono;
    private TextView tvClienteDetalleCorreo;
    private MaterialButton btnLlamarCliente;
    private MaterialButton btnWhatsAppCliente;

    private TextView tvKpiPedidos;
    private TextView tvKpiPagado;
    private TextView tvKpiSaldo;

    private TextView tvSinPedidosCliente;
    private RecyclerView rvPedidosCliente;
    private PedidoAdapter pedidoAdapter;

    private PedidoRepository pedidoRepository;
    private PagoRepository pagoRepository;
    private UsuarioRepository usuarioRepository;

    private List<Pedido> todosLosPedidos = new ArrayList<>();
    private List<Pago> todosLosPagos = new ArrayList<>();
    private List<Usuario> todosLosUsuarios = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cliente_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            clienteKey = getArguments().getString("clienteKey", "");
        }

        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = appContainer.getPedidoRepository();
        pagoRepository = appContainer.getPagoRepository();
        usuarioRepository = appContainer.getUsuarioRepository();

        tvClienteDetalleNombre = view.findViewById(R.id.tvClienteDetalleNombre);
        tvClienteDetalleTelefono = view.findViewById(R.id.tvClienteDetalleTelefono);
        tvClienteDetalleCorreo = view.findViewById(R.id.tvClienteDetalleCorreo);
        btnLlamarCliente = view.findViewById(R.id.btnLlamarCliente);
        btnWhatsAppCliente = view.findViewById(R.id.btnWhatsAppCliente);

        tvKpiPedidos = view.findViewById(R.id.tvKpiPedidos);
        tvKpiPagado = view.findViewById(R.id.tvKpiPagado);
        tvKpiSaldo = view.findViewById(R.id.tvKpiSaldo);

        tvSinPedidosCliente = view.findViewById(R.id.tvSinPedidosCliente);
        rvPedidosCliente = view.findViewById(R.id.rvPedidosCliente);

        rvPedidosCliente.setLayoutManager(new LinearLayoutManager(requireContext()));
        pedidoAdapter = new PedidoAdapter(pedido -> {
            if (!isAdded()) return;
            Bundle args = new Bundle();
            args.putString("pedidoId", pedido.getId());
            NavHostFragment.findNavController(this).navigate(R.id.action_cliente_to_pedido, args);
        });
        rvPedidosCliente.setAdapter(pedidoAdapter);

        observarDatos();
    }

    private void observarDatos() {
        if (pedidoRepository != null) {
            pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> {
                todosLosPedidos = pedidos != null ? new ArrayList<>(pedidos) : new ArrayList<>();
                actualizarUI();
            });
        }

        if (pagoRepository != null) {
            pagoRepository.getTodosLosPagos().observe(getViewLifecycleOwner(), pagos -> {
                todosLosPagos = pagos != null ? new ArrayList<>(pagos) : new ArrayList<>();
                actualizarUI();
            });
        }

        if (usuarioRepository != null) {
            usuarioRepository.getUsuarios().observe(getViewLifecycleOwner(), usuarios -> {
                todosLosUsuarios = usuarios != null ? new ArrayList<>(usuarios) : new ArrayList<>();
                actualizarUI();
            });
        }
    }

    private void actualizarUI() {
        if (!isAdded()) return;

        List<ClienteUtils.ResumenCliente> resumenes = ClienteUtils.resumenes(todosLosPedidos, todosLosPagos, todosLosUsuarios);
        ClienteUtils.ResumenCliente cliente = null;
        for (ClienteUtils.ResumenCliente r : resumenes) {
            if (r.getClave().equals(clienteKey)) {
                cliente = r;
                break;
            }
        }

        List<Pedido> pedidosCliente = ClienteUtils.pedidosDelCliente(todosLosPedidos, clienteKey);

        if (cliente != null) {
            tvClienteDetalleNombre.setText(cliente.getNombre());
            tvClienteDetalleTelefono.setText("Tel: " + ClienteUtils.formatoTelefono(cliente.getTelefono()));

            if (cliente.getCorreo() != null && !cliente.getCorreo().trim().isEmpty()) {
                tvClienteDetalleCorreo.setVisibility(View.VISIBLE);
                tvClienteDetalleCorreo.setText(cliente.getCorreo().trim());
            } else {
                tvClienteDetalleCorreo.setVisibility(View.GONE);
            }

            final String t10 = ClienteUtils.telefono10(cliente.getTelefono());
            if (!t10.isEmpty()) {
                btnLlamarCliente.setEnabled(true);
                btnLlamarCliente.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + t10));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException | SecurityException e) {
                        Snackbar.make(requireView(), R.string.msg_no_app, Snackbar.LENGTH_SHORT).show();
                    }
                });

                btnWhatsAppCliente.setEnabled(true);
                btnWhatsAppCliente.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://wa.me/52" + t10));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException | SecurityException e) {
                        Snackbar.make(requireView(), R.string.msg_no_app, Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else {
                btnLlamarCliente.setEnabled(false);
                btnWhatsAppCliente.setEnabled(false);
            }

            tvKpiPedidos.setText(String.valueOf(cliente.getPedidosValidos()));
            tvKpiPagado.setText(FormatUtils.formatoMoneda(cliente.getTotalPagadoNeto()));
            tvKpiSaldo.setText(FormatUtils.formatoMoneda(cliente.getSaldoPendiente()));
        }

        if (pedidosCliente.isEmpty()) {
            tvSinPedidosCliente.setVisibility(View.VISIBLE);
            rvPedidosCliente.setVisibility(View.GONE);
        } else {
            tvSinPedidosCliente.setVisibility(View.GONE);
            rvPedidosCliente.setVisibility(View.VISIBLE);
            pedidoAdapter.setDatos(pedidosCliente, PedidoAdapter.TAB_HISTORIAL);
        }
    }
}
