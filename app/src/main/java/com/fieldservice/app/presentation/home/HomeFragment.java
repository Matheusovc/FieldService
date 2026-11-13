package com.fieldservice.app.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fieldservice.app.R;
import com.fieldservice.app.data.AppContainer;
import com.fieldservice.app.databinding.FragmentHomeBinding;
import com.fieldservice.app.presentation.ticketdetails.TicketDetailsActivity;
import com.fieldservice.app.ui.components.TicketAdapter;

/** Aba inicial: resumo do dia e chamados prioritários do técnico. */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TicketAdapter priorityTicketsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // No tema Claro, oculta a textura de fundo (usa fundo claro sólido).
        if (com.fieldservice.app.data.SettingsPrefs.isLight(requireContext())) {
            binding.imageHomeBg.setVisibility(View.GONE);
        }

        priorityTicketsAdapter = new TicketAdapter(ticket ->
                startActivity(TicketDetailsActivity.newIntent(requireContext(), ticket.getId()))
        );
        binding.recyclerPriorityTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPriorityTickets.setAdapter(priorityTicketsAdapter);

        HomeViewModel viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HomeViewModel(
                        AppContainer.getTicketRepository(),
                        AppContainer.getAuthRepository()
                );
            }
        }).get(HomeViewModel.class);

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    /** Saudação conforme o horário do dispositivo (Bom dia / Boa tarde / Boa noite). */
    private String timeGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return getString(R.string.greeting_morning);
        } else if (hour >= 12 && hour < 18) {
            return getString(R.string.greeting_afternoon);
        }
        return getString(R.string.greeting_evening);
    }

    private void render(HomeUiState state) {
        binding.textGreeting.setText(
                getString(R.string.home_greeting, timeGreeting(), state.getTechnicianFirstName()));
        binding.textPendingCount.setText(String.valueOf(state.getPendingCount()));
        binding.textInProgressCount.setText(String.valueOf(state.getInProgressCount()));
        binding.textCompletedCount.setText(String.valueOf(state.getCompletedCount()));

        boolean hasPriorityTickets = !state.getPriorityTickets().isEmpty();
        binding.recyclerPriorityTickets.setVisibility(hasPriorityTickets ? View.VISIBLE : View.GONE);
        binding.groupEmptyPriority.setVisibility(hasPriorityTickets ? View.GONE : View.VISIBLE);
        priorityTicketsAdapter.submitList(state.getPriorityTickets());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
