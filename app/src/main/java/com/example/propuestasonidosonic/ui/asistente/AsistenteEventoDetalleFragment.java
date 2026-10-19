package com.example.propuestasonidosonic.ui.asistente;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.AsistenteUtils;
import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.example.propuestasonidosonic.util.Reloj;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsistenteEventoDetalleFragment extends Fragment {

    private String pedidoId;

    private PedidoRepository pedidoRepository;
    private EventoRepository eventoRepository;
    private EquipoRepository equipoRepository;
    private SessionManager sessionManager;
    private Reloj reloj;

    private LinearLayout layoutEventoNoDisponible;
    private MaterialButton btnVolverNoDisponible;
    private NestedScrollView scrollDetalleAsistente;

    // Sección Evento
    private TextView tvNombreEvento;
    private TextView tvFechaEvento;
    private TextView tvHorariosEvento;
    private TextView tvDireccionEvento;
    private MaterialButton btnAbrirMapas;

    // Sección Paquete
    private TextView tvPaqueteNombre;
    private TextView tvPaqueteComponentes;

    // Sección Contacto
    private TextView tvAvisoContactoNoDisponible;
    private LinearLayout layoutDatosContacto;
    private TextView tvClienteNombre;
    private TextView tvClienteTelefono;
    private MaterialButton btnLlamarCliente;

    // Sección Equipo (Checklist)
    private TextView tvResumenChecklistEncabezado;
    private TextView tvAvisoChecklistNoEditable;
    private LinearLayout layoutChecklistItems;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContainer appContainer = SonidoSonicApp.getApp(requireContext()).getAppContainer();
        pedidoRepository = appContainer.getPedidoRepository();
        eventoRepository = appContainer.getEventoRepository();
        equipoRepository = appContainer.getEquipoRepository();
        sessionManager = appContainer.getSessionManager();
        reloj = appContainer.getReloj();

        if (getArguments() != null) {
            pedidoId = getArguments().getString("pedidoId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asistente_evento_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutEventoNoDisponible = view.findViewById(R.id.layoutEventoNoDisponible);
        btnVolverNoDisponible = view.findViewById(R.id.btnVolverNoDisponible);
        scrollDetalleAsistente = view.findViewById(R.id.scrollDetalleAsistente);

        tvNombreEvento = view.findViewById(R.id.tvNombreEvento);
        tvFechaEvento = view.findViewById(R.id.tvFechaEvento);
        tvHorariosEvento = view.findViewById(R.id.tvHorariosEvento);
        tvDireccionEvento = view.findViewById(R.id.tvDireccionEvento);
        btnAbrirMapas = view.findViewById(R.id.btnAbrirMapas);

        tvPaqueteNombre = view.findViewById(R.id.tvPaqueteNombre);
        tvPaqueteComponentes = view.findViewById(R.id.tvPaqueteComponentes);

        tvAvisoContactoNoDisponible = view.findViewById(R.id.tvAvisoContactoNoDisponible);
        layoutDatosContacto = view.findViewById(R.id.layoutDatosContacto);
        tvClienteNombre = view.findViewById(R.id.tvClienteNombre);
        tvClienteTelefono = view.findViewById(R.id.tvClienteTelefono);
        btnLlamarCliente = view.findViewById(R.id.btnLlamarCliente);

        tvResumenChecklistEncabezado = view.findViewById(R.id.tvResumenChecklistEncabezado);
        tvAvisoChecklistNoEditable = view.findViewById(R.id.tvAvisoChecklistNoEditable);
        layoutChecklistItems = view.findViewById(R.id.layoutChecklistItems);

        btnVolverNoDisponible.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        pedidoRepository.getPedidos().observe(getViewLifecycleOwner(), pedidos -> cargarDetalle());
        eventoRepository.getEventos().observe(getViewLifecycleOwner(), eventos -> cargarDetalle());
    }

    private void cargarDetalle() {
        if (!isAdded()) return;

        Usuario usuario = sessionManager.getUsuarioActual();
        String asistenteId = (usuario != null) ? usuario.getId() : null;

        Pedido pedido = (pedidoId != null) ? pedidoRepository.getPedidoPorId(pedidoId) : null;

        boolean visible = false;
        if (pedido != null && asistenteId != null) {
            List<Pedido> delAsistente = AsistenteUtils.eventosDelAsistente(Collections.singletonList(pedido), asistenteId);
            visible = !delAsistente.isEmpty();
        }

        if (!visible || pedido == null) {
            scrollDetalleAsistente.setVisibility(View.GONE);
            layoutEventoNoDisponible.setVisibility(View.VISIBLE);
            return;
        }

        scrollDetalleAsistente.setVisibility(View.VISIBLE);
        layoutEventoNoDisponible.setVisibility(View.GONE);

        // 1. Evento
        tvNombreEvento.setText(pedido.getNombreEvento());
        tvFechaEvento.setText(FormatUtils.fechaLarga(pedido.getInicioMillis()));

        String horaLlegada = FormatUtils.hora(AsistenteUtils.llegadaMillis(pedido));
        String horaIni = FormatUtils.hora(pedido.getInicioMillis());
        String horaFin = FormatUtils.hora(pedido.getFinMillis());
        tvHorariosEvento.setText(getString(R.string.horario_asistente_formato, horaLlegada, horaIni, horaFin));

        tvDireccionEvento.setText(pedido.getDireccion());
        btnAbrirMapas.setOnClickListener(v -> {
            String uriStr = "geo:0,0?q=" + Uri.encode(pedido.getDireccion());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Snackbar.make(requireView(), R.string.error_no_app_mapas, Snackbar.LENGTH_SHORT).show();
            }
        });

        // 2. Paquete
        tvPaqueteNombre.setText(pedido.getPaqueteNombre());
        if (pedido.getComponentes() != null && !pedido.getComponentes().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ComponentePaquete c : pedido.getComponentes()) {
                if (c == null) continue;
                sb.append("• ").append(c.getCantidad()).append(" × ").append(c.getNombreModelo()).append("\n");
            }
            tvPaqueteComponentes.setText(sb.toString().trim());
        } else {
            tvPaqueteComponentes.setText(pedido.getPaqueteNombre());
        }

        // 3. Contacto del Cliente
        long ahora = (reloj != null) ? reloj.ahora() : System.currentTimeMillis();
        boolean contactoVis = AsistenteUtils.contactoVisible(pedido, ahora);

        if (contactoVis) {
            tvAvisoContactoNoDisponible.setVisibility(View.GONE);
            layoutDatosContacto.setVisibility(View.VISIBLE);
            tvClienteNombre.setText(pedido.getClienteNombre());
            tvClienteTelefono.setText(pedido.getClienteTelefono());
            btnLlamarCliente.setOnClickListener(v -> {
                String tel = pedido.getClienteTelefono() != null ? pedido.getClienteTelefono().trim() : "";
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Snackbar.make(requireView(), R.string.error_no_app_telefono, Snackbar.LENGTH_SHORT).show();
                }
            });
        } else {
            tvAvisoContactoNoDisponible.setVisibility(View.VISIBLE);
            layoutDatosContacto.setVisibility(View.GONE);
        }

        // 4. Equipo (Checklist)
        String eventoId = pedido.getEventoId();
        if (eventoId != null) {
            eventoRepository.getEventoPorId(eventoId, new Callback<Evento>() {
                @Override
                public void onSuccess(Evento evento) {
                    if (!isAdded() || evento == null) return;
                    sincronizarYPintarChecklist(pedido, evento, ahora);
                }

                @Override
                public void onError(String error) {
                    // Si no se encuentra evento, ocultar contenedor
                    layoutChecklistItems.removeAllViews();
                }
            });
        } else {
            layoutChecklistItems.removeAllViews();
        }
    }

    private void sincronizarYPintarChecklist(Pedido pedido, Evento evento, long ahora) {
        Map<String, Equipo> equiposMap = new HashMap<>();
        List<Equipo> todosEquipos = equipoRepository.listarTodos();
        if (todosEquipos != null) {
            for (Equipo eq : todosEquipos) {
                if (eq != null && eq.getId() != null) {
                    equiposMap.put(eq.getId(), eq);
                }
            }
        }

        List<ChecklistItem> actual = evento.getChecklist();
        List<ChecklistItem> sincronizado;
        if (actual == null || actual.isEmpty()) {
            sincronizado = AsistenteUtils.checklistInicial(pedido.getEquipoIds(), equiposMap);
            eventoRepository.actualizarChecklist(evento.getId(), sincronizado, null);
            evento.setChecklist(sincronizado);
        } else {
            sincronizado = AsistenteUtils.sincronizarChecklist(actual, pedido.getEquipoIds(), equiposMap);
            if (!sincronizado.equals(actual)) {
                eventoRepository.actualizarChecklist(evento.getId(), sincronizado, null);
                evento.setChecklist(sincronizado);
            }
        }

        boolean editable = AsistenteUtils.checklistEditable(pedido, ahora);
        pintarChecklist(evento, editable);
    }

    private void pintarChecklist(Evento evento, boolean editable) {
        if (!isAdded()) return;

        List<ChecklistItem> items = evento.getChecklist();
        int[] resumen = AsistenteUtils.resumenChecklist(items);
        tvResumenChecklistEncabezado.setText(getString(R.string.checklist_resumen_encabezado,
                resumen[1], resumen[0], resumen[2], resumen[0]));

        if (editable) {
            tvAvisoChecklistNoEditable.setVisibility(View.GONE);
        } else {
            tvAvisoChecklistNoEditable.setVisibility(View.VISIBLE);
        }

        layoutChecklistItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                final ChecklistItem item = items.get(i);

                View itemView = inflater.inflate(R.layout.item_checklist_unidad, layoutChecklistItems, false);

                TextView tvNombreUnidad = itemView.findViewById(R.id.tvNombreUnidad);
                MaterialCheckBox cbSalio = itemView.findViewById(R.id.cbSalio);
                MaterialCheckBox cbRegreso = itemView.findViewById(R.id.cbRegreso);
                TextInputEditText etObservacion = itemView.findViewById(R.id.etObservacion);
                MaterialButton btnGuardarObservacion = itemView.findViewById(R.id.btnGuardarObservacion);

                tvNombreUnidad.setText(item.getNombre());
                cbSalio.setChecked(item.isSalio());
                cbRegreso.setChecked(item.isRegreso());
                etObservacion.setText(item.getObservacion());

                cbSalio.setEnabled(editable);
                cbRegreso.setEnabled(editable);
                etObservacion.setEnabled(editable);
                btnGuardarObservacion.setEnabled(editable);

                if (editable) {
                    cbSalio.setOnClickListener(v -> {
                        boolean nuevoSalio = cbSalio.isChecked();
                        List<ChecklistItem> actualizados = AsistenteUtils.marcar(evento.getChecklist(), index, "salio", nuevoSalio);
                        evento.setChecklist(actualizados);
                        eventoRepository.actualizarChecklist(evento.getId(), actualizados, null);
                        pintarChecklist(evento, editable);
                    });

                    cbRegreso.setOnClickListener(v -> {
                        boolean nuevoRegreso = cbRegreso.isChecked();
                        if (nuevoRegreso && !item.isSalio()) {
                            cbRegreso.setChecked(false);
                            return;
                        }
                        List<ChecklistItem> actualizados = AsistenteUtils.marcar(evento.getChecklist(), index, "regreso", nuevoRegreso);
                        evento.setChecklist(actualizados);
                        eventoRepository.actualizarChecklist(evento.getId(), actualizados, null);
                        pintarChecklist(evento, editable);
                    });

                    btnGuardarObservacion.setOnClickListener(v -> {
                        String texto = (etObservacion.getText() != null) ? etObservacion.getText().toString() : "";
                        List<ChecklistItem> actualizados = AsistenteUtils.conObservacion(evento.getChecklist(), index, texto);
                        evento.setChecklist(actualizados);
                        eventoRepository.actualizarChecklist(evento.getId(), actualizados, null);
                        pintarChecklist(evento, editable);
                    });

                    etObservacion.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            String texto = (etObservacion.getText() != null) ? etObservacion.getText().toString() : "";
                            if (!texto.equals(item.getObservacion())) {
                                List<ChecklistItem> actualizados = AsistenteUtils.conObservacion(evento.getChecklist(), index, texto);
                                evento.setChecklist(actualizados);
                                eventoRepository.actualizarChecklist(evento.getId(), actualizados, null);
                                pintarChecklist(evento, editable);
                            }
                        }
                    });
                }

                layoutChecklistItems.addView(itemView);
            }
        }
    }
}
