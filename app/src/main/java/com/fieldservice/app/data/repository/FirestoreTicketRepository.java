package com.fieldservice.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fieldservice.app.data.mock.MockTicketData;
import com.fieldservice.app.domain.model.Priority;
import com.fieldservice.app.domain.model.Ticket;
import com.fieldservice.app.domain.model.TicketStatus;
import com.fieldservice.app.domain.repository.TicketRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação de {@link TicketRepository} baseada no Cloud Firestore.
 * Os chamados ficam na coleção "tickets"; uma escuta em tempo real mantém o
 * {@link LiveData} sincronizado com o servidor (e com o cache offline do Firestore).
 * Na primeira execução, se a coleção estiver vazia, os chamados de exemplo são
 * semeados a partir de {@link MockTicketData}, preservando a mesma demonstração do app.
 */
public class FirestoreTicketRepository implements TicketRepository {

    private static final String TAG = "FirestoreTicketRepo";
    private static final String COLLECTION = "tickets";

    private final CollectionReference ticketsRef;
    private final MutableLiveData<List<Ticket>> tickets = new MutableLiveData<>(new ArrayList<>());
    private boolean seedChecked = false;

    public FirestoreTicketRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.ticketsRef = db.collection(COLLECTION);

        ticketsRef.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erro ao observar chamados", error);
                        return;
                    }
                    if (snapshots == null) {
                        return;
                    }

                    // Semeia os dados de exemplo apenas quando o servidor confirma coleção vazia.
                    if (snapshots.isEmpty() && !snapshots.getMetadata().isFromCache() && !seedChecked) {
                        seedChecked = true;
                        seedInitialTickets();
                        return;
                    }

                    List<Ticket> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Ticket ticket = toTicket(doc);
                        if (ticket != null) {
                            result.add(ticket);
                        }
                    }
                    tickets.postValue(result);
                });
    }

    @Override
    public LiveData<List<Ticket>> observeTickets() {
        return tickets;
    }

    @Override
    public void getTicketById(String id, TicketCallback callback) {
        // Primeiro tenta na lista já carregada (resposta imediata).
        List<Ticket> current = tickets.getValue();
        if (current != null) {
            for (Ticket ticket : current) {
                if (ticket.getId().equals(id)) {
                    callback.onResult(ticket);
                    return;
                }
            }
        }
        // Caso ainda não esteja em memória, busca o documento diretamente.
        ticketsRef.document(id).get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists() ? toTicket(doc) : null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar chamado " + id, e);
                    callback.onResult(null);
                });
    }

    @Override
    public void acceptTicket(String id, OperationCallback callback) {
        updateTicketStatus(id, TicketStatus.ACCEPTED, callback);
    }

    @Override
    public void updateTicketStatus(String id, TicketStatus status, OperationCallback callback) {
        ticketsRef.document(id)
                .update("status", status.name())
                .addOnSuccessListener(unused -> callback.onComplete())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar status do chamado " + id, e);
                    callback.onComplete();
                });
    }

    private void seedInitialTickets() {
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (Ticket ticket : MockTicketData.initialTickets()) {
            batch.set(ticketsRef.document(ticket.getId()), toMap(ticket));
        }
        batch.commit()
                .addOnSuccessListener(unused -> Log.i(TAG, "Chamados de exemplo semeados no Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Falha ao semear chamados", e));
    }

    private static Map<String, Object> toMap(Ticket ticket) {
        Map<String, Object> map = new HashMap<>();
        map.put("number", ticket.getNumber());
        map.put("title", ticket.getTitle());
        map.put("description", ticket.getDescription());
        map.put("customerName", ticket.getCustomerName());
        map.put("address", ticket.getAddress());
        map.put("priority", ticket.getPriority().name());
        map.put("status", ticket.getStatus().name());
        map.put("createdAt", ticket.getCreatedAt());
        return map;
    }

    private static Ticket toTicket(@NonNull DocumentSnapshot doc) {
        try {
            Date createdAt = doc.getDate("createdAt");
            return new Ticket(
                    doc.getId(),
                    orEmpty(doc.getString("number")),
                    orEmpty(doc.getString("title")),
                    orEmpty(doc.getString("description")),
                    orEmpty(doc.getString("customerName")),
                    orEmpty(doc.getString("address")),
                    parsePriority(doc.getString("priority")),
                    parseStatus(doc.getString("status")),
                    createdAt != null ? createdAt : new Date()
            );
        } catch (Exception e) {
            Log.e(TAG, "Documento de chamado inválido: " + doc.getId(), e);
            return null;
        }
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    private static Priority parsePriority(String value) {
        try {
            return value != null ? Priority.valueOf(value) : Priority.MEDIUM;
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }

    private static TicketStatus parseStatus(String value) {
        try {
            return value != null ? TicketStatus.valueOf(value) : TicketStatus.OPEN;
        } catch (IllegalArgumentException e) {
            return TicketStatus.OPEN;
        }
    }
}
