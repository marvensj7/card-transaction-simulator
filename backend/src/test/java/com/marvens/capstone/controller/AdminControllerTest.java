package com.marvens.capstone.controller;

import java.util.List;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.exception.AccessDeniedException;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountService accounts;
    @MockitoBean private TransactionService transactions;

    @Test
    void adminAccountPageUsesDefaultsAndIncludesOnlyOwnerSummaryFields() throws Exception {
        when(accounts.getAdminAccounts(13L, 0, 20))
                .thenReturn(new PageImpl<>(List.of(ControllerFixtures.account()), PageRequest.of(0, 20), 21));
        var response = mvc.perform(get("/api/admin/accounts").principal(new AuthenticatedUser(13L))
                        .header("X-User-Id", "1").param("userId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalItems").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].ownerId").value(9))
                .andExpect(jsonPath("$.items[0].ownerDisplayName").value("Demo Owner"))
                .andExpect(jsonPath("$.items[0].ownerEmail").value("owner@example.test"))
                .andExpect(jsonPath("$.items[0].outstandingBalance").value("25.00"))
                .andExpect(jsonPath("$.items[0].user").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("passwordHash", "fictional-secret-hash-marker", "accessToken");
        verify(accounts).getAdminAccounts(13L, 0, 20);
    }

    @Test
    void bothAdminListsCapSizeAndUseTheRequestedPage() throws Exception {
        when(accounts.getAdminAccounts(13L, 2, 50)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));
        when(transactions.getAdminTransactions(13L, 2, 50))
                .thenReturn(new PageImpl<>(List.of(ControllerFixtures.purchase(ControllerFixtures.account())), PageRequest.of(2, 50), 101));
        mvc.perform(get("/api/admin/accounts").principal(new AuthenticatedUser(13L)).param("page", "2").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/admin/transactions").principal(new AuthenticatedUser(13L)).param("page", "2").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].ownerEmail").value("owner@example.test"))
                .andExpect(jsonPath("$.items[0].amount").value("25.00"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-10-06T15:30:00.123456Z"))
                .andExpect(jsonPath("$.totalItems").value(101)).andExpect(jsonPath("$.totalPages").value(3));
        verify(accounts).getAdminAccounts(13L, 2, 50);
        verify(transactions).getAdminTransactions(13L, 2, 50);
    }

    @Test
    void adminTransactionListAlsoUsesTheDefaultPage() throws Exception {
        when(transactions.getAdminTransactions(13L, 0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mvc.perform(get("/api/admin/transactions").principal(new AuthenticatedUser(13L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalPages").value(0));
        verify(transactions).getAdminTransactions(13L, 0, 20);
    }

    @Test
    void freezeAndReactivateReturnTheUpdatedAdminAccount() throws Exception {
        for (var state : CreditAccount.Status.values()) {
            var account = ControllerFixtures.account();
            account.setStatus(state);
            when(accounts.changeStatus(13L, 7L, state)).thenReturn(account);
            mvc.perform(patch("/api/admin/accounts/7/status").principal(new AuthenticatedUser(13L))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + state + "\",\"userId\":1}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state.name()))
                    .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.ownerId").value(9));
            verify(accounts).changeStatus(13L, 7L, state);
        }
    }

    @Test
    void invalidStatusPathsAndPageArgumentsNeverReachTheServices() throws Exception {
        for (String body : new String[] {"{}", "{\"status\":null}", "{\"status\":\"CLOSED\"}", "{\"status\":0}", "{"}) {
            mvc.perform(patch("/api/admin/accounts/7/status").principal(new AuthenticatedUser(13L))
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/admin/accounts/0/status").principal(new AuthenticatedUser(13L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        for (String route : new String[] {"accounts", "transactions"}) {
            for (String query : new String[] {"page=-1", "size=0", "size=-1", "size=bad"}) {
                mvc.perform(get("/api/admin/" + route + "?" + query).principal(new AuthenticatedUser(13L)))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(accounts, transactions);
    }

    @Test
    void roleHeadersCannotBypassTheStoredRoleCheck() throws Exception {
        when(accounts.getAdminAccounts(9L, 0, 20)).thenThrow(new AccessDeniedException());
        mvc.perform(get("/api/admin/accounts").principal(new AuthenticatedUser(9L))
                .header("X-Role", "ADMIN").header("X-User-Id", "13"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verify(accounts).getAdminAccounts(9L, 0, 20);
    }

    @Test
    void unauthenticatedAdminCallsCannotReadOrChangeAnything() throws Exception {
        mvc.perform(get("/api/admin/accounts").header("X-Role", "ADMIN")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/transactions").param("userId", "13")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/accounts/7/status").header("X-User-Id", "13")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FROZEN\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts, transactions);
    }
}
