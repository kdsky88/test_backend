-- 할 일 하위 체크리스트. @ElementCollection + @OrderColumn(position)에 매핑.
CREATE TABLE todo_subtasks (
    todo_id VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    title VARCHAR(100) NOT NULL,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (todo_id, position),
    CONSTRAINT fk_todo_subtasks_todo FOREIGN KEY (todo_id) REFERENCES todos (id) ON DELETE CASCADE
);
