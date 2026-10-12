package com.example.propuestasonidosonic.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

public final class CompartirRecibo {

    private CompartirRecibo() {}

    /**
     * Comparte el archivo PDF generado mediante Intent.ACTION_SEND con chooser.
     */
    public static void compartir(Context context, File archivo, String tituloDialogo) {
        if (context == null || archivo == null || !archivo.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String chooserTitulo = (tituloDialogo != null && !tituloDialogo.isEmpty()) ? tituloDialogo : "Compartir recibo";
        Intent chooser = Intent.createChooser(intent, chooserTitulo);
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }
}
