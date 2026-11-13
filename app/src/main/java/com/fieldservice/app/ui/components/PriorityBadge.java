package com.fieldservice.app.ui.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.fieldservice.app.R;
import com.fieldservice.app.domain.model.Priority;

/** Selo de prioridade do chamado. Não depende só da cor: o texto identifica o nível. */
public final class PriorityBadge {

    private static final int BACKGROUND_ALPHA = (int) (0.16f * 255);

    private PriorityBadge() {
    }

    public static String label(Priority priority) {
        switch (priority) {
            case LOW:
                return "Baixa";
            case MEDIUM:
                return "Média";
            case HIGH:
                return "Alta";
            case CRITICAL:
                return "Crítica";
            default:
                return "";
        }
    }

    private static int colorRes(Priority priority) {
        switch (priority) {
            case LOW:
                return R.color.priority_low;
            case MEDIUM:
                return R.color.priority_medium;
            case HIGH:
                return R.color.priority_high;
            case CRITICAL:
                return R.color.priority_critical;
            default:
                return R.color.color_on_surface;
        }
    }

    /** Cor resolvida da prioridade — usada também na faixa de destaque do card. */
    public static int colorInt(Context context, Priority priority) {
        return ContextCompat.getColor(context, colorRes(priority));
    }

    public static void bind(TextView view, Priority priority) {
        Context context = view.getContext();
        int color = ContextCompat.getColor(context, colorRes(priority));

        view.setText(label(priority));
        view.setTextColor(color);

        Drawable background = ContextCompat.getDrawable(context, R.drawable.bg_badge);
        if (background != null) {
            background = background.mutate();
            background.setTint(ColorUtils.setAlphaComponent(color, BACKGROUND_ALPHA));
            view.setBackground(background);
        }

        // Ponto colorido à esquerda: reforça a distinção sem depender apenas da cor do texto.
        Drawable dot = ContextCompat.getDrawable(context, R.drawable.ic_dot);
        if (dot != null) {
            dot = dot.mutate();
            dot.setTint(color);
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        }
    }
}
