CREATE TABLE pending_leave_confirmations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  leave_type VARCHAR(80) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  application_id BIGINT UNIQUE,
  version BIGINT,
  CONSTRAINT uk_pending_leave_confirmation_conversation_user UNIQUE (conversation_id, user_id),
  CONSTRAINT fk_pending_leave_confirmation_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id),
  CONSTRAINT fk_pending_leave_confirmation_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_pending_leave_confirmation_application FOREIGN KEY (application_id) REFERENCES leave_applications(id)
);
