-- A leave can have two immutable accounting entries: approval debit and eligible cancellation credit.
ALTER TABLE balance_transaction DROP FOREIGN KEY fk_balance_transaction_application;
ALTER TABLE balance_transaction DROP INDEX application_id;
CREATE INDEX ix_balance_transaction_application ON balance_transaction(application_id);
ALTER TABLE balance_transaction
  ADD CONSTRAINT fk_balance_transaction_application
  FOREIGN KEY (application_id) REFERENCES leave_applications(id);
