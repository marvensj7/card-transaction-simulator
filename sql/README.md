# Local MySQL setup

Use MySQL 8.0.16 or newer, where `CHECK` constraints are enforced. Run these files in order from MySQL Workbench or the `mysql` client. For example, from the repository root with a locally configured MySQL login path:

```text
mysql --login-path=capstone --execute="source sql/01_schema.sql"
mysql --login-path=capstone --execute="source sql/02_seed.sql"
```

1. `01_schema.sql` creates `card_transaction_simulator` and its four tables.
2. `02_seed.sql` adds two fictional customers, their zero-balance accounts and masked demo cards, and one administrator.

Run both files again whenever needed. The schema uses `IF NOT EXISTS`, and the seed inserts only missing demo rows. Existing balances, statuses, passwords, cards, and transaction history are preserved. This is initial setup, not a migration tool for future schema changes.

The seed password values are BCrypt hashes. Keep any demo sign-in password outside source control and application logs; replace the seed hashes before first use if you want a different local password. The later Spring application must map `DEMO_4242` to one documented fictional test number without storing the number or test security code in MySQL. It must also check the card/account and refund relationships and commit each balance change with its history row.
