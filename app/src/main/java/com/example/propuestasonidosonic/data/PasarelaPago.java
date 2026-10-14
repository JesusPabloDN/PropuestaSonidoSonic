package com.example.propuestasonidosonic.data;

public interface PasarelaPago {

    class TarjetaDatos {
        private final String titular;
        private final String numero;
        private final int mes;
        private final int anio;
        private final String cvv;

        public TarjetaDatos(String titular, String numero, int mes, int anio, String cvv) {
            this.titular = titular;
            this.numero = numero;
            this.mes = mes;
            this.anio = anio;
            this.cvv = cvv;
        }

        public String getTitular() {
            return titular;
        }

        public String getNumero() {
            return numero;
        }

        public int getMes() {
            return mes;
        }

        public int getAnio() {
            return anio;
        }

        public String getCvv() {
            return cvv;
        }

        @Override
        public String toString() {
            return "TarjetaDatos{titular='" + titular + "'}";
        }
    }

    class ResultadoPago {
        private final boolean aprobado;
        private final String referencia;
        private final String motivoRechazo;
        private final String ultimos4;

        public ResultadoPago(boolean aprobado, String referencia, String motivoRechazo, String ultimos4) {
            this.aprobado = aprobado;
            this.referencia = referencia;
            this.motivoRechazo = motivoRechazo;
            this.ultimos4 = ultimos4;
        }

        public boolean isAprobado() {
            return aprobado;
        }

        public String getReferencia() {
            return referencia;
        }

        public String getMotivoRechazo() {
            return motivoRechazo;
        }

        public String getUltimos4() {
            return ultimos4;
        }
    }

    void cobrar(double monto, TarjetaDatos tarjeta, Callback<ResultadoPago> callback);
}
