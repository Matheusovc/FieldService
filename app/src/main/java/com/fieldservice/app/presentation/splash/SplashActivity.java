package com.fieldservice.app.presentation.splash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.fieldservice.app.R;
import com.fieldservice.app.data.SettingsPrefs;
import com.fieldservice.app.databinding.ActivitySplashBinding;
import com.fieldservice.app.presentation.login.LoginActivity;

/**
 * Abertura animada do app. Toca sozinha ao abrir e conta, em 5 beats, o fluxo de
 * atendimento (empresa → cliente abre chamado → técnico é notificado → equipa-se →
 * vai ao atendimento), encerrando na marca e seguindo para o login.
 *
 * É puramente visual: não altera nenhuma regra ou fluxo existente. Pode ser pulada
 * a qualquer momento (botão "Pular" ou toque na tela).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long IN_MS = 380L;
    private static final long HOLD_MS = 650L;
    private static final long OUT_MS = 260L;

    private static final String[] EMOJIS = {"🏢", "📱", "🔔", "👷", "🚶"};
    private static final int[] CAPTIONS = {
            R.string.splash_beat_company,
            R.string.splash_beat_client,
            R.string.splash_beat_notify,
            R.string.splash_beat_tech,
            R.string.splash_beat_fast,
    };

    private ActivitySplashBinding binding;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SettingsPrefs.themeStyleRes(this));
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View.OnClickListener skip = v -> goToLogin();
        binding.buttonSkip.setOnClickListener(skip);
        binding.getRoot().setOnClickListener(skip);

        // Inicia a sequência automaticamente, logo após a montagem da tela.
        binding.getRoot().postDelayed(() -> playBeat(0), 220L);
    }

    private void playBeat(int i) {
        if (navigated) {
            return;
        }
        if (i >= EMOJIS.length) {
            showBrand();
            return;
        }

        binding.textEmoji.setText(EMOJIS[i]);
        binding.textCaption.setText(getString(CAPTIONS[i]));

        binding.textEmoji.setAlpha(0f);
        binding.textEmoji.setScaleX(0.6f);
        binding.textEmoji.setScaleY(0.6f);
        binding.textEmoji.setRotation(0f);
        binding.textEmoji.setTranslationX(0f);
        binding.textEmoji.setTranslationY(28f);
        binding.textCaption.setAlpha(0f);
        binding.textCaption.setTranslationY(16f);

        binding.textEmoji.animate()
                .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(IN_MS)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .start();
        binding.textCaption.animate()
                .alpha(1f).translationY(0f)
                .setDuration(IN_MS)
                .start();

        // O sino "toca" (balança) durante a pausa.
        if (i == 2) {
            binding.textEmoji.postDelayed(() -> shake(binding.textEmoji), IN_MS);
        }

        binding.getRoot().postDelayed(() -> exitBeat(i), IN_MS + HOLD_MS);
    }

    private void exitBeat(int i) {
        if (navigated) {
            return;
        }
        boolean walking = (i == EMOJIS.length - 1);
        binding.textEmoji.animate()
                .alpha(0f)
                .translationX(walking ? 280f : 0f)
                .translationY(walking ? 0f : -28f)
                .setDuration(walking ? 460L : OUT_MS)
                .start();
        binding.textCaption.animate()
                .alpha(0f)
                .setDuration(OUT_MS)
                .withEndAction(() -> playBeat(i + 1))
                .start();
    }

    private void shake(View v) {
        v.animate().rotationBy(-16f).setDuration(90L).withEndAction(() ->
                v.animate().rotationBy(32f).setDuration(120L).withEndAction(() ->
                        v.animate().rotationBy(-24f).setDuration(110L).withEndAction(() ->
                                v.animate().rotation(0f).setDuration(90L).start()
                        ).start()
                ).start()
        ).start();
    }

    private void showBrand() {
        if (navigated) {
            return;
        }
        binding.brandContainer.setAlpha(0f);
        binding.brandContainer.setScaleX(0.82f);
        binding.brandContainer.setScaleY(0.82f);
        binding.brandContainer.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(560L)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();
        binding.getRoot().postDelayed(this::goToLogin, 950L);
    }

    private void goToLogin() {
        if (navigated) {
            return;
        }
        navigated = true;
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
