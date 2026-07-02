-- 공유(담당자): todos.assigned_to_id → users(id)
ALTER TABLE todos ADD COLUMN assigned_to_id BIGINT;
ALTER TABLE todos ADD CONSTRAINT fk_todos_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id);
CREATE INDEX idx_todos_assigned_to ON todos (assigned_to_id);
