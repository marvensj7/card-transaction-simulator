package com.marvens.capstone.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import com.marvens.capstone.dto.LoginRequest;
import com.marvens.capstone.dto.LoginResponse;
import com.marvens.capstone.dto.RegisterRequest;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.ConflictException;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import com.marvens.capstone.security.JwtTokens;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final AppUserRepository userRepository;
    private final CreditAccountRepository accountRepository;
    private final DemoCardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokens jwtTokenService;
    private final String dummyPasswordHash;

    public AuthService(AppUserRepository userRepository, CreditAccountRepository accountRepository,
            DemoCardRepository cardRepository, PasswordEncoder passwordEncoder, JwtTokens jwtTokenService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        validatePasswordByteLength(request.password);
        String normalizedEmail = request.email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Registration could not be completed with this email.");
        }

        AppUser user = new AppUser();
        user.setDisplayName(request.displayName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password));
        user.setRole(AppUser.Role.USER);
        try {
            // Flush here so a concurrent duplicate email returns the same safe conflict.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            throw new ConflictException("Registration could not be completed with this email.");
        }

        CreditAccount customerAccount = new CreditAccount();
        customerAccount.setUser(user);
        customerAccount.setCreditLimit(new BigDecimal("1000.00"));
        customerAccount.setOutstandingBalance(new BigDecimal("0.00"));
        customerAccount.setStatus(CreditAccount.Status.ACTIVE);
        accountRepository.save(customerAccount);

        YearMonth cardExpiry = YearMonth.now(ZoneOffset.UTC).plusYears(5);
        DemoCard assignedCard = new DemoCard();
        assignedCard.setAccount(customerAccount);
        assignedCard.setLabel("Credit Circuit Demo");
        FictionalCardNumbers.assignTo(assignedCard);
        assignedCard.setExpiryMonth((byte) cardExpiry.getMonthValue());
        assignedCard.setExpiryYear((short) cardExpiry.getYear());
        cardRepository.save(assignedCard);

        return user;
    }

    public LoginResponse login(LoginRequest request) {
        validatePasswordByteLength(request.password);
        String normalizedEmail = request.email.trim().toLowerCase(Locale.ROOT);
        AppUser user = userRepository.findByEmail(normalizedEmail);

        // Run BCrypt even for an unknown email to reduce differences in response time.
        String passwordHash = dummyPasswordHash;
        if (user != null) {
            passwordHash = user.getPasswordHash();
        }
        boolean passwordMatches = passwordEncoder.matches(request.password, passwordHash);
        if (user == null || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }

        return new LoginResponse(user, jwtTokenService.issueAccessToken(user));
    }

    public AppUser currentUser(Long userId) {
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }

        return user;
    }

    private void validatePasswordByteLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must fit within 72 UTF-8 bytes.");
        }
    }
}
