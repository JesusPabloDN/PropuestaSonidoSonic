package com.example.propuestasonidosonic;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.propuestasonidosonic.data.AppContainer;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    private TextView tvDrawerUserName;
    private TextView tvDrawerUserRole;
    private LinearLayout drawerItemEquipo;
    private LinearLayout drawerItemTheme;
    private ImageView ivDrawerThemeIcon;
    private TextView tvDrawerThemeLabel;
    private LinearLayout drawerItemLogout;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.main_content).setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            findViewById(R.id.bottomNavigationView).setPadding(0, 0, 0, systemBars.bottom);

            // Bajar el encabezado del drawer para que no quede pegado a la barra de estado
            int extraTopSpacing = getResources().getDimensionPixelSize(R.dimen.spacing_xlarge);
            int extraBottomSpacing = getResources().getDimensionPixelSize(R.dimen.spacing_medium);
            findViewById(R.id.drawerView).setPadding(0, systemBars.top + extraTopSpacing, 0, systemBars.bottom + extraBottomSpacing);
            return windowInsets;
        });

        AppContainer appContainer = SonidoSonicApp.getApp(this).getAppContainer();
        sessionManager = appContainer.getSessionManager();

        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserRole = findViewById(R.id.tvDrawerUserRole);
        drawerItemEquipo = findViewById(R.id.drawerItemEquipo);
        drawerItemTheme = findViewById(R.id.drawerItemTheme);
        ivDrawerThemeIcon = findViewById(R.id.ivDrawerThemeIcon);
        tvDrawerThemeLabel = findViewById(R.id.tvDrawerThemeLabel);
        drawerItemLogout = findViewById(R.id.drawerItemLogout);

        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Definir destinos de nivel superior (con ícono hamburguesa que abre el drawer)
        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_dashboard);
        topLevelDestinations.add(R.id.nav_inventario);
        topLevelDestinations.add(R.id.nav_agenda);
        topLevelDestinations.add(R.id.nav_pedidos);
        topLevelDestinations.add(R.id.nav_finanzas);
        topLevelDestinations.add(R.id.clienteHomeFragment);
        topLevelDestinations.add(R.id.asistenteHomeFragment);

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (navController == null) {
                return false;
            }
            int currentId = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId() : 0;
            if (currentId == itemId) {
                return true;
            }
            if (itemId == R.id.nav_dashboard) {
                navController.popBackStack(R.id.nav_dashboard, false);
                return true;
            }

            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.nav_dashboard, false)
                    .build();
            navController.navigate(itemId, null, navOptions);
            return true;
        });

        bottomNavigationView.setOnItemReselectedListener(item -> {
            // No hacer nada al reseleccionar la pestaña activa
        });

        configurarNavegacionVisibilidad();
        configurarDrawer();
        configurarBackPressed();
    }

    private void configurarNavegacionVisibilidad() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();

            if (id == R.id.loginFragment) {
                toolbar.setVisibility(View.GONE);
                bottomNavigationView.setVisibility(View.GONE);
                drawerLayout.closeDrawer(GravityCompat.START);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else if (id == R.id.registerFragment) {
                toolbar.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.GONE);
                drawerLayout.closeDrawer(GravityCompat.START);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else if (id == R.id.nav_dashboard || id == R.id.nav_inventario ||
                       id == R.id.nav_agenda || id == R.id.nav_pedidos ||
                       id == R.id.nav_finanzas) {
                toolbar.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.VISIBLE);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                android.view.MenuItem menuItem = bottomNavigationView.getMenu().findItem(id);
                if (menuItem != null) {
                    menuItem.setChecked(true);
                }
            } else {
                toolbar.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.GONE);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        });
    }

    private void configurarDrawer() {
        // Observar sesión para datos del usuario en encabezado del drawer
        sessionManager.getUsuarioActualLiveData().observe(this, usuario -> {
            if (usuario != null) {
                tvDrawerUserName.setText(usuario.getNombre());
                String rolTexto;
                if (Usuario.ROL_SOCIO.equals(usuario.getRol())) {
                    rolTexto = getString(R.string.drawer_header_rol_socio);
                } else if (Usuario.ROL_CLIENTE.equals(usuario.getRol())) {
                    rolTexto = getString(R.string.drawer_header_rol_cliente);
                } else if (Usuario.ROL_ASISTENTE.equals(usuario.getRol())) {
                    rolTexto = getString(R.string.drawer_header_rol_asistente);
                } else {
                    rolTexto = usuario.getRol();
                }
                tvDrawerUserRole.setText(rolTexto);
                boolean esSocio = Usuario.ROL_SOCIO.equals(usuario.getRol());
                drawerItemEquipo.setVisibility(esSocio ? View.VISIBLE : View.GONE);
            } else {
                tvDrawerUserName.setText("");
                tvDrawerUserRole.setText("");
                drawerItemEquipo.setVisibility(View.GONE);
            }
        });

        drawerItemEquipo.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            if (navController != null) {
                int currentId = navController.getCurrentDestination() != null
                        ? navController.getCurrentDestination().getId() : 0;
                if (currentId != R.id.usuariosFragment) {
                    NavOptions navOptions = new NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build();
                    navController.navigate(R.id.usuariosFragment, null, navOptions);
                }
            }
        });

        // Configurar estado actual de tema en el drawer
        actualizarVistaTema();

        drawerItemTheme.setOnClickListener(v -> {
            boolean darkModeActual = ThemeHelper.isDarkMode(this);
            ThemeHelper.setDarkMode(this, !darkModeActual);
        });

        drawerItemLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_cerrar_sesion_titulo)
                    .setMessage(R.string.dialog_cerrar_sesion_mensaje)
                    .setNegativeButton(R.string.dialog_cancelar, null)
                    .setPositiveButton(R.string.dialog_confirmar, (dialog, which) -> {
                        drawerLayout.closeDrawer(GravityCompat.START);
                        sessionManager.cerrarSesion();
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build();
                        navController.navigate(R.id.loginFragment, null, navOptions);
                    })
                    .show();
        });
    }

    private void actualizarVistaTema() {
        boolean darkMode = ThemeHelper.isDarkMode(this);
        if (darkMode) {
            ivDrawerThemeIcon.setImageResource(R.drawable.ic_light_mode_24);
            tvDrawerThemeLabel.setText(R.string.menu_modo_claro);
        } else {
            ivDrawerThemeIcon.setImageResource(R.drawable.ic_dark_mode_24);
            tvDrawerThemeLabel.setText(R.string.menu_modo_oscuro);
        }
    }

    private void configurarBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                if (navController != null) {
                    int currentId = navController.getCurrentDestination() != null
                            ? navController.getCurrentDestination().getId() : 0;
                    if (currentId == R.id.nav_dashboard ||
                        currentId == R.id.clienteHomeFragment ||
                        currentId == R.id.asistenteHomeFragment) {
                        moveTaskToBack(true);
                        return;
                    }
                    if (currentId == R.id.nav_inventario ||
                        currentId == R.id.nav_agenda ||
                        currentId == R.id.nav_pedidos ||
                        currentId == R.id.nav_finanzas) {
                        navController.popBackStack(R.id.nav_dashboard, false);
                        return;
                    }
                    if (navController.popBackStack()) {
                        return;
                    }
                }
                moveTaskToBack(true);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}