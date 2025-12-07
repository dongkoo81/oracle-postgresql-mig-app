-- Oracle 테스트 테이블 생성 (mesuser 계정 사용)
-- sqlplus mesuser/mespass@10.1.5.18:1521/oracle19c @01_oracle_mesuser_test.sql

-- 시퀀스 생성
CREATE SEQUENCE test_product_seq START WITH 1 INCREMENT BY 1;

-- 테스트 테이블 (전처리 필요한 케이스)
CREATE TABLE test_product (
    product_id NUMBER(19) PRIMARY KEY,
    product_code VARCHAR2(50) NOT NULL,
    product_name VARCHAR2(200) NOT NULL, -- 전처리 케이스 1: Y/N을 Boolean으로 변환
    is_active CHAR(1) DEFAULT 'Y', -- 전처리 케이스 2: 날짜 문자열을 DATE로 변환
    created_date_str VARCHAR2(20),
    created_date DATE, -- 전처리 케이스 3: 금액 문자열을 숫자로 변환
    price_str VARCHAR2(50),
    price NUMBER(15,2), -- 전처리 케이스 4: JSON 문자열
    metadata_json CLOB, -- 전처리 케이스 5: 구분자로 연결된 태그
    tags VARCHAR2(500),-- 전처리 케이스 6: 밀리초 → 초 단위
    created_at TIMESTAMP(6)
);

-- 샘플 데이터 삽입
INSERT INTO test_product VALUES (
    test_product_seq.NEXTVAL,
    'PROD001',
    'Test Product 1',
    'Y',
    '2025-12-03',
    TO_DATE('2025-12-03', 'YYYY-MM-DD'),
    '1,234,567.89',
    1234567.89,
    '{"category": "electronics", "brand": "Samsung"}',
    'tag1,tag2,tag3',
    SYSTIMESTAMP
);

INSERT INTO test_product VALUES (
    test_product_seq.NEXTVAL,
    'PROD002',
    'Test Product 2',
    'N',
    '2025/12/03',
    TO_DATE('2025-12-03', 'YYYY-MM-DD'),
    '$2,500.00',
    2500.00,
    '{"category": "furniture", "brand": "IKEA"}',
    'tag4,tag5',
    SYSTIMESTAMP
);

INSERT INTO test_product VALUES (
    test_product_seq.NEXTVAL,
    'PROD003',
    'Test Product 3',
    'Y',
    '03-DEC-2025',
    TO_DATE('2025-12-03', 'YYYY-MM-DD'),
    '999999',
    999999.00,
    '{"category": "clothing", "brand": "Nike", "size": ["S", "M", "L"]}',
    'tag6',
    SYSTIMESTAMP
);

COMMIT;

-- 확인
SELECT * FROM test_product;

EXIT;
