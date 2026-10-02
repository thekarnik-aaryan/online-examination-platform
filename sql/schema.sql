-- Online Examination System schema (MySQL 8+). Run as an administrative MySQL user.
CREATE DATABASE IF NOT EXISTS online_exam CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_exam;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(30) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NULL,
  role ENUM('ADMIN','FACULTY','STUDENT') NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT ck_users_username CHECK (CHAR_LENGTH(username) >= 3)
) ENGINE=InnoDB;

CREATE TABLE settings (
  setting_key VARCHAR(50) PRIMARY KEY,
  setting_value VARCHAR(200) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE subjects (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_subjects_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE exams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject_id BIGINT NOT NULL,
  faculty_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(500) NULL,
  duration_minutes INT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  published TINYINT(1) NOT NULL DEFAULT 0,
  randomize TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
  CONSTRAINT fk_exams_faculty FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT ck_exams_duration CHECK (duration_minutes BETWEEN 1 AND 300),
  CONSTRAINT ck_exams_window CHECK (end_time > start_time),
  INDEX idx_exams_faculty (faculty_id),
  INDEX idx_exams_window (published, start_time, end_time)
) ENGINE=InnoDB;

CREATE TABLE questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  question_text VARCHAR(1000) NOT NULL,
  marks INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT ck_questions_marks CHECK (marks BETWEEN 1 AND 100),
  INDEX idx_questions_exam (exam_id)
) ENGINE=InnoDB;

CREATE TABLE options (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_no TINYINT NOT NULL,
  option_text VARCHAR(300) NOT NULL,
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
  CONSTRAINT uq_options_no UNIQUE (question_id, option_no),
  CONSTRAINT ck_options_no CHECK (option_no BETWEEN 1 AND 6)
) ENGINE=InnoDB;

CREATE TABLE exam_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  started_at DATETIME NOT NULL,
  deadline DATETIME NOT NULL,
  submitted_at DATETIME NULL,
  status ENUM('IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED') NOT NULL DEFAULT 'IN_PROGRESS',
  question_order VARCHAR(2000) NOT NULL,   -- comma-separated question ids in the order shown (reproducibility)
  CONSTRAINT fk_attempts_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE RESTRICT,
  CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uq_attempt_once UNIQUE (exam_id, student_id),
  INDEX idx_attempts_student (student_id, status)
) ENGINE=InnoDB;

CREATE TABLE student_answers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  option_id BIGINT NULL,
  answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE RESTRICT,
  CONSTRAINT fk_answers_option FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE RESTRICT,
  CONSTRAINT uq_answer_once UNIQUE (attempt_id, question_id)
) ENGINE=InnoDB;

CREATE TABLE results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  score INT NOT NULL,
  total_marks INT NOT NULL,
  percentage DECIMAL(5,2) NOT NULL,
  correct_count INT NOT NULL,
  incorrect_count INT NOT NULL,
  unattempted_count INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_results_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE RESTRICT,
  CONSTRAINT uq_result_once UNIQUE (attempt_id),
  CONSTRAINT ck_results_score CHECK (score >= 0 AND score <= total_marks),
  CONSTRAINT ck_results_pct CHECK (percentage BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  username VARCHAR(50) NULL,
  action VARCHAR(50) NOT NULL,
  details VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_created (created_at),
  INDEX idx_audit_action (action)
) ENGINE=InnoDB;

-- Integrity triggers: submitted answers and results are immutable at the database level.
DELIMITER $$
CREATE TRIGGER trg_answers_no_update BEFORE UPDATE ON student_answers FOR EACH ROW
BEGIN
  IF (SELECT status FROM exam_attempts WHERE id = OLD.attempt_id) <> 'IN_PROGRESS' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Answers of a submitted attempt are immutable';
  END IF;
END$$
CREATE TRIGGER trg_answers_no_insert BEFORE INSERT ON student_answers FOR EACH ROW
BEGIN
  IF (SELECT status FROM exam_attempts WHERE id = NEW.attempt_id) <> 'IN_PROGRESS' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt already submitted';
  END IF;
END$$
CREATE TRIGGER trg_answers_no_delete BEFORE DELETE ON student_answers FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Answers cannot be deleted';
END$$
CREATE TRIGGER trg_results_no_update BEFORE UPDATE ON results FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Results are immutable';
END$$
CREATE TRIGGER trg_results_no_delete BEFORE DELETE ON results FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Results cannot be deleted';
END$$
CREATE TRIGGER trg_attempt_status BEFORE UPDATE ON exam_attempts FOR EACH ROW
BEGIN
  IF OLD.status <> 'IN_PROGRESS' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted attempts are immutable';
  END IF;
END$$
CREATE TRIGGER trg_audit_no_update BEFORE UPDATE ON audit_logs FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log is append-only';
END$$
CREATE TRIGGER trg_audit_no_delete BEFORE DELETE ON audit_logs FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log is append-only';
END$$
DELIMITER ;

-- Least-privilege application account (change the password before running!).
-- No DROP/ALTER/CREATE/TRIGGER rights. Immutability of results/answers/audit is enforced by the triggers above.
CREATE USER IF NOT EXISTS 'exam_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT SELECT, INSERT, UPDATE, DELETE ON online_exam.* TO 'exam_app'@'localhost';
