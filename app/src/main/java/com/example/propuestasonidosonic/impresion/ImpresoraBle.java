package com.example.propuestasonidosonic.impresion;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Imprime un ticket en la Nelko PM290 por Bluetooth de bajo consumo (BLE), sin la app de Nelko.
 *
 * Nelko no publica un SDK; el protocolo es el que se capturó de la app oficial con una PM290C:
 * servicio FF00, se escribe en FF02 y la impresora avisa por FF01. Después del encabezado,
 * la impresora indica cuántos bytes del dibujo acepta ("f0 00 HH LL"); se mandan en partes
 * de 240 bytes y, al terminar, avisa "63 00".
 *
 * Todo corre en un hilo aparte; los resultados se entregan en el hilo principal.
 * Quien la llame debe haber verificado los permisos de Bluetooth.
 */
public final class ImpresoraBle {

    public interface Listener {
        void onProgreso(String mensaje);

        /** @param confirmado true si la impresora avisó que terminó de imprimir. */
        void onTerminado(boolean confirmado);

        void onError(String mensaje);
    }

    private static final UUID SERVICIO = uuid16(0xFF00);
    private static final UUID ESCRITURA = uuid16(0xFF02);
    private static final UUID AVISOS = uuid16(0xFF01);
    private static final UUID CCCD = uuid16(0x2902);

    private static final int MTU_DESEADO = 247;
    private static final int PARTE_MAXIMA = 240;
    private static final long PAUSA_ENTRE_PARTES_MS = 15;
    private static final long PAUSA_ANTES_DE_ENVIAR_MS = 400;

    private final Context context;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final LinkedBlockingQueue<byte[]> avisos = new LinkedBlockingQueue<>();
    private final Semaphore escrituraLista = new Semaphore(0);

    private CountDownLatch conectado;
    private CountDownLatch serviciosListos;
    private CountDownLatch mtuListo;
    private CountDownLatch descriptorListo;
    private volatile boolean desconectado;
    private volatile int mtu = 23;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic caracteristicaEscritura;

    private ImpresoraBle(Context context) {
        this.context = context.getApplicationContext();
    }

    private static UUID uuid16(int corto) {
        return UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", corto));
    }

    /** Envía el ticket (mapa de bits de 384 px de ancho) a la impresora con esa dirección. */
    public static void imprimir(Context context, String direccion, Bitmap ticket, Listener listener) {
        ImpresoraBle impresora = new ImpresoraBle(context);
        new Thread(() -> impresora.trabajo(direccion, ticket, listener), "impresion-ticket").start();
    }

    private void progreso(Listener l, String m) {
        principal.post(() -> l.onProgreso(m));
    }

    @SuppressLint("MissingPermission")
    private void trabajo(String direccion, Bitmap ticket, Listener listener) {
        try {
            int ancho = ticket.getWidth();
            int alto = ticket.getHeight();
            int[] pixeles = new int[ancho * alto];
            ticket.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
            byte[] raster = RasterTermico.desdePixeles(pixeles, ancho, alto);

            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adaptador = manager != null ? manager.getAdapter() : null;
            if (adaptador == null || !adaptador.isEnabled()) {
                throw new ErrorImpresion("El Bluetooth está apagado.");
            }

            progreso(listener, "Conectando con la impresora…");
            BluetoothDevice dispositivo = adaptador.getRemoteDevice(direccion);
            conectado = new CountDownLatch(1);
            serviciosListos = new CountDownLatch(1);
            gatt = dispositivo.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
            if (!conectado.await(12, TimeUnit.SECONDS) || desconectado) {
                throw new ErrorImpresion("No se pudo conectar. Revisa que la impresora esté encendida, cerca del teléfono "
                        + "y que la app de Nelko/Labelnize no esté conectada a ella.");
            }

            mtuListo = new CountDownLatch(1);
            if (gatt.requestMtu(MTU_DESEADO)) {
                mtuListo.await(3, TimeUnit.SECONDS);
            }

            if (!gatt.discoverServices() || !serviciosListos.await(10, TimeUnit.SECONDS)) {
                throw new ErrorImpresion("La impresora no respondió al buscar sus servicios.");
            }
            BluetoothGattService servicio = gatt.getService(SERVICIO);
            if (servicio == null) {
                throw new ErrorImpresion("Este dispositivo no tiene el servicio de impresión FF00. "
                        + "¿Elegiste la impresora correcta?");
            }
            caracteristicaEscritura = servicio.getCharacteristic(ESCRITURA);
            BluetoothGattCharacteristic caracteristicaAvisos = servicio.getCharacteristic(AVISOS);
            if (caracteristicaEscritura == null || caracteristicaAvisos == null) {
                throw new ErrorImpresion("La impresora no tiene las características FF01/FF02 que se esperaban.");
            }
            activarAvisos(caracteristicaAvisos);

            // 1. Estado de la impresora
            avisos.clear();
            escribir(RasterTermico.consultaEstado());
            byte[] estado = avisos.poll(2, TimeUnit.SECONDS);
            if (estado != null && estado.length > 0 && estado[0] != 0x00 && (estado[0] & 0xff) != 0xf0) {
                throw new ErrorImpresion("La impresora no está lista (estado " + RasterTermico.hex(estado)
                        + "). Revisa el papel y la tapa.");
            }

            // 2. Encabezado del trabajo
            progreso(listener, "Enviando ticket…");
            avisos.clear();
            escribirEnPartes(RasterTermico.encabezado(alto));

            // 3. Dibujo, según lo que la impresora va aceptando
            int enviados = 0;
            while (enviados < raster.length) {
                int permitidos = esperarPermiso();
                Thread.sleep(PAUSA_ANTES_DE_ENVIAR_MS);
                int hasta = Math.min(raster.length, enviados + permitidos);
                int parte = Math.max(20, Math.min(PARTE_MAXIMA, mtu - 3));
                while (enviados < hasta) {
                    int n = Math.min(parte, hasta - enviados);
                    byte[] trozo = new byte[n];
                    System.arraycopy(raster, enviados, trozo, 0, n);
                    escribir(trozo);
                    enviados += n;
                    Thread.sleep(PAUSA_ENTRE_PARTES_MS);
                }
                int porcentaje = (int) (enviados * 100L / raster.length);
                progreso(listener, "Enviando ticket… " + porcentaje + " %");
            }

            // 4. Confirmación de fin
            progreso(listener, "Imprimiendo…");
            boolean confirmado = esperarFin();
            cerrar();
            principal.post(() -> listener.onTerminado(confirmado));
        } catch (ErrorImpresion e) {
            cerrar();
            principal.post(() -> listener.onError(e.getMessage()));
        } catch (InterruptedException e) {
            cerrar();
            Thread.currentThread().interrupt();
            principal.post(() -> listener.onError("Se interrumpió la impresión."));
        } catch (Exception e) {
            cerrar();
            String detalle = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            principal.post(() -> listener.onError("Error al imprimir: " + detalle));
        }
    }

    private int esperarPermiso() throws InterruptedException, ErrorImpresion {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            byte[] aviso = avisos.poll(limite - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (aviso == null) break;
            int permitidos = RasterTermico.bytesPermitidos(aviso);
            if (permitidos > 0) return permitidos;
            if (desconectado) break;
        }
        throw new ErrorImpresion("La impresora no pidió el dibujo del ticket. "
                + "Apágala y vuelve a encenderla antes de intentar de nuevo.");
    }

    private boolean esperarFin() throws InterruptedException {
        long limite = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < limite) {
            byte[] aviso = avisos.poll(limite - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (aviso == null) return false;
            if (RasterTermico.esFin(aviso)) return true;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void activarAvisos(BluetoothGattCharacteristic c) throws InterruptedException, ErrorImpresion {
        gatt.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(CCCD);
        if (d == null) return;
        descriptorListo = new CountDownLatch(1);
        boolean ok;
        if (Build.VERSION.SDK_INT >= 33) {
            ok = gatt.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS;
        } else {
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            ok = gatt.writeDescriptor(d);
        }
        if (!ok || !descriptorListo.await(5, TimeUnit.SECONDS)) {
            throw new ErrorImpresion("No se pudieron activar los avisos de la impresora.");
        }
    }

    private void escribirEnPartes(byte[] datos) throws InterruptedException, ErrorImpresion {
        int parte = Math.max(20, Math.min(PARTE_MAXIMA, mtu - 3));
        for (int i = 0; i < datos.length; i += parte) {
            int n = Math.min(parte, datos.length - i);
            byte[] trozo = new byte[n];
            System.arraycopy(datos, i, trozo, 0, n);
            escribir(trozo);
            Thread.sleep(PAUSA_ENTRE_PARTES_MS);
        }
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void escribir(byte[] datos) throws InterruptedException, ErrorImpresion {
        if (desconectado) throw new ErrorImpresion("La impresora se desconectó.");
        boolean sinRespuesta = (caracteristicaEscritura.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
        int tipo = sinRespuesta ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
        escrituraLista.drainPermits();
        for (int intento = 0; intento < 50; intento++) {
            boolean aceptada;
            if (Build.VERSION.SDK_INT >= 33) {
                aceptada = gatt.writeCharacteristic(caracteristicaEscritura, datos, tipo) == BluetoothStatusCodes.SUCCESS;
            } else {
                caracteristicaEscritura.setWriteType(tipo);
                caracteristicaEscritura.setValue(datos);
                aceptada = gatt.writeCharacteristic(caracteristicaEscritura);
            }
            if (aceptada) {
                if (!escrituraLista.tryAcquire(3, TimeUnit.SECONDS)) {
                    throw new ErrorImpresion("La impresora dejó de recibir datos.");
                }
                return;
            }
            Thread.sleep(20); // la cola de Bluetooth del teléfono estaba ocupada
        }
        throw new ErrorImpresion("El teléfono no pudo enviar datos a la impresora.");
    }

    @SuppressLint("MissingPermission")
    private void cerrar() {
        if (gatt != null) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (Exception ignored) {
            }
            gatt = null;
        }
    }

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                conectado.countDown();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                desconectado = true;
                conectado.countDown();
                if (serviciosListos != null) serviciosListos.countDown();
                escrituraLista.release();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int nuevoMtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) mtu = nuevoMtu;
            if (mtuListo != null) mtuListo.countDown();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            serviciosListos.countDown();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor d, int status) {
            if (descriptorListo != null) descriptorListo.countDown();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            escrituraLista.release();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value) {
            if (AVISOS.equals(c.getUuid()) && value != null) avisos.offer(value.clone());
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            // Android 12 y anteriores usan esta versión
            if (Build.VERSION.SDK_INT >= 33) return;
            byte[] value = c.getValue();
            if (AVISOS.equals(c.getUuid()) && value != null) avisos.offer(value.clone());
        }
    };

    private static final class ErrorImpresion extends Exception {
        ErrorImpresion(String mensaje) {
            super(mensaje);
        }
    }
}
