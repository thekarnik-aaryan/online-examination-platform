# Online Examination System (Java Swing + MySQL)

Desktop exam platform for a project: Admin / Faculty / Student roles, timed MCQ exams, automatic evaluation, audit logging.

> **Build status:** All main sources were compile-checked with `javac` (JDK 21) against a stub of the BCrypt library.
> The Maven build, the real BCrypt/MySQL libraries, the JUnit tests and the MySQL scripts have **not** been executed.
> Run `mvn clean package` and follow `docs/TESTING.md` on your machine.

## Stack
Java 17+, Swing, JDBC, MySQL 8+, Maven, `at.favre.lib:bcrypt` (BCrypt, cost 12), `mysql-connector-j`.

## Setup
1. Install **JDK 17+** and **Maven 3.8+**.
2. Install **MySQL 8+**.
3. Edit the password in the last lines of `sql/schema.sql` (`CREATE USER ... 'CHANGE_ME'`), then create the schema:
   `mysql -u root -p < sql/schema.sql`
4. Load demo data: `mysql -u root -p online_exam < sql/seed.sql`
5. Configure credentials: `cp config.example.properties config.properties`, then edit `db.user` / `db.password`.
   Environment variables `EXAM_DB_URL`, `EXAM_DB_USER`, `EXAM_DB_PASSWORD`, `EXAM_CONFIG` override the file.
6. Build (runs unit tests; DB tests are skipped unless `EXAM_DB_URL` is set): `mvn clean package`
7. Run from the project folder (so `config.properties` is found): `java -jar target/OnlineExaminationSystem.jar`

## DEMO credentials (change immediately; delete before real use)
| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@12345` |
| Faculty | `faculty1` | `Faculty@12345` |
| Student | `student1` | `Student@12345` |

The seeded exam *Core Java Basics Quiz* (5 questions, 10 minutes) is published and open for a year from seeding.

## Project layout
```
pom.xml  config.example.properties  .gitignore  README.md
sql/schema.sql  sql/seed.sql   docs/TESTING.md
src/main/java/Main.java
  config/ (AppConfig, Database)   model/ (records)   dao/ (JDBC only)
  service/ (rules, authz, transactions)   security/ (hasher, session, authz)
  ui/ (Swing)   util/ (Validator, Log)   exception/
src/test/java/ (Validator, PasswordHasher, Grader, DB integration)
```

## Design decisions
- Questions belong directly to one exam (no shared question bank / `exam_questions` table).
- One attempt per student per exam (`UNIQUE(exam_id, student_id)`); the question order is stored in `exam_attempts.question_order`.
- Answers are saved to the DB as soon as selected; the exam can be resumed until the deadline.
- Deadline = min(start + duration, exam end time). When it passes, the screen auto-submits. If the app was closed, expired attempts are evaluated automatically the next time that student opens the dashboard.
- Questions/options are locked once any student has attempted the exam. Exams with attempts cannot be deleted (unpublish instead).
- Students never receive the correct-answer flags before submission.
- Grading: marks per question, no negative marking.
- Admin "manage results" = view only. Results are immutable by design (DB triggers).

## Security controls implemented
Authentication with BCrypt; role checks (`Authz.require`) at the top of every service method; session management; PreparedStatement everywhere; input validation (`Validator`); exam eligibility checks (published, time window, ownership, one attempt); answer/result immutability enforced in services **and** by MySQL triggers; transactional submission with row locks (`SELECT ... FOR UPDATE`); audit log (login success/failure/blocked, logout, exam start/submit, admin changes, access denied) which is append-only via triggers; lockout after 5 failed logins for 5 minutes (configurable); generic user-facing errors with details only in `logs/exam-system.log`; no secrets in code (external config/env); FK/UNIQUE/CHECK constraints.

## Database least privilege
The app account `exam_app` gets only `SELECT, INSERT, UPDATE, DELETE` on `online_exam.*` — no DDL, no `GRANT`, no `TRIGGER`. Use a separate admin account for schema changes. Bind MySQL to localhost or a private network and enable TLS if the DB is remote.

## Backup & recovery
```
mysqldump -u root -p --single-transaction --routines --triggers online_exam > backup_$(date +%F).sql
mysql -u root -p -e "DROP DATABASE IF EXISTS online_exam; CREATE DATABASE online_exam CHARACTER SET utf8mb4;"
mysql -u root -p online_exam < backup_YYYY-MM-DD.sql
```
Schedule the dump (cron / Task Scheduler), store copies off-machine, and test restores. Use `--triggers` so integrity triggers survive a restore.

## Known limitations
- **A desktop client cannot guarantee a secure exam.** Client-side anti-cheating (full screen, focus detection, etc.) is bypassable; the app does not attempt it. Use supervised labs for high-stakes exams.
- Time checks use the client machine clock (both the countdown and the stored deadline). A user who changes the system clock can gain time. A real deployment needs a server-side API with its own clock.
- Direct DB connections from every client: each client machine needs DB credentials, so use the least-privilege account on a trusted LAN only.
- Database calls run on the Swing event thread (simple, but the UI can pause during slow queries or BCrypt hashing).
- Lockout is per existing username; unknown usernames are audited but not rate-limited.
- Fixed 4-option single-answer MCQs; no CSV import, pagination, password-reset email or result export.
- `schema.sql` uses `DELIMITER`, so run it with the `mysql` client (not a JDBC runner).
