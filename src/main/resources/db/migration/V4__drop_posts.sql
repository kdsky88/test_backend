-- 미사용 게시글(Post) 기능 제거. 초기 baseline(V1)에 딸려온 잔재로 할일 앱은 안 씀.
-- IF EXISTS: 이미 수동 삭제된 DB(운영)는 no-op, 새 DB는 V1이 만든 테이블을 여기서 드롭.
DROP TABLE IF EXISTS posts;
