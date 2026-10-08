# Local MySQL setup

Use MySQL 8.0.16 or newer, where `CHECK` constraints are enforced. Run these files in order from MySQL Workbench or the `mysql` client. For example, from the repository root with a locally configured MySQL login path:

```text
mysql --login-path=capstone --execute="source sql/01_schema.sql"
mysql --login-path=capstone --execute="source sql/02_seed.sql"
```

1. `01_schema.sql` creates `card_transaction_simulator` and its four tables.
2. `02_seed.sql` adds two fictional customers, their zero-balance accounts and masked demo cards, and one administrator.

Run both files again whenever needed. The schema uses `IF NOT EXISTS`, and the seed inserts only missing demo rows. Existing balances, statuses, passwords, cards, and transaction history are preserved. This is initial setup, not a migration tool for future schema changes.

The seed password values are BCrypt hashes. Keep any demo sign-in password outside source control and application logs; replace the seed hashes before first use if you want a different local password.

Follow the [backend setup and verification instructions](../backend/README.md) to configure the database connection and run MySQL checks. Hibernate uses `validate`; the SQL scripts remain the source of the schema.

## Application rules

The service maps `DEMO_4242` to the fictional profile described as 4242 repeated four times in memory. MySQL stores the profile and last four digits, never the full number or test security code. Services check card/account ownership and refund eligibility and commit each balance change with its history row. The existing account/request-ID and original-purchase uniqueness rules back retry handling and the one-refund limit.
