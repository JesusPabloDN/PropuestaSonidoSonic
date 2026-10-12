package com.example.propuestasonidosonic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.data.memory.InMemoryUsuarioRepository;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.PasswordValidator;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Etapa1UnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testPasswordValidator_RN14() {
        // Válidas: >= 8 caracteres, al menos 1 mayúscula, 1 número y 1 carácter especial
        assertTrue(PasswordValidator.isValid("Socio#2026"));
        assertTrue(PasswordValidator.isValid("Password123!"));
        assertTrue(PasswordValidator.isValid("Admin@2024"));
        assertTrue(PasswordValidator.isValid("Test.Pass1"));

        // Inválidas
        assertFalse("Muy corta", PasswordValidator.isValid("P#1a"));
        assertFalse("Sin mayúscula", PasswordValidator.isValid("password123!"));
        assertFalse("Sin número", PasswordValidator.isValid("Password!#$"));
        assertFalse("Sin carácter especial", PasswordValidator.isValid("Password123"));
        assertFalse("Nulo", PasswordValidator.isValid(null));
        assertFalse("Vacío", PasswordValidator.isValid(""));
    }

    @Test
    public void testCuentaSemillaSocio() {
        UsuarioRepository repo = new InMemoryUsuarioRepository();
        AtomicReference<Usuario> usuarioRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        repo.autenticar("socio@sonidosonic.com", "Socio#2026", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                usuarioRef.set(result);
            }

            @Override
            public void onError(String mensaje) {
                errorRef.set(mensaje);
            }
        });

        assertNotNull("La cuenta semilla debe autenticarse", usuarioRef.get());
        assertNull("No debe haber error en autenticación semilla", errorRef.get());
        assertEquals("Socio Principal", usuarioRef.get().getNombre());
        assertEquals(Usuario.ROL_SOCIO, usuarioRef.get().getRol());
        assertTrue(usuarioRef.get().isCuentaActiva());
    }

    @Test
    public void testAutenticacionFallida() {
        UsuarioRepository repo = new InMemoryUsuarioRepository();
        AtomicReference<String> errorRef = new AtomicReference<>();

        // Contraseña incorrecta
        repo.autenticar("socio@sonidosonic.com", "PasswordErroneo", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {}

            @Override
            public void onError(String mensaje) {
                errorRef.set(mensaje);
            }
        });
        assertNotNull(errorRef.get());

        // Correo no existente
        AtomicReference<String> errorRef2 = new AtomicReference<>();
        repo.autenticar("noexiste@sonidosonic.com", "Socio#2026", new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {}

            @Override
            public void onError(String mensaje) {
                errorRef2.set(mensaje);
            }
        });
        assertNotNull(errorRef2.get());
    }

    @Test
    public void testRegistroClienteYCorreoUnico() {
        UsuarioRepository repo = new InMemoryUsuarioRepository();

        Usuario nuevo = new Usuario();
        nuevo.setNombre("Cliente Prueba");
        nuevo.setCorreo("cliente@ejemplo.com");
        nuevo.setTelefono("5559876543");
        nuevo.setRol(Usuario.ROL_CLIENTE);
        nuevo.setContrasena("Cliente#2026");
        nuevo.setCuentaActiva(true);

        AtomicBoolean registroExitoso = new AtomicBoolean(false);
        repo.registrarUsuario(nuevo, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {
                registroExitoso.set(true);
            }

            @Override
            public void onError(String mensaje) {}
        });
        assertTrue("El cliente debe registrarse correctamente", registroExitoso.get());

        // Intentar registrar el mismo correo debe fallar
        Usuario duplicado = new Usuario();
        duplicado.setNombre("Otro");
        duplicado.setCorreo("cliente@ejemplo.com");
        duplicado.setContrasena("Cliente#2026");
        AtomicReference<String> errorDuplicado = new AtomicReference<>();
        repo.registrarUsuario(duplicado, new Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario result) {}

            @Override
            public void onError(String mensaje) {
                errorDuplicado.set(mensaje);
            }
        });
        assertNotNull("No se debe permitir correo duplicado", errorDuplicado.get());
    }

    @Test
    public void testSessionManager() {
        SessionManager sessionManager = new SessionManager();
        assertNull(sessionManager.getUsuarioActual());
        assertFalse(sessionManager.haySesion());

        Usuario socio = new Usuario();
        socio.setNombre("Socio 1");
        socio.setRol(Usuario.ROL_SOCIO);

        sessionManager.iniciarSesion(socio);
        assertTrue(sessionManager.haySesion());
        assertEquals(socio, sessionManager.getUsuarioActual());

        sessionManager.cerrarSesion();
        assertFalse(sessionManager.haySesion());
        assertNull(sessionManager.getUsuarioActual());
    }

    @Test
    public void testAppContainerExponeTodosLosRepositorios() {
        AppContainer container = new AppContainer();
        assertNotNull(container.getUsuarioRepository());
        assertNotNull(container.getEquipoRepository());
        assertNotNull(container.getPaqueteRepository());
        assertNotNull(container.getEventoRepository());
        assertNotNull(container.getPedidoRepository());
        assertNotNull(container.getDiaOperativoRepository());
        assertNotNull(container.getPagoRepository());
        assertNotNull(container.getFinanzasRepository());
        assertNotNull(container.getSessionManager());
    }

    @Test
    public void testRolAsistenteConstanteYSesion() {
        assertEquals("ASISTENTE", Usuario.ROL_ASISTENTE);
        Usuario asistente = new Usuario();
        asistente.setNombre("Asistente Prueba");
        asistente.setRol(Usuario.ROL_ASISTENTE);

        SessionManager sm = new SessionManager();
        sm.iniciarSesion(asistente);
        assertEquals(Usuario.ROL_ASISTENTE, sm.getUsuarioActual().getRol());
    }
}
