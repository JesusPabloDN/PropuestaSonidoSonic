package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.model.BitacoraEntrada;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEquipoRepository implements EquipoRepository {
    private final List<Equipo> equipos = new ArrayList<>();
    private final MutableLiveData<List<Equipo>> equiposLiveData = new MutableLiveData<>(new ArrayList<>());

    private final List<BitacoraEntrada> bitacoras = new ArrayList<>();
    private final Map<String, MutableLiveData<List<BitacoraEntrada>>> bitacoraLiveDataMap = new HashMap<>();

    private EventoRepository eventoRepository;

    public InMemoryEquipoRepository() {}

    public void setEventoRepository(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    private void notificarEquipos() {
        equiposLiveData.setValue(new ArrayList<>(equipos));
    }

    private void notificarBitacora(String equipoId) {
        MutableLiveData<List<BitacoraEntrada>> liveData = bitacoraLiveDataMap.get(equipoId);
        if (liveData != null) {
            List<BitacoraEntrada> filtradas = new ArrayList<>();
            for (BitacoraEntrada b : bitacoras) {
                if (b.getEquipoId().equals(equipoId)) {
                    filtradas.add(b);
                }
            }
            filtradas.sort((b1, b2) -> Long.compare(b2.getFecha(), b1.getFecha()));
            liveData.setValue(filtradas);
        }
    }

    @Override
    public LiveData<List<Equipo>> getEquipos() {
        return equiposLiveData;
    }

    @Override
    public synchronized void getEquipoPorId(String id, Callback<Equipo> callback) {
        for (Equipo e : equipos) {
            if (e.getId().equals(id)) {
                callback.onSuccess(e);
                return;
            }
        }
        callback.onError("Equipo no encontrado");
    }

    @Override
    public synchronized void agregarEquipo(Equipo equipo, Callback<Equipo> callback) {
        // FIRESTORE: aquí irá una transacción
        equipos.add(equipo);
        notificarEquipos();
        callback.onSuccess(equipo);
    }

    @Override
    public synchronized void agregarUnidades(String nombre, String categoria, double precioRenta,
                                            String estadoFisico, int cantidad, Callback<List<Equipo>> callback) {
        agregarUnidades(nombre, categoria, precioRenta, estadoFisico, cantidad, null, callback);
    }

    @Override
    public synchronized void agregarUnidades(String nombre, String categoria, double precioRenta,
                                            String estadoFisico, int cantidad, String fotoUri, Callback<List<Equipo>> callback) {
        // FIRESTORE: aquí irá una transacción
        int maxNumero = 0;
        for (Equipo e : equipos) {
            if (e.getNombre().equalsIgnoreCase(nombre)) {
                if (e.getNumeroUnidad() > maxNumero) {
                    maxNumero = e.getNumeroUnidad();
                }
            }
        }

        List<Equipo> nuevos = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            Equipo nuevo = new Equipo();
            nuevo.setNombre(nombre);
            nuevo.setCategoria(categoria);
            nuevo.setPrecioRenta(precioRenta);
            nuevo.setEstadoFisico(estadoFisico);
            nuevo.setNumeroUnidad(maxNumero + i);
            nuevo.setFotoUri(fotoUri);
            equipos.add(nuevo);
            nuevos.add(nuevo);
        }
        notificarEquipos();
        callback.onSuccess(nuevos);
    }

    @Override
    public synchronized void actualizar(Equipo equipo, Callback<Equipo> callback) {
        // FIRESTORE: aquí irá una transacción
        for (int i = 0; i < equipos.size(); i++) {
            if (equipos.get(i).getId().equals(equipo.getId())) {
                equipos.set(i, equipo);
                notificarEquipos();
                callback.onSuccess(equipo);
                return;
            }
        }
        callback.onError("Equipo no encontrado");
    }

    @Override
    public synchronized void eliminar(String id, Callback<Void> callback) {
        // Verificar si la unidad está en un evento no cancelado
        if (eventoRepository != null) {
            List<Evento> activos = eventoRepository.eventosActivosParaUnidad(id);
            if (!activos.isEmpty()) {
                Evento ev = activos.get(0);
                callback.onError("No se puede eliminar la unidad porque está reservada en el evento «" + ev.getNombre() + "»");
                return;
            }
        }

        boolean removed = equipos.removeIf(e -> e.getId().equals(id));
        if (removed) {
            notificarEquipos();
            callback.onSuccess(null);
        } else {
            callback.onError("Equipo no encontrado para eliminar");
        }
    }

    @Override
    public synchronized void cambiarEstado(String equipoId, String nuevoEstado, Callback<Equipo> callback) {
        // FIRESTORE: aquí irá una transacción
        for (Equipo e : equipos) {
            if (e.getId().equals(equipoId)) {
                e.setEstadoFisico(nuevoEstado);
                notificarEquipos();
                callback.onSuccess(e);
                return;
            }
        }
        callback.onError("Equipo no encontrado");
    }

    @Override
    public synchronized void agregarBitacora(BitacoraEntrada entrada, Callback<BitacoraEntrada> callback) {
        // FIRESTORE: aquí irá una transacción
        bitacoras.add(entrada);
        notificarBitacora(entrada.getEquipoId());
        callback.onSuccess(entrada);
    }

    @Override
    public synchronized LiveData<List<BitacoraEntrada>> obtenerBitacora(String equipoId) {
        if (!bitacoraLiveDataMap.containsKey(equipoId)) {
            bitacoraLiveDataMap.put(equipoId, new MutableLiveData<>(new ArrayList<>()));
        }
        notificarBitacora(equipoId);
        return bitacoraLiveDataMap.get(equipoId);
    }

    @Override
    public synchronized String estadoEfectivo(Equipo equipo, long ahoraMillis) {
        // El estado físico manda si es reparación o dañado
        if (Equipo.ESTADO_EN_REPARACION.equals(equipo.getEstadoFisico())) {
            return Equipo.ESTADO_EN_REPARACION;
        }
        if (Equipo.ESTADO_DANADO.equals(equipo.getEstadoFisico())) {
            return Equipo.ESTADO_DANADO;
        }
        // "Rentado" es derivado de una reserva vigente en este momento
        if (eventoRepository != null) {
            List<Evento> activas = eventoRepository.reservasVigentes(ahoraMillis);
            for (Evento ev : activas) {
                if (ev.getEquipoIds() != null && ev.getEquipoIds().contains(equipo.getId())) {
                    return Equipo.ESTADO_RENTADO;
                }
            }
        }
        return Equipo.ESTADO_DISPONIBLE;
    }

    @Override
    public synchronized Evento proximaReserva(String equipoId, long ahoraMillis) {
        if (eventoRepository != null) {
            List<Evento> futuras = eventoRepository.reservasFuturas(equipoId, ahoraMillis);
            if (!futuras.isEmpty()) {
                return futuras.get(0);
            }
        }
        return null;
    }

    @Override
    public synchronized List<com.example.propuestasonidosonic.model.ModeloInventario> obtenerModelos() {
        java.util.Map<String, com.example.propuestasonidosonic.model.ModeloInventario> modelosMap = new java.util.LinkedHashMap<>();
        for (Equipo e : equipos) {
            String clave = com.example.propuestasonidosonic.util.ModeloUtils.clave(e.getCategoria(), e.getNombre());
            com.example.propuestasonidosonic.model.ModeloInventario mod = modelosMap.get(clave);
            if (mod == null) {
                mod = new com.example.propuestasonidosonic.model.ModeloInventario();
                mod.setClave(clave);
                mod.setNombreModelo(com.example.propuestasonidosonic.util.ModeloUtils.nombreBase(e.getNombre()));
                mod.setCategoria(e.getCategoria());
                mod.setPrecioUnitario(e.getPrecioRenta());
                mod.setTotalUnidades(0);
                mod.setUnidadesDisponibles(0);
                modelosMap.put(clave, mod);
            }
            mod.setTotalUnidades(mod.getTotalUnidades() + 1);
            if (Equipo.ESTADO_DISPONIBLE.equals(e.getEstadoFisico())) {
                mod.setUnidadesDisponibles(mod.getUnidadesDisponibles() + 1);
            }
        }

        List<com.example.propuestasonidosonic.model.ModeloInventario> lista = new ArrayList<>(modelosMap.values());
        lista.sort((m1, m2) -> {
            int comp = m1.getCategoria().compareToIgnoreCase(m2.getCategoria());
            if (comp != 0) return comp;
            return m1.getNombreModelo().compareToIgnoreCase(m2.getNombreModelo());
        });
        return lista;
    }

    @Override
    public synchronized com.example.propuestasonidosonic.model.ModeloInventario obtenerModelo(String clave) {
        if (clave == null) return null;
        for (com.example.propuestasonidosonic.model.ModeloInventario mod : obtenerModelos()) {
            if (clave.equals(mod.getClave())) {
                return mod;
            }
        }
        return null;
    }
}
