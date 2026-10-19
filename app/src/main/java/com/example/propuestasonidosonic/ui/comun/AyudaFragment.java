package com.example.propuestasonidosonic.ui.comun;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.propuestasonidosonic.R;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.domain.ContactoUtils;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

public class AyudaFragment extends Fragment {

    public AyudaFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ayuda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout layoutFaqContainer = view.findViewById(R.id.layoutFaqContainer);
        MaterialCardView cardContacto = view.findViewById(R.id.cardContactoAyuda);
        MaterialButton btnWhatsApp = view.findViewById(R.id.btnWhatsAppAyuda);

        SessionManager sessionManager = SessionManager.getInstancia(requireContext());
        String rol = sessionManager.getRol();

        int resPreguntas;
        int resRespuestas;
        boolean mostrarContacto = false;

        if (Usuario.ROL_SOCIO.equals(rol)) {
            resPreguntas = R.array.faq_socio_preguntas;
            resRespuestas = R.array.faq_socio_respuestas;
            mostrarContacto = false;
        } else if (Usuario.ROL_ASISTENTE.equals(rol)) {
            resPreguntas = R.array.faq_asistente_preguntas;
            resRespuestas = R.array.faq_asistente_respuestas;
            mostrarContacto = true;
        } else {
            // Cliente o fallback
            resPreguntas = R.array.faq_cliente_preguntas;
            resRespuestas = R.array.faq_cliente_respuestas;
            mostrarContacto = true;
        }

        String[] preguntas = getResources().getStringArray(resPreguntas);
        String[] respuestas = getResources().getStringArray(resRespuestas);

        layoutFaqContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < preguntas.length && i < respuestas.length; i++) {
            String pregunta = preguntas[i];
            String respuesta = respuestas[i];

            View itemView = inflater.inflate(R.layout.item_faq_pregunta, layoutFaqContainer, false);
            TextView tvPregunta = itemView.findViewById(R.id.tvPreguntaTitulo);
            TextView tvRespuesta = itemView.findViewById(R.id.tvRespuestaTexto);
            ImageView ivChevron = itemView.findViewById(R.id.ivChevron);
            View layoutHeader = itemView.findViewById(R.id.layoutPreguntaHeader);
            View layoutRespuesta = itemView.findViewById(R.id.layoutRespuestaContainer);

            tvPregunta.setText(pregunta);
            tvRespuesta.setText(respuesta);

            layoutHeader.setOnClickListener(v -> {
                boolean expandido = layoutRespuesta.getVisibility() == View.VISIBLE;
                if (expandido) {
                    layoutRespuesta.setVisibility(View.GONE);
                    ivChevron.animate().rotation(0f).setDuration(200).start();
                } else {
                    layoutRespuesta.setVisibility(View.VISIBLE);
                    ivChevron.animate().rotation(90f).setDuration(200).start();
                }
            });

            layoutFaqContainer.addView(itemView);
        }

        if (mostrarContacto) {
            cardContacto.setVisibility(View.VISIBLE);
            btnWhatsApp.setOnClickListener(v -> {
                String mensaje = getString(R.string.mensaje_whatsapp_ayuda);
                String url = ContactoUtils.urlWhatsApp(Config.NEGOCIO_WHATSAPP, mensaje);
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Snackbar.make(view, R.string.msg_no_app, Snackbar.LENGTH_SHORT).show();
                }
            });
        } else {
            cardContacto.setVisibility(View.GONE);
        }
    }
}
