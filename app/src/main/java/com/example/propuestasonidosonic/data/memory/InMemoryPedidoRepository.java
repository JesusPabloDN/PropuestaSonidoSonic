package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.EventoRepository;
import com.example.propuestasonidosonic.data.PagoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.AsignadorUnidades;
import com.example.propuestasonidosonic.domain.ContextoDisponibilidad;
import com.example.propuestasonidosonic.domain.MoneyUtils;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.PoliticaCancelacion;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.Equipo;
import com.example.propuestasonidosonic.model.Evento;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Pago;
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
    private final PagoRepository pagoRepository;
    private final EventoRepository eventoRepository;
    private final Reloj reloj;
    private FirebaseFirestore firestore;

    public InMemoryPedidoRepository() {
        this(null, null, null, null, null, null, new RelojSistema());
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    Reloj reloj) {
        this(equipoRepository, paqueteRepository, usuarioRepository, diaOperativoRepository, null, null, reloj);
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    PagoRepository pagoRepository,
                                    Reloj reloj) {
        this(equipoRepository, paqueteRepository, usuarioRepository, diaOperativoRepository, pagoRepository, null, reloj);
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    PagoRepository pagoRepository,
                                    EventoRepository eventoRepository,
                                    Reloj reloj) {
        this.equipoRepository = equipoRepository;
        this.paqueteRepository = paqueteRepository;
        this.usuarioRepository = usuarioRepository;
        this.diaOperativoRepository = diaOperativoRepository;
        this.pagoRepository = pagoRepository;
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
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Pedido p = doc.toObject(Pedido.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().trim().isEmpty()) {
                                    p.setId(doc.getId());
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

    @Override
    public synchronized int liberarApartadosVencidos() {
        long ahora = reloj.ahora();
        int cambiados = 0;
        for (Pedido p : pedidos) {
            if (Pedido.ESTADO_APARTADO.equals(p.getEstado()) && p.getVencimientoApartadoMillis() <= ahora) {
                p.setEstado(Pedido.ESTADO_VENCIDO);
                p.setFechaActualizacion(ahora);
                guardarEnFirestore(p);
                cambiados++;
            } else if (Pedido.ESTADO_CONFIRMADO.equals(p.getEstado())) {
                long finBloqueo = p.getBloqueoFinMillis() > 0 ? p.getBloqueoFinMillis() : p.getFinMillis();
                if (finBloqueo <= ahora) {
                    p.setEstado(Pedido.ESTADO_COMPLETADO);
                    p.setFechaActualizacion(ahora);
                    guardarEnFirestore(p);
                    if (p.getEventoId() != null && eventoRepository != null) {
                        eventoRepository.finalizarEvento(p.getEventoId(), null);
                    }
                    cambiados++;
                }
            }
        }
        if (cambiados > 0) {
            notificarCambios();
        }
        return cambiados;
    }

    @Override
    public synchronized void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback) {
        liberarApartadosVencidos();

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

            long ahora = reloj.ahora();
            for (Pedido p : pedidos) {
                if (solicitud.getClienteId().equals(p.getClienteId())
                        && Pedido.ESTADO_APARTADO.equals(p.getEstado())
                        && p.getVencimientoApartadoMillis() > ahora) {
                    if (callback != null) {
                        callback.onError("Ya tienes una reserva pendiente de pago. Págala o espera a que venza.");
                    }
                    return;
                }
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

        double total = MotorDisponibilidad.calcularTotal(paquete, solicitud.getDuracionMin());
        pedido.setTotal(total);
        pedido.setAnticipoRequerido(MotorDisponibilidad.calcularAnticipo(total));
        pedido.setAnticipoPagado(0.0);
        pedido.setEstado(Pedido.ESTADO_APARTADO);

        if (Pedido.ORIGEN_APP.equals(origen)) {
            pedido.setRequiereRevision(eval.isRequiereRevision());
            pedido.setMotivosRevision(new ArrayList<>(eval.getMotivosRevision()));
        } else {
            pedido.setRequiereRevision(false);
            pedido.setMotivosRevision(new ArrayList<String>());
        }

        pedido.setFechaCreacion(ahora);
        pedido.setFechaActualizacion(ahora);

        long vencimiento;
        if (Pedido.ORIGEN_APP.equals(origen)) {
            vencimiento = TiempoUtils.sumarMinutos(ahora, Config.APARTADO_APP_MIN);
        } else {
            int horas = solicitud.getHorasApartado() != null ? solicitud.getHorasApartado() : Config.APARTADO_MANUAL_HORAS;
            vencimiento = ahora + ((long) horas) * 3600_000L;
        }
        pedido.setVencimientoApartadoMillis(vencimiento);

        pedidos.add(pedido);
        guardarEnFirestore(pedido);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(pedido));
        }
    }

    @Override
    public synchronized void cancelarReserva(String pedidoId, String canceladoPor, String motivo, Callback<Pedido> callback) {
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

        String estado = target.getEstado();
        if (!Pedido.ESTADO_APARTADO.equals(estado)
                && !Pedido.ESTADO_PAGADO.equals(estado)
                && !Pedido.ESTADO_CONFIRMADO.equals(estado)
                && !Pedido.ESTADO_EN_REVISION.equals(estado)) {
            if (callback != null) callback.onError("Esta reserva ya no se puede cancelar");
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
        } else if (Pedido.CANCELA_CLIENTE.equals(quienCancela)) {
            if (motivo == null || motivo.trim().isEmpty()) {
                motivo = "Cancelado por el cliente";
            }
        }

        long ahora = reloj.ahora();
        double reembolso;
        if (Pedido.CANCELA_CLIENTE.equals(quienCancela) && Pedido.ESTADO_EN_REVISION.equals(target.getEstado())) {
            reembolso = MoneyUtils.redondear2(target.getAnticipoPagado());
        } else {
            reembolso = PoliticaCancelacion.calcularReembolso(target.getAnticipoPagado(),
                    target.getInicioMillis(), ahora, quienCancela);
        }

        final String motivoFinal = motivo;
        if (reembolso > 0 && pagoRepository != null) {
            String metodo = target.getMetodoAnticipo() != null ? target.getMetodoAnticipo() : Pago.METODO_EFECTIVO;
            Pago pagoReembolso = new Pago(UUID.randomUUID().toString(), target.getId(),
                    target.getClienteId(), reembolso, Pago.TIPO_REEMBOLSO, metodo, ahora);

            final Pedido finalTarget = target;
            pagoRepository.registrarPago(pagoReembolso, new Callback<Pago>() {
                @Override
                public void onSuccess(Pago result) {
                    synchronized (InMemoryPedidoRepository.this) {
                        aplicarCancelacion(finalTarget, quienCancela, motivoFinal, reembolso, ahora, callback);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError("Error al registrar el reembolso: " + error);
                }
            });
        } else {
            aplicarCancelacion(target, quienCancela, motivoFinal, reembolso, ahora, callback);
        }
    }

    private void aplicarCancelacion(Pedido target, String canceladoPor, String motivo,
                                    double reembolso, long ahora, Callback<Pedido> callback) {
        target.setEstado(Pedido.ESTADO_CANCELADO);
        target.setCanceladoPor(canceladoPor);
        target.setMotivoCancelacion(motivo != null ? motivo.trim() : "");
        target.setReembolsoMonto(reembolso);
        target.setFechaActualizacion(ahora);

        if (target.getEventoId() != null && eventoRepository != null) {
            eventoRepository.cancelarEvento(target.getEventoId(), null);
        }
        target.setEquipoIds(new ArrayList<String>());
        target.setAsignacionIncompleta(false);

        reasignarUnidadesInterno(ahora);

        String notaCancelacion = (motivo != null && !motivo.trim().isEmpty())
                ? "Cancelado por " + canceladoPor + ": " + motivo.trim()
                : "Cancelado por " + canceladoPor;
        if (target.getNotas() != null && !target.getNotas().isEmpty()) {
            target.setNotas(target.getNotas() + "\n" + notaCancelacion);
        } else {
            target.setNotas(notaCancelacion);
        }

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void registrarAnticipo(String pedidoId, double monto, String metodo, Callback<Pedido> callback) {
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

        if (!Pedido.ESTADO_APARTADO.equals(target.getEstado())) {
            if (callback != null) callback.onError("Solo se puede registrar anticipo en reservas apartadas");
            return;
        }

        long ahora = reloj.ahora();
        if (target.getVencimientoApartadoMillis() > 0 && target.getVencimientoApartadoMillis() <= ahora) {
            target.setEstado(Pedido.ESTADO_VENCIDO);
            target.setFechaActualizacion(ahora);
            guardarEnFirestore(target);
            notificarCambios();
            if (callback != null) callback.onError("El apartado ya venció");
            return;
        }

        if (metodo == null || (!Pedido.METODO_EFECTIVO.equals(metodo)
                && !Pedido.METODO_TRANSFERENCIA.equals(metodo)
                && !Pedido.METODO_TARJETA.equals(metodo))) {
            if (callback != null) callback.onError("Método de pago inválido");
            return;
        }

        if (monto <= 0) {
            if (callback != null) callback.onError("El monto debe ser mayor a 0");
            return;
        }

        double montoRedondeado = MoneyUtils.redondear2(monto);
        double anticipoPendiente = target.getAnticipoRequerido() - target.getAnticipoPagado();
        if (montoRedondeado < anticipoPendiente - 0.005) {
            if (callback != null) callback.onError("El monto no cubre el anticipo requerido");
            return;
        }

        double saldoTotal = target.getTotal() - target.getAnticipoPagado();
        if (montoRedondeado > saldoTotal + 0.005) {
            if (callback != null) callback.onError("El monto excede el saldo restante del pedido");
            return;
        }
        if (montoRedondeado > saldoTotal) {
            montoRedondeado = saldoTotal;
        }

        if (pagoRepository != null) {
            Pago pago = new Pago(UUID.randomUUID().toString(), target.getId(),
                    target.getClienteId(), montoRedondeado, Pago.TIPO_ANTICIPO, metodo, ahora);

            final Pedido finalTarget = target;
            final double finalMonto = montoRedondeado;
            pagoRepository.registrarPago(pago, new Callback<Pago>() {
                @Override
                public void onSuccess(Pago result) {
                    synchronized (InMemoryPedidoRepository.this) {
                        aplicarAnticipo(finalTarget, finalMonto, metodo, ahora, callback);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError("Error al registrar el pago: " + error);
                }
            });
        } else {
            aplicarAnticipo(target, montoRedondeado, metodo, ahora, callback);
        }
    }

    private void aplicarAnticipo(Pedido target, double monto, String metodo, long ahora, Callback<Pedido> callback) {
        target.setAnticipoPagado(MoneyUtils.redondear2(target.getAnticipoPagado() + monto));
        target.setMetodoAnticipo(metodo);
        target.setFechaAnticipo(ahora);
        target.setVencimientoApartadoMillis(0);

        if (target.estaLiquidado()) {
            target.setCobrado(true);
            target.setFechaCobro(ahora);
        }

        if (target.isRequiereRevision()) {
            target.setEstado(Pedido.ESTADO_EN_REVISION);
        } else {
            target.setEstado(Pedido.ESTADO_CONFIRMADO);
        }

        target.setFechaActualizacion(ahora);

        if (target.getEventoId() == null) {
            reasignarUnidadesInterno(ahora);

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

            if (eventoRepository != null) {
                eventoRepository.crearEvento(ev, new Callback<Evento>() {
                    @Override
                    public void onSuccess(Evento result) {
                        target.setEventoId(result.getId());
                        guardarEnFirestore(target);
                    }

                    @Override
                    public void onError(String error) {
                    }
                });
            }
        }

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void liquidarSaldo(String pedidoId, double monto, String metodo, Callback<Pago> callback) {
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

        String estado = target.getEstado();
        if (!Pedido.ESTADO_CONFIRMADO.equals(estado)
                && !Pedido.ESTADO_PAGADO.equals(estado)
                && !Pedido.ESTADO_EN_REVISION.equals(estado)
                && !Pedido.ESTADO_COMPLETADO.equals(estado)) {
            if (callback != null) callback.onError("Solo se puede cobrar saldo de pedidos confirmados, en revisión o completados");
            return;
        }

        if (monto <= 0) {
            if (callback != null) callback.onError("El monto debe ser mayor a 0");
            return;
        }

        if (metodo == null || (!Pedido.METODO_EFECTIVO.equals(metodo)
                && !Pedido.METODO_TRANSFERENCIA.equals(metodo)
                && !Pedido.METODO_TARJETA.equals(metodo))) {
            if (callback != null) callback.onError("Método de pago inválido");
            return;
        }

        double saldoPendiente = target.getSaldoPendiente();
        if (saldoPendiente <= 0.005) {
            if (callback != null) callback.onError("El pedido ya está liquidado");
            return;
        }

        double montoRedondeado = MoneyUtils.redondear2(monto);
        if (montoRedondeado > saldoPendiente + 0.005) {
            if (callback != null) callback.onError("El monto excede el saldo pendiente");
            return;
        }
        if (montoRedondeado > saldoPendiente) {
            montoRedondeado = saldoPendiente;
        }

        boolean liquidaTotal = MoneyUtils.iguales(montoRedondeado, saldoPendiente);
        String tipoPago = liquidaTotal ? Pago.TIPO_TOTAL : Pago.TIPO_ABONO;
        long ahora = reloj.ahora();

        Pago pago = new Pago(UUID.randomUUID().toString(), target.getId(),
                target.getClienteId(), montoRedondeado, tipoPago, metodo, ahora);

        if (pagoRepository != null) {
            final Pedido finalTarget = target;
            final double finalMonto = montoRedondeado;
            pagoRepository.registrarPago(pago, new Callback<Pago>() {
                @Override
                public void onSuccess(Pago result) {
                    synchronized (InMemoryPedidoRepository.this) {
                        aplicarLiquidacion(finalTarget, result != null ? result : pago, finalMonto, ahora, callback);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError("Error al registrar el pago: " + error);
                }
            });
        } else {
            aplicarLiquidacion(target, pago, montoRedondeado, ahora, callback);
        }
    }

    private void aplicarLiquidacion(Pedido target, Pago pago, double monto, long ahora, Callback<Pago> callback) {
        double nuevoAnticipo = MoneyUtils.redondear2(target.getAnticipoPagado() + monto);
        target.setAnticipoPagado(nuevoAnticipo);
        target.setFechaActualizacion(ahora);
        if (target.estaLiquidado()) {
            target.setCobrado(true);
            target.setFechaCobro(ahora);
        }
        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(pago);
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

        if (!Pedido.ESTADO_APARTADO.equals(target.getEstado())
                && !Pedido.ESTADO_CONFIRMADO.equals(target.getEstado())) {
            if (callback != null) callback.onError("Solo se pueden reprogramar reservas apartadas o confirmadas");
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

        if (!Pedido.ESTADO_EN_REVISION.equals(target.getEstado())) {
            if (callback != null) callback.onError("El pedido no está en revisión");
            return;
        }

        long ahora = reloj.ahora();
        target.setEstado(Pedido.ESTADO_CONFIRMADO);
        target.setRequiereRevision(false);
        target.setFechaActualizacion(ahora);

        guardarEnFirestore(target);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized void rechazarPedido(String pedidoId, String motivo, Callback<Pedido> callback) {
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

        if (!Pedido.ESTADO_EN_REVISION.equals(target.getEstado())) {
            if (callback != null) callback.onError("El pedido no está en revisión");
            return;
        }

        if (motivo == null || motivo.trim().length() < 3) {
            if (callback != null) callback.onError("El motivo de rechazo es obligatorio (mínimo 3 caracteres)");
            return;
        }

        final String motivoTrim = motivo.trim();
        long ahora = reloj.ahora();
        double totalPagado = target.getAnticipoPagado();
        final double reembolso = MoneyUtils.redondear2(totalPagado);

        if (reembolso > 0 && pagoRepository != null) {
            String metodo = target.getMetodoAnticipo() != null ? target.getMetodoAnticipo() : Pago.METODO_EFECTIVO;
            Pago pagoReembolso = new Pago(UUID.randomUUID().toString(), target.getId(),
                    target.getClienteId(), reembolso, Pago.TIPO_REEMBOLSO, metodo, ahora);

            final Pedido finalTarget = target;
            pagoRepository.registrarPago(pagoReembolso, new Callback<Pago>() {
                @Override
                public void onSuccess(Pago result) {
                    synchronized (InMemoryPedidoRepository.this) {
                        aplicarRechazo(finalTarget, motivoTrim, reembolso, ahora, callback);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError("Error al registrar el reembolso: " + error);
                }
            });
        } else {
            aplicarRechazo(target, motivoTrim, reembolso, ahora, callback);
        }
    }

    private void aplicarRechazo(Pedido target, String motivo, double reembolso, long ahora, Callback<Pedido> callback) {
        target.setEstado(Pedido.ESTADO_RECHAZADO);
        target.setMotivoRechazo(motivo);
        target.setReembolsoMonto(reembolso);
        target.setFechaActualizacion(ahora);

        if (target.getEventoId() != null && eventoRepository != null) {
            eventoRepository.cancelarEvento(target.getEventoId(), null);
        }
        target.setEquipoIds(new ArrayList<String>());
        target.setAsignacionIncompleta(false);

        reasignarUnidadesInterno(ahora);

        String notaRechazo = "Rechazado: " + motivo;
        if (target.getNotas() != null && !target.getNotas().isEmpty()) {
            target.setNotas(target.getNotas() + "\n" + notaRechazo);
        } else {
            target.setNotas(notaRechazo);
        }

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
            boolean activo = Pedido.ESTADO_CONFIRMADO.equals(p.getEstado())
                    || Pedido.ESTADO_PAGADO.equals(p.getEstado())
                    || Pedido.ESTADO_EN_REVISION.equals(p.getEstado());
            if (!activo) continue;

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

    @Override
    public synchronized double calcularReembolsoPrevio(String pedidoId, String canceladoPor) {
        Pedido p = getPedidoPorId(pedidoId);
        if (p == null) return 0.0;
        if (Pedido.CANCELA_CLIENTE.equals(canceladoPor) && Pedido.ESTADO_EN_REVISION.equals(p.getEstado())) {
            return MoneyUtils.redondear2(p.getAnticipoPagado());
        }
        return PoliticaCancelacion.calcularReembolso(p.getAnticipoPagado(), p.getInicioMillis(), reloj.ahora(), canceladoPor);
    }
}
