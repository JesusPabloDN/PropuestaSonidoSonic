package com.example.propuestasonidosonic.ui.socio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.impresion.ImpresoraBle;
import com.example.propuestasonidosonic.impresion.PreferenciasImpresora;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.TicketRender;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Impresora de tickets (solo socios): elegir la impresora Bluetooth en este teléfono,
 * imprimir un ticket de prueba y ver cómo queda.
 */
public class ImpresoraFragment extends Fragment {

    private static final long DURACION_BUSQUEDA_MS = 12_000L;
    private static final ParcelUuid SERVICIO_ANUNCIADO = ParcelUuid.fromString("0000af30-0000-1000-8000-00805f9b34fb");

    private PreferenciasImpresora preferencias;

    private TextView tvImpresoraNombre;
    private TextView tvImpresoraDireccion;
    private MaterialButton btnBuscarImpresora;
    private MaterialButton btnImprimirPrueba;
    private MaterialButton btnOlvidarImpresora;
    private LinearProgressIndicator progresoImpresion;
    private TextView tvEstadoImpresion;
    private TextView tvMedidasTicket;
    private ImageView ivVistaPrevia;

    private Bitmap ticketPrueba;
    private boolean imprimiendo;
    private Runnable accionPendiente;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> permisosLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (!isAdded()) return;
                if (tienePermisos()) {
                    continuarPendiente();
                } else {
                    accionPendiente = null;
                    mostrarAviso(R.string.impresora_sin_permiso_titulo, R.string.impresora_sin_permiso_mensaje, true);
                }
            });

    private final ActivityResultLauncher<Intent> activarBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (!isAdded()) return;
                BluetoothAdapter adaptador = adaptador();
                if (adaptador != null && adaptador.isEnabled()) {
                    continuarPendiente();
                } else {
                    accionPendiente = null;
                    mostrarAviso(R.string.impresora_bluetooth_apagado_titulo, R.string.impresora_bluetooth_apagado_mensaje, false);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_impresora, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferencias = new PreferenciasImpresora(requireContext());

        tvImpresoraNombre = view.findViewById(R.id.tvImpresoraNombre);
        tvImpresoraDireccion = view.findViewById(R.id.tvImpresoraDireccion);
        btnBuscarImpresora = view.findViewById(R.id.btnBuscarImpresora);
        btnImprimirPrueba = view.findViewById(R.id.btnImprimirPrueba);
        btnOlvidarImpresora = view.findViewById(R.id.btnOlvidarImpresora);
        progresoImpresion = view.findViewById(R.id.progresoImpresion);
        tvEstadoImpresion = view.findViewById(R.id.tvEstadoImpresion);
        tvMedidasTicket = view.findViewById(R.id.tvMedidasTicket);
        ivVistaPrevia = view.findViewById(R.id.ivVistaPrevia);

        btnBuscarImpresora.setOnClickListener(v -> conRequisitos(this::buscarImpresoras));
        btnImprimirPrueba.setOnClickListener(v -> conRequisitos(this::imprimirPrueba));
        btnOlvidarImpresora.setOnClickListener(v -> {
            preferencias.olvidar();
            actualizarImpresora();
        });

        ticketPrueba = TicketRender.generarBitmap(requireContext(), pedidoDePrueba());
        ivVistaPrevia.setImageBitmap(ticketPrueba);
        tvMedidasTicket.setText(getString(R.string.impresora_medidas_ticket,
                ticketPrueba.getWidth() / 8, (int) Math.ceil(ticketPrueba.getHeight() / 8.0)));

        actualizarImpresora();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private void actualizarImpresora() {
        boolean hay = preferencias.hayImpresora();
        if (hay) {
            String nombre = preferencias.getNombre();
            tvImpresoraNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : getString(R.string.impresora_sin_nombre));
            tvImpresoraDireccion.setText(getString(R.string.impresora_direccion_formato, preferencias.getDireccion()));
            tvImpresoraDireccion.setVisibility(View.VISIBLE);
            btnBuscarImpresora.setText(R.string.btn_cambiar_impresora);
        } else {
            tvImpresoraNombre.setText(R.string.impresora_ninguna);
            tvImpresoraDireccion.setVisibility(View.GONE);
            btnBuscarImpresora.setText(R.string.btn_buscar_impresora);
        }
        btnImprimirPrueba.setEnabled(hay && !imprimiendo);
        btnBuscarImpresora.setEnabled(!imprimiendo);
        btnOlvidarImpresora.setVisibility(hay ? View.VISIBLE : View.GONE);
        btnOlvidarImpresora.setEnabled(!imprimiendo);
    }

    // ---------- Requisitos: permisos, Bluetooth encendido y ubicación (Android 11 o anterior) ----------

    private String[] permisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private boolean tienePermisos() {
        for (String p : permisosNecesarios()) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private BluetoothAdapter adaptador() {
        BluetoothManager manager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return manager != null ? manager.getAdapter() : null;
    }

    private void conRequisitos(Runnable accion) {
        if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
                || adaptador() == null) {
            mostrarAviso(R.string.impresora_sin_ble_titulo, R.string.impresora_sin_ble_mensaje, false);
            return;
        }
        accionPendiente = accion;
        continuarPendiente();
    }

    private void continuarPendiente() {
        Runnable accion = accionPendiente;
        if (accion == null) return;
        if (!tienePermisos()) {
            permisosLauncher.launch(permisosNecesarios());
            return;
        }
        BluetoothAdapter adaptador = adaptador();
        if (adaptador == null || !adaptador.isEnabled()) {
            activarBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !ubicacionActiva()) {
            // En Android 11 o anterior, la búsqueda Bluetooth no encuentra nada con la ubicación apagada
            accionPendiente = null;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.impresora_ubicacion_titulo)
                    .setMessage(R.string.impresora_ubicacion_mensaje)
                    .setNegativeButton(R.string.dialog_cancelar, null)
                    .setPositiveButton(R.string.impresora_abrir_ajustes, (d, w) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .show();
            return;
        }
        accionPendiente = null;
        accion.run();
    }

    private boolean ubicacionActiva() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return lm != null && LocationManagerCompat.isLocationEnabled(lm);
    }

    private void mostrarAviso(int titulo, int mensaje, boolean ofrecerAjustesApp) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(R.string.dialog_aceptar_impresora, null);
        if (ofrecerAjustesApp) {
            b.setNeutralButton(R.string.impresora_abrir_ajustes, (d, w) -> {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
                startActivity(i);
            });
        }
        b.show();
    }

    // ---------- Búsqueda de impresoras ----------

    private static final class Encontrado {
        final String direccion;
        final String nombre;
        final boolean pareceNelko;
        int senal;

        Encontrado(String direccion, String nombre, boolean pareceNelko, int senal) {
            this.direccion = direccion;
            this.nombre = nombre;
            this.pareceNelko = pareceNelko;
            this.senal = senal;
        }
    }

    @SuppressLint("MissingPermission")
    private void buscarImpresoras() {
        BluetoothAdapter adaptador = adaptador();
        BluetoothLeScanner scanner = adaptador != null ? adaptador.getBluetoothLeScanner() : null;
        if (scanner == null) {
            mostrarAviso(R.string.impresora_bluetooth_apagado_titulo, R.string.impresora_bluetooth_apagado_mensaje, false);
            return;
        }

        View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_buscar_impresora, null);
        LinearProgressIndicator progreso = vista.findViewById(R.id.progresoBusqueda);
        TextView tvEstado = vista.findViewById(R.id.tvEstadoBusqueda);
        ListView lista = vista.findViewById(R.id.lvImpresoras);

        Map<String, Encontrado> encontrados = new LinkedHashMap<>();
        List<Encontrado> ordenados = new ArrayList<>();
        ArrayAdapter<Encontrado> adapter = new ArrayAdapter<Encontrado>(requireContext(),
                android.R.layout.simple_list_item_2, android.R.id.text1, ordenados) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View fila = super.getView(position, convertView, parent);
                Encontrado e = getItem(position);
                TextView t1 = fila.findViewById(android.R.id.text1);
                TextView t2 = fila.findViewById(android.R.id.text2);
                if (e != null) {
                    t1.setText(e.pareceNelko ? getString(R.string.impresora_recomendada_formato, e.nombre) : e.nombre);
                    t2.setText(e.direccion);
                }
                return fila;
            }
        };
        lista.setAdapter(adapter);

        final boolean[] buscando = {true};
        ScanCallback callback = new ScanCallback() {
            @Override
            @SuppressLint("MissingPermission")
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice d = result.getDevice();
                String nombre = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                if (nombre == null || nombre.trim().isEmpty()) {
                    try {
                        nombre = d.getName();
                    } catch (SecurityException ignored) {
                        nombre = null;
                    }
                }
                if (nombre == null || nombre.trim().isEmpty()) return;
                List<ParcelUuid> servicios = result.getScanRecord() != null ? result.getScanRecord().getServiceUuids() : null;
                boolean nelko = nombre.toUpperCase(Locale.ROOT).startsWith("PM")
                        || (servicios != null && servicios.contains(SERVICIO_ANUNCIADO));
                Encontrado previo = encontrados.get(d.getAddress());
                if (previo != null) {
                    previo.senal = result.getRssi();
                    return;
                }
                encontrados.put(d.getAddress(), new Encontrado(d.getAddress(), nombre.trim(), nelko, result.getRssi()));
                handler.post(() -> {
                    ordenados.clear();
                    ordenados.addAll(encontrados.values());
                    Collections.sort(ordenados, (a, b) -> {
                        if (a.pareceNelko != b.pareceNelko) return a.pareceNelko ? -1 : 1;
                        return Integer.compare(b.senal, a.senal);
                    });
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onScanFailed(int errorCode) {
                handler.post(() -> {
                    progreso.setVisibility(View.GONE);
                    tvEstado.setText(getString(R.string.impresora_busqueda_fallo, errorCode));
                });
            }
        };

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.impresora_elige_titulo)
                .setView(vista)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .create();

        Runnable detener = () -> {
            if (!buscando[0]) return;
            buscando[0] = false;
            try {
                scanner.stopScan(callback);
            } catch (Exception ignored) {
            }
            if (progreso.isAttachedToWindow()) {
                progreso.setVisibility(View.GONE);
                tvEstado.setText(encontrados.isEmpty()
                        ? R.string.impresora_busqueda_sin_resultados
                        : R.string.impresora_busqueda_terminada);
            }
        };

        lista.setOnItemClickListener((parent, v, position, id) -> {
            Encontrado e = ordenados.get(position);
            preferencias.guardar(e.direccion, e.nombre);
            detener.run();
            dialogo.dismiss();
            actualizarImpresora();
            mostrarEstado(getString(R.string.impresora_guardada, e.nombre), false);
        });
        dialogo.setOnDismissListener(d -> detener.run());

        ScanSettings ajustes = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        dialogo.show();
        try {
            scanner.startScan(null, ajustes, callback);
        } catch (SecurityException e) {
            dialogo.dismiss();
            mostrarAviso(R.string.impresora_sin_permiso_titulo, R.string.impresora_sin_permiso_mensaje, true);
            return;
        }
        handler.postDelayed(detener, DURACION_BUSQUEDA_MS);
    }

    // ---------- Impresión de prueba ----------

    private Pedido pedidoDePrueba() {
        long ahora = System.currentTimeMillis();
        Pedido p = new Pedido();
        p.setNumero(0);
        p.setClienteNombre(getString(R.string.prueba_cliente));
        p.setNombreEvento(getString(R.string.prueba_evento));
        p.setDireccion(getString(R.string.prueba_lugar));
        p.setPaqueteNombre(getString(R.string.prueba_paquete));
        p.setTotal(0.0);
        p.setInicioMillis(ahora + 24 * 3600_000L);
        p.setFinMillis(ahora + 28 * 3600_000L);
        p.setPagoConfirmado(true);
        p.setMetodoPago(Pedido.METODO_EFECTIVO);
        p.setFechaPagoMillis(ahora);
        Usuario socio = SonidoSonicApp.getApp(requireContext()).getAppContainer().getSessionManager().getUsuarioActual();
        p.setPagoConfirmadoPorNombre(socio != null ? socio.getNombre() : null);
        return p;
    }

    private void imprimirPrueba() {
        String direccion = preferencias.getDireccion();
        if (direccion == null || imprimiendo) return;

        // Se regenera para que la fecha y hora sean las de este momento
        ticketPrueba = TicketRender.generarBitmap(requireContext(), pedidoDePrueba());
        ivVistaPrevia.setImageBitmap(ticketPrueba);
        if (ticketPrueba.getHeight() > TicketRender.ALTO_MAXIMO_PUNTOS) {
            mostrarEstado(getString(R.string.impresora_ticket_muy_largo), true);
            return;
        }

        imprimiendo = true;
        actualizarImpresora();
        progresoImpresion.setVisibility(View.VISIBLE);
        mostrarEstado(getString(R.string.impresora_preparando), false);

        ImpresoraBle.imprimir(requireContext(), direccion, ticketPrueba, new ImpresoraBle.Listener() {
            @Override
            public void onProgreso(String mensaje) {
                if (isAdded() && getView() != null) mostrarEstado(mensaje, false);
            }

            @Override
            public void onTerminado(boolean confirmado) {
                terminar(getString(confirmado ? R.string.impresora_listo : R.string.impresora_enviado_sin_confirmar), false);
            }

            @Override
            public void onError(String mensaje) {
                terminar(mensaje, true);
            }
        });
    }

    private void terminar(String mensaje, boolean error) {
        imprimiendo = false;
        if (!isAdded() || getView() == null) return;
        progresoImpresion.setVisibility(View.GONE);
        actualizarImpresora();
        mostrarEstado(mensaje, error);
    }

    private void mostrarEstado(String mensaje, boolean error) {
        tvEstadoImpresion.setVisibility(View.VISIBLE);
        tvEstadoImpresion.setText(mensaje);
        tvEstadoImpresion.setTextColor(com.google.android.material.color.MaterialColors.getColor(tvEstadoImpresion,
                error ? androidx.appcompat.R.attr.colorError : com.google.android.material.R.attr.colorOnSurface));
    }
}
