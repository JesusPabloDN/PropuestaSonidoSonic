package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.DiaOperativoRepository;
import com.example.propuestasonidosonic.data.EquipoRepository;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.data.PedidoRepository;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.domain.ContextoDisponibilidad;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.domain.ResultadoDisponibilidad;
import com.example.propuestasonidosonic.domain.SolicitudReserva;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.DiaOperativo;
import com.example.propuestasonidosonic.model.ModeloInventario;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.Reloj;
import com.example.propuestasonidosonic.util.RelojSistema;
import com.example.propuestasonidosonic.util.TiempoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryPedidoRepository implements PedidoRepository {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final MutableLiveData<List<Pedido>> pedidosLiveData = new MutableLiveData<>(new ArrayList<>());
    private long consecutivoNumero = 1001;

    private final EquipoRepository equipoRepository;
    private final PaqueteRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DiaOperativoRepository diaOperativoRepository;
    private final Reloj reloj;

    public InMemoryPedidoRepository() {
        this(null, null, null, null, new RelojSistema());
    }

    public InMemoryPedidoRepository(EquipoRepository equipoRepository,
                                    PaqueteRepository paqueteRepository,
                                    UsuarioRepository usuarioRepository,
                                    DiaOperativoRepository diaOperativoRepository,
                                    Reloj reloj) {
        this.equipoRepository = equipoRepository;
        this.paqueteRepository = paqueteRepository;
        this.usuarioRepository = usuarioRepository;
        this.diaOperativoRepository = diaOperativoRepository;
        this.reloj = reloj != null ? reloj : new RelojSistema();
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
        // FIRESTORE: transacción
        long ahora = reloj.ahora();
        int liberados = 0;
        for (Pedido p : pedidos) {
            if (Pedido.ESTADO_APARTADO.equals(p.getEstado()) && p.getVencimientoApartadoMillis() <= ahora) {
                p.setEstado(Pedido.ESTADO_VENCIDO);
                p.setFechaActualizacion(ahora);
                liberados++;
            }
        }
        if (liberados > 0) {
            notificarCambios();
        }
        return liberados;
    }

    @Override
    public synchronized void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback) {
        // FIRESTORE: transacción
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
        List<ModeloInventario> modelos = (equipoRepository != null) ? equipoRepository.obtenerModelos() : new ArrayList<ModeloInventario>();
        List<Pedido> reservas = obtenerTodos();
        List<Usuario> socios = (usuarioRepository != null) ? usuarioRepository.obtenerActivosPorRol(Usuario.ROL_SOCIO) : new ArrayList<Usuario>();
        List<Usuario> asistentes = (usuarioRepository != null) ? usuarioRepository.obtenerActivosPorRol(Usuario.ROL_ASISTENTE) : new ArrayList<Usuario>();
        Map<String, DiaOperativo> dias = (diaOperativoRepository != null) ? diaOperativoRepository.obtenerTodosPorClave() : Collections.<String, DiaOperativo>emptyMap();

        ContextoDisponibilidad ctx = new ContextoDisponibilidad(ahora, modelos, reservas, socios, asistentes, dias);

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
        pedido.setRequiereRevision(eval.isRequiereRevision());
        pedido.setMotivosRevision(new ArrayList<>(eval.getMotivosRevision()));
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
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(pedido));
        }
    }

    @Override
    public synchronized void cancelarReserva(String pedidoId, String motivo, Callback<Pedido> callback) {
        // FIRESTORE: transacción
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

        target.setEstado(Pedido.ESTADO_CANCELADO);
        String notaCancelacion = (motivo != null && !motivo.trim().isEmpty())
                ? "Motivo cancelación: " + motivo.trim()
                : "Cancelado";
        if (target.getNotas() != null && !target.getNotas().isEmpty()) {
            target.setNotas(target.getNotas() + "\n" + notaCancelacion);
        } else {
            target.setNotas(notaCancelacion);
        }
        long ahora = reloj.ahora();
        target.setFechaActualizacion(ahora);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Pedido(target));
        }
    }

    @Override
    public synchronized List<Pedido> obtenerReservasActivasDeOperador(String operadorId, long desdeMillis, long hastaMillis) {
        List<Pedido> resultado = new ArrayList<>();
        if (operadorId == null) return resultado;
        long ahora = reloj.ahora();
        for (Pedido p : pedidos) {
            if (MotorDisponibilidad.bloquea(p, ahora)
                    && p.getOperadorIds() != null
                    && p.getOperadorIds().contains(operadorId)) {
                long bIni = p.getBloqueoInicioMillis();
                long bFin = p.getBloqueoFinMillis();
                if (bIni == 0 && bFin == 0) {
                    bIni = p.getInicioMillis();
                    bFin = p.getFinMillis();
                }
                if (TiempoUtils.solapan(bIni, bFin, desdeMillis, hastaMillis)) {
                    resultado.add(new Pedido(p));
                }
            }
        }
        return resultado;
    }
}

