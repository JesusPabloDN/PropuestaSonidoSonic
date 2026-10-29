package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.RelojSistema;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InMemoryPagoRepository implements PagoRepository {
    public static final String COLECCION_PAGOS = "pagos";

    private final List<Pago> pagos = new ArrayList<>();
    private final MutableLiveData<List<Pago>> todosLosPagosLiveData = new MutableLiveData<>(new ArrayList<>());
    private final Map<String, MutableLiveData<List<Pago>>> pagosPorPedidoMap = new HashMap<>();
    private final Reloj reloj;
    private long consecutivoFolio = 1;
    private FirebaseFirestore firestore;

    public InMemoryPagoRepository() {
        this(new RelojSistema());
    }

    public InMemoryPagoRepository(Reloj reloj) {
        this.reloj = reloj != null ? reloj : new RelojSistema();
    }

    public synchronized void conectarFirestore(FirebaseFirestore db) {
        if (db == null || this.firestore != null) return;
        this.firestore = db;
        this.firestore.collection(COLECCION_PAGOS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    synchronized (InMemoryPagoRepository.this) {
                        pagos.clear();
                        long maxFolio = 0;
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Pago p = doc.toObject(Pago.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().trim().isEmpty()) {
                                    p.setId(doc.getId());
                                }
                                pagos.add(p);
                                long num = extraerConsecutivoFolio(p.getFolio());
                                if (num > maxFolio) {
                                    maxFolio = num;
                                }
                            }
                        }
                        if (maxFolio >= consecutivoFolio) {
                            consecutivoFolio = maxFolio + 1;
                        }
                        notificarTodos();
                    }
                });
    }

    private long extraerConsecutivoFolio(String folio) {
        if (folio == null) return 0;
        String limpio = folio.trim();
        if (limpio.startsWith("R-") || limpio.startsWith("r-")) {
            try {
                return Long.parseLong(limpio.substring(2));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void guardarEnFirestore(Pago pago) {
        if (firestore != null && pago != null && pago.getId() != null) {
            firestore.collection(COLECCION_PAGOS)
                    .document(pago.getId())
                    .set(new Pago(pago));
        }
    }

    private void notificarTodos() {
        List<Pago> copiaTodos = new ArrayList<>();
        for (Pago p : pagos) {
            copiaTodos.add(new Pago(p));
        }
        todosLosPagosLiveData.setValue(copiaTodos);

        for (Map.Entry<String, MutableLiveData<List<Pago>>> entry : pagosPorPedidoMap.entrySet()) {
            String pedidoId = entry.getKey();
            MutableLiveData<List<Pago>> liveData = entry.getValue();
            if (liveData != null && pedidoId != null) {
                List<Pago> filtrados = new ArrayList<>();
                for (Pago p : pagos) {
                    if (pedidoId.equals(p.getPedidoId())) {
                        filtrados.add(new Pago(p));
                    }
                }
                liveData.setValue(filtrados);
            }
        }
    }

    private void notificarCambios(String pedidoId) {
        List<Pago> copiaTodos = new ArrayList<>();
        for (Pago p : pagos) {
            copiaTodos.add(new Pago(p));
        }
        todosLosPagosLiveData.setValue(copiaTodos);

        if (pedidoId != null) {
            MutableLiveData<List<Pago>> liveData = pagosPorPedidoMap.get(pedidoId);
            if (liveData != null) {
                List<Pago> filtrados = new ArrayList<>();
                for (Pago p : pagos) {
                    if (pedidoId.equals(p.getPedidoId())) {
                        filtrados.add(new Pago(p));
                    }
                }
                liveData.setValue(filtrados);
            }
        }
    }

    @Override
    public synchronized LiveData<List<Pago>> getPagosPorPedido(String pedidoId) {
        if (!pagosPorPedidoMap.containsKey(pedidoId)) {
            pagosPorPedidoMap.put(pedidoId, new MutableLiveData<>(new ArrayList<>()));
        }
        notificarCambios(pedidoId);
        return pagosPorPedidoMap.get(pedidoId);
    }

    @Override
    public LiveData<List<Pago>> getTodosLosPagos() {
        return todosLosPagosLiveData;
    }

    @Override
    public synchronized void registrarPago(Pago pago, Callback<Pago> callback) {
        if (pago == null) {
            if (callback != null) callback.onError("Pago nulo");
            return;
        }

        if (pago.getMonto() <= 0) {
            if (callback != null) callback.onError("El monto debe ser mayor a 0");
            return;
        }

        if (pago.getTipo() == null || pago.getTipo().trim().isEmpty()) {
            if (callback != null) callback.onError("Tipo de pago requerido");
            return;
        }

        if (pago.getMetodo() == null || pago.getMetodo().trim().isEmpty()) {
            if (callback != null) callback.onError("Método de pago requerido");
            return;
        }

        if (pago.getFolio() == null || pago.getFolio().trim().isEmpty()) {
            pago.setFolio(String.format(Locale.US, "R-%04d", consecutivoFolio++));
        }

        if (pago.getFecha() <= 0) {
            pago.setFecha(reloj.ahora());
        }

        Pago guardado = new Pago(pago);
        pagos.add(guardado);
        guardarEnFirestore(guardado);
        notificarCambios(guardado.getPedidoId());
        if (callback != null) {
            callback.onSuccess(new Pago(guardado));
        }
    }
}
