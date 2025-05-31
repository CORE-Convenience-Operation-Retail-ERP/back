-- 채팅 메시지 테이블에 새로운 기능 필드 추가
-- 읽음 상태, 읽은 사용자 목록, 이모지 반응 필드

ALTER TABLE chat_message 
ADD COLUMN is_read BOOLEAN DEFAULT FALSE COMMENT '메시지 읽음 상태',
ADD COLUMN read_by JSON COMMENT '읽은 사용자 목록 (JSON 형태)',
ADD COLUMN reactions JSON COMMENT '이모지 반응 (JSON 형태)';

-- 기존 메시지들의 읽음 상태를 false로 초기화
UPDATE chat_message SET is_read = FALSE WHERE is_read IS NULL; 