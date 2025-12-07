-- PostgreSQL 테스트 테이블 생성 (mesuser 스키마 사용)
-- psql -h target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com -U mesuser -d target -f 02_postgresql_mesuser_test.sql

-- 테스트 테이블 생성 (전처리 후 구조)
CREATE TABLE test_product (
    product_id bigint PRIMARY KEY,
    product_code varchar(50) NOT NULL,
    product_name varchar(200) NOT NULL,-- 전처리 완료: Boolean으로 변환
    is_active boolean, -- 전처리 완료: DATE로 변환
    created_date date, -- 전처리 완료: 숫자로 변환
    price numeric(15,2), -- 전처리 완료: JSONB로 변환
    metadata_json jsonb, -- 전처리 완료: 배열로 변환
    tags text[], -- 전처리 완료: 초 단위로 변환
    created_at timestamp(0)
);

-- 확인
\d mesuser.test_product
