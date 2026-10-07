package com.marvens.capstone.entity;

import java.math.BigDecimal;
import java.util.Arrays;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Opt in with -Pmysql-verification. Uses the existing schema, never H2 or generated DDL.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EntityMappingIT {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void userWithoutAccountAndAccountWithoutCardCanBeStored() {
        AppUser admin = EntityFixtures.user();
        admin.setRole(AppUser.Role.ADMIN);
        entityManager.persist(admin);

        AppUser customer = EntityFixtures.user();
        entityManager.persist(customer);
        CreditAccount account = EntityFixtures.account(customer);
        entityManager.persist(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(AppUser.class, admin.getId()).getRole()).isEqualTo(AppUser.Role.ADMIN);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credit_accounts WHERE user_id = ?",
                Long.class, admin.getId())).isZero();
        CreditAccount loaded = entityManager.find(CreditAccount.class, account.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demo_cards WHERE account_id = ?",
                Long.class, account.getId())).isZero();
        assertThat(loaded.getUser().getId()).isEqualTo(customer.getId());
        assertThat(loaded.getOutstandingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void purchaseAndRefundRoundTripWithExactMoneyAndMicroseconds() {
        CardTransaction purchase = persistPurchase();
        CardTransaction refund = EntityFixtures.purchase(purchase.getAccount(), purchase.getCard());
        refund.setType(CardTransaction.Type.REFUND);
        refund.setOriginalPurchase(purchase);
        refund.setOutstandingAfter(new BigDecimal("0.00"));
        purchase.getAccount().setOutstandingBalance(new BigDecimal("0.00"));
        entityManager.persist(refund);
        entityManager.flush();
        entityManager.clear();

        CardTransaction loaded = entityManager.find(CardTransaction.class, refund.getId());
        assertThat(loaded.getOriginalPurchase().getId()).isEqualTo(purchase.getId());
        assertThat(loaded.getType()).isEqualTo(CardTransaction.Type.REFUND);
        assertThat(loaded.getStatus()).isEqualTo(CardTransaction.Status.APPROVED);
        assertThat(loaded.getAmount()).isEqualTo(new BigDecimal("25.10"));
        assertThat(loaded.getCreatedAt()).isEqualTo(purchase.getCreatedAt());
        assertThat(loaded.getRequestId()).isEqualTo(refund.getRequestId());
        assertThat(loaded.getReasonCode()).isNull();
        assertThat(loaded.getAccount().getId()).isEqualTo(purchase.getAccount().getId());
        assertThat(loaded.getCard().getId()).isEqualTo(purchase.getCard().getId());
        assertThat(loaded.getCard().getAccount().getId()).isEqualTo(loaded.getAccount().getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?",
                Long.class, loaded.getAccount().getId())).isEqualTo(2L);
    }

    @Test
    void removingCardCannotCascadeAwayItsHistory() {
        CardTransaction purchase = persistPurchase();
        Long cardId = purchase.getCard().getId();
        entityManager.clear();
        entityManager.remove(entityManager.find(DemoCard.class, cardId));

        // With no REMOVE cascade, MySQL refuses the parent deletion instead.
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE id = ?",
                Long.class, purchase.getId())).isEqualTo(1L);
    }

    @Test
    void removingRefundDoesNotDeleteOriginalPurchaseOrParents() {
        CardTransaction purchase = persistPurchase();
        CardTransaction refund = EntityFixtures.purchase(purchase.getAccount(), purchase.getCard());
        refund.setType(CardTransaction.Type.REFUND);
        refund.setOriginalPurchase(purchase);
        refund.setOutstandingAfter(new BigDecimal("0.00"));
        entityManager.persist(refund);
        entityManager.flush();
        entityManager.remove(refund);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(CardTransaction.class, purchase.getId())).isNotNull();
        assertThat(entityManager.find(CreditAccount.class, purchase.getAccount().getId())).isNotNull();
        assertThat(entityManager.find(DemoCard.class, purchase.getCard().getId())).isNotNull();
        assertThat(entityManager.find(AppUser.class, purchase.getAccount().getUser().getId())).isNotNull();
    }

    @Test
    void existingSchemaHasExactColumnsAndNullability() {
        // Hibernate validate checks SQL types; this adds lengths, scale and nullability.
        var columns = jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', column_name, ' ', column_type, ' ', is_nullable)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('app_users', 'credit_accounts', 'demo_cards', 'card_transactions')
                ORDER BY table_name, ordinal_position
                """, String.class);
        assertThat(columns).containsExactlyElementsOf(lines("""
                app_users.id bigint NO
                app_users.display_name varchar(100) NO
                app_users.email varchar(150) NO
                app_users.password_hash varchar(100) NO
                app_users.role varchar(20) NO
                card_transactions.id bigint NO
                card_transactions.account_id bigint NO
                card_transactions.card_id bigint NO
                card_transactions.type varchar(20) NO
                card_transactions.status varchar(20) NO
                card_transactions.amount decimal(14,2) NO
                card_transactions.outstanding_after decimal(14,2) NO
                card_transactions.merchant_name varchar(100) NO
                card_transactions.reason_code varchar(40) YES
                card_transactions.created_at datetime(6) NO
                card_transactions.request_id char(36) NO
                card_transactions.original_purchase_id bigint YES
                credit_accounts.id bigint NO
                credit_accounts.user_id bigint NO
                credit_accounts.credit_limit decimal(14,2) NO
                credit_accounts.outstanding_balance decimal(14,2) NO
                credit_accounts.status varchar(20) NO
                demo_cards.id bigint NO
                demo_cards.account_id bigint NO
                demo_cards.test_profile varchar(20) NO
                demo_cards.label varchar(50) NO
                demo_cards.last_four char(4) NO
                demo_cards.expiry_month tinyint NO
                demo_cards.expiry_year smallint NO
                """));
    }

    @Test
    void existingUniqueConstraintsAndForeignKeysProtectHistory() {
        var uniqueKeys = jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', index_name, ' ',
                       GROUP_CONCAT(column_name ORDER BY seq_in_index))
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND non_unique = 0 AND index_name <> 'PRIMARY'
                GROUP BY table_name, index_name ORDER BY table_name, index_name
                """, String.class);
        assertThat(uniqueKeys).containsExactlyElementsOf(lines("""
                app_users.uq_app_users_email email
                card_transactions.uq_card_transactions_original_purchase original_purchase_id
                card_transactions.uq_card_transactions_request account_id,request_id
                credit_accounts.uq_credit_accounts_user user_id
                demo_cards.uq_demo_cards_account account_id
                """));

        var foreignKeys = jdbc.queryForList("""
                SELECT CONCAT(k.constraint_name, ' ', k.table_name, '.', k.column_name, ' -> ',
                       k.referenced_table_name, '.', k.referenced_column_name, ' ', r.delete_rule)
                FROM information_schema.key_column_usage k
                JOIN information_schema.referential_constraints r
                  ON k.constraint_schema = r.constraint_schema AND k.constraint_name = r.constraint_name
                WHERE k.constraint_schema = DATABASE()
                ORDER BY k.constraint_name
                """, String.class);
        assertThat(foreignKeys).containsExactlyElementsOf(lines("""
                fk_card_transactions_account card_transactions.account_id -> credit_accounts.id NO ACTION
                fk_card_transactions_card card_transactions.card_id -> demo_cards.id NO ACTION
                fk_card_transactions_purchase card_transactions.original_purchase_id -> card_transactions.id NO ACTION
                fk_credit_accounts_user credit_accounts.user_id -> app_users.id NO ACTION
                fk_demo_cards_account demo_cards.account_id -> credit_accounts.id NO ACTION
                """));
        assertThat(jdbc.queryForObject("""
                SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'card_transactions'
                  AND index_name = 'idx_card_transactions_account_history'
                """, String.class)).isEqualTo("account_id,id");
    }

    private CardTransaction persistPurchase() {
        AppUser user = EntityFixtures.user();
        CreditAccount account = EntityFixtures.account(user);
        DemoCard card = EntityFixtures.card(account);
        CardTransaction purchase = EntityFixtures.purchase(account, card);
        account.setOutstandingBalance(purchase.getOutstandingAfter());
        // Explicit saves demonstrate that mappings do not cascade persistence or deletion.
        entityManager.persist(user);
        entityManager.persist(account);
        entityManager.persist(card);
        entityManager.persist(purchase);
        entityManager.flush();
        return purchase;
    }

    private static java.util.List<String> lines(String text) {
        return Arrays.asList(text.strip().split("\\R"));
    }
}
