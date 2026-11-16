CREATE TABLE policy_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  version VARCHAR(40),
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_policy_document_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE policy_chunks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  source_page INT,
  content LONGTEXT NOT NULL,
  vector_id VARCHAR(120),
  CONSTRAINT uk_policy_chunk_order UNIQUE (document_id, chunk_index),
  CONSTRAINT fk_policy_chunk_document FOREIGN KEY (document_id) REFERENCES policy_documents(id)
);

CREATE TABLE chat_conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_chat_conversation_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_chat_conversation_user_created (user_id, created_at)
);

CREATE TABLE chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content LONGTEXT NOT NULL,
  citations TEXT,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id),
  INDEX ix_chat_message_conversation_created (conversation_id, created_at)
);
