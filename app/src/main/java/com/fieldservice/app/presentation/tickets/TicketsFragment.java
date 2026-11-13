package com.fieldservice.app.presentation.tickets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;

import com.fieldservice.app.R;
import com.fieldservice.app.data.AppContainer;
import com.fieldservice.app.databinding.FragmentTicketsBinding;
import com.fieldservice.app.domain.model.Ticket;
import com.fieldservice.app.presentation.UiState;
import com.fieldservice.app.presentation.ticketdetails.TicketDetailsActivity;
import com.fieldservice.app.ui.components.TicketAdapter;

import java.util.List;
import java.util.Map;

/** Aba de chamados: lista com filtros por situação. */
public class TicketsFragment extends Fragment {

    private FragmentTicketsBinding binding;
    private TicketsViewModel viewModel;
    private TicketAdapter ticketAdapter;
    private android.animation.ObjectAnimator shimmerAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TicketsViewModel(AppContainer.getTicketRepository());
            }
        }).get(TicketsViewModel.class);

        setUpFilterChips();
        setUpRecyclerView();
        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getFilterCounts().observe(getViewLifecycleOwner(), this::renderCounts);
    }

    private void setUpFilterChips() {
        for (TicketFilter filter : TicketFilter.values()) {
            Chip chip = (Chip) getLayoutInflater()
                    .inflate(R.layout.item_filter_chip, binding.chipGroupFilters, false);
            chip.setText(filter.getLabel());
            chip.setId(View.generateViewId());
            chip.setTag(filter);
            chip.setOnClickListener(v -> viewModel.onFilterSelected(filter));
            binding.chipGroupFilters.addView(chip);
        }
        Chip firstChip = (Chip) binding.chipGroupFilters.getChildAt(0);
        firstChip.setChecked(true);
    }

    /** Atualiza o rótulo de cada chip com a contagem (ex.: "Todos  7"). */
    private void renderCounts(Map<TicketFilter, Integer> counts) {
        for (int i = 0; i < binding.chipGroupFilters.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupFilters.getChildAt(i);
            TicketFilter filter = (TicketFilter) chip.getTag();
            Integer count = counts.get(filter);
            chip.setText(filter.getLabel() + "  " + (count == null ? 0 : count));
        }
    }

    private void setUpRecyclerView() {
        ticketAdapter = new TicketAdapter(ticket ->
                startActivity(TicketDetailsActivity.newIntent(requireContext(), ticket.getId()))
        );
        binding.recyclerTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTickets.setAdapter(ticketAdapter);
    }

    private void render(UiState<List<Ticket>> state) {
        UiState.Type type = state.getType();

        boolean loading = type == UiState.Type.LOADING;
        binding.groupSkeleton.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            startShimmer();
        } else {
            stopShimmer();
        }
        binding.recyclerTickets.setVisibility(type == UiState.Type.SUCCESS ? View.VISIBLE : View.GONE);
        binding.groupEmpty.setVisibility(type == UiState.Type.EMPTY ? View.VISIBLE : View.GONE);
        binding.groupError.setVisibility(type == UiState.Type.ERROR ? View.VISIBLE : View.GONE);

        if (type == UiState.Type.ERROR && state.getErrorMessage() != null) {
            binding.textErrorMessage.setText(state.getErrorMessage());
        }

        if (type == UiState.Type.SUCCESS) {
            ticketAdapter.submitList(state.getData());
            // Re-dispara a animação em cascata a cada troca de filtro.
            binding.recyclerTickets.scheduleLayoutAnimation();
        }
    }

    /** Inicia a varredura de brilho sobre o skeleton (após o layout definir a largura). */
    private void startShimmer() {
        binding.groupSkeleton.post(() -> {
            if (binding == null) {
                return;
            }
            int width = binding.groupSkeleton.getWidth();
            int shimmerWidth = binding.viewShimmer.getWidth();
            if (width == 0) {
                return;
            }
            stopShimmer();
            shimmerAnimator = android.animation.ObjectAnimator.ofFloat(
                    binding.viewShimmer, View.TRANSLATION_X, -shimmerWidth, width);
            shimmerAnimator.setDuration(1150);
            shimmerAnimator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            shimmerAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
            shimmerAnimator.start();
        });
    }

    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopShimmer();
        binding = null;
    }
}
