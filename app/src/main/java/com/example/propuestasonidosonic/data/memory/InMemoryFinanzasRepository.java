package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.FinanzasRepository;
import com.example.propuestasonidosonic.model.MovimientoFinanciero;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFinanzasRepository implements FinanzasRepository {
    public static final String COLECCION_MOVIMIENTOS = "movimientos_financieros";

    private final List<MovimientoFinanciero> movimientos = new ArrayList<>();
    private final MutableLiveData<List<MovimientoFinanciero>> movimientosLiveData = new MutableLiveData<>(new ArrayList<>());
    private FirebaseFirestore firestore;

    public InMemoryFinanzasRepository() {}

    public synchronized void conectarFirestore(FirebaseFirestore db) {
        if (db == null || this.firestore != null) return;
        this.firestore = db;
        this.firestore.collection(COLECCION_MOVIMIENTOS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    synchronized (InMemoryFinanzasRepository.this) {
                        movimientos.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            MovimientoFinanciero m = doc.toObject(MovimientoFinanciero.class);
                            if (m != null) {
                                if (m.getId() == null || m.getId().trim().isEmpty()) {
                                    m.setId(doc.getId());
                                }
                                movimientos.add(m);
                            }
                        }
                        notificarCambios();
                    }
                });
    }

    private void guardarEnFirestore(MovimientoFinanciero movimiento) {
        if (firestore != null && movimiento != null && movimiento.getId() != null) {
            firestore.collection(COLECCION_MOVIMIENTOS)
                    .document(movimiento.getId())
                    .set(movimiento);
        }
    }

    private void eliminarEnFirestore(String id) {
        if (firestore != null && id != null) {
            firestore.collection(COLECCION_MOVIMIENTOS)
                    .document(id)
                    .delete();
        }
    }

    private void notificarCambios() {
        movimientosLiveData.setValue(new ArrayList<>(movimientos));
    }

    @Override
    public LiveData<List<MovimientoFinanciero>> getMovimientos() {
        return movimientosLiveData;
    }

    @Override
    public synchronized void registrarMovimiento(MovimientoFinanciero movimiento, Callback<MovimientoFinanciero> callback) {
        movimientos.add(movimiento);
        guardarEnFirestore(movimiento);
        notificarCambios();
        callback.onSuccess(movimiento);
    }

    @Override
    public synchronized void eliminarMovimiento(String id, Callback<Void> callback) {
        boolean removed = movimientos.removeIf(m -> m.getId().equals(id));
        if (removed) {
            eliminarEnFirestore(id);
            notificarCambios();
            callback.onSuccess(null);
        } else {
            callback.onError("Movimiento no encontrado");
        }
    }
}
