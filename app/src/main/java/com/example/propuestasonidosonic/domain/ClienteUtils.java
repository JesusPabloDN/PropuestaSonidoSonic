package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.model.Pago;
import com.example.propuestasonidosonic.model.Pedido;
import com.example.propuestasonidosonic.model.Usuario;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ClienteUtils {

    private ClienteUtils() {}

    public static String soloDigitos(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String telefono10(String s) {
        String d = soloDigitos(s);
        if (d.length() >= 10) {
            return d.substring(d.length() - 10);
        }
        return "";
    }

    public static String normalizarNombre(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "";
        String norm = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        norm = norm.replaceAll("\\p{M}", "");
        norm = norm.toLowerCase(Locale.ROOT);
        norm = norm.replaceAll("\\s+", " ").trim();
        return norm;
    }

    public static String clave(String clienteId, String telefono, String nombre) {
        String t10 = telefono10(telefono);
        if (!t10.isEmpty()) {
            return "tel:" + t10;
        }
        if (clienteId != null && !clienteId.trim().isEmpty()) {
            return "id:" + clienteId.trim();
        }
        return "nom:" + normalizarNombre(nombre);
    }

    public static String formatoTelefono(String telefono) {
        String t10 = telefono10(telefono);
        if (t10.length() == 10) {
            return "(" + t10.substring(0, 3) + ") " + t10.substring(3, 6) + "-" + t10.substring(6);
        }
        return (telefono != null && !telefono.trim().isEmpty()) ? telefono.trim() : "—";
    }

    public static class ResumenCliente {
        private final String clave;
        private final String nombre;
        private final String telefono;
        private final String correo;
        private final String clienteId;
        private final int pedidosValidos;
        private final double totalPagadoNeto;
        private final double saldoPendiente;
        private final long ultimoEventoMillis;

        public ResumenCliente(String clave, String nombre, String telefono, String correo,
                              String clienteId, int pedidosValidos, double totalPagadoNeto,
                              double saldoPendiente, long ultimoEventoMillis) {
            this.clave = clave != null ? clave : "";
            this.nombre = nombre != null ? nombre : "";
            this.telefono = telefono != null ? telefono : "";
            this.correo = correo;
            this.clienteId = clienteId;
            this.pedidosValidos = pedidosValidos;
            this.totalPagadoNeto = totalPagadoNeto;
            this.saldoPendiente = saldoPendiente;
            this.ultimoEventoMillis = ultimoEventoMillis;
        }

        public String getClave() {
            return clave;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTelefono() {
            return telefono;
        }

        public String getCorreo() {
            return correo;
        }

        public String getClienteId() {
            return clienteId;
        }

        public int getPedidosValidos() {
            return pedidosValidos;
        }

        public double getTotalPagadoNeto() {
            return totalPagadoNeto;
        }

        public double getSaldoPendiente() {
            return saldoPendiente;
        }

        public long getUltimoEventoMillis() {
            return ultimoEventoMillis;
        }
    }

    private static class ClienteAcumulador {
        String clave;
        String nombre = "";
        String telefono = "";
        String correo = null;
        String clienteId = null;
        int pedidosValidos = 0;
        double ingresos = 0.0;
        double reembolsos = 0.0;
        double saldoPendiente = 0.0;
        long ultimoEventoMillis = 0L;
        long inicioMasReciente = -1L;
        long fechaTelefonoMasReciente = -1L;
        final Set<String> pedidoIds = new HashSet<>();
    }

    public static List<ResumenCliente> resumenes(List<Pedido> pedidos, List<Pago> pagos, List<Usuario> usuarios) {
        Map<String, ClienteAcumulador> mapa = new HashMap<>();

        // 1. Mapear usuarios cliente para vincular correo y clienteId
        Map<String, Usuario> usuariosPorTel = new HashMap<>();
        Map<String, Usuario> usuariosPorId = new HashMap<>();
        List<Usuario> clientesUsuarios = new ArrayList<>();

        if (usuarios != null) {
            for (Usuario u : usuarios) {
                if (u == null || !Usuario.ROL_CLIENTE.equals(u.getRol())) continue;
                clientesUsuarios.add(u);
                if (u.getId() != null) {
                    usuariosPorId.put(u.getId(), u);
                }
                String t10 = telefono10(u.getTelefono());
                if (!t10.isEmpty()) {
                    usuariosPorTel.put(t10, u);
                }
            }
        }

        // 2. Procesar pedidos
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                if (p == null) continue;
                String k = clave(p.getClienteId(), p.getClienteTelefono(), p.getClienteNombre());
                ClienteAcumulador ac = mapa.get(k);
                if (ac == null) {
                    ac = new ClienteAcumulador();
                    ac.clave = k;
                    mapa.put(k, ac);
                }

                if (p.getId() != null) {
                    ac.pedidoIds.add(p.getId());
                }

                long inicio = p.getInicioMillis();
                if (inicio > ac.ultimoEventoMillis) {
                    ac.ultimoEventoMillis = inicio;
                }

                // Nombre del pedido más reciente
                if (inicio > ac.inicioMasReciente) {
                    ac.inicioMasReciente = inicio;
                    if (p.getClienteNombre() != null && !p.getClienteNombre().trim().isEmpty()) {
                        ac.nombre = p.getClienteNombre().trim();
                    }
                }

                // Teléfono más reciente no vacío
                if (p.getClienteTelefono() != null && !p.getClienteTelefono().trim().isEmpty()) {
                    if (inicio >= ac.fechaTelefonoMasReciente) {
                        ac.fechaTelefonoMasReciente = inicio;
                        ac.telefono = p.getClienteTelefono().trim();
                    }
                }

                // Pedidos válidos para totales
                String st = p.getEstado();
                boolean esValido = Pedido.ESTADO_CONFIRMADO.equals(st)
                        || Pedido.ESTADO_PAGADO.equals(st)
                        || Pedido.ESTADO_EN_REVISION.equals(st)
                        || Pedido.ESTADO_COMPLETADO.equals(st);

                if (esValido) {
                    ac.pedidosValidos++;
                    ac.saldoPendiente += p.getSaldoPendiente();
                }
            }
        }

        // 3. Vincular datos de Usuario a acumuladores existentes
        for (ClienteAcumulador ac : mapa.values()) {
            Usuario uMatch = null;
            String t10 = telefono10(ac.telefono);
            if (!t10.isEmpty() && usuariosPorTel.containsKey(t10)) {
                uMatch = usuariosPorTel.get(t10);
            }
            if (uMatch == null && ac.clave.startsWith("id:")) {
                String id = ac.clave.substring(3);
                if (usuariosPorId.containsKey(id)) {
                    uMatch = usuariosPorId.get(id);
                }
            }
            if (uMatch != null) {
                ac.correo = uMatch.getCorreo();
                ac.clienteId = uMatch.getId();
                if (ac.telefono.isEmpty() && uMatch.getTelefono() != null) {
                    ac.telefono = uMatch.getTelefono();
                }
                if (ac.nombre.isEmpty() && uMatch.getNombre() != null) {
                    ac.nombre = uMatch.getNombre();
                }
            }
        }

        // 4. Incluir usuarios cliente sin pedidos
        for (Usuario u : clientesUsuarios) {
            String k = clave(u.getId(), u.getTelefono(), u.getNombre());
            if (!mapa.containsKey(k)) {
                ClienteAcumulador ac = new ClienteAcumulador();
                ac.clave = k;
                ac.nombre = u.getNombre() != null ? u.getNombre().trim() : "";
                ac.telefono = u.getTelefono() != null ? u.getTelefono().trim() : "";
                ac.correo = u.getCorreo();
                ac.clienteId = u.getId();
                mapa.put(k, ac);
            }
        }

        // 5. Mapear pagos de cada pedido a su cliente correspondiente
        Map<String, ClienteAcumulador> pedidoIdToCliente = new HashMap<>();
        for (ClienteAcumulador ac : mapa.values()) {
            for (String pid : ac.pedidoIds) {
                pedidoIdToCliente.put(pid, ac);
            }
        }

        if (pagos != null) {
            for (Pago pago : pagos) {
                if (pago == null) continue;
                ClienteAcumulador ac = null;
                if (pago.getPedidoId() != null) {
                    ac = pedidoIdToCliente.get(pago.getPedidoId());
                }
                if (ac == null && pago.getClienteId() != null) {
                    String userKey = "id:" + pago.getClienteId();
                    ac = mapa.get(userKey);
                    if (ac == null) {
                        Usuario u = usuariosPorId.get(pago.getClienteId());
                        if (u != null) {
                            String k = clave(u.getId(), u.getTelefono(), u.getNombre());
                            ac = mapa.get(k);
                        }
                    }
                }
                if (ac != null) {
                    String tipo = pago.getTipo();
                    if (Pago.TIPO_ANTICIPO.equals(tipo) || Pago.TIPO_ABONO.equals(tipo) || Pago.TIPO_TOTAL.equals(tipo)) {
                        ac.ingresos += pago.getMonto();
                    } else if (Pago.TIPO_REEMBOLSO.equals(tipo)) {
                        ac.reembolsos += pago.getMonto();
                    }
                }
            }
        }

        // 6. Construir lista final
        List<ResumenCliente> lista = new ArrayList<>();
        for (ClienteAcumulador ac : mapa.values()) {
            double neto = MoneyUtils.redondear2(ac.ingresos - ac.reembolsos);
            double saldo = MoneyUtils.redondear2(ac.saldoPendiente);
            lista.add(new ResumenCliente(
                    ac.clave,
                    ac.nombre,
                    ac.telefono,
                    ac.correo,
                    ac.clienteId,
                    ac.pedidosValidos,
                    neto,
                    saldo,
                    ac.ultimoEventoMillis
            ));
        }

        // 7. Ordenar alfabéticamente por nombre normalizado
        Collections.sort(lista, new Comparator<ResumenCliente>() {
            @Override
            public int compare(ResumenCliente o1, ResumenCliente o2) {
                String n1 = normalizarNombre(o1.getNombre());
                String n2 = normalizarNombre(o2.getNombre());
                return n1.compareTo(n2);
            }
        });

        return lista;
    }

    public static List<Pedido> pedidosDelCliente(List<Pedido> pedidos, String clave) {
        List<Pedido> resultado = new ArrayList<>();
        if (pedidos == null || clave == null) {
            return resultado;
        }

        for (Pedido p : pedidos) {
            if (p == null) continue;
            String k = clave(p.getClienteId(), p.getClienteTelefono(), p.getClienteNombre());
            boolean coincide = k.equals(clave);
            if (!coincide && clave.startsWith("tel:")) {
                String tel10 = clave.substring(4);
                if (tel10.equals(telefono10(p.getClienteTelefono()))) {
                    coincide = true;
                }
            }
            if (!coincide && clave.startsWith("id:") && p.getClienteId() != null) {
                String id = clave.substring(3);
                if (id.equals(p.getClienteId())) {
                    coincide = true;
                }
            }
            if (coincide) {
                resultado.add(new Pedido(p));
            }
        }

        Collections.sort(resultado, new Comparator<Pedido>() {
            @Override
            public int compare(Pedido o1, Pedido o2) {
                return Long.compare(o2.getInicioMillis(), o1.getInicioMillis());
            }
        });

        return resultado;
    }

    public static List<ResumenCliente> buscar(List<ResumenCliente> lista, String consulta) {
        if (lista == null) return new ArrayList<>();
        if (consulta == null || consulta.trim().isEmpty()) {
            return new ArrayList<>(lista);
        }

        String qNorm = normalizarNombre(consulta);
        String qDig = soloDigitos(consulta);
        boolean buscarPorTelefono = qDig.length() >= 3;

        List<ResumenCliente> filtrados = new ArrayList<>();
        for (ResumenCliente c : lista) {
            if (c == null) continue;
            boolean coincideNombre = false;
            if (!qNorm.isEmpty()) {
                String nomNorm = normalizarNombre(c.getNombre());
                if (nomNorm.contains(qNorm)) {
                    coincideNombre = true;
                }
            }

            boolean coincideTelefono = false;
            if (buscarPorTelefono) {
                String telDig = soloDigitos(c.getTelefono());
                if (telDig.contains(qDig)) {
                    coincideTelefono = true;
                }
            }

            if (coincideNombre || coincideTelefono) {
                filtrados.add(c);
            }
        }

        return filtrados;
    }
}
