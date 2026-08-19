-- 여행별 경비 기록. 여행 삭제 시 경비도 함께 삭제(CASCADE).
CREATE TABLE expenses (
    id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    owner_id BIGINT NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    category VARCHAR(20) NOT NULL,
    memo VARCHAR(200),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_expenses_trip ON expenses (trip_id);
