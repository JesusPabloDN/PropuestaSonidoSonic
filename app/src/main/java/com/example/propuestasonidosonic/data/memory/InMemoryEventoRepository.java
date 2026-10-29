package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.Evento;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class InMemoryEventoRepository implements EventoRepository {
    public static final String COLECCION_EVENTOS = "eventos";

    private final List<Evento> eventos = new ArrayList<>();
    private final MutableLiveData<List<Evento>> eventosLiveData = new MutableLiveData<>(new ArrayList<>());
    private FirebaseFirestore firestore;

    public InMemoryEventoRepository() {}

    public synchronized void conectarFirestore(FirebaseFirestore db) {
        if (db == null || this.firestore != null) return;
        this.firestore = db;
        this.firestore.collection(COLECCION_EVENTOS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    synchronized (InMemoryEventoRepository.this) {
                        eventos.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Evento ev = doc.toObject(Evento.class);
                            if (ev != null) {
                                if (ev.getId() == null || ev.getId().trim().isEmpty()) {
                                    ev.setId(doc.getId());
                                }
                                eventos.add(ev);
                            }
                        }
                        notificarCambios();
                    }
                });
    }

    private void guardarEnFirestore(Evento evento) {
        if (firestore != null && evento != null && evento.getId() != null) {
            firestore.collection(COLECCION_EVENTOS)
                    .document(evento.getId())
                    .set(evento);
        }
    }

    private void notificarCambios() {
        eventosLiveData.setValue(new ArrayList<>(eventos));
    }

    private long desde(Evento ev) {
        if (ev == null) return 0L;
        return ev.getBloqueoInicioMillis() > 0 ? ev.getBloqueoInicioMillis() : ev.getInicioMillis();
    }

    private long hasta(Evento ev) {
        if (ev == null) return 0L;
        return ev.getBloqueoFinMillis() > 0 ? ev.getBloqueoFinMillis() : ev.getFinMillis();
    }

    @Override
    public LiveData<List<Evento>> getEventos() {
        return eventosLiveData;
    }

    @Override
    public synchronized void getEventoPorId(String id, Callback<Evento> callback) {
        for (Evento ev : eventos) {
            if (ev.getId().equals(id)) {
                callback.onSuccess(ev);
                return;
            }
        }
        callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized void crearEvento(Evento evento, Callback<Evento> callback) {
        // Verificación básica de traslape
        if (evento.getEquipoIds() != null && !evento.getEquipoIds().isEmpty()) {
            List<String> ocupadas = unidadesOcupadas(desde(evento), hasta(evento), null);
            for (String eqId : evento.getEquipoIds()) {
                if (ocupadas.contains(eqId)) {
                    if (callback != null) callback.onError("El equipo ya se encuentra ocupado en ese horario");
                    return;
                }
            }
        }
        eventos.add(evento);
        guardarEnFirestore(evento);
        notificarCambios();
        if (callback != null) callback.onSuccess(evento);
    }

    @Override
    public synchronized void actualizarEvento(Evento evento, Callback<Evento> callback) {
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).getId().equals(evento.getId())) {
                eventos.set(i, evento);
                guardarEnFirestore(evento);
                notificarCambios();
                if (callback != null) callback.onSuccess(evento);
                return;
            }
        }
        if (callback != null) callback.onError("Evento no encontrado para actualizar");
    }

    @Override
    public synchronized void cancelarEvento(String id, Callback<Evento> callback) {
        for (Evento ev : eventos) {
            if (ev.getId().equals(id)) {
                ev.setEstado(Evento.ESTADO_CANCELADO);
                guardarEnFirestore(ev);
                notificarCambios();
                if (callback != null) callback.onSuccess(ev);
                return;
            }
        }
        if (callback != null) callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized void finalizarEvento(String id, Callback<Evento> callback) {
        for (Evento ev : eventos) {
            if (ev.getId().equals(id)) {
                ev.setEstado(Evento.ESTADO_FINALIZADO);
                guardarEnFirestore(ev);
                notificarCambios();
                if (callback != null) callback.onSuccess(ev);
                return;
            }
        }
        if (callback != null) callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized void actualizarChecklist(String eventoId, List<ChecklistItem> checklist, Callback<Void> callback) {
        for (Evento ev : eventos) {
            if (ev.getId().equals(eventoId)) {
                ev.setChecklist(checklist);
                guardarEnFirestore(ev);
                notificarCambios();
                if (callback != null) {
                    callback.onSuccess(null);
                }
                return;
            }
        }
        if (callback != null) {
            callback.onError("Evento no encontrado");
        }
    }

    private List<Evento> filtrarEventosDelDia(List<Evento> fuente, long fechaMillis) {
        Calendar calTarget = Calendar.getInstance();
        calTarget.setTimeInMillis(fechaMillis);

        List<Evento> res = new ArrayList<>();
        if (fuente == null) return res;
        Calendar calEv = Calendar.getInstance();
        for (Evento ev : fuente) {
            calEv.setTimeInMillis(ev.getInicioMillis());
            if (calEv.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calEv.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)) {
                res.add(ev);
            }
        }
        return res;
    }

    @Override
    public synchronized LiveData<List<Evento>> eventosDelDia(long fechaMillis) {
        MediatorLiveData<List<Evento>> delDia = new MediatorLiveData<>();
        delDia.setValue(filtrarEventosDelDia(eventos, fechaMillis));
        delDia.addSource(eventosLiveData, lista -> delDia.setValue(filtrarEventosDelDia(lista, fechaMillis)));
        return delDia;
    }

    @Override
    public synchronized List<Evento> reservasVigentes(long ahoraMillis) {
        List<Evento> activas = new ArrayList<>();
        for (Evento ev : eventos) {
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado()) && !Evento.ESTADO_FINALIZADO.equals(ev.getEstado())) {
                if (desde(ev) <= ahoraMillis && ahoraMillis <= hasta(ev)) {
                    activas.add(ev);
                }
            }
        }
        return activas;
    }

    @Override
    public synchronized List<Evento> reservasFuturas(String unidadId, long ahoraMillis) {
        List<Evento> futuras = new ArrayList<>();
        for (Evento ev : eventos) {
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado()) && !Evento.ESTADO_FINALIZADO.equals(ev.getEstado())) {
                // Evento cuya ventana de reserva/bloqueo inicia después de ahora
                if (desde(ev) > ahoraMillis) {
                    if (ev.getEquipoIds() != null && ev.getEquipoIds().contains(unidadId)) {
                        futuras.add(ev);
                    }
                }
            }
        }
        futuras.sort((e1, e2) -> Long.compare(desde(e1), desde(e2)));
        return futuras;
    }

    @Override
    public synchronized List<Evento> eventosActivosParaUnidad(String unidadId) {
        List<Evento> activos = new ArrayList<>();
        for (Evento ev : eventos) {
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado()) && !Evento.ESTADO_FINALIZADO.equals(ev.getEstado())) {
                if (ev.getEquipoIds() != null && ev.getEquipoIds().contains(unidadId)) {
                    activos.add(ev);
                }
            }
        }
        return activos;
    }

    @Override
    public synchronized List<String> unidadesOcupadas(long inicioMillis, long finMillis, String ignorarEventoId) {
        List<String> ocupadas = new ArrayList<>();
        for (Evento ev : eventos) {
            if (ignorarEventoId != null && ignorarEventoId.equals(ev.getId())) {
                continue;
            }
            if (Evento.ESTADO_CANCELADO.equals(ev.getEstado()) || Evento.ESTADO_FINALIZADO.equals(ev.getEstado())) {
                continue;
            }
            // Traslape sobre la ventana de bloqueo/evento: desde(ev) < finMillis && inicioMillis < hasta(ev)
            if (desde(ev) < finMillis && inicioMillis < hasta(ev)) {
                if (ev.getEquipoIds() != null) {
                    for (String eqId : ev.getEquipoIds()) {
                        if (!ocupadas.contains(eqId)) {
                            ocupadas.add(eqId);
                        }
                    }
                }
            }
        }
        return ocupadas;
    }

    @Override
    public synchronized void reemplazarAsignaciones(Map<String, List<String>> equipoIdsPorEventoId, Callback<Void> callback) {
        if (equipoIdsPorEventoId != null) {
            for (Evento ev : eventos) {
                if (Evento.ESTADO_CANCELADO.equals(ev.getEstado()) || Evento.ESTADO_FINALIZADO.equals(ev.getEstado())) {
                    continue;
                }
                if (equipoIdsPorEventoId.containsKey(ev.getId())) {
                    List<String> nuevosIds = equipoIdsPorEventoId.get(ev.getId());
                    ev.setEquipoIds(nuevosIds != null ? new ArrayList<>(nuevosIds) : new ArrayList<String>());
                    guardarEnFirestore(ev);
                }
            }
            notificarCambios();
        }
        if (callback != null) {
            callback.onSuccess(null);
        }
    }
}
