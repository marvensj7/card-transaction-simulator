package com.marvens.capstone.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import com.marvens.capstone.dto.*;
import com.marvens.capstone.entity.*;
import com.marvens.capstone.exception.ConflictException;
import com.marvens.capstone.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final AppUserRepository users;
    private final CreditAccountRepository accounts;
    private final DemoCardRepository cards;
    private final PasswordEncoder passwords;

    public AuthService(AppUserRepository users, CreditAccountRepository accounts,
            DemoCardRepository cards, PasswordEncoder passwords) {
        this.users = users;
        this.accounts = accounts;
        this.cards = cards;
        this.passwords = passwords;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        checkPasswordLength(request.password);
        String email = request.email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ConflictException("Registration could not be completed with this email.");
        AppUser user = new AppUser();
        user.setDisplayName(request.displayName.trim());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password));
        user.setRole(AppUser.Role.USER);
        try {
            // Flush here so a concurrent duplicate email returns the same safe conflict.
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            throw new ConflictException("Registration could not be completed with this email.");
        }
        CreditAccount account = new CreditAccount();
        account.setUser(user);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setOutstandingBalance(new BigDecimal("0.00"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        accounts.save(account);
        YearMonth expiry = YearMonth.now(ZoneOffset.UTC).plusYears(5);
        DemoCard card = new DemoCard();
        card.setAccount(account);
        card.setLabel("Credit Circuit Demo");
        card.setTestProfile("DEMO_4242");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) expiry.getMonthValue());
        card.setExpiryYear((short) expiry.getYear());
        cards.save(card);
        return new UserResponse(user);
    }

    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must fit within 72 UTF-8 bytes.");
        }
    }
}
