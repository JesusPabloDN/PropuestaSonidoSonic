package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InMemoryPaqueteRepository implements PaqueteRepository {
    private final List<Paquete> paquetes = new ArrayList<>();
    private final MutableLiveData<List<Paquete>> paquetesLiveData = new MutableLiveData<>(new ArrayList<>());
    private EquipoRepository equipoRepository;

    public InMemoryPaqueteRepository() {}

    public InMemoryPaqueteRepository(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public void setEquipoRepository(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    private void notificarCambios() {
        List<Paquete> copiaLista = new ArrayList<>();
        for (Paquete p : paquetes) {
            copiaLista.add(new Paquete(p));
        }
        copiaLista.sort((p1, p2) -> {
            String n1 = p1.getNombre() != null ? p1.getNombre() : "";
            String n2 = p2.getNombre() != null ? p2.getNombre() : "";
            return n1.compareToIgnoreCase(n2);
        });
        paquetesLiveData.setValue(copiaLista);
    }

    @Override
    public LiveData<List<Paquete>> getPaquetes() {
        return paquetesLiveData;
    }

    @Override
    public synchronized Paquete getPaquetePorId(String id) {
        if (id == null) return null;
        for (Paquete p : paquetes) {
            if (p.getId().equals(id)) {
                return new Paquete(p);
            }
        }
        return null;
    }

    @Override
    public synchronized void guardarPaquete(Paquete paquete, Callback<Paquete> callback) {
        // FIRESTORE: transacción
        if (paquete == null) {
            callback.onError("El paquete no puede ser nulo");
            return;
        }

        // 1. Nombre obligatorio, max 60 caracteres, único sin distinguir mayúsculas
        String nombre = paquete.getNombre() != null ? paquete.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            callback.onError("El nombre del paquete es obligatorio");
            return;
        }
        if (nombre.length() > 60) {
            callback.onError("El nombre no puede exceder 60 caracteres");
            return;
        }
        for (Paquete existente : paquetes) {
            if (!existente.getId().equals(paquete.getId())) {
                if (existente.getNombre().equalsIgnoreCase(nombre)) {
                    callback.onError("Ya existe un paquete con ese nombre");
                    return;
                }
            }
        }

        // 2. Componentes con al menos 1 elemento
        List<ComponentePaquete> componentes = paquete.getComponentes();
        if (componentes == null || componentes.isEmpty()) {
            callback.onError("El paquete debe incluir al menos un componente");
            return;
        }

        // 3. Cada componente con cantidad >= 1, sin duplicados
        Set<String> clavesVistas = new HashSet<>();
        for (ComponentePaquete c : componentes) {
            if (c.getCantidad() < 1) {
                callback.onError("La cantidad de cada componente debe ser al menos 1");
                return;
            }
            if (c.getModeloClave() == null || !clavesVistas.add(c.getModeloClave())) {
                callback.onError("No puede haber componentes duplicados en el paquete");
                return;
            }

            // 4. Cada modeloClave debe existir hoy en inventario
            if (equipoRepository != null) {
                ModeloInventario mod = equipoRepository.obtenerModelo(c.getModeloClave());
                if (mod == null) {
                    callback.onError("El modelo " + (c.getNombreModelo() != null ? c.getNombreModelo() : "") + " ya no existe en el inventario");
                    return;
                }
                // 5. Cantidad <= totalUnidades del modelo
                if (c.getCantidad() > mod.getTotalUnidades()) {
                    callback.onError("Solo hay " + mod.getTotalUnidades() + " unidades de " + mod.getNombreModelo());
                    return;
                }
                // Normalizar datos desde inventario
                c.setNombreModelo(mod.getNombreModelo());
                c.setCategoria(mod.getCategoria());
            }
        }

        // 6. Precio > 0
        if (paquete.getPrecio() <= 0) {
            callback.onError("El precio debe ser mayor a 0");
            return;
        }

        // 7. Duración base entre 1 y 24
        if (paquete.getDuracionBaseHoras() < 1 || paquete.getDuracionBaseHoras() > 24) {
            callback.onError("La duración base debe ser entre 1 y 24 horas");
            return;
        }

        // 8. Precio hora extra >= 0
        if (paquete.getPrecioHoraExtra() < 0) {
            callback.onError("El precio por hora extra debe ser mayor o igual a 0");
            return;
        }

        // 9. Operadores requeridos 1 o 2
        if (paquete.getOperadoresRequeridos() != 1 && paquete.getOperadoresRequeridos() != 2) {
            callback.onError("El personal requerido debe ser 1 o 2 personas");
            return;
        }

        // 10. Buffer montaje y desmontaje entre 0 y 480
        if (paquete.getBufferMontajeMin() < 0 || paquete.getBufferMontajeMin() > 480 ||
                paquete.getBufferDesmontajeMin() < 0 || paquete.getBufferDesmontajeMin() > 480) {
            callback.onError("Los tiempos de montaje y desmontaje deben ser entre 0 y 480 minutos");
            return;
        }

        // Guardado limpio con copias
        Paquete paqueteAGuardar = new Paquete(paquete);
        paqueteAGuardar.setNombre(nombre);
        paqueteAGuardar.setDescripcion(paquete.getDescripcion() != null ? paquete.getDescripcion().trim() : "");

        boolean existe = false;
        if (paqueteAGuardar.getId() == null || paqueteAGuardar.getId().trim().isEmpty()) {
            paqueteAGuardar.setId(UUID.randomUUID().toString());
            paqueteAGuardar.setFechaCreacion(System.currentTimeMillis());
        }

        for (int i = 0; i < paquetes.size(); i++) {
            if (paquetes.get(i).getId().equals(paqueteAGuardar.getId())) {
                paquetes.set(i, paqueteAGuardar);
                existe = true;
                break;
            }
        }

        if (!existe) {
            if (paqueteAGuardar.getFechaCreacion() == 0) {
                paqueteAGuardar.setFechaCreacion(System.currentTimeMillis());
            }
            paquetes.add(paqueteAGuardar);
        }

        notificarCambios();
        callback.onSuccess(new Paquete(paqueteAGuardar));
    }

    @Override
    public synchronized void eliminarPaquete(String id, Callback<Void> callback) {
        // FIRESTORE: aquí irá una transacción
        // TODO etapa de Reservas: impedir eliminar si hay reservas futuras que usan este paquete
        boolean removed = paquetes.removeIf(p -> p.getId().equals(id));
        if (removed) {
            notificarCambios();
            callback.onSuccess(null);
        } else {
            callback.onError("Paquete no encontrado");
        }
    }
}
