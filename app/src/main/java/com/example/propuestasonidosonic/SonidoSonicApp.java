package com.example.propuestasonidosonic;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.util.ThemeHelper;

public class SonidoSonicApp extends Application {
    private AppContainer appContainer;
    private final Handler cleanupHandler = new Handler(Looper.getMainLooper());
    private final Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {
            if (appContainer != null && appContainer.getPedidoRepository() != null) {
                appContainer.getPedidoRepository().liberarApartadosVencidos();
            }
            cleanupHandler.postDelayed(this, 60_000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer();
        ThemeHelper.applySavedTheme(this);
        cleanupHandler.postDelayed(cleanupRunnable, 60_000L);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public static SonidoSonicApp getApp(Context context) {
        return (SonidoSonicApp) context.getApplicationContext();
    }
}
