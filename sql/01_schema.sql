-- Credit Card Transaction Simulator, section 2.1
-- MySQL 8.0.16+ enforces the CHECK constraints below.
-- Safe to rerun: existing tables, balances, and history are left in place.
CREATE DATABASE IF NOT EXISTS card_transaction_simulator
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE card_transaction_simulator;

CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_app_users_email UNIQUE (email),
    CONSTRAINT chk_app_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_app_users_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT chk_app_users_email CHECK (CHAR_LENGTH(TRIM(email)) > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS credit_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    credit_limit DECIMAL(14,2) NOT NULL,
    outstanding_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_credit_accounts_user UNIQUE (user_id),
    CONSTRAINT fk_credit_accounts_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT chk_credit_accounts_limit CHECK (credit_limit > 0),
    CONSTRAINT chk_credit_accounts_balance CHECK (
        outstanding_balance >= 0 AND outstanding_balance <= credit_limit
    ),
    CONSTRAINT chk_credit_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS demo_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    test_profile VARCHAR(20) NOT NULL,
    label VARCHAR(50) NOT NULL,
    last_four CHAR(4) NOT NULL,
    expiry_month TINYINT NOT NULL,
    expiry_year SMALLINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_demo_cards_account UNIQUE (account_id),
    CONSTRAINT fk_demo_cards_account FOREIGN KEY (account_id) REFERENCES credit_accounts(id),
    CONSTRAINT chk_demo_cards_profile CHECK (CHAR_LENGTH(TRIM(test_profile)) > 0),
    CONSTRAINT chk_demo_cards_label CHECK (CHAR_LENGTH(TRIM(label)) > 0),
    CONSTRAINT chk_demo_cards_last_four CHECK (REGEXP_LIKE(last_four, '^[0-9]{4}$')),
    CONSTRAINT chk_demo_cards_month CHECK (expiry_month BETWEEN 1 AND 12),
    CONSTRAINT chk_demo_cards_year CHECK (expiry_year BETWEEN 2000 AND 9999)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS card_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    type VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    outstanding_after DECIMAL(14,2) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    reason_code VARCHAR(40) NULL,
    created_at DATETIME(6) NOT NULL,
    request_id CHAR(36) NOT NULL,
    original_purchase_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_card_transactions_account FOREIGN KEY (account_id) REFERENCES credit_accounts(id),
    CONSTRAINT fk_card_transactions_card FOREIGN KEY (card_id) REFERENCES demo_cards(id),
    CONSTRAINT fk_card_transactions_purchase FOREIGN KEY (original_purchase_id) REFERENCES card_transactions(id),
    CONSTRAINT uq_card_transactions_request UNIQUE (account_id, request_id),
    CONSTRAINT uq_card_transactions_original_purchase UNIQUE (original_purchase_id),
    INDEX idx_card_transactions_account_history (account_id, id),
    CONSTRAINT chk_card_transactions_type CHECK (type IN ('PURCHASE', 'REFUND')),
    CONSTRAINT chk_card_transactions_status CHECK (status IN ('APPROVED', 'DECLINED')),
    CONSTRAINT chk_card_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_card_transactions_balance CHECK (outstanding_after >= 0),
    CONSTRAINT chk_card_transactions_merchant CHECK (CHAR_LENGTH(TRIM(merchant_name)) > 0),
    CONSTRAINT chk_card_transactions_purchase_link CHECK (
        (type = 'PURCHASE' AND original_purchase_id IS NULL)
        OR (type = 'REFUND' AND status = 'APPROVED' AND original_purchase_id IS NOT NULL)
    )
) ENGINE=InnoDB;

-- The service checks account ownership of a card and refund, compares the
-- transaction balance with the account limit, and saves balance and history
-- together. Those cross-row rules cannot be expressed by a column CHECK.
