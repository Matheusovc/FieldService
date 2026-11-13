package com.fieldservice.app.presentation.ticketdetails;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.fieldservice.app.R;
import com.fieldservice.app.data.AppContainer;
import com.fieldservice.app.databinding.ActivityTicketDetailsBinding;
import com.fieldservice.app.databinding.ItemTimelineStepBinding;
import com.fieldservice.app.domain.model.Ticket;
import com.fieldservice.app.domain.model.TicketStatus;
import com.fieldservice.app.presentation.UiState;
import com.fieldservice.app.ui.components.PriorityBadge;
import com.fieldservice.app.ui.components.StatusBadge;

/**
 * Detalhes de um chamado. Mostra a próxima ação do fluxo de atendimento de acordo com o
 * status atual (aceitar → iniciar deslocamento → registrar chegada → iniciar atendimento →
 * finalizar), resolvida por {@link TicketAction}.
 */
public class TicketDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_TICKET_ID = "extra_ticket_id";

    /** Cria o Intent para abrir os detalhes de um chamado específico. */
    public static Intent newIntent(Context context, String ticketId) {
        Intent intent = new Intent(context, TicketDetailsActivity.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        return intent;
    }

    private ActivityTicketDetailsBinding binding;
    private TicketDetailsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(com.fieldservice.app.data.SettingsPrefs.themeStyleRes(this));
        super.onCreate(savedInstanceState);
        binding = ActivityTicketDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarDetails);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbarDetails.setNavigationOnClickListener(v -> finish());

        String ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TicketDetailsViewModel(AppContainer.getTicketRepository(), ticketId);
            }
        }).get(TicketDetailsViewModel.class);

        viewModel.getUiState().observe(this, this::render);
        viewModel.getIsUpdating().observe(this, this::renderUpdating);
    }

    private void render(UiState<Ticket> state) {
        boolean success = state.getType() == UiState.Type.SUCCESS;
        binding.contentDetails.setVisibility(success ? View.VISIBLE : View.GONE);
        binding.textNotFound.setVisibility(success ? View.GONE : View.VISIBLE);
        if (!success) {
            return;
        }

        Ticket ticket = state.getData();
        // Anima suavemente as mudanças (badge de status e botão) ao avançar o atendimento.
        AutoTransition transition = new AutoTransition();
        transition.setDuration(280);
        TransitionManager.beginDelayedTransition(binding.contentDetails, transition);

        binding.textNumber.setText(ticket.getNumber());
        binding.textCustomer.setText(ticket.getCustomerName());
        binding.textTitle.setText(ticket.getTitle());
        PriorityBadge.bind(binding.textPriorityBadge, ticket.getPriority());
        StatusBadge.bind(binding.textStatusBadge, ticket.getStatus());
        binding.textClientValue.setText(ticket.getCustomerName());
        binding.textAddressValue.setText(ticket.getAddress());
        binding.textDescriptionValue.setText(ticket.getDescription());

        renderTimeline(ticket.getStatus());

        TicketAction action = TicketAction.forStatus(ticket.getStatus());
        if (action != null) {
            binding.actionBar.setVisibility(View.VISIBLE);
            binding.buttonAction.setText(action.labelRes);
            binding.buttonAction.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                viewModel.advance(action.nextStatus);
            });
        } else {
            binding.actionBar.setVisibility(View.GONE);
            binding.buttonAction.setOnClickListener(null);
        }
    }

    // Fluxo linear do atendimento representado na timeline.
    private static final TicketStatus[] FLOW = {
            TicketStatus.ASSIGNED,
            TicketStatus.ACCEPTED,
            TicketStatus.TRAVELING,
            TicketStatus.ON_SITE,
            TicketStatus.IN_PROGRESS,
            TicketStatus.COMPLETED
    };

    /** Desenha a linha do tempo do atendimento, destacando o progresso até o status atual. */
    private void renderTimeline(TicketStatus status) {
        binding.timelineContainer.removeAllViews();

        int current = indexOf(status);
        if (status == TicketStatus.WAITING_CONFIRMATION) {
            current = indexOf(TicketStatus.IN_PROGRESS);
        }

        int primary = ContextCompat.getColor(this, R.color.color_primary);
        int success = ContextCompat.getColor(this, R.color.color_success);
        int muted = resolveThemeColor(R.attr.fsOnSurfaceMuted);
        int onSurface = resolveThemeColor(R.attr.fsOnSurface);
        int activeColor = (status == TicketStatus.COMPLETED) ? success : primary;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < FLOW.length; i++) {
            ItemTimelineStepBinding row =
                    ItemTimelineStepBinding.inflate(inflater, binding.timelineContainer, false);

            row.stepLabel.setText(StatusBadge.label(FLOW[i]));

            boolean reached = i <= current;
            Drawable dot = row.dot.getDrawable().mutate();
            dot.setTint(reached ? activeColor : muted);

            row.lineTop.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            row.lineBottom.setVisibility(i == FLOW.length - 1 ? View.INVISIBLE : View.VISIBLE);
            row.lineTop.setBackgroundColor(i <= current ? activeColor : muted);
            row.lineBottom.setBackgroundColor(i < current ? activeColor : muted);

            if (i == current) {
                row.stepLabel.setTextColor(onSurface);
                row.stepLabel.setTypeface(null, Typeface.BOLD);
            } else if (i < current) {
                row.stepLabel.setTextColor(onSurface);
            } else {
                row.stepLabel.setTextColor(muted);
            }

            binding.timelineContainer.addView(row.getRoot());
        }
    }

    /** Resolve uma cor definida como atributo do tema atual (ex.: R.attr.fsOnSurface). */
    private int resolveThemeColor(int attrRes) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attrRes, tv, true);
        return tv.data;
    }

    private int indexOf(TicketStatus status) {
        for (int i = 0; i < FLOW.length; i++) {
            if (FLOW[i] == status) {
                return i;
            }
        }
        return -1;
    }

    private void renderUpdating(boolean updating) {
        binding.buttonAction.setEnabled(!updating);
        binding.progressAction.setVisibility(updating ? View.VISIBLE : View.GONE);
    }
}
