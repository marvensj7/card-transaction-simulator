package com.marvens.capstone.service;

import java.util.ArrayList;
import java.util.List;
import com.marvens.capstone.dto.AccountResponse;
import com.marvens.capstone.dto.CardResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public List<AccountResponse> getAccounts(Long userId) {
        requireRole(userId, AppUser.Role.USER);
        List<AccountResponse> result = new ArrayList<>();
        CreditAccount account = accounts.findByUser_Id(userId);
        if (account != null) {
            result.add(new AccountResponse(account));
        }
        return result;
    }

    public CreditAccount getOwnedAccount(Long userId, Long accountId) {
        requireRole(userId, AppUser.Role.USER);
        CreditAccount account = accounts.findByIdAndUser_Id(accountId, userId);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account is unavailable.");
        }
        return account;
    }

    public List<CardResponse> getCards(Long userId, Long accountId) {
        getOwnedAccount(userId, accountId);
        List<CardResponse> result = new ArrayList<>();
        DemoCard card = cards.findByAccount_Id(accountId);
        if (card != null) {
            result.add(new CardResponse(card));
        }
        return result;
    }

    public List<AccountResponse> getAdminAccounts(Long adminId) {
        requireRole(adminId, AppUser.Role.ADMIN);
        List<AccountResponse> result = new ArrayList<>();
        for (CreditAccount account : accounts.findAllByOrderByIdAsc()) {
            result.add(new AccountResponse(account));
        }
        return result;
    }

    @Transactional
    public AccountResponse changeStatus(Long adminId, Long accountId, CreditAccount.Status status) {
        requireRole(adminId, AppUser.Role.ADMIN);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or FROZEN.");
        }
        CreditAccount account = accounts.findLockedById(accountId);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account is unavailable.");
        }
        account.setStatus(status);
        accounts.save(account);
        return new AccountResponse(account);
    }

    void requireRole(Long userId, AppUser.Role requiredRole) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
        AppUser user = users.findById(userId).orElse(null);
        if (user == null || user.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user role cannot perform this operation.");
        }
    }
}
