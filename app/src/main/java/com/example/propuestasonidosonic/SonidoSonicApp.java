package com.example.propuestasonidosonic;

import android.app.Application;
import android.content.Context;

import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.util.ThemeHelper;

public class SonidoSonicApp extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer();
        ThemeHelper.applySavedTheme(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }

    public static SonidoSonicApp getApp(Context context) {
        return (SonidoSonicApp) context.getApplicationContext();
    }
}
