package com.marvens.capstone.controller;

import com.marvens.capstone.security.AuthenticatedUser;
import java.util.List;
import com.marvens.capstone.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountService accounts;

    @Test
    void accountResponseAndServiceIdentityComeFromTheTrustedPrincipal() throws Exception {
        when(accounts.getAccounts(9L)).thenReturn(List.of(ControllerFixtures.account()));
        mvc.perform(get("/api/accounts").principal(new AuthenticatedUser(9L))
                        .header("X-User-Id", "1").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].creditLimit").value("1000.00"))
                .andExpect(jsonPath("$[0].availableCredit").value("975.00"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        verify(accounts).getAccounts(9L);
        verifyNoMoreInteractions(accounts);
    }

    @Test
    void cardsAreMaskedAndOwnershipIdsArePassedSeparately() throws Exception {
        when(accounts.getCards(9L, 7L)).thenReturn(List.of(ControllerFixtures.card(ControllerFixtures.account())));
        mvc.perform(get("/api/accounts/7/cards").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maskedNumber").value("\u2022\u2022\u2022\u2022 4242"))
                .andExpect(jsonPath("$[0].expiryMonth").value(12))
                .andExpect(jsonPath("$[0].expiryYear").value(2030))
                .andExpect(jsonPath("$[0].testProfile").value("DEMO_4242"))
                .andExpect(jsonPath("$[0].lastFour").doesNotExist())
                .andExpect(jsonPath("$[0].account").doesNotExist());
        verify(accounts).getCards(9L, 7L);
    }

    @Test
    void missingIdentityIsRejectedBeforeServiceAccess() throws Exception {
        mvc.perform(get("/api/accounts").header("X-User-Id", "9"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts/7/cards").param("userId", "9"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts);
    }

    @Test
    void invalidAccountPathIsRejectedBeforeTheService() throws Exception {
        mvc.perform(get("/api/accounts/0/cards").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/accounts/not-an-id/cards").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }
}
