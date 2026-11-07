package com.fieldservice.app.presentation.main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.google.android.material.bottomnavigation.BottomNavigationMenuView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fieldservice.app.R;
import com.fieldservice.app.databinding.ActivityMainBinding;
import com.fieldservice.app.presentation.home.HomeFragment;
import com.fieldservice.app.presentation.profile.ProfileFragment;
import com.fieldservice.app.presentation.tickets.TicketsFragment;

/**
 * Tela principal após o login: hospeda as abas (Início, Chamados, Perfil) por meio de
 * uma barra de navegação inferior. Cada aba é um Fragment; as instâncias são mantidas e
 * apenas mostradas/ocultadas, preservando o estado ao alternar entre elas e ao girar a tela.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "home";
    private static final String TAG_TICKETS = "tickets";
    private static final String TAG_PROFILE = "profile";

    private ActivityMainBinding binding;

    private Fragment homeFragment;
    private Fragment ticketsFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(com.fieldservice.app.data.SettingsPrefs.themeStyleRes(this));
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            ticketsFragment = new TicketsFragment();
            profileFragment = new ProfileFragment();
            fm.beginTransaction()
                    .add(R.id.navHostContainer, profileFragment, TAG_PROFILE).hide(profileFragment)
                    .add(R.id.navHostContainer, ticketsFragment, TAG_TICKETS).hide(ticketsFragment)
                    .add(R.id.navHostContainer, homeFragment, TAG_HOME)
                    .commit();
            activeFragment = homeFragment;
        } else {
            // Reaproveita as instâncias que o FragmentManager já restaurou (ex.: após rotação).
            homeFragment = fm.findFragmentByTag(TAG_HOME);
            ticketsFragment = fm.findFragmentByTag(TAG_TICKETS);
            profileFragment = fm.findFragmentByTag(TAG_PROFILE);
            activeFragment = homeFragment;
            if (ticketsFragment != null && !ticketsFragment.isHidden()) {
                activeFragment = ticketsFragment;
            } else if (profileFragment != null && !profileFragment.isHidden()) {
                activeFragment = profileFragment;
            }
        }

        binding.bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        Fragment target;
        int id = item.getItemId();
        // Efeito "liquid": o ícone selecionado dá um pulinho elástico (além do
        // indicador de vidro que desliza entre as abas, animado pelo Material).
        animateNavIcon(id);
        if (id == R.id.navTickets) {
            target = ticketsFragment;
        } else if (id == R.id.navProfile) {
            target = profileFragment;
        } else {
            target = homeFragment;
        }
        if (target == null || target == activeFragment) {
            return true;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
        return true;
    }

    /**
     * Anima o ícone da aba selecionada com um "pulo" elástico (escala com overshoot),
     * reforçando a sensação líquida da navegação. É defensivo: se a estrutura interna
     * da BottomNavigationView mudar, apenas não anima (sem travar o app).
     */
    private void animateNavIcon(int itemId) {
        try {
            BottomNavigationMenuView menuView = (BottomNavigationMenuView) binding.bottomNav.getChildAt(0);
            if (menuView == null) {
                return;
            }
            Menu menu = binding.bottomNav.getMenu();
            int index = -1;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == itemId) {
                    index = i;
                    break;
                }
            }
            if (index < 0 || index >= menuView.getChildCount()) {
                return;
            }
            View icon = menuView.getChildAt(index)
                    .findViewById(com.google.android.material.R.id.navigation_bar_item_icon_view);
            if (icon == null) {
                return;
            }
            icon.animate().cancel();
            icon.setScaleX(0.78f);
            icon.setScaleY(0.78f);
            icon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator(3.2f))
                    .start();
        } catch (Exception ignored) {
            // Mantém a navegação funcionando mesmo que a animação falhe.
        }
    }
}
