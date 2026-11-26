package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.AsignadorUnidades;
import com.example.propuestasonidosonic.domain.ContextoDisponibilidad;
import com.example.propuestasonidosonic.domain.EstadosPedido;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.ModeloUtils;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.RelojSistema;
import com.example.propuestasonidosonic.util.TiempoUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InMemoryPedidoRepository implements PedidoRepository {
    public static final String COLECCION_PEDIDOS = "pedidos";

    private final List<Pedido> pedidos = new ArrayList<>();
    private final MutableLiveData<List<Pedido>> pedidosLiveData = new MutableLiveData<>(new ArrayList<>());
    private long consecutivoNumero = 1001;

    private final EquipoRepository equipoRepository;
    private final PaqueteRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DiaOperativoRepository diaOperativoRepository;
    private final EventoRepository eventoRepository;
    private final Reloj reloj;
    private FirebaseFirestore firestore;

    public InMemoryPedidoRepository() {
        this(null, null, null, null, null, new RelojSistema());
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    Reloj reloj) {
        this(equipoRepository, paqueteRepository, usuarioRepository, diaOperativoRepository, null, reloj);
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    EventoRepository eventoRepository,
                                    Reloj reloj) {
        this.equipoRepository = equipoRepository;
        this.paqueteRepository = paqueteRepository;
        this.usuarioRepository = usuarioRepository;
        this.diaOperativoRepository = diaOperativoRepository;
        this.eventoRepository = eventoRepository;
        this.reloj = reloj != null ? reloj : new RelojSistema();
    }

    public synchronized void conectarFirestore(FirebaseFirestore db) {
        if (db == null || this.firestore != null) return;
        this.firestore = db;
        this.firestore.collection(COLECCION_PEDIDOS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    synchronized (InMemoryPedidoRepository.this) {
                        pedidos.clear();
                        long maxNumero = 1000;
                        List<Pedido> migrados = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Pedido p = doc.toObject(Pedido.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().trim().isEmpty()) {
                                    p.setId(doc.getId());
                                }
                                if (MigracionPedidos.normalizar(p, doc)) {
                                    migrados.add(p);
                                }
                                pedidos.add(p);
                                if (p.getNumero() > maxNumero) {
                                    maxNumero = p.getNumero();
                                }
                            }
                        }
                        if (maxNumero >= consecutivoNumero) {
                            consecutivoNumero = maxNumero + 1;
                        }
                        long ahora = reloj.ahora();
                        for (Pedido p : migrados) {
                            guardarEnFirestore(p);
                            // Los pedidos confirmados por la migración necesitan su evento en la agenda
                            if (Pedido.ESTADO_CONFIRMADO.equals(p.getEstado()) && p.getEventoId() == null
                                    && EstadosPedido.finBloqueo(p) > ahora) {
                                crearEventoSiFalta(p);
                            }
                        }
                        notificarCambios();
                    }
                });
    }

    private void guardarEnFirestore(Pedido pedido) {
        if (firestore != null && pedido != null && pedido.getId() != null) {
            firestore.collection(COLECCION_PEDIDOS)
                    .document(pedido.getId())
                    .set(new Pedido(pedido));
        }
    }

    private void notificarCambios() {
        List<Pedido> copia = new ArrayList<>();
        for (Pedido p : pedidos) {
            copia.add(new Pedido(p));
        }
        // Más recientes primero
        Collections.sort(copia, new Comparator<Pedido>() {
            @Override
            public int compare(Pedido o1, Pedido o2) {
                return Long.compare(o2.getFechaCreacion(), o1.getFechaCreacion());
            }
        });
        pedidosLiveData.setValue(copia);
    }

    private ContextoDisponibilidad construirContexto(long ahora) {
        List<ModeloInventario> modelos = (equipoRepository != null) ? equipoRepository.obtenerModelos() : new ArrayList<ModeloInventario>();
        List<Pedido> reservas = obtenerTodos();
        List<Usuario> socios = (usuarioRepository != null) ? usuarioRepository.obtenerActivosPorRol(Usuario.ROL_SOCIO) : new ArrayList<Usuario>();
        List<Usuario> asistentes = (usuarioRepository != null) ? usuarioRepository.obtenerActivosPorRol(Usuario.ROL_ASISTENTE) : new ArrayList<Usuario>();
        Map<String, DiaOperativo> dias = (diaOperativoRepository != null) ? diaOperativoRepository.obtenerTodosPorClave() : Collections.<String, DiaOperativo>emptyMap();
        return new ContextoDisponibilidad(ahora, modelos, reservas, socios, asistentes, dias);
    }

    @Override
    public LiveData<List<Pedido>> getPedidos() {
        return pedidosLiveData;
    }

    @Override
    public synchronized Pedido getPedidoPorId(String id) {
        if (id == null) return null;
        for (Pedido p : pedidos) {
            if (id.equals(p.getId())) {
                return new Pedido(p);
            }
        }
        return null;
    }

    @Override
    public synchronized List<Pedido> obtenerTodos() {
        List<Pedido> copia = new ArrayList<>();
        for (Pedido p : pedidos) {
            copia.add(new Pedido(p));
        }
        return copia;
    }

    private Pedido buscar(String pedidoId) {
        if (pedidoId == null) return null;
        for (Pedido p : pedidos) {
            if (pedidoId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized int actualizarEstadosPorTiempo() {
        long ahora = reloj.ahora();
        int cambiados = 0;
        boolean liberoRecursos = false;
        for (Pedido p : pedidos) {
            if (Pedido.ESTADO_POR_APROBAR.equals(p.getEstado()) && p.getInicioMillis() <= ahora) {
                // Nadie aprobó la solicitud antes de la fecha del evento
                p.setEstado(Pedido.ESTADO_VENCIDO);
                p.setEquipoIds(new ArrayList<String>());
                p.setAsignacionIncompleta(false);
                p.setFechaActualizacion(ahora);
                guardarEnFirestore(p);
                liberoRecursos = true;
                cambiados++;
            } else if (Pedido.ESTADO_CONFIRMADO.equals(p.getEstado()) && EstadosPedido.finBloqueo(p) <= ahora) {
                // El evento terminó: queda finalizado si ya se pagó, si no, por cobrar
                p.setEstado(p.isPagoConfirmado() ? Pedido.ESTADO_FINALIZADO : Pedido.ESTADO_POR_COBRAR);
                p.setFechaActualizacion(ahora);
                guardarEnFirestore(p);
                if (p.getEventoId() != null && eventoRepository != null) {
                    eventoRepository.finalizarEvento(p.getEventoId(), null);
                }
                cambiados++;
            }
        }
        if (liberoRecursos) {
            reasignarUnidadesInterno(ahora);
        }
        if (cambiados > 0) {
            notificarCambios();
        }
        return cambiados;
    }

    @Override
    public synchronized void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback) {
        actualizarEstadosPorTiempo();

        if (solicitud == null) {
            if (callback != null) callback.onError("Solicitud nula");
            return;
        }

        String origen = solicitud.getOrigen();
        if (!Pedido.ORIGEN_APP.equals(origen)
                && !Pedido.ORIGEN_LLAMADA.equals(origen)
                && !Pedido.ORIGEN_WHATSAPP.equals(origen)
                && !Pedido.ORIGEN_PRESENCIAL.equals(origen)) {
            if (callback != null) callback.onError("Canal de origen inválido");
            return;
        }

        if (solicitud.getNombreEvento() == null
                || solicitud.getNombreEvento().trim().isEmpty()
                || solicitud.getNombreEvento().trim().length() > 80) {
            if (callback != null) callback.onError("El nombre del evento es obligatorio y debe tener máximo 80 caracteres");
            return;
        }

        if (solicitud.getClienteNombre() == null || solicitud.getClienteNombre().trim().isEmpty()) {
            if (callback != null) callback.onError("El nombre del cliente es obligatorio");
            return;
        }

        String telefono = solicitud.getClienteTelefono() != null ? solicitud.getClienteTelefono().trim() : "";
        String digitosTel = telefono.replaceAll("\\D", "");
        if (digitosTel.length() < 10) {
            if (callback != null) callback.onError("El teléfono del cliente debe tener al menos 10 dígitos");
            return;
        }

        if (solicitud.getDireccion() == null || solicitud.getDireccion().trim().length() < 10) {
            if (callback != null) callback.onError("La dirección debe tener al menos 10 caracteres");
            return;
        }

        if (Pedido.ORIGEN_APP.equals(origen)) {
            if (solicitud.getClienteId() == null || solicitud.getClienteId().trim().isEmpty()) {
                if (callback != null) callback.onError("El identificador del cliente es obligatorio para pedidos desde la app");
                return;
            }

            int pendientes = 0;
            for (Pedido p : pedidos) {
                if (solicitud.getClienteId().equals(p.getClienteId())
                        && Pedido.ESTADO_POR_APROBAR.equals(p.getEstado())) {
                    pendientes++;
                }
            }
            if (pendientes >= Config.SOLICITUDES_POR_APROBAR_MAX) {
                if (callback != null) {
                    callback.onError("Ya tienes una solicitud por aprobar. Espera la respuesta del negocio o cancélala para enviar otra.");
                }
                return;
            }
        }

        if (paqueteRepository == null) {
            if (callback != null) callback.onError("Repositorio de paquetes no configurado");
            return;
        }

        Paquete paquete = paqueteRepository.getPaquetePorId(solicitud.getPaqueteId());
        if (paquete == null) {
            if (callback != null) callback.onError("Paquete no encontrado");
            return;
        }

        long ahora = reloj.ahora();
        ContextoDisponibilidad ctx = construirContexto(ahora);

        ResultadoDisponibilidad eval = MotorDisponibilidad.evaluar(ctx, paquete, solicitud.getInicioMillis(),
                solicitud.getDuracionMin(), null);

        if (!eval.isViable()) {
            if (callback != null) {
                callback.onError(eval.getDetalle() != null ? eval.getDetalle() : eval.getMotivoNoViable());
            }
            return;
        }

        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID().toString());
        pedido.setNumero(consecutivoNumero++);
        pedido.setPaqueteId(paquete.getId());
        pedido.setPaqueteNombre(paquete.getNombre());
        pedido.setCanalOrigen(origen);
        pedido.setNombreEvento(solicitud.getNombreEvento().trim());
        pedido.setClienteId(solicitud.getClienteId());
        pedido.setClienteNombre(solicitud.getClienteNombre().trim());
        pedido.setClienteTelefono(telefono);
        pedido.setDireccion(solicitud.getDireccion().trim());
        pedido.setNotas(solicitud.getNotas() != null ? solicitud.getNotas().trim() : "");

        pedido.setInicioMillis(solicitud.getInicioMillis());
        long fin = TiempoUtils.sumarMinutos(solicitud.getInicioMillis(), solicitud.getDuracionMin());
        pedido.setFinMillis(fin);
        pedido.setBloqueoInicioMillis(eval.getBloqueoInicio());
        pedido.setBloqueoFinMillis(eval.getBloqueoFin());

        List<ComponentePaquete> componentesCopia = new ArrayList<>();
        if (paquete.getComponentes() != null) {
            for (ComponentePaquete c : paquete.getComponentes()) {
                componentesCopia.add(new ComponentePaquete(c));
            }
        }
        pedido.setComponentes(componentesCopia);
        pedido.setOperadoresRequeridos(paquete.getOperadoresRequeridos());
        pedido.setOperadorIds(new ArrayList<>(eval.getOperadorIds()));

        pedido.setTotal(MotorDisponibilidad.calcularTotal(paquete, solicitud.getDuracionMin()));
        pedido.setPagoConfirmado(false);

        boolean desdeApp = Pedido.ORIGEN_APP.equals(origen);
        // Las solicitudes de la app esperan la aprobación de un socio; las del socio se confirman de inmediato
        pedido.setEstado(desdeApp ? Pedido.ESTADO_POR_APROBAR : Pedido.ESTADO_CONFIRMADO);
        pedido.setMotivosRevision(desdeApp
                ? new ArrayList<>(eval.getMotivosRevision())
                : new ArrayList<String>());

        pedido.setFechaCreacion(ahora);
        pedido.setFechaActualizacion(ahora);

        pedidos.add(pedido);
        reasignarUnidadesInterno(ahora);
        if (!desdeApp) {
            crearEventoSiFalta(pedido);
        }
        guardarEnFirestore(pedido);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(pedido));
        }
    }

    @Override
    public synchronized void cancelarReserva(String pedidoId, String canceladoPor, String motivo, Callback<Pedido> callback) {
        Pedido target = buscar(pedidoId);
        if (target == null) {
            if (callback != null) callback.onError("Pedido no encontrado");
            return;
        }

        if (!EstadosPedido.ocupaRecursos(target.getEstado())) {
            if (callback != null) callback.onError("Este pedido ya no se puede cancelar");
            return;
        }

        String quienCancela = (canceladoPor != null && !canceladoPor.trim().isEmpty())
                ? canceladoPor.trim()
                : Pedido.CANCELA_SOCIO;

        if (Pedido.CANCELA_SOCIO.equals(quienCancela)) {
            if (motivo == null || motivo.trim().isEmpty()) {
                if (callback != null) callback.onError("El motivo de cancelación es obligatorio");
                return;
            }
        } else if (motivo == null || motivo.trim().isEmpty()) {
            motivo = "Cancelado por el cliente";
        }

        long ahora = reloj.ahora();
        target.setEstado(Pedido.ESTADO_CANCELADO);
        target.setCanceladoPor(quienCancela);
        target.setMotivoCancelacion(motivo.trim());
        target.setFechaActualizacion(ahora);
        liberarRecursos(target, ahora);
        agregarNota(target, "Cancelado por " + quienCancela + ": " + motivo.trim());

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void confirmarPago(String pedidoId, String metodo, String socioId,
                                           String socioNombre, Callback<Pedido> callback) {
        Pedido target = buscar(pedidoId);
        if (target == null) {
            if (callback != null) callback.onError("Pedido no encontrado");
            return;
        }
        if (target.isPagoConfirmado()) {
            if (callback != null) callback.onError("El pago de este pedido ya fue confirmado");
            return;
        }
        if (!EstadosPedido.puedeConfirmarPago(target)) {
            if (callback != null) callback.onError("Solo se puede confirmar el pago de pedidos confirmados o por cobrar");
            return;
        }
        if (!EstadosPedido.metodoValido(metodo)) {
            if (callback != null) callback.onError("Elige si el pago fue en efectivo o por transferencia");
            return;
        }

        long ahora = reloj.ahora();
        target.setPagoConfirmado(true);
        target.setMetodoPago(metodo);
        target.setFechaPagoMillis(ahora);
        target.setPagoConfirmadoPorId(socioId);
        target.setPagoConfirmadoPorNombre(socioNombre);
        // Si el evento ya terminó, con el pago el pedido queda finalizado con éxito
        if (Pedido.ESTADO_POR_COBRAR.equals(target.getEstado())
                || EstadosPedido.finBloqueo(target) <= ahora) {
            target.setEstado(Pedido.ESTADO_FINALIZADO);
        }
        target.setFechaActualizacion(ahora);

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    /** Crea el evento de la agenda (personal, equipo y checklist) de un pedido confirmado. */
    private void crearEventoSiFalta(Pedido target) {
        if (target == null || target.getEventoId() != null || eventoRepository == null) return;

        String socioId = "";
        String asistenteId = null;
        if (usuarioRepository != null && target.getOperadorIds() != null) {
            for (String opId : target.getOperadorIds()) {
                Usuario u = usuarioRepository.getUsuarioPorId(opId);
                if (u != null) {
                    if (Usuario.ROL_SOCIO.equals(u.getRol()) && socioId.isEmpty()) {
                        socioId = u.getId();
                    } else if (Usuario.ROL_ASISTENTE.equals(u.getRol()) && asistenteId == null) {
                        asistenteId = u.getId();
                    }
                }
            }
        }

        Evento ev = new Evento();
        ev.setNombre(target.getNombreEvento());
        ev.setInicioMillis(target.getInicioMillis());
        ev.setFinMillis(target.getFinMillis());
        ev.setBloqueoInicioMillis(target.getBloqueoInicioMillis());
        ev.setBloqueoFinMillis(target.getBloqueoFinMillis());
        ev.setDireccion(target.getDireccion() != null && !target.getDireccion().trim().isEmpty() ? target.getDireccion().trim() : null);
        ev.setPedidoId(target.getId());
        ev.setEstado(Evento.ESTADO_PROGRAMADO);
        ev.setEquipoIds(new ArrayList<>(target.getEquipoIds()));
        ev.setSocioResponsableId(socioId);
        ev.setAsistenteId(asistenteId);

        final String pedidoIdEvento = target.getId();
        eventoRepository.crearEvento(ev, new Callback<Evento>() {
            @Override
            public void onSuccess(Evento result) {
                synchronized (InMemoryPedidoRepository.this) {
                    Pedido actual = buscar(pedidoIdEvento);
                    if (actual != null && result != null) {
                        actual.setEventoId(result.getId());
                        guardarEnFirestore(actual);
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /** Cancela el evento de la agenda y libera las unidades que tenía apartadas. */
    private void liberarRecursos(Pedido target, long ahora) {
        if (target.getEventoId() != null && eventoRepository != null) {
            eventoRepository.cancelarEvento(target.getEventoId(), null);
        }
        target.setEquipoIds(new ArrayList<String>());
        target.setAsignacionIncompleta(false);
        reasignarUnidadesInterno(ahora);
    }

    private static void agregarNota(Pedido target, String nota) {
        if (target.getNotas() != null && !target.getNotas().isEmpty()) {
            target.setNotas(target.getNotas() + "\n" + nota);
        } else {
            target.setNotas(nota);
        }
    }

    @Override
    public synchronized void reprogramarReserva(String pedidoId, long nuevoInicioMillis, Callback<Pedido> callback) {
        Pedido target = null;
        for (Pedido p : pedidos) {
            if (p.getId().equals(pedidoId)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            if (callback != null) callback.onError("Pedido no encontrado");
            return;
        }

        if (!EstadosPedido.ocupaRecursos(target.getEstado())) {
            if (callback != null) callback.onError("Solo se pueden reprogramar pedidos por aprobar o confirmados");
            return;
        }

        long ahora = reloj.ahora();
        if (nuevoInicioMillis < ahora) {
            if (callback != null) callback.onError("No se puede reprogramar a un horario en el pasado");
            return;
        }

        if (paqueteRepository == null) {
            if (callback != null) callback.onError("Repositorio de paquetes no configurado");
            return;
        }

        Paquete paquete = paqueteRepository.getPaquetePorId(target.getPaqueteId());
        if (paquete == null) {
            if (callback != null) callback.onError("Paquete no encontrado");
            return;
        }

        int duracionMin = (int) ((target.getFinMillis() - target.getInicioMillis()) / 60_000L);
        ContextoDisponibilidad ctx = construirContexto(ahora);
        ResultadoDisponibilidad eval = MotorDisponibilidad.evaluar(ctx, paquete, nuevoInicioMillis, duracionMin, target.getId());

        if (!eval.isViable()) {
            if (callback != null) {
                callback.onError(eval.getDetalle() != null ? eval.getDetalle() : eval.getMotivoNoViable());
            }
            return;
        }

        target.setInicioMillis(nuevoInicioMillis);
        target.setFinMillis(TiempoUtils.sumarMinutos(nuevoInicioMillis, duracionMin));
        target.setBloqueoInicioMillis(eval.getBloqueoInicio());
        target.setBloqueoFinMillis(eval.getBloqueoFin());
        target.setOperadorIds(new ArrayList<>(eval.getOperadorIds()));
        target.setReprogramaciones(target.getReprogramaciones() + 1);
        target.setFechaActualizacion(ahora);

        reasignarUnidadesInterno(ahora);

        final Pedido finalTarget = target;
        if (finalTarget.getEventoId() != null && eventoRepository != null) {
            eventoRepository.getEventoPorId(finalTarget.getEventoId(), new Callback<Evento>() {
                @Override
                public void onSuccess(Evento ev) {
                    if (ev != null) {
                        ev.setInicioMillis(finalTarget.getInicioMillis());
                        ev.setFinMillis(finalTarget.getFinMillis());
                        ev.setBloqueoInicioMillis(finalTarget.getBloqueoInicioMillis());
                        ev.setBloqueoFinMillis(finalTarget.getBloqueoFinMillis());
                        ev.setEquipoIds(new ArrayList<>(finalTarget.getEquipoIds()));
                        eventoRepository.actualizarEvento(ev, null);
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void aprobarPedido(String pedidoId, Callback<Pedido> callback) {
        Pedido target = buscar(pedidoId);
        if (target == null) {
            if (callback != null) callback.onError("Pedido no encontrado");
            return;
        }

        if (!Pedido.ESTADO_POR_APROBAR.equals(target.getEstado())) {
            if (callback != null) callback.onError("El pedido no está por aprobar");
            return;
        }

        long ahora = reloj.ahora();
        if (target.getInicioMillis() <= ahora) {
            if (callback != null) callback.onError("La fecha del evento ya pasó; la solicitud venció");
            return;
        }

        target.setEstado(Pedido.ESTADO_CONFIRMADO);
        target.setFechaActualizacion(ahora);
        reasignarUnidadesInterno(ahora);
        crearEventoSiFalta(target);

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void rechazarPedido(String pedidoId, String motivo, Callback<Pedido> callback) {
        Pedido target = buscar(pedidoId);
        if (target == null) {
            if (callback != null) callback.onError("Pedido no encontrado");
            return;
        }

        if (!Pedido.ESTADO_POR_APROBAR.equals(target.getEstado())) {
            if (callback != null) callback.onError("El pedido no está por aprobar");
            return;
        }

        if (motivo == null || motivo.trim().length() < 3) {
            if (callback != null) callback.onError("El motivo de rechazo es obligatorio (mínimo 3 caracteres)");
            return;
        }

        long ahora = reloj.ahora();
        target.setEstado(Pedido.ESTADO_RECHAZADO);
        target.setMotivoRechazo(motivo.trim());
        target.setFechaActualizacion(ahora);
        liberarRecursos(target, ahora);
        agregarNota(target, "Rechazado: " + motivo.trim());

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void reasignarUnidades(Callback<Void> callback) {
        long ahora = reloj.ahora();
        reasignarUnidadesInterno(ahora);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void reasignarUnidadesInterno(long ahora) {
        if (equipoRepository == null) return;

        List<Equipo> todosEquipos = equipoRepository.listarTodos();
        Map<String, Equipo> equiposPorId = new HashMap<>();
        List<AsignadorUnidades.UnidadDisponible> unidadesDisponibles = new ArrayList<>();

        for (Equipo eq : todosEquipos) {
            if (eq == null) continue;
            equiposPorId.put(eq.getId(), eq);
            if (Equipo.ESTADO_DISPONIBLE.equals(eq.getEstadoFisico())) {
                String modeloClave = ModeloUtils.clave(eq.getCategoria(), eq.getNombre());
                unidadesDisponibles.add(new AsignadorUnidades.UnidadDisponible(
                        eq.getId(), modeloClave, eq.getNumeroUnidad()));
            }
        }

        List<AsignadorUnidades.Demanda> demandas = new ArrayList<>();
        List<Pedido> pedidosActivos = new ArrayList<>();

        for (Pedido p : pedidos) {
            if (p == null) continue;
            if (!EstadosPedido.ocupaRecursos(p.getEstado())) continue;

            pedidosActivos.add(p);
            long desde = p.getBloqueoInicioMillis() > 0 ? p.getBloqueoInicioMillis() : p.getInicioMillis();
            long hasta = p.getBloqueoFinMillis() > 0 ? p.getBloqueoFinMillis() : p.getFinMillis();
            boolean fija = desde <= ahora;

            if (p.getComponentes() != null) {
                for (ComponentePaquete comp : p.getComponentes()) {
                    if (comp == null || comp.getCantidad() <= 0) continue;
                    List<String> previas = filtrarUnidadesPorModelo(p.getEquipoIds(), comp.getModeloClave(), equiposPorId);
                    demandas.add(new AsignadorUnidades.Demanda(
                            p.getId(),
                            comp.getModeloClave(),
                            comp.getCantidad(),
                            desde,
                            hasta,
                            previas,
                            fija
                    ));
                }
            }
        }

        AsignadorUnidades.Resultado resultado = AsignadorUnidades.asignar(demandas, unidadesDisponibles);

        Map<String, List<String>> equipoIdsPorEventoId = new HashMap<>();
        for (Pedido p : pedidosActivos) {
            List<String> asignadas = resultado.unidadesPorPedido.get(p.getId());
            if (asignadas == null) asignadas = new ArrayList<>();
            boolean incompleta = resultado.pedidosIncompletos.contains(p.getId());
            boolean cambio = !Objects.equals(p.getEquipoIds(), asignadas) || p.isAsignacionIncompleta() != incompleta;
            p.setEquipoIds(asignadas);
            p.setAsignacionIncompleta(incompleta);

            if (cambio) {
                guardarEnFirestore(p);
            }

            if (p.getEventoId() != null) {
                equipoIdsPorEventoId.put(p.getEventoId(), asignadas);
            }
        }

        if (eventoRepository != null && !equipoIdsPorEventoId.isEmpty()) {
            eventoRepository.reemplazarAsignaciones(equipoIdsPorEventoId, null);
        }
    }

    private List<String> filtrarUnidadesPorModelo(List<String> equipoIds, String modeloClave, Map<String, Equipo> equiposPorId) {
        List<String> filtradas = new ArrayList<>();
        if (equipoIds == null || modeloClave == null) return filtradas;
        for (String id : equipoIds) {
            Equipo eq = equiposPorId.get(id);
            if (eq != null) {
                String clave = ModeloUtils.clave(eq.getCategoria(), eq.getNombre());
                if (modeloClave.equals(clave)) {
                    filtradas.add(id);
                }
            }
        }
        return filtradas;
    }

    @Override
    public synchronized List<Pedido> obtenerReservasActivasDeOperador(String operadorId, long desdeMillis, long hastaMillis) {
        List<Pedido> resultado = new ArrayList<>();
        if (operadorId == null) return resultado;
        long ahora = reloj.ahora();
        for (Pedido p : pedidos) {
            if (p == null) continue;
            if (!MotorDisponibilidad.bloquea(p, ahora)) continue;
            if (p.getOperadorIds() != null && p.getOperadorIds().contains(operadorId)) {
                if (TiempoUtils.solapan(p.getInicioMillis(), p.getFinMillis(), desdeMillis, hastaMillis)) {
                    resultado.add(new Pedido(p));
                }
            }
        }
        return resultado;
    }

    @Override
    public synchronized ResultadoDisponibilidad evaluarDisponibilidad(String paqueteId, long inicioMillis, int duracionMin, String ignorarPedidoId) {
        if (paqueteRepository == null) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.PAQUETE_INCOMPLETO, "Repositorio de paquetes no configurado");
        }
        Paquete paquete = paqueteRepository.getPaquetePorId(paqueteId);
        if (paquete == null) {
            return ResultadoDisponibilidad.noViable(ResultadoDisponibilidad.PAQUETE_INCOMPLETO, "Paquete no encontrado");
        }
        ContextoDisponibilidad ctx = construirContexto(reloj.ahora());
        return MotorDisponibilidad.evaluar(ctx, paquete, inicioMillis, duracionMin, ignorarPedidoId);
    }

    @Override
    public synchronized List<Long> consultarHorarios(String paqueteId, long unMomentoDelDia, int duracionMin, String ignorarPedidoId) {
        if (paqueteRepository == null) return new ArrayList<>();
        Paquete paquete = paqueteRepository.getPaquetePorId(paqueteId);
        if (paquete == null) return new ArrayList<>();
        ContextoDisponibilidad ctx = construirContexto(reloj.ahora());
        return MotorDisponibilidad.horariosDisponibles(ctx, paquete, unMomentoDelDia, duracionMin, ignorarPedidoId);
    }

    @Override
    public synchronized List<Long> consultarAlternativas(String paqueteId, long desdeMillis, int duracionMin, int max) {
        if (paqueteRepository == null) return new ArrayList<>();
        Paquete paquete = paqueteRepository.getPaquetePorId(paqueteId);
        if (paquete == null) return new ArrayList<>();
        ContextoDisponibilidad ctx = construirContexto(reloj.ahora());
        return MotorDisponibilidad.proximasAlternativas(ctx, paquete, desdeMillis, duracionMin, max);
    }
}
