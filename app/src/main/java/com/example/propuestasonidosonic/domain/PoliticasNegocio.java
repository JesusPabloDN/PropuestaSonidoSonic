package com.example.propuestasonidosonic.domain;

import com.example.propuestasonidosonic.util.Config;

public class PoliticasNegocio {

    private final int horasRevision;
    private final int solicitudesPorAprobarMax;
    private final int bufferMontajeMin;
    private final int bufferDesmontajeMin;
    private final int maxEventosSimultaneos;
    private final int primerInicioMin;
    private final int ultimoInicioMin;
    private final int pasoHorarioMin;
    private final int duracionMinMin;
    private final int duracionMaxMin;
    private final boolean preferirAsistentes;

    public PoliticasNegocio(int horasRevision, int solicitudesPorAprobarMax, int bufferMontajeMin,
                            int bufferDesmontajeMin, int maxEventosSimultaneos,
                            int primerInicioMin, int ultimoInicioMin,
                            int pasoHorarioMin, int duracionMinMin,
                            int duracionMaxMin, boolean preferirAsistentes) {
        this.horasRevision = horasRevision;
        this.solicitudesPorAprobarMax = solicitudesPorAprobarMax;
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
                Config.ANTICIPACION_REVISION_HORAS,
                Config.SOLICITUDES_POR_APROBAR_MAX,
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

    public int getHorasRevision() {
        return horasRevision;
    }

    public int getSolicitudesPorAprobarMax() {
        return solicitudesPorAprobarMax;
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
