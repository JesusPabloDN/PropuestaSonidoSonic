package com.example.propuestasonidosonic.ui.cliente;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.SonidoSonicApp;
import com.example.propuestasonidosonic.data.PaqueteRepository;
import com.example.propuestasonidosonic.domain.MotorDisponibilidad;
import com.example.propuestasonidosonic.model.CategoriaEquipo;
import com.example.propuestasonidosonic.model.ComponentePaquete;
import com.example.propuestasonidosonic.model.Paquete;
import com.example.propuestasonidosonic.util.FormatUtils;
import com.google.android.material.button.MaterialButton;

public class PaqueteDetalleClienteFragment extends Fragment {

    private ImageView ivFotoDetalle;
    private TextView tvNombreDetalle;
    private TextView tvPrecioDetalle;
    private TextView tvDescripcionDetalle;
    private LinearLayout llComponentesDetalle;
    private TextView tvDuracionDetalle;
    private TextView tvHoraExtraDetalle;
    private TextView tvAnticipoDetalle;
    private MaterialButton btnReservar;

    private PaqueteRepository paqueteRepository;
    private String paqueteId;
    private Paquete paqueteActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paquete_detalle_cliente, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paqueteRepository = SonidoSonicApp.getApp(requireContext()).getAppContainer().getPaqueteRepository();

        if (getArguments() != null) {
            paqueteId = getArguments().getString("paqueteId");
        }

        ivFotoDetalle = view.findViewById(R.id.ivFotoDetalle);
        tvNombreDetalle = view.findViewById(R.id.tvNombreDetalle);
        tvPrecioDetalle = view.findViewById(R.id.tvPrecioDetalle);
        tvDescripcionDetalle = view.findViewById(R.id.tvDescripcionDetalle);
        llComponentesDetalle = view.findViewById(R.id.llComponentesDetalle);
        tvDuracionDetalle = view.findViewById(R.id.tvDuracionDetalle);
        tvHoraExtraDetalle = view.findViewById(R.id.tvHoraExtraDetalle);
        tvAnticipoDetalle = view.findViewById(R.id.tvAnticipoDetalle);
        btnReservar = view.findViewById(R.id.btnReservar);

        cargarPaquete();

        btnReservar.setOnClickListener(v -> {
            if (paqueteActual == null) return;
            Bundle args = new Bundle();
            args.putString("paqueteId", paqueteActual.getId());
            Navigation.findNavController(requireView()).navigate(R.id.action_paqueteDetalle_to_reservar, args);
        });
    }

    private void cargarPaquete() {
        if (paqueteId == null || paqueteRepository == null) return;
        paqueteActual = paqueteRepository.getPaquetePorId(paqueteId);

        if (paqueteActual == null) {
            Toast.makeText(requireContext(), "Paquete no encontrado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        tvNombreDetalle.setText(paqueteActual.getNombre());
        tvPrecioDetalle.setText(FormatUtils.formatoMoneda(paqueteActual.getPrecio()));

        String desc = paqueteActual.getDescripcion();
        if (desc != null && !desc.trim().isEmpty()) {
            tvDescripcionDetalle.setText(desc.trim());
            tvDescripcionDetalle.setVisibility(View.VISIBLE);
        } else {
            tvDescripcionDetalle.setVisibility(View.GONE);
        }

        // Foto
        String catPrincipal = CategoriaEquipo.BOCINAS;
        if (paqueteActual.getComponentes() != null && !paqueteActual.getComponentes().isEmpty()) {
            catPrincipal = paqueteActual.getComponentes().get(0).getCategoria();
        }
        int fallbackDrawable = CategoriaEquipo.getImagenPorDefecto(catPrincipal);
        if (paqueteActual.getFotoUri() != null && !paqueteActual.getFotoUri().trim().isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(paqueteActual.getFotoUri()))
                    .centerCrop()
                    .error(fallbackDrawable)
                    .into(ivFotoDetalle);
        } else {
            Glide.with(this)
                    .load(fallbackDrawable)
                    .centerCrop()
                    .into(ivFotoDetalle);
        }

        // Componentes incluidos
        llComponentesDetalle.removeAllViews();
        if (paqueteActual.getComponentes() != null) {
            for (ComponentePaquete c : paqueteActual.getComponentes()) {
                TextView tvItem = new TextView(requireContext());
                tvItem.setTextSize(14);
                tvItem.setPadding(0, 4, 0, 4);
                tvItem.setTextColor(getResources().getColor(android.R.color.tab_indicator_text, null));
                String texto = "• " + c.getCantidad() + " × " + c.getNombreModelo() + " (" + c.getCategoria() + ")";
                tvItem.setText(texto);
                llComponentesDetalle.addView(tvItem);
            }
        }

        tvDuracionDetalle.setText(getString(R.string.paquete_detalle_duracion_formato, paqueteActual.getDuracionBaseHoras()));
        tvHoraExtraDetalle.setText(getString(R.string.paquete_detalle_hora_extra_formato, FormatUtils.formatoMoneda(paqueteActual.getPrecioHoraExtra())));

        double totalBase = paqueteActual.getPrecio();
        double anticipo = MotorDisponibilidad.calcularAnticipo(totalBase);
        tvAnticipoDetalle.setText(getString(R.string.paquete_detalle_anticipo_formato, FormatUtils.formatoMoneda(anticipo)));
    }
}
