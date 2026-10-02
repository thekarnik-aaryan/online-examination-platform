-- DEMO DATA ONLY. Change/delete these accounts before any real use.
-- Demo passwords (BCrypt cost 12): admin / Admin@12345, faculty1 / Faculty@12345, student1 / Student@12345
USE online_exam;

INSERT INTO users (username, password_hash, full_name, email, role) VALUES
('admin',    '$2b$12$3MTYM8Shf405YDjOxJy8.eubeJ2feKfwPmla.hf8WmqzMha25G0h6', 'System Administrator', 'admin@example.com', 'ADMIN'),
('faculty1', '$2b$12$jZvLlLP2BESa2ZgSMbqJ3urKscVbGFLp1r0Du3jMxwMAEUhBN3Yjq', 'Dr. Asha Verma', 'faculty1@example.com', 'FACULTY'),
('student1', '$2b$12$6zAjfNF50ouFnYrlW9Ms.eeCTsJzojAZMk8XZ6Gm5nWNBurlRuPki', 'Rohan Patil', 'student1@example.com', 'STUDENT');

INSERT INTO settings (setting_key, setting_value) VALUES ('student_registration_enabled', 'true');

INSERT INTO subjects (code, name) VALUES ('JAVA101', 'Core Java'), ('DBMS201', 'Database Management Systems');

-- Demo exam: window is wide so it is available right after seeding (adjust as needed).
INSERT INTO exams (subject_id, faculty_id, title, description, duration_minutes, start_time, end_time, published, randomize)
SELECT s.id, u.id, 'Core Java Basics Quiz', 'Five MCQs on Java fundamentals. One correct answer each.',
       10, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 365 DAY, 1, 1
FROM subjects s, users u WHERE s.code='JAVA101' AND u.username='faculty1';

SET @exam := (SELECT id FROM exams WHERE title='Core Java Basics Quiz');

INSERT INTO questions (exam_id, question_text, marks) VALUES
(@exam, 'Which keyword is used to inherit a class in Java?', 2),
(@exam, 'Which of these is NOT a primitive data type in Java?', 2),
(@exam, 'What is the default value of an int instance variable?', 2),
(@exam, 'Which collection does not allow duplicate elements?', 2),
(@exam, 'Which method is the entry point of a Java application?', 2);

INSERT INTO options (question_id, option_no, option_text, is_correct)
SELECT q.id, o.n, o.t, o.c FROM questions q JOIN (
  SELECT 'Which keyword is used to inherit a class in Java?' qt, 1 n, 'implements' t, 0 c UNION ALL
  SELECT 'Which keyword is used to inherit a class in Java?', 2, 'extends', 1 UNION ALL
  SELECT 'Which keyword is used to inherit a class in Java?', 3, 'inherits', 0 UNION ALL
  SELECT 'Which keyword is used to inherit a class in Java?', 4, 'super', 0 UNION ALL
  SELECT 'Which of these is NOT a primitive data type in Java?', 1, 'int', 0 UNION ALL
  SELECT 'Which of these is NOT a primitive data type in Java?', 2, 'boolean', 0 UNION ALL
  SELECT 'Which of these is NOT a primitive data type in Java?', 3, 'String', 1 UNION ALL
  SELECT 'Which of these is NOT a primitive data type in Java?', 4, 'char', 0 UNION ALL
  SELECT 'What is the default value of an int instance variable?', 1, 'null', 0 UNION ALL
  SELECT 'What is the default value of an int instance variable?', 2, '1', 0 UNION ALL
  SELECT 'What is the default value of an int instance variable?', 3, '0', 1 UNION ALL
  SELECT 'What is the default value of an int instance variable?', 4, 'undefined', 0 UNION ALL
  SELECT 'Which collection does not allow duplicate elements?', 1, 'ArrayList', 0 UNION ALL
  SELECT 'Which collection does not allow duplicate elements?', 2, 'LinkedList', 0 UNION ALL
  SELECT 'Which collection does not allow duplicate elements?', 3, 'Vector', 0 UNION ALL
  SELECT 'Which collection does not allow duplicate elements?', 4, 'HashSet', 1 UNION ALL
  SELECT 'Which method is the entry point of a Java application?', 1, 'start()', 0 UNION ALL
  SELECT 'Which method is the entry point of a Java application?', 2, 'run()', 0 UNION ALL
  SELECT 'Which method is the entry point of a Java application?', 3, 'main()', 1 UNION ALL
  SELECT 'Which method is the entry point of a Java application?', 4, 'init()', 0
) o ON o.qt = q.question_text WHERE q.exam_id = @exam;
