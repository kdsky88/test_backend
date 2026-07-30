-- 일정 장소: 위경도 + 장소명(모두 선택적).
ALTER TABLE todos ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE todos ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE todos ADD COLUMN place_name VARCHAR(200);
