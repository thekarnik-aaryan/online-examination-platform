# Test plan

Automated: `mvn test` runs `ValidatorTest`, `PasswordHasherTest`, `GraderTest`.
`DatabaseIntegrationTest` runs only when `EXAM_DB_URL` (+ `EXAM_DB_USER`, `EXAM_DB_PASSWORD`) point to a **freshly seeded test DB** (`schema.sql` + `seed.sql`). Re-seed before each run: it consumes student1's single attempt.

Manual checklist (Expected result in brackets). Not yet executed.

| # | Area | Steps | Expected |
|---|---|---|---|
| 1 | Valid login | Log in as admin, faculty1, student1 | Correct dashboard per role |
| 2 | Invalid login | Wrong password; unknown user | "Invalid username or password."; LOGIN_FAILURE in audit |
| 3 | Brute force | 5 wrong passwords for `student1`, then the right one | Blocked for 5 min; LOGIN_BLOCKED audited |
| 4 | SQL injection | Username `admin' OR '1'='1`; user create/subject name `'; DROP TABLE users;--` | Rejected / stored as plain text; tables intact |
| 5 | Unauthorized access | Student/faculty calling admin services (JUnit tests 3, 6) | AccessDeniedException; ACCESS_DENIED audited |
| 6 | Empty/invalid input | Blank fields, bad dates, letters in numbers, 31-char username | Friendly validation message |
| 7 | Password hashing | `SELECT password_hash FROM users` | Only `$2b$12$...` values |
| 8 | Timer / auto-submit | Create a 1-minute exam, start, wait | Auto-submit at 00:00, result dialog, status AUTO_SUBMITTED |
| 9 | Auto-submit after crash | Start exam, kill the app, wait past deadline, log in again | Attempt evaluated; visible in My Results |
| 10 | Manual submit | Answer some, Submit, confirm | Confirmation shows unanswered count; result correct |
| 11 | Duplicate submission | Call `submit` twice (JUnit test 5) | Second call rejected |
| 12 | Answer integrity | After submit, `UPDATE student_answers SET option_id=...` in MySQL; `saveAnswer` via service | Trigger error / service rejection |
| 13 | Result integrity | `UPDATE results SET score=100` / `DELETE FROM results` | Trigger error |
| 14 | Eligibility | Unpublished exam; before start; after end; second attempt | Not listed / clear message |
| 15 | DB failure | Stop MySQL while running | Generic "A database error occurred" dialog, details in `logs/` only |
| 16 | Logout/session | Logout, then back to login; close window | Login screen; LOGOUT audited; process exits on close |
| 17 | CRUD | Users, subjects, exams, questions: add/edit/delete | Persisted; audit entries written |
| 18 | FK/constraints | Delete subject that has exams; duplicate username/subject code; exam end <= start | Friendly constraint messages |
| 19 | Boundaries | Duration 0/1/300/301; marks 0/1/100/101; 100 vs 101 questions; 4 identical options; 3 options | 0, 301, 101 rejected; limits accepted |
| 20 | Faculty ownership | Faculty edits another faculty's exam via admin-created exam | Denied |
| 21 | Randomization | Two students start the same randomized exam | Different order; `question_order` stored |
