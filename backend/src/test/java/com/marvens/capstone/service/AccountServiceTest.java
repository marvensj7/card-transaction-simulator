package com.marvens.capstone.service;

import java.util.Optional;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.AccessDeniedException;
import com.marvens.capstone.exception.InvalidRequestException;
import com.marvens.capstone.exception.ResourceNotFoundException;
import com.marvens.capstone.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final CreditAccountRepository accounts = mock(CreditAccountRepository.class);
    private final DemoCardRepository cards = mock(DemoCardRepository.class);
    private final AccountService service = new AccountService(users, accounts, cards);

    @Test
    void accountAndCardLookupsCheckOwnershipAndAllowMissingOptionalRows() {
        when(users.findRoleById(1L)).thenReturn(Optional.of(AppUser.Role.USER));
        assertThat(service.getAccounts(1L)).isEmpty();
        CreditAccount account = new CreditAccount();
        when(accounts.findByUser_Id(1L)).thenReturn(Optional.of(account));
        when(accounts.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(account));
        assertThat(service.getAccounts(1L)).containsExactly(account);
        assertThat(service.getCards(1L, 7L)).isEmpty();
        DemoCard card = new DemoCard();
        when(cards.findByAccount_Id(7L)).thenReturn(Optional.of(card));
        assertThat(service.getCards(1L, 7L)).containsExactly(card);
        assertThatThrownBy(() -> service.getCards(1L, 8L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminCanFreezeAndReactivateWithAnAccountLock() {
        when(users.findRoleById(2L)).thenReturn(Optional.of(AppUser.Role.ADMIN));
        CreditAccount account = new CreditAccount();
        when(accounts.findForUpdate(7L)).thenReturn(Optional.of(account));
        when(accounts.saveAndFlush(account)).thenReturn(account);
        assertThat(service.changeStatus(2L, 7L, CreditAccount.Status.FROZEN).getStatus())
                .isEqualTo(CreditAccount.Status.FROZEN);
        assertThat(service.changeStatus(2L, 7L, CreditAccount.Status.ACTIVE).getStatus())
                .isEqualTo(CreditAccount.Status.ACTIVE);
        verify(accounts, times(2)).findForUpdate(7L);
        assertThatThrownBy(() -> service.changeStatus(2L, 7L, null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.changeStatus(2L, 8L, CreditAccount.Status.ACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rolesAreReadFromDatabaseAndNotAcceptedFromRequest() {
        when(users.findRoleById(1L)).thenReturn(Optional.of(AppUser.Role.USER));
        when(users.findRoleById(2L)).thenReturn(Optional.of(AppUser.Role.ADMIN));
        assertThatThrownBy(() -> service.getAdminAccounts(1L, null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.changeStatus(1L, 7L, CreditAccount.Status.FROZEN))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAccounts(2L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAccounts(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAccounts(99L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pagingUsesDefaultsCapsSizeAndRejectsInvalidValues() {
        when(users.findRoleById(2L)).thenReturn(Optional.of(AppUser.Role.ADMIN));
        service.getAdminAccounts(2L, null, null);
        verify(accounts).findAllByOrderByIdAsc(PageRequest.of(0, 20));
        service.getAdminAccounts(2L, 3, 100);
        verify(accounts).findAllByOrderByIdAsc(PageRequest.of(3, 50));
        assertThatThrownBy(() -> service.getAdminAccounts(2L, -1, 20)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getAdminAccounts(2L, 0, 0)).isInstanceOf(InvalidRequestException.class);
    }
}
