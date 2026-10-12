package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.Evento;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InMemoryEventoRepository implements EventoRepository {
    private final List<Evento> eventos = new ArrayList<>();
    private final MutableLiveData<List<Evento>> eventosLiveData = new MutableLiveData<>(new ArrayList<>());

    public InMemoryEventoRepository() {}

    private void notificarCambios() {
        eventosLiveData.setValue(new ArrayList<>(eventos));
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
        // FIRESTORE: aquí irá una transacción
        // Verificación básica de traslape
        if (evento.getEquipoIds() != null && !evento.getEquipoIds().isEmpty()) {
            List<String> ocupadas = unidadesOcupadas(evento.getInicioMillis(), evento.getFinMillis(), null);
            for (String eqId : evento.getEquipoIds()) {
                if (ocupadas.contains(eqId)) {
                    callback.onError("El equipo ya se encuentra ocupado en ese horario");
                    return;
                }
            }
        }
        eventos.add(evento);
        notificarCambios();
        callback.onSuccess(evento);
    }

    @Override
    public synchronized void actualizarEvento(Evento evento, Callback<Evento> callback) {
        // FIRESTORE: aquí irá una transacción
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).getId().equals(evento.getId())) {
                eventos.set(i, evento);
                notificarCambios();
                callback.onSuccess(evento);
                return;
            }
        }
        callback.onError("Evento no encontrado para actualizar");
    }

    @Override
    public synchronized void cancelarEvento(String id, Callback<Evento> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Evento ev : eventos) {
            if (ev.getId().equals(id)) {
                ev.setEstado(Evento.ESTADO_CANCELADO);
                notificarCambios();
                callback.onSuccess(ev);
                return;
            }
        }
        callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized void finalizarEvento(String id, Callback<Evento> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Evento ev : eventos) {
            if (ev.getId().equals(id)) {
                ev.setEstado(Evento.ESTADO_FINALIZADO);
                notificarCambios();
                callback.onSuccess(ev);
                return;
            }
        }
        callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized void actualizarChecklist(String eventoId, List<ChecklistItem> checklist, Callback<Void> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Evento ev : eventos) {
            if (ev.getId().equals(eventoId)) {
                ev.setChecklist(checklist);
                notificarCambios();
                callback.onSuccess(null);
                return;
            }
        }
        callback.onError("Evento no encontrado");
    }

    @Override
    public synchronized LiveData<List<Evento>> eventosDelDia(long fechaMillis) {
        MutableLiveData<List<Evento>> delDia = new MutableLiveData<>(new ArrayList<>());
        Calendar calTarget = Calendar.getInstance();
        calTarget.setTimeInMillis(fechaMillis);

        List<Evento> res = new ArrayList<>();
        Calendar calEv = Calendar.getInstance();
        for (Evento ev : eventos) {
            calEv.setTimeInMillis(ev.getInicioMillis());
            if (calEv.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calEv.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)) {
                res.add(ev);
            }
        }
        delDia.setValue(res);
        return delDia;
    }

    @Override
    public synchronized List<Evento> reservasVigentes(long ahoraMillis) {
        List<Evento> activas = new ArrayList<>();
        for (Evento ev : eventos) {
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado())) {
                if (ev.getInicioMillis() <= ahoraMillis && ahoraMillis <= ev.getFinMillis()) {
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
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado())) {
                // Evento que inicia después de ahora
                if (ev.getInicioMillis() > ahoraMillis) {
                    if (ev.getEquipoIds() != null && ev.getEquipoIds().contains(unidadId)) {
                        futuras.add(ev);
                    }
                }
            }
        }
        futuras.sort((e1, e2) -> Long.compare(e1.getInicioMillis(), e2.getInicioMillis()));
        return futuras;
    }

    @Override
    public synchronized List<Evento> eventosActivosParaUnidad(String unidadId) {
        List<Evento> activos = new ArrayList<>();
        for (Evento ev : eventos) {
            if (!Evento.ESTADO_CANCELADO.equals(ev.getEstado())) {
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
            if (Evento.ESTADO_CANCELADO.equals(ev.getEstado())) {
                continue;
            }
            // Traslape: inicioA < finB && inicioB < finA
            if (ev.getInicioMillis() < finMillis && inicioMillis < ev.getFinMillis()) {
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
}
