package com.marvens.capstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marvens.capstone.controller.AuthController;
import com.marvens.capstone.entity.*;
import com.marvens.capstone.repository.*;
import com.marvens.capstone.security.SecurityConfiguration;
import com.marvens.capstone.service.AuthService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({AuthService.class, SecurityConfiguration.class, com.marvens.capstone.security.JwtTokens.class})
class AuthApiTest extends SecurityTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @MockitoBean AppUserRepository users;
    @MockitoBean CreditAccountRepository accounts;
    @MockitoBean DemoCardRepository cards;
    private String password;

    @BeforeEach
    void setup() {
        password = java.util.UUID.randomUUID().toString();
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            AppUser user = call.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 10L);
            return user;
        });
    }

    ObjectNode registration() {
        return json.createObjectNode().put("displayName", " New Customer ")
                .put("email", "CUSTOMER@example.test").put("password", password);
    }

    @Test
    void loginVerifiesBcryptAndCurrentUserRequiresTheSignedToken() throws Exception {
        AppUser user = TestData.user(AppUser.Role.USER);
        user.setPasswordHash(passwords.encode(password));
        ReflectionTestUtils.setField(user, "id", 10L);
        when(users.findByEmail("customer@example.test")).thenReturn(user);
        when(users.findById(10L)).thenReturn(java.util.Optional.of(user));
        ObjectNode input = json.createObjectNode().put("email", "CUSTOMER@example.test").put("password", password);
        String result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(input.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(result).path("accessToken").asText();
        assertThat(result).doesNotContain(password, "passwordHash");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
        when(users.findById(10L)).thenReturn(java.util.Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectAndUnknownCredentialsHaveTheSameMessage() throws Exception {
        AppUser user = TestData.user(AppUser.Role.USER);
        user.setPasswordHash(passwords.encode(java.util.UUID.randomUUID().toString()));
        when(users.findByEmail("customer@example.test")).thenReturn(user);
        String first = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().put("email", "customer@example.test").put("password", password).toString()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().put("email", "missing@example.test").put("password", password).toString()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(first).isEqualTo(second).contains("Email or password is incorrect.");
    }

    @Test
    void registrationCreatesOnlyACustomerWithAnAccountAndMaskedCard() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registration().toString())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").value("customer@example.test"))
                .andReturn().getResponse().getContentAsString();
        ArgumentCaptor<AppUser> user = ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(user.capture());
        assertThat(passwords.matches(password, user.getValue().getPasswordHash())).isTrue();
        assertThat(user.getValue().getDisplayName()).isEqualTo("New Customer");
        ArgumentCaptor<CreditAccount> account = ArgumentCaptor.forClass(CreditAccount.class);
        verify(accounts).save(account.capture());
        assertThat(account.getValue().getOutstandingBalance()).isZero();
        assertThat(account.getValue().getCreditLimit()).isEqualByComparingTo("1000");
        verify(cards).save(any(DemoCard.class));
        assertThat(body).doesNotContain(password, "passwordHash", "accessToken", TestData.testNumber());
    }

    @Test
    void duplicateEmailAndConcurrentDuplicatesUseTheSameSafeConflict() throws Exception {
        when(users.existsByEmail("customer@example.test")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration().toString()))
                .andExpect(status().isConflict());
        when(users.existsByEmail("customer@example.test")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("private-db-marker")).when(users).saveAndFlush(any());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration().toString()))
                .andExpect(status().isConflict()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-db-marker"))));
        verifyNoInteractions(accounts, cards);
    }

    @Test
    void registrationRejectsInvalidFieldsAdminChoiceAndOversizedUtf8Passwords() throws Exception {
        for (String field : new String[] {"email", "displayName", "password"}) {
            ObjectNode request = registration();
            request.put(field, " ");
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                    .andExpect(status().isBadRequest());
        }
        ObjectNode request = registration().put("role", "ADMIN");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
        request = registration().put("password", "\u00e9".repeat(40));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users, accounts, cards);
    }
}
