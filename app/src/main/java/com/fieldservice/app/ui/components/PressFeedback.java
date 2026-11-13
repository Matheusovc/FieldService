package com.fieldservice.app.ui.components;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Microinteração de toque: encolhe levemente a view ao pressionar e a devolve
 * ao soltar, dando sensação tátil "premium" aos cards. Retorna {@code false}
 * para não consumir o evento — o ripple e o OnClickListener continuam ativos.
 */
public final class PressFeedback {

    private PressFeedback() {
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
