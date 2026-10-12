package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.ChecklistItem;
import com.example.propuestasonidosonic.model.Evento;
import java.util.List;
import java.util.Map;

public interface EventoRepository {
    LiveData<List<Evento>> getEventos();
    void getEventoPorId(String id, Callback<Evento> callback);
    void crearEvento(Evento evento, Callback<Evento> callback);
    void actualizarEvento(Evento evento, Callback<Evento> callback);
    void cancelarEvento(String id, Callback<Evento> callback);
    void finalizarEvento(String id, Callback<Evento> callback);
    void actualizarChecklist(String eventoId, List<ChecklistItem> checklist, Callback<Void> callback);
    LiveData<List<Evento>> eventosDelDia(long fechaMillis);
    List<Evento> reservasVigentes(long ahoraMillis);
    List<Evento> reservasFuturas(String unidadId, long ahoraMillis);
    List<Evento> eventosActivosParaUnidad(String unidadId);
    List<String> unidadesOcupadas(long inicioMillis, long finMillis, String ignorarEventoId);
    void reemplazarAsignaciones(Map<String, List<String>> equipoIdsPorEventoId, Callback<Void> callback);
}
