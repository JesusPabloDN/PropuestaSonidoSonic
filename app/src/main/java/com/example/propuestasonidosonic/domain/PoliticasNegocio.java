package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.util.Config;

public class PoliticasNegocio {

    private final int porcentajeAnticipo;
    private final int minutosApartadoApp;
    private final int[] opcionesHorasApartadoManual;
    private final int horasRevision;
    private final int diasReembolsoTotal;
    private final int horasTramoParcial;
    private final int porcentajeReembolsoParcial;
    private final int bufferMontajeMin;
    private final int bufferDesmontajeMin;
    private final int maxEventosSimultaneos;
    private final int primerInicioMin;
    private final int ultimoInicioMin;
    private final int pasoHorarioMin;
    private final int duracionMinMin;
    private final int duracionMaxMin;
    private final boolean preferirAsistentes;

    public PoliticasNegocio(int porcentajeAnticipo, int minutosApartadoApp,
                            int[] opcionesHorasApartadoManual, int horasRevision,
                            int diasReembolsoTotal, int horasTramoParcial,
                            int porcentajeReembolsoParcial, int bufferMontajeMin,
                            int bufferDesmontajeMin, int maxEventosSimultaneos,
                            int primerInicioMin, int ultimoInicioMin,
                            int pasoHorarioMin, int duracionMinMin,
                            int duracionMaxMin, boolean preferirAsistentes) {
        this.porcentajeAnticipo = porcentajeAnticipo;
        this.minutosApartadoApp = minutosApartadoApp;
        this.opcionesHorasApartadoManual = opcionesHorasApartadoManual != null
                ? opcionesHorasApartadoManual.clone() : new int[0];
        this.horasRevision = horasRevision;
        this.diasReembolsoTotal = diasReembolsoTotal;
        this.horasTramoParcial = horasTramoParcial;
        this.porcentajeReembolsoParcial = porcentajeReembolsoParcial;
        this.bufferMontajeMin = bufferMontajeMin;
        this.bufferDesmontajeMin = bufferDesmontajeMin;
        this.maxEventosSimultaneos = maxEventosSimultaneos;
        this.primerInicioMin = primerInicioMin;
        this.ultimoInicioMin = ultimoInicioMin;
        this.pasoHorarioMin = pasoHorarioMin;
        this.duracionMinMin = duracionMinMin;
        this.duracionMaxMin = duracionMaxMin;
        this.preferirAsistentes = preferirAsistentes;
    }

    public static PoliticasNegocio actual() {
        return new PoliticasNegocio(
                (int) Math.round(Config.ANTICIPO_PORCENTAJE * 100),
                Config.APARTADO_APP_MIN,
                Config.APARTADO_MANUAL_OPCIONES_HORAS,
                Config.ANTICIPACION_REVISION_HORAS,
                Config.REEMBOLSO_TOTAL_DIAS,
                Config.REEMBOLSO_PARCIAL_HORAS,
                (int) Math.round(Config.REEMBOLSO_PARCIAL_PORCENTAJE * 100),
                Config.BUFFER_MONTAJE_MIN_DEFAULT,
                Config.BUFFER_DESMONTAJE_MIN_DEFAULT,
                Config.MAX_EVENTOS_SIMULTANEOS,
                Config.PRIMER_INICIO_MIN,
                Config.ULTIMO_INICIO_MIN,
                Config.PASO_HORARIO_MIN,
                Config.DURACION_MIN_MIN,
                Config.DURACION_MAX_MIN,
                Config.PREFERIR_ASISTENTES
        );
    }

    public int getPorcentajeAnticipo() {
        return porcentajeAnticipo;
    }

    public int getMinutosApartadoApp() {
        return minutosApartadoApp;
    }

    public int[] getOpcionesHorasApartadoManual() {
        return opcionesHorasApartadoManual.clone();
    }

    public int getHorasRevision() {
        return horasRevision;
    }

    public int diasReembolsoTotal() {
        return diasReembolsoTotal;
    }

    public int getDiasReembolsoTotal() {
        return diasReembolsoTotal;
    }

    public int getHorasTramoParcial() {
        return horasTramoParcial;
    }

    public int getPorcentajeReembolsoParcial() {
        return porcentajeReembolsoParcial;
    }

    public int getBufferMontajeMin() {
        return bufferMontajeMin;
    }

    public int getBufferDesmontajeMin() {
        return bufferDesmontajeMin;
    }

    public int getMaxEventosSimultaneos() {
        return maxEventosSimultaneos;
    }

    public int getPrimerInicioMin() {
        return primerInicioMin;
    }

    public int getUltimoInicioMin() {
        return ultimoInicioMin;
    }

    public int getPasoHorarioMin() {
        return pasoHorarioMin;
    }

    public int getDuracionMinMin() {
        return duracionMinMin;
    }

    public int getDuracionMaxMin() {
        return duracionMaxMin;
    }

    public boolean isPreferirAsistentes() {
        return preferirAsistentes;
    }
}
