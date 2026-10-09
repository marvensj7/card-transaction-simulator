package com.marvens.capstone.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.marvens.capstone.dto.PageResponse;
import com.marvens.capstone.dto.AccountResponse;
import com.marvens.capstone.dto.CardResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import com.marvens.capstone.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AppUserRepository userRepository;
    private final CreditAccountRepository accountRepository;
    private final DemoCardRepository cardRepository;

    public AccountService(AppUserRepository userRepository, CreditAccountRepository accountRepository, DemoCardRepository cardRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    public List<AccountResponse> getAccounts(Long userId) {
        requireRole(userId, AppUser.Role.USER);
        List<AccountResponse> accountResponses = new ArrayList<>();
        CreditAccount customerAccount = accountRepository.findByUser_Id(userId);
        if (customerAccount != null) {
            accountResponses.add(new AccountResponse(customerAccount));
        }
        return accountResponses;
    }

    public CreditAccount getOwnedAccount(Long userId, Long accountId) {
        requireRole(userId, AppUser.Role.USER);
        CreditAccount customerAccount = accountRepository.findByIdAndUser_Id(accountId, userId);
        if (customerAccount == null) {
            throw new ResourceNotFoundException("Account is unavailable.");
        }
        return customerAccount;
    }

    public List<CardResponse> getCards(Long userId, Long accountId) {
        getOwnedAccount(userId, accountId);
        List<CardResponse> cardResponses = new ArrayList<>();
        DemoCard assignedCard = cardRepository.findByAccount_Id(accountId);
        if (assignedCard != null) {
            cardResponses.add(new CardResponse(assignedCard));
        }
        return cardResponses;
    }

    public PageResponse<AccountResponse> getAdminAccounts(Long adminId, int page, int size) {
        requireRole(adminId, AppUser.Role.ADMIN);
        List<AccountResponse> accountResponses = new ArrayList<>();
        Page<CreditAccount> customerAccounts = accountRepository.findAllByOrderByIdAsc(PageRequest.of(page, size));
        for (CreditAccount customerAccount : customerAccounts) {
            accountResponses.add(new AccountResponse(customerAccount));
        }
        return new PageResponse<>(accountResponses, customerAccounts);
    }

    @Transactional
    public AccountResponse changeStatus(Long adminId, Long accountId, CreditAccount.Status status) {
        requireRole(adminId, AppUser.Role.ADMIN);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or FROZEN.");
        }
        CreditAccount customerAccount = accountRepository.findLockedById(accountId);
        if (customerAccount == null) {
            throw new ResourceNotFoundException("Account is unavailable.");
        }
        customerAccount.setStatus(status);
        accountRepository.save(customerAccount);
        return new AccountResponse(customerAccount);
    }

    void requireRole(Long userId, AppUser.Role requiredRole) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user role cannot perform this operation.");
        }
    }
}
