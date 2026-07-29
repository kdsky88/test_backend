-- 여행: 일정(todos)을 묶는 상위 컨테이너.
CREATE TABLE trips (
    id VARCHAR(36) NOT NULL,
    owner_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    destination VARCHAR(100),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_trips_owner ON trips (owner_id);

-- 일정 → 여행 연결(선택적). 여행을 지워도 일정은 남기고 연결만 해제.
ALTER TABLE todos ADD COLUMN trip_id VARCHAR(36);
ALTER TABLE todos ADD CONSTRAINT fk_todos_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE SET NULL;
CREATE INDEX idx_todos_trip ON todos (trip_id);
