-- 1. 기본 테이블 (독립 테이블)
CREATE TABLE `department` (
                              `dept_id` INT NOT NULL COMMENT '부서 고유 번호',
                              `dept_name` varchar(30) NOT NULL COMMENT '부서명',
                              PRIMARY KEY (`dept_id`)
);

CREATE TABLE `store` (
                         `store_id` int NOT NULL COMMENT '매장고유번호',
                         `store_name` varchar(225) NOT NULL COMMENT '지점 이름',
                         `store_addr` varchar(225) NOT NULL COMMENT '지점 주소',
                         `store_tel` varchar(30) NOT NULL COMMENT '지점 연락처',
                         `store_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지점 등록 시간',
                         PRIMARY KEY (`store_id`)
);

CREATE TABLE `store_inquiry` (
                                 `inquiry_id` INT NOT NULL AUTO_INCREMENT COMMENT '문의/컴플레인 고유 번호',
                                 `store_id` INT NOT NULL COMMENT '매장 고유 번호',
                                 `inq_phone` VARCHAR(30) NOT NULL COMMENT '문의자 연락처',
                                 `inq_content` VARCHAR(255) NOT NULL COMMENT '문의/컴플레인 내용',
                                 `inq_type` TINYINT NOT NULL COMMENT '1: 컴플레인, 2: 칭찬, 3: 건의/문의',
                                 `inq_status` TINYINT NOT NULL DEFAULT 2 COMMENT '1: 완료, 2: 대기, 3: 취소/반려',
                                 `inq_level` TINYINT NULL COMMENT '1 ~ 5',
                                 `inq_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '문의 작성 일자',
                                 PRIMARY KEY (`inquiry_id`),
                                 FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`)
);

CREATE TABLE `category` (
                            `category_id` int NOT NULL AUTO_INCREMENT COMMENT '자동생성, 카테고리 id',
                            `category_name` VARCHAR(30) NOT NULL COMMENT '식품, 용품, 신선식품, 샌드위치...',
                            `category_filter` int NULL COMMENT '대분류1, 중분류2, 소분류3 - 레벨 구분용',
                            `parent_category_id` int NULL COMMENT '부모 카테고리 ID (최상위 카테고리는 NULL)',
                            PRIMARY KEY (`category_id`),
                            CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_category_id`)
                            REFERENCES `category` (`category_id`)
                            ON DELETE CASCADE ON UPDATE CASCADE
);

-- 2. 1차 의존성 테이블
CREATE TABLE `employee` (
                            `emp_id` INT NOT NULL COMMENT '사원 고유 번호',
                            `store_id` INT NULL COMMENT '매장고유번호',
                            `depart_id` INT NULL COMMENT '부서 고유번호',
                            `emp_name` VARCHAR(30) NOT NULL COMMENT '사원 이름',
                            `emp_role` varchar(30) NOT NULL COMMENT '사원 직급(ROLE)',
                            `emp_gender` TINYINT NOT NULL COMMENT '성별',
                            `emp_phone` VARCHAR(30) NOT NULL COMMENT '사원전화번호',
                            `emp_addr` VARCHAR(30) NOT NULL COMMENT '사원 주소',
                            `emp_birth` VARCHAR(30) NOT NULL COMMENT '생년월일',
                            `login_id` VARCHAR(30) NOT NULL UNIQUE COMMENT '사원 계정 ID',
                            `login_pwd` VARCHAR(30) NOT NULL COMMENT '사원 계정 비밀번호',
                            `emp_img` VARCHAR(255) NULL COMMENT '사원 프로필 사진',
                            `emp_bank` TINYINT NOT NULL COMMENT '급여 은행명',
                            `emp_acount` VARCHAR(30) NOT NULL COMMENT '급여 계좌번호',
                            `emp_status` VARCHAR(30) NOT NULL COMMENT '근무 상태',
                            `hire_date` DATETIME NOT NULL COMMENT '회원 입사일',
                            `work_type` TINYINT NOT NULL COMMENT '1.정규직 2.계약직 3.점주',
                            `email_auth` TINYINT NULL COMMENT '이메일 인증 완료 여부',
                            `emp_ext` INT NULL COMMENT '사무실 내선 번호',
                            PRIMARY KEY (`emp_id`),
                            FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                            FOREIGN KEY (`depart_id`) REFERENCES `department` (`dept_id`)
);

CREATE TABLE `product` (
                           `product_id` int NOT NULL AUTO_INCREMENT COMMENT 'autoincrement',
                           `category_id` int NOT NULL COMMENT '자동생성 , 카테고리 id',
                           `pro_name` varchar(255) NOT NULL COMMENT '제품 이름',
                           `pro_barcode` bigint NOT NULL COMMENT '바코드 넘버',
                           `pro_cost` int NOT NULL COMMENT '원가',
                           `pro_sell_cost` int NOT NULL COMMENT '판매가',
                           `pro_created_at` DATETIME NOT NULL COMMENT '생성했을때 시각',
                           `pro_update_at` DATETIME NULL COMMENT '수정했을때 시각',
                           `pro_image` varchar(225) NULL COMMENT '이미지 링크',
                           `is_promo`	TINYINT	NULL	COMMENT '1기본 2 이벤트 3 이벤트',
                           `pro_stock_limit` int NOT NULL COMMENT '발주임계치',
                           `expiration_period` INT NOT NULL COMMENT '유통기한(입고일 기준 n일)',
                           PRIMARY KEY (`product_id`),
                           FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
);

-- 3. 2차 의존성 테이블
CREATE TABLE `email_token` (
                               `etoken_id` int NOT NULL COMMENT '고유 토큰 번호',
                               `emp_id` int NOT NULL COMMENT '고유번호',
                               `etoken` varchar(100) NOT NULL COMMENT '이메일로 발송한 인증 토큰',
                               `etoken_exp` datetime NOT NULL COMMENT '유효 기간',
                               `etoken_used` boolean NOT NULL DEFAULT false COMMENT '인증 사용 여부',
                               `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '토큰 생성 시각',
                               PRIMARY KEY (`etoken_id`),
                               FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `pw_reset_token` (
                                  `prtoken_id` int NOT NULL COMMENT '고유 토큰 번호',
                                  `emp_id` int NOT NULL COMMENT '고유번호',
                                  `reset_token` varchar(100) NOT NULL COMMENT '이메일로 발송한 임시 토큰',
                                  `prtoken_exp` datetime NOT NULL COMMENT '유효 시간',
                                  `prtoken_used` boolean NOT NULL DEFAULT false COMMENT '재설정 링크 사용여부',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '토큰 발급 시간',
                                  PRIMARY KEY (`prtoken_id`),
                                  FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `store_stock` (
                               `stock_id` int NOT NULL COMMENT '재고넘버',
                               `store_id` int NOT NULL COMMENT '매장 고유번호',
                               `product_id` int NOT NULL COMMENT '상품 고유번호',
                               `quantity` int NOT NULL COMMENT '재고 갯수',
                               `last_in_date` DATETIME NULL COMMENT '입고 완료 날짜',
                               `stock_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1. 정상 2. 유통기한 임박 3. 유통기한경과 4.재고부족 5. 입고예정 7.폐기 대기',
                               PRIMARY KEY (`stock_id`),
                               FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                               FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `purchase_order` (
                                  `order_id` int NOT NULL COMMENT '발주 고유 번호',
                                  `store_id` int NOT NULL COMMENT '매장고유번호',
                                  `order_date` DATETIME NOT NULL COMMENT '발주한 등록 시간',
                                  `order_status` TINYINT NOT NULL COMMENT '1.대기,2입고대기 3.완료',
                                  `total_amount` int NOT NULL COMMENT '발주 총 금액',
                                  `total_quantity` int NOT NULL COMMENT '발주 총 갯수',
                                  PRIMARY KEY (`order_id`),
                                  FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`)
);

CREATE TABLE `purchase_order_item` (
                                       `item_id` int NOT NULL COMMENT '발주 개별코드',
                                       `order_id` int NOT NULL COMMENT '발주 고유 번호',
                                       `product_id` int NOT NULL COMMENT '상품번호',
                                       `order_quantity` int NOT NULL COMMENT '발주 수량',
                                       `unit_price` int NOT NULL COMMENT '발주 단가',
                                       `total_price` int NOT NULL COMMENT '발주 수량 *단가',
                                       `order_state` TINYINT NOT NULL COMMENT '1.발주완료 2. 발주 취소 3.입고대기 4. 입고완료',
                                       `is_abnormal` TINYINT NOT NULL COMMENT '오발주 감지여부(1.정상 ,2.이상)',
                                       `is_fully_received`TINYINT NOT NULL COMMENT '1.입고완료 2. 부분입고 3,발주 취소',
                                       `received_quantity` int NULL COMMENT '실제 입고수량',
                                       PRIMARY KEY (`item_id`),
                                       FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`order_id`),
                                       FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `disposal` (
                            `disposal_id` INT NOT NULL AUTO_INCREMENT COMMENT '폐기 넘버',
                            `stock_id` INT NOT NULL COMMENT '재고 넘버',
                            `product_id` INT NOT NULL COMMENT '상품 고유번호',
                            `disposal_date` DATETIME NOT NULL COMMENT '폐기 날짜',
                            `disposal_quantity` INT NOT NULL COMMENT '폐기 수량',
                            `processed_by` VARCHAR(30) NOT NULL COMMENT '폐기한 사람',
                            `total_loss_amount` INT NOT NULL COMMENT '폐기 총 금액',
                            `disposal_reason` VARCHAR(30) NOT NULL DEFAULT '유통기한만료' COMMENT '폐기 사유',
                            `pro_name` VARCHAR(255) NOT NULL COMMENT '상품명',  -- 상품명 추가
                            PRIMARY KEY (`disposal_id`),
                            FOREIGN KEY (`stock_id`) REFERENCES `store_stock` (`stock_id`),  -- store_stock과 연결
                            FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)  -- product와 연결
);

CREATE TABLE category_sales_stats (
                                      category_sales_stats_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리별 매출 통계 고유 ID',
                                      store_id INT NOT NULL COMMENT '매장 고유 ID',
                                      category_id INT NOT NULL COMMENT '상품 카테고리 ID',
                                      stat_date DATE NOT NULL COMMENT '매출 통계 기준 일자',
                                      total_quantity INT NOT NULL DEFAULT 0 COMMENT '카테고리 내 총 판매 수량',
                                      total_sales INT NOT NULL DEFAULT 0 COMMENT '카테고리 내 총 매출 금액',
                                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '데이터 생성 시각',

                                      UNIQUE KEY uq_store_category_date (store_id, category_id, stat_date),
                                      FOREIGN KEY (store_id) REFERENCES store(store_id),
                                      FOREIGN KEY (category_id) REFERENCES category(category_id)
);





CREATE TABLE `sales_hourly` (
                                `sales_hourly_id` INT NOT NULL COMMENT '시간대별 매출통계 고유번호',
                                `store_id` INT NOT NULL COMMENT '매장고유번호',
                                `sho_date` DATE NOT NULL COMMENT '기준 날짜',
                                `sho_hour` TINYINT NOT NULL COMMENT '0-23시(24시간 기준)',
                                `sho_quantity` INT NOT NULL DEFAULT 0 COMMENT '해당 시간에 판매된 수량',
                                `sho_total` INT NOT NULL DEFAULT 0 COMMENT '해당 시간대 총 매출액',
                                `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '데이터 생성 시간',
                                PRIMARY KEY (`sales_hourly_id`),
                                UNIQUE KEY `uq_store_hourly` (`store_id`, `sho_date`, `sho_hour`),
                                FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`)
);


CREATE TABLE `sales_stats` (
                               `sales_stats_id` int NOT NULL COMMENT '통계 고유 번호',
                               `store_id` int NOT NULL COMMENT '매장고유번호',
                               `product_id` int NOT NULL COMMENT '상품 고유 번호',
                               `sst_date` date NOT NULL COMMENT '해당 매출 날짜',
                               `sst_quantity` int NOT NULL DEFAULT 0 COMMENT '해당 날짜에 판매된 수량',
                               `sst_total` int NOT NULL DEFAULT 0 COMMENT '해당 날짜 매출 총액',
                               `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '데이터 생성 시간',
                               PRIMARY KEY (`sales_stats_id`),
                               UNIQUE KEY `uq_store_product_date` (`store_id`, `product_id`, `sst_date`),
                               FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                               FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `sales_transaction` (
                                     `transaction_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT 'POS 거래 고유 ID',
                                     `store_id` INT NOT NULL COMMENT '매장 고유번호',
                                     `emp_id` INT NULL COMMENT '결제 담당자(점주) ID',
                                     `part_timer_id` INT NULL COMMENT '결제 담당 아르바이트 ID',
                                     `total_price` INT NOT NULL COMMENT '총 상품 정가 합산',
                                     `discount_total` INT DEFAULT 0 COMMENT '총 할인 금액',
                                     `final_amount` INT NOT NULL COMMENT '최종 결제 금액 (할인 적용 후)',
                                     `payment_method` VARCHAR(20) NOT NULL COMMENT '결제 수단 (ex. 카드, 현금 등)',
                                     `transaction_status` INT NOT NULL DEFAULT 0 COMMENT '거래 상태 (0: 완료, 1: 환불, 2: 취소, 3: 실패, 4: 승인 대기)',
                                     `refund_amount` INT DEFAULT 0 COMMENT '환불 금액 (전체 환불 시 사용)',
                                     `refund_reason` VARCHAR(255) DEFAULT NULL COMMENT '환불 사유 (환불 시 작성)',
                                     `refunded_at` DATETIME DEFAULT NULL COMMENT '환불 시간',
                                     `paid_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '결제 시간',
                                     `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '거래 생성 시간',
                                     `age_group` INT NULL COMMENT '연령대',
                                     `gender` INT NULL COMMENT '성별 (0=남성, 1=여성 등)',
                                     FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                                     FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
                                     FOREIGN KEY (`part_timer_id`) REFERENCES `part_timer` (`part_timer_id`)
);

CREATE TABLE `sales_detail` (
                                `sales_detail_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '매출 상세 고유 ID',
                                `transaction_id` INT NOT NULL COMMENT '거래 ID',
                                `product_id` INT NOT NULL COMMENT '상품 ID',
                                `sales_quantity` INT NOT NULL DEFAULT 1 COMMENT '판매 수량',
                                `unit_price` INT NOT NULL COMMENT '상품 단가 (판매가)',
                                `discount_price` INT DEFAULT 0 COMMENT '할인 금액',
                                `final_amount` INT NOT NULL COMMENT '총 결제 금액 (수량*단가 - 할인)',
                                `cost_price` INT NOT NULL COMMENT '상품 원가',
                                `real_income` INT NOT NULL COMMENT '실 수익 = final_amount - cost_price',
                                `refund_amount` INT DEFAULT 0 COMMENT '환불 금액 (상품별 환불 추적용)',
                                `is_promo` INT DEFAULT 0 COMMENT '프로모션 상품 상태 (0: 기본상품, 1: 단종상품, 2: 1+1, 3: 2+1)',
                                FOREIGN KEY (`transaction_id`) REFERENCES `sales_transaction` (`transaction_id`),
                                FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `sales_settlement` (
                                    `settlement_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '매출 정산 고유 ID',
                                    `store_id` INT NOT NULL COMMENT '매장 고유번호',
                                    `emp_id` INT NULL COMMENT '점주 ID (수동 정산 시)',
                                    `part_timer_id` INT NULL COMMENT '정산 대상 아르바이트 ID (교대 정산 전용)',
                                    `settlement_date` DATE NOT NULL COMMENT '정산 기준일',
                                    `start_date` DATE NULL COMMENT '정산 시작일',
                                    `end_date` DATE NULL COMMENT '정산 종료일',
                                    `shift_start_time` DATETIME NULL COMMENT '교대 시작 시각 (교대 정산 전용)',
                                    `shift_end_time` DATETIME NULL COMMENT '교대 종료 시각 (교대 정산 전용)',
                                    `total_revenue` INT NOT NULL COMMENT '총 매출 (할인 전)',
                                    `discount_total` INT DEFAULT 0 COMMENT '총 할인 금액',
                                    `refund_total` INT DEFAULT 0 COMMENT '총 환불 금액',
                                    `final_amount` INT NOT NULL COMMENT '최종 결제 금액',
                                    `settlement_type` ENUM('DAILY', 'SHIFT', 'MONTHLY', 'YEARLY') NOT NULL COMMENT '정산 종류',
                                    `transaction_count` INT DEFAULT 0 COMMENT '총 거래 건수',
                                    `refund_count` INT DEFAULT 0 COMMENT '총 환불 건수',
                                    `is_manual` TINYINT(1) DEFAULT 0 COMMENT '정산 유형 (0: 자동, 1: 수동)',
                                    `hq_sent_at` DATETIME DEFAULT NULL COMMENT '본사 전송 시각',
                                    `hq_status` ENUM('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING' COMMENT '본사 전송 상태',
                                    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '정산 생성 시각',
                                    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정산 수정 시각',
                                    FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                                    FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
                                    FOREIGN KEY (`part_timer_id`) REFERENCES `part_timer` (`part_timer_id`),

                                    UNIQUE KEY `unique_store_date_type` (`store_id`, `settlement_date`, `settlement_type`)

);

CREATE TABLE `sales_statistics` (
                                    `stats_id` INT NOT NULL COMMENT '통계 ID',
                                    `store_id` int NOT NULL COMMENT '매장고유번호',
                                    `category_id` int NOT NULL COMMENT '자동생성 , 카테고리 id',
                                    `date` DATE NOT NULL COMMENT '날짜',
                                    `hour` DATETIME NULL COMMENT '시간 (시간별 통계 시)',
                                    `total_sales` DECIMAL(10,2) NOT NULL COMMENT '총 매출액',
                                    `transaction_count` INT NOT NULL COMMENT '거래 건수',
                                    `avg_transaction` DECIMAL(10,2) NOT NULL COMMENT '평균 거래액',
                                    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
                                    PRIMARY KEY (`stats_id`),
                                    FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                                    FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
);

CREATE TABLE `inventory_statistics` (
                                        `stats_id` INT NOT NULL COMMENT '통계 ID',
                                        `store_id` int NOT NULL COMMENT '매장고유번호',
                                        `category_id` int NOT NULL COMMENT '자동생성 , 카테고리 id',
                                        `inven_date` DATE NOT NULL COMMENT '날짜',
                                        `inven_turnover_rate` DECIMAL(5,2) NOT NULL COMMENT '재고 회전율',
                                        `inven_stock_value` DECIMAL(10,2) NOT NULL COMMENT '재고 가치',
                                        `inven_low_stock_count` INT NOT NULL COMMENT '부족 재고 상품 수',
                                        `inven_excess_stock_count` INT NOT NULL COMMENT '과잉 재고 상품 수',
                                        `inven_expired_soon_count` INT NOT NULL COMMENT '유통기한 임박 상품 수',
                                        `inven_created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
                                        PRIMARY KEY (`stats_id`),
                                        FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                                        FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
);

CREATE TABLE `order_stats` (
                               `ostats_id` int NOT NULL COMMENT '발주 통계 고유 번호',
                               `store_id` int NOT NULL COMMENT '매장고유번호',
                               `product_id` int NOT NULL COMMENT 'autoincrement',
                               `ostats_date` date NOT NULL COMMENT '발주한 날짜',
                               `ostats_quantity` int NOT NULL DEFAULT 0 COMMENT '발주된 수량',
                               `ostats_total` int NOT NULL DEFAULT 0 COMMENT '총 발주된 금액',
                               `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '데이터 생성 시간',
                               PRIMARY KEY (`ostats_id`),
                               UNIQUE KEY `uq_store_product_date` (`store_id`, `product_id`, `ostats_date`),
                               FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                               FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

CREATE TABLE `demand_prediction` (
                                     `prediction_id` INT NOT NULL COMMENT '예측 ID',
                                     `store_id` int NOT NULL COMMENT '매장고유번호',
                                     `product_id` int NOT NULL COMMENT 'autoincrement',
                                     `dmd_date` DATE NOT NULL COMMENT '예측 날짜',
                                     `dmd_predicted_quantity` INT NOT NULL COMMENT '예측 수량',
                                     `dmd_confidence_level` DECIMAL(5,2) NOT NULL COMMENT '신뢰도 (0-1)',
                                     `dmd_weather_factor` DECIMAL(5,2) NULL COMMENT '날씨 요인',
                                     `dmd_seasonal_factor` DECIMAL(5,2) NULL COMMENT '계절 요인',
                                     `dmd_created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
                                     PRIMARY KEY (`prediction_id`),
                                     FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                                     FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);


CREATE TABLE `annual_leave` (
                                `leave_id` INT NOT NULL COMMENT '연차번호',
                                `emp_id` INT NOT NULL COMMENT '사원 고유 번호',
                                `year` INT NOT NULL COMMENT '근속 연도',
                                `total_days` INT NOT NULL COMMENT '보유 연차 총합',
                                `used_days` INT NOT NULL COMMENT '사용한연차',
                                `rem_days` INT NULL COMMENT '잔여연차 개수',
                                `uadate_at` DATETIME NULL COMMENT '최근 수정 날짜',
                                PRIMARY KEY (`leave_id`),
                                FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `leave_req` (
                             `req_id` INT NOT NULL COMMENT '연차신청고유번호',
                             `emp_id` INT NOT NULL COMMENT '사원 고유 번호',
                             `req_date` DATE NOT NULL COMMENT '요청날짜',
                             `req_reason` VARCHAR(255) NULL COMMENT '연차사유',
                             `req_status` TINYINT NULL DEFAULT 0 COMMENT '0 : 실패, 2 성공',
                             `created_at` DATETIME NULL COMMENT '생성날짜',
                             PRIMARY KEY (`req_id`),
                             FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `appr_log` (
                            `log_id` INT NOT NULL COMMENT '로그 고유번호',
                            `req_id` INT NOT NULL COMMENT '연차신청고유번호',
                            `emp_id` INT NOT NULL COMMENT '사원고유번호',
                            `appr_status` TINYINT NOT NULL COMMENT '1.승인2. 반려3. 대기',
                            `appr_at` DATETIME NOT NULL COMMENT '결제시간',
                            `note` VARCHAR(255) NULL COMMENT '사유',
                            PRIMARY KEY (`log_id`),
                            FOREIGN KEY (`req_id`) REFERENCES `leave_req` (`req_id`),
                            FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `dashboard_layout` (
                                    `layout_id` INT NOT NULL COMMENT '레이아웃 ID',
                                    `emp_id` int NOT NULL COMMENT '고유번호',
                                    `dash_widget_code` VARCHAR(30) NOT NULL COMMENT '위젯 코드',
                                    `dash_grid_positions` VARCHAR(50) NOT NULL COMMENT '위치 번호들 (쉼표로 구분, 예: "1,2,6,7")',
                                    `dash_created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
                                    `dash_updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
                                    PRIMARY KEY (`layout_id`),
                                    FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `tbl_board_posts` (
                                   `post_id` INT NOT NULL AUTO_INCREMENT COMMENT '게시글 고유 번호 (기본 키)',
                                   `emp_id` int NOT NULL COMMENT '사원 고유 번호',
                                   `board_type` INT NOT NULL COMMENT '공지시항 , 건의사항, 점포문의 분리코드',
                                   `board_title` VARCHAR(255) NOT NULL COMMENT '게시글 제목',
                                   `board_content` VARCHAR(255) NOT NULL COMMENT '게시글 본문 내용',
                                   `board_created_at` DATETIME NOT NULL COMMENT '게시글 작성 시간',
                                   PRIMARY KEY (`post_id`),
                                   FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);

CREATE TABLE `tbl_board_comments` (
                                      `comment_id` INT NOT NULL AUTO_INCREMENT COMMENT '답변 고유 번호 (기본 키)',
                                      `post_id` INT NOT NULL COMMENT '게시글 고유 번호 (기본 키)',
                                      `com_content` VARCHAR(255) NOT NULL COMMENT '답변 본문 내용',
                                      `com_created_at` DATETIME NOT NULL COMMENT '답변 작성 시간',
                                      PRIMARY KEY (`comment_id`),
                                      FOREIGN KEY (`post_id`) REFERENCES `tbl_board_posts` (`post_id`)
);

CREATE TABLE `part_timer` (
                              `part_timer_id`	int	NOT NULL	COMMENT '아르바이트 고유 ID',
                              `store_id`	int	NOT NULL	COMMENT '매장고유번호',
                              `part_name`	VARCHAR(50)	NOT NULL	COMMENT '이름',
                              `part_gender`	TINYINT	NOT NULL	COMMENT '0 : 남, 1 여',
                              `part_phone`	VARCHAR(30)	NOT NULL	COMMENT '연락처',
                              `part_addres`	varchar(50)	NOT NULL	COMMENT '주소',
                              `birth_date`	DATE	NOT NULL	COMMENT '생년월일',
                              `hire_date`	DATETIME	NOT NULL	COMMENT '입사일',
                              `resign_date`	DATETIME	NULL	COMMENT '퇴사일',
                              `salary_type`	TINYINT	NOT NULL	COMMENT '급여 형태 (1: 시급, 2: 월급)',
                              `hourly_wage`	INT	NULL	COMMENT '시급 (시급제일 경우 필수)',
                              `account_bank`	VARCHAR(30)	NOT NULL	COMMENT '급여 은행명',
                              `account_number`	VARCHAR(30)	NOT NULL	COMMENT '급여 계좌번호',
                              `part_status`	TINYINT	NOT NULL	DEFAULT 1	COMMENT '재직 상태 (1: 재직, 2: 퇴사, 3: 휴직)',
                              `created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '등록 시각',
                              PRIMARY KEY (`part_timer_id`),
                              FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`)

);

CREATE TABLE `shift_schedule` (
                                  `schedule_id` int NOT NULL COMMENT '스케줄 고유 번호',
                                  `part_timer_id`	int	NOT NULL	COMMENT '아르바이트 고유 ID',
                                  `work_date` DATETIME NOT NULL COMMENT '근무시작일',
                                  `start_time` DATETIME NOT NULL COMMENT '근무 시작시간',
                                  `end_time` DATETIME NOT NULL COMMENT '근무 마감시간',
                                  PRIMARY KEY (`schedule_id`),
                                  FOREIGN KEY (`part_timer_id`) REFERENCES `part_timer` (`part_timer_id`)
);

CREATE TABLE `salary` (
                          `salary_id` INT NOT NULL COMMENT '급여 고유 번호',
                          `emp_id` INT NULL COMMENT '사원 고유 번호',
                          `part_timer_id`	int	NULL	COMMENT '아르바이트 고유 ID',
                          `store_id`	int NULL	COMMENT '매장고유번호',
                          `calculated_at` DATETIME NOT NULL COMMENT '정산날짜',
                          `base_salary` INT NOT NULL COMMENT '기본급 (월급 or 시급 * 총 근무시간)',
                          `bonus` INT NOT NULL COMMENT '상여급',
                          `deduct_total` INT NOT NULL COMMENT '공제 금액 (4대보험, 지각/결근 등)',
                          `deduct_extra` INT NULL COMMENT '기타공제금액',
                          `net_salary` INT NOT NULL COMMENT '실 수령액',
                          `pay_date` DATETIME NOT NULL COMMENT '급여일자',
                          `pay_status` TINYINT NOT NULL COMMENT '1.지급대기 2. 지급완료',
                          PRIMARY KEY (`salary_id`),
                          FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
                          FOREIGN KEY (`part_timer_id`) REFERENCES `part_timer` (`part_timer_id`),
                          FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`)

);
CREATE TABLE `attendance` (
                              `attend_id` INT NOT NULL COMMENT '근태 기록 고유번호',
                              `emp_id` INT NULL COMMENT '사원 고유 번호',
                              `leave_id` INT NULL COMMENT '연차번호',
                              `part_timer_id`	int	NULL	COMMENT '아르바이트 고유 ID',
                              `store_id`	int NULL	COMMENT '매장고유번호',
                              `work_date` DATETIME NOT NULL COMMENT '근무일자',
                              `in_time` DATETIME NOT NULL COMMENT '출근시간',
                              `out_time` DATETIME NULL COMMENT '퇴근시간',
                              `attend_status` TINYINT NOT NULL COMMENT '1: 출근, 2: 지각, 3: 조퇴, 4: 결근, 5: 연차, 6: 병가',
                              PRIMARY KEY (`attend_id`),
                              FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
                              FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
                              FOREIGN KEY (`part_timer_id`) REFERENCES `part_timer` (`part_timer_id`),
                              FOREIGN KEY (`leave_id`) REFERENCES `annual_leave` (`leave_id`)
);

CREATE TABLE `product_details` (
                                   `pro_detail_id`	INT	NOT NULL AUTO_INCREMENT,
                                   `product_id`	int	NOT NULL	COMMENT 'autoincrement',
                                   `manufacturer`	VARCHAR(100)	NOT NULL	COMMENT '제조사',
                                   `manu_num`	VARCHAR(30)	NULL	COMMENT '제조사번호',
                                   `shelf_life`	VARCHAR(50)	NULL	COMMENT '제조일로부터 12개월..',
                                   `allergens`	VARCHAR(255)	NULL	COMMENT '알러지',
                                   `storage_method`	VARCHAR(100)	NULL	COMMENT '보관방법, 냉동냉장,..',
                                   PRIMARY KEY (`pro_detail_id`),
                                   FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
);

-- 알림 테이블 생성
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    `user_id` INT NOT NULL COMMENT '사용자 ID',
    `target_dept_id` INT NULL COMMENT '대상 부서 ID',
    `event_type` VARCHAR(50) NULL COMMENT '알림 이벤트 타입',
    `type` VARCHAR(20) NOT NULL COMMENT '알림 타입',
    `content` VARCHAR(255) NOT NULL COMMENT '알림 내용',
    `link` VARCHAR(255) NULL COMMENT '이동할 링크',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `employee` (`emp_id`)
) COMMENT='사용자 알림'; 

