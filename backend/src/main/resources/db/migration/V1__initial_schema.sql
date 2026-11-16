CREATE TABLE department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  code VARCHAR(255)
);

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  department_id BIGINT,
  manager_id BIGINT,
  CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id)
);

CREATE TABLE leave_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  active BOOLEAN NOT NULL
);

CREATE TABLE leave_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  applicant_id BIGINT NOT NULL,
  leave_type VARCHAR(255) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  working_days INT NOT NULL,
  status VARCHAR(255) NOT NULL,
  reason VARCHAR(255),
  reviewer_note VARCHAR(255),
  version BIGINT,
  CONSTRAINT fk_leave_applications_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
  INDEX ix_leave_applicant_dates (applicant_id, start_date, end_date)
);

CREATE TABLE leave_balance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  leave_type_id BIGINT NOT NULL,
  remaining_days DECIMAL(7,2) NOT NULL,
  version BIGINT,
  CONSTRAINT uk_leave_balance_user_type UNIQUE (user_id, leave_type_id),
  CONSTRAINT fk_leave_balance_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_leave_balance_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(id)
);

CREATE TABLE balance_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  balance_id BIGINT NOT NULL,
  application_id BIGINT UNIQUE,
  delta DECIMAL(7,2) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_balance_transaction_balance FOREIGN KEY (balance_id) REFERENCES leave_balance(id),
  CONSTRAINT fk_balance_transaction_application FOREIGN KEY (application_id) REFERENCES leave_applications(id)
);

CREATE TABLE holiday (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  holiday_date DATE NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id BIGINT,
  action VARCHAR(255) NOT NULL,
  entity_type VARCHAR(255) NOT NULL,
  entity_id BIGINT,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  message VARCHAR(255) NOT NULL,
  read_at DATETIME,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);
