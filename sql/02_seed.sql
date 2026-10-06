-- Fictional local demo data. Run after 01_schema.sql.
-- Existing users, account balances, statuses, cards, and history are not changed.
USE card_transaction_simulator;
START TRANSACTION;

-- BCrypt cost 12. No plaintext password or card number is stored in this file.
INSERT INTO app_users (display_name, email, password_hash, role)
SELECT 'Casey Brooks', 'casey.demo@example.test',
       '$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE email = 'casey.demo@example.test');

INSERT INTO app_users (display_name, email, password_hash, role)
SELECT 'Jordan Reed', 'jordan.demo@example.test',
       '$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE email = 'jordan.demo@example.test');

INSERT INTO app_users (display_name, email, password_hash, role)
SELECT 'Demo Administrator', 'admin.demo@example.test',
       '$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE email = 'admin.demo@example.test');

-- An empty history matches the opening zero balance; purchases will add rows.
INSERT INTO credit_accounts (user_id, credit_limit, outstanding_balance, status)
SELECT u.id, 1000.00, 0.00, 'ACTIVE'
FROM app_users AS u
WHERE u.email = 'casey.demo@example.test' AND u.role = 'USER'
  AND NOT EXISTS (SELECT 1 FROM credit_accounts AS a WHERE a.user_id = u.id);

INSERT INTO credit_accounts (user_id, credit_limit, outstanding_balance, status)
SELECT u.id, 500.00, 0.00, 'ACTIVE'
FROM app_users AS u
WHERE u.email = 'jordan.demo@example.test' AND u.role = 'USER'
  AND NOT EXISTS (SELECT 1 FROM credit_accounts AS a WHERE a.user_id = u.id);

-- DEMO_4242 is a profile name for a later application allowlist, not a PAN.
INSERT INTO demo_cards (account_id, test_profile, label, last_four, expiry_month, expiry_year)
SELECT a.id, 'DEMO_4242', 'Casey Demo Card', '4242', 12, 2030
FROM credit_accounts AS a
JOIN app_users AS u ON u.id = a.user_id
WHERE u.email = 'casey.demo@example.test'
  AND NOT EXISTS (SELECT 1 FROM demo_cards AS c WHERE c.account_id = a.id);

INSERT INTO demo_cards (account_id, test_profile, label, last_four, expiry_month, expiry_year)
SELECT a.id, 'DEMO_4242', 'Jordan Demo Card', '4242', 12, 2030
FROM credit_accounts AS a
JOIN app_users AS u ON u.id = a.user_id
WHERE u.email = 'jordan.demo@example.test'
  AND NOT EXISTS (SELECT 1 FROM demo_cards AS c WHERE c.account_id = a.id);

COMMIT;
