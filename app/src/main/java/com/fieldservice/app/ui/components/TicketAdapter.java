package com.fieldservice.app.ui.components;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fieldservice.app.databinding.ItemTicketCardBinding;
import com.fieldservice.app.domain.model.Ticket;
import com.fieldservice.app.utils.DateFormatter;

import java.util.ArrayList;
import java.util.List;

/** Adapter padrão usado nas listas de chamados (Home e Chamados). */
public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private final List<Ticket> tickets = new ArrayList<>();
    private final OnTicketClickListener listener;

    public TicketAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Ticket> newTickets) {
        tickets.clear();
        tickets.addAll(newTickets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTicketCardBinding binding = ItemTicketCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TicketViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(tickets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static final class TicketViewHolder extends RecyclerView.ViewHolder {

        private final ItemTicketCardBinding binding;

        TicketViewHolder(ItemTicketCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Ticket ticket, OnTicketClickListener listener) {
            binding.textNumber.setText(ticket.getNumber());
            binding.textCustomer.setText(ticket.getCustomerName());
            binding.textDate.setText(DateFormatter.format(ticket.getCreatedAt()));
            binding.textTitle.setText(ticket.getTitle());
            binding.textAddress.setText(ticket.getAddress());
            binding.viewPriorityAccent.setBackgroundColor(
                    PriorityBadge.colorInt(binding.getRoot().getContext(), ticket.getPriority()));
            PriorityBadge.bind(binding.textPriorityBadge, ticket.getPriority());
            StatusBadge.bind(binding.textStatusBadge, ticket.getStatus());
            binding.getRoot().setOnClickListener(v -> listener.onTicketClick(ticket));
        }
    }
}
