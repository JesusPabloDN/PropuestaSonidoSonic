package com.example.propuestasonidosonic.ui.socio;

import androidx.lifecycle.ViewModel;

import com.example.propuestasonidosonic.model.ComponentePaquete;

import java.util.ArrayList;
import java.util.List;

public class PaqueteFormViewModel extends ViewModel {
    private final List<ComponentePaquete> componentes = new ArrayList<>();
    private boolean inicializado = false;

    public List<ComponentePaquete> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<ComponentePaquete> lista) {
        componentes.clear();
        if (lista != null) {
            for (ComponentePaquete c : lista) {
                componentes.add(new ComponentePaquete(c));
            }
        }
    }

    public void agregarComponente(ComponentePaquete componente) {
        componentes.add(componente);
    }

    public void removerComponente(ComponentePaquete componente) {
        componentes.remove(componente);
    }

    public boolean isInicializado() {
        return inicializado;
    }

    public void setInicializado(boolean inicializado) {
        this.inicializado = inicializado;
    }
}
