package com.example.propuestasonidosonic.data;

import com.example.propuestasonidosonic.data.memory.InMemoryEquipoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryEventoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryFinanzasRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPagoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPaqueteRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryPedidoRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;

public class AppContainer {
    private final UsuarioRepository usuarioRepository;
    private final InMemoryEquipoRepository equipoRepository;
    private final PaqueteRepository paqueteRepository;
    private final EventoRepository eventoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final FinanzasRepository finanzasRepository;
    private final SessionManager sessionManager;

    public AppContainer() {
        this.usuarioRepository = new InMemoryUsuarioRepository();
        this.equipoRepository = new InMemoryEquipoRepository();
        this.paqueteRepository = new InMemoryPaqueteRepository(this.equipoRepository);
        this.eventoRepository = new InMemoryEventoRepository();
        this.pedidoRepository = new InMemoryPedidoRepository();
        this.pagoRepository = new InMemoryPagoRepository();
        this.finanzasRepository = new InMemoryFinanzasRepository();
        this.sessionManager = SessionManager.getInstance();

        // Enlazar EventoRepository en EquipoRepository para calcular estadoEfectivo
        this.equipoRepository.setEventoRepository(this.eventoRepository);
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
}
