package com.fieldservice.app.data;

import com.fieldservice.app.data.repository.FirestoreTicketRepository;
import com.fieldservice.app.data.repository.MockAuthRepository;
import com.fieldservice.app.domain.repository.AuthRepository;
import com.fieldservice.app.domain.repository.TicketRepository;

/**
 * Provedor manual das dependências do app (sem Hilt, para manter a primeira versão simples).
 * As telas dependem apenas das interfaces {@link AuthRepository}/{@link TicketRepository}; trocar
 * a implementação mock por uma baseada em API significa mudar apenas esta classe.
 */
public final class AppContainer {

    private static final AuthRepository authRepository = new MockAuthRepository();
    // Chamados agora vêm do Cloud Firestore. Para reverter ao mock em memória,
    // basta trocar por: new MockTicketRepository().
    private static final TicketRepository ticketRepository = new FirestoreTicketRepository();

    private AppContainer() {
    }

    public static AuthRepository getAuthRepository() {
        return authRepository;
    }

    public static TicketRepository getTicketRepository() {
        return ticketRepository;
    }
}
