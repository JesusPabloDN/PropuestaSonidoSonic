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
import com.google.firebase.firestore.FirebaseFirestore;

public class AppContainer {
    private final InMemoryUsuarioRepository usuarioRepository;
    private final InMemoryEquipoRepository equipoRepository;
    private final InMemoryPaqueteRepository paqueteRepository;
    private final InMemoryEventoRepository eventoRepository;
    private final InMemoryDiaOperativoRepository diaOperativoRepository;
    private final InMemoryPedidoRepository pedidoRepository;
    private final InMemoryPagoRepository pagoRepository;
    private final InMemoryFinanzasRepository finanzasRepository;
    private final SessionManager sessionManager;
    private final PasarelaPago pasarelaPago;
    private final com.example.propuestasonidosonic.util.Reloj reloj;

    public AppContainer() {
        this.reloj = new RelojSistema();
        this.equipoRepository = new InMemoryEquipoRepository(this.reloj);
        this.paqueteRepository = new InMemoryPaqueteRepository(this.equipoRepository);
        this.sessionManager = SessionManager.getInstance();
        this.usuarioRepository = new InMemoryUsuarioRepository(this.sessionManager);
        this.diaOperativoRepository = new InMemoryDiaOperativoRepository();
        this.pagoRepository = new InMemoryPagoRepository(this.reloj);
        this.eventoRepository = new InMemoryEventoRepository();
        this.pasarelaPago = new com.example.propuestasonidosonic.data.memory.PasarelaSimulada();
        this.pedidoRepository = new InMemoryPedidoRepository(
                this.equipoRepository,
                this.paqueteRepository,
                this.usuarioRepository,
                this.diaOperativoRepository,
                this.pagoRepository,
                this.eventoRepository,
                this.reloj
        );
        this.finanzasRepository = new InMemoryFinanzasRepository();

        // Enlazar EventoRepository en EquipoRepository para calcular estadoEfectivo
        this.equipoRepository.setEventoRepository(this.eventoRepository);

        // Disparar recálculo de asignaciones cuando cambie el inventario
        this.equipoRepository.setOnInventarioCambio(() -> this.pedidoRepository.reasignarUnidades(null));
    }

    public void conectarFirestore() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            conectarFirestore(db);
        } catch (Exception ignored) {
            // En caso de ejecutarse en un entorno de pruebas sin FirebaseApp inicializado
        }
    }

    public void conectarFirestore(FirebaseFirestore db) {
        if (db == null) return;
        this.usuarioRepository.conectarFirestore(db);
        this.equipoRepository.conectarFirestore(db);
        this.paqueteRepository.conectarFirestore(db);
        this.diaOperativoRepository.conectarFirestore(db);
        this.pagoRepository.conectarFirestore(db);
        this.eventoRepository.conectarFirestore(db);
        this.pedidoRepository.conectarFirestore(db);
        this.finanzasRepository.conectarFirestore(db);
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

    public com.example.propuestasonidosonic.util.Reloj getReloj() {
        return reloj;
    }
}
