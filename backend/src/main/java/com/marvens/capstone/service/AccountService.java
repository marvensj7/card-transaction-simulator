package com.marvens.capstone.service;

import java.util.List;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.AccessDeniedException;
import com.marvens.capstone.exception.InvalidRequestException;
import com.marvens.capstone.exception.ResourceNotFoundException;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AppUserRepository users;
    private final CreditAccountRepository accounts;
    private final DemoCardRepository cards;

    public AccountService(AppUserRepository users, CreditAccountRepository accounts, DemoCardRepository cards) {
        this.users = users;
        this.accounts = accounts;
        this.cards = cards;
    }

    public List<CreditAccount> getAccounts(Long userId) {
        requireCustomer(userId);
        return accounts.findByUser_Id(userId).map(List::of).orElseGet(List::of);
    }

    public CreditAccount getAccount(Long userId, Long accountId) {
        requireCustomer(userId);
        return accounts.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account is unavailable."));
    }

    public List<DemoCard> getCards(Long userId, Long accountId) {
        getAccount(userId, accountId);
        return cards.findByAccount_Id(accountId).map(List::of).orElseGet(List::of);
    }

    public Page<CreditAccount> getAdminAccounts(Long adminId, Integer page, Integer size) {
        requireAdmin(adminId);
        return accounts.findAllByOrderByIdAsc(RequestChecks.page(page, size));
    }

    @Transactional
    public CreditAccount changeStatus(Long adminId, Long accountId, CreditAccount.Status status) {
        requireAdmin(adminId);
        if (status == null) {
            throw new InvalidRequestException("Account status must be ACTIVE or FROZEN.");
        }
        CreditAccount account = accounts.findForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account is unavailable."));
        account.setStatus(status);
        return accounts.saveAndFlush(account);
    }

    // IDs here come from a trusted caller; JWT verification belongs to the security section.
    void requireCustomer(Long userId) { requireRole(userId, AppUser.Role.USER); }
    void requireAdmin(Long userId) { requireRole(userId, AppUser.Role.ADMIN); }

    private void requireRole(Long userId, AppUser.Role required) {
        if (userId == null || users.findRoleById(userId).orElse(null) != required) {
            throw new AccessDeniedException();
        }
    }
}
