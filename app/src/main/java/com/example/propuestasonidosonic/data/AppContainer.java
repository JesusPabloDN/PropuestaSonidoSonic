package com.example.propuestasonidosonic.data;

import com.example.propuestasonidosonic.data.memory.InMemoryDiaOperativoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryFinanzasRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPagoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.util.RelojSistema;

public class AppContainer {
    private final UsuarioRepository usuarioRepository;
    private final InMemoryEquipoRepository equipoRepository;
    private final PaqueteRepository paqueteRepository;
    private final EventoRepository eventoRepository;
    private final DiaOperativoRepository diaOperativoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final FinanzasRepository finanzasRepository;
    private final SessionManager sessionManager;
    private final PasarelaPago pasarelaPago;

    public AppContainer() {
        this.equipoRepository = new InMemoryEquipoRepository();
        this.paqueteRepository = new InMemoryPaqueteRepository(this.equipoRepository);
        this.sessionManager = SessionManager.getInstance();
        this.usuarioRepository = new InMemoryUsuarioRepository(this.sessionManager);
        this.diaOperativoRepository = new InMemoryDiaOperativoRepository();
        this.pagoRepository = new InMemoryPagoRepository();
        this.eventoRepository = new InMemoryEventoRepository();
        this.pasarelaPago = new com.example.propuestasonidosonic.data.memory.PasarelaSimulada();
        this.pedidoRepository = new InMemoryPedidoRepository(
                this.equipoRepository,
                this.paqueteRepository,
                this.usuarioRepository,
                this.diaOperativoRepository,
                this.pagoRepository,
                this.eventoRepository,
                new RelojSistema()
        );
        this.finanzasRepository = new InMemoryFinanzasRepository();

        // Enlazar EventoRepository en EquipoRepository para calcular estadoEfectivo
        this.equipoRepository.setEventoRepository(this.eventoRepository);

        // Disparar recálculo de asignaciones cuando cambie el inventario
        this.equipoRepository.setOnInventarioCambio(() -> this.pedidoRepository.reasignarUnidades(null));
    }

    public UsuarioRepository getUsuarioRepository() {
        return usuarioRepository;
    }

    public EquipoRepository getEquipoRepository() {
        return equipoRepository;
    }

    public PaqueteRepository getPaqueteRepository() {
        return paqueteRepository;
    }

    public EventoRepository getEventoRepository() {
        return eventoRepository;
    }

    public DiaOperativoRepository getDiaOperativoRepository() {
        return diaOperativoRepository;
    }

    public PedidoRepository getPedidoRepository() {
        return pedidoRepository;
    }

    public PagoRepository getPagoRepository() {
        return pagoRepository;
    }

    public FinanzasRepository getFinanzasRepository() {
        return finanzasRepository;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public PasarelaPago getPasarelaPago() {
        return pasarelaPago;
    }
}
