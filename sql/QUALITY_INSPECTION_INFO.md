# QUALITY_INSPECTION 테이블 - 파티션 테이블

## 개요

품질 검사 이력을 저장하는 파티션 테이블입니다. Oracle의 Composite Partition 기능을 활용하여 Range(월별) + List(결과별) 파티션을 구성합니다.

## 테이블 구조

```sql
CREATE TABLE QUALITY_INSPECTION (
    INSPECTION_ID NUMBER(19) NOT NULL,           -- 검사 ID (Sequence)
    PRODUCT_ID NUMBER(19) NOT NULL,              -- 제품 ID (FK)
    ORDER_ID NUMBER(19),                         -- 주문 ID (FK, Optional)
    INSPECTION_DATE DATE NOT NULL,               -- 검사 일자
    RESULT VARCHAR2(20) NOT NULL,                -- 검사 결과 (PASS/FAIL/PENDING)
    DEFECT_COUNT NUMBER(10) DEFAULT 0,           -- 불량 개수
    INSPECTOR_NAME VARCHAR2(100),                -- 검사자 이름
    NOTES CLOB,                                  -- 검사 비고
    CONSTRAINT PK_QUALITY_INSPECTION PRIMARY KEY (INSPECTION_ID, INSPECTION_DATE, RESULT)
)
PARTITION BY RANGE (INSPECTION_DATE)
SUBPARTITION BY LIST (RESULT)
```

## 파티션 전략

### Range Partition (월별)
- 2024년 12월 ~ 2025년 12월 (13개월)
- MAXVALUE 파티션 포함

### List Subpartition (결과별)
- **PASS**: 합격
- **FAIL**: 불합격
- **PENDING**: 검사 대기

### 파티션 예시
```
p_202412_sp_pass      : 2024년 12월 합격 데이터
p_202412_sp_fail      : 2024년 12월 불합격 데이터
p_202412_sp_pending   : 2024년 12월 대기 데이터
p_202501_sp_pass      : 2025년 1월 합격 데이터
...
```

## 인덱스

```sql
CREATE INDEX IDX_INSPECTION_PRODUCT ON QUALITY_INSPECTION(PRODUCT_ID) LOCAL;
CREATE INDEX IDX_INSPECTION_ORDER ON QUALITY_INSPECTION(ORDER_ID) LOCAL;
CREATE INDEX IDX_INSPECTION_DATE ON QUALITY_INSPECTION(INSPECTION_DATE) LOCAL;
```

모든 인덱스는 LOCAL 인덱스로 각 파티션별로 생성됩니다.

## 샘플 데이터

```sql
-- 합격 데이터
INSERT INTO QUALITY_INSPECTION (INSPECTION_ID, PRODUCT_ID, ORDER_ID, INSPECTION_DATE, RESULT, DEFECT_COUNT, INSPECTOR_NAME, NOTES)
VALUES (QUALITY_INSPECTION_SEQ.NEXTVAL, 1, 1, SYSDATE, 'PASS', 0, 'John Smith', 'All quality checks passed');

-- 불합격 데이터
INSERT INTO QUALITY_INSPECTION (INSPECTION_ID, PRODUCT_ID, ORDER_ID, INSPECTION_DATE, RESULT, DEFECT_COUNT, INSPECTOR_NAME, NOTES)
VALUES (QUALITY_INSPECTION_SEQ.NEXTVAL, 3, 2, SYSDATE, 'FAIL', 3, 'Mike Johnson', 'Minor surface scratches detected');

-- 대기 데이터
INSERT INTO QUALITY_INSPECTION (INSPECTION_ID, PRODUCT_ID, ORDER_ID, INSPECTION_DATE, RESULT, DEFECT_COUNT, INSPECTOR_NAME, NOTES)
VALUES (QUALITY_INSPECTION_SEQ.NEXTVAL, 1, 2, SYSDATE, 'PENDING', 0, 'Sarah Lee', 'Awaiting final approval');
```

## 파티션 조회 쿼리

### 1. 특정 월의 합격 데이터만 조회
```sql
SELECT * FROM QUALITY_INSPECTION
WHERE INSPECTION_DATE >= TO_DATE('2024-12-01', 'YYYY-MM-DD')
  AND INSPECTION_DATE < TO_DATE('2025-01-01', 'YYYY-MM-DD')
  AND RESULT = 'PASS';
```

### 2. 파티션 정보 확인
```sql
SELECT TABLE_NAME, PARTITION_NAME, SUBPARTITION_NAME, HIGH_VALUE, NUM_ROWS
FROM USER_TAB_SUBPARTITIONS
WHERE TABLE_NAME = 'QUALITY_INSPECTION'
ORDER BY PARTITION_NAME, SUBPARTITION_NAME;
```

### 3. 월별 합격률 통계
```sql
SELECT 
    TO_CHAR(INSPECTION_DATE, 'YYYY-MM') AS MONTH,
    RESULT,
    COUNT(*) AS COUNT,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY TO_CHAR(INSPECTION_DATE, 'YYYY-MM')), 2) AS PERCENTAGE
FROM QUALITY_INSPECTION
GROUP BY TO_CHAR(INSPECTION_DATE, 'YYYY-MM'), RESULT
ORDER BY MONTH, RESULT;
```

### 4. 제품별 불량률
```sql
SELECT 
    p.PRODUCT_NAME,
    COUNT(*) AS TOTAL_INSPECTIONS,
    SUM(CASE WHEN qi.RESULT = 'FAIL' THEN 1 ELSE 0 END) AS FAIL_COUNT,
    ROUND(SUM(CASE WHEN qi.RESULT = 'FAIL' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS FAIL_RATE
FROM QUALITY_INSPECTION qi
JOIN PRODUCT p ON qi.PRODUCT_ID = p.PRODUCT_ID
GROUP BY p.PRODUCT_NAME
ORDER BY FAIL_RATE DESC;
```

### 5. 검사자별 실적
```sql
SELECT 
    INSPECTOR_NAME,
    COUNT(*) AS TOTAL_INSPECTIONS,
    SUM(CASE WHEN RESULT = 'PASS' THEN 1 ELSE 0 END) AS PASS_COUNT,
    SUM(CASE WHEN RESULT = 'FAIL' THEN 1 ELSE 0 END) AS FAIL_COUNT,
    SUM(CASE WHEN RESULT = 'PENDING' THEN 1 ELSE 0 END) AS PENDING_COUNT
FROM QUALITY_INSPECTION
WHERE INSPECTION_DATE >= TRUNC(SYSDATE, 'MM')
GROUP BY INSPECTOR_NAME
ORDER BY TOTAL_INSPECTIONS DESC;
```

## 파티션 관리

### 새 파티션 추가 (2026년 1월)
```sql
ALTER TABLE QUALITY_INSPECTION
SPLIT PARTITION p_max AT (TO_DATE('2026-02-01', 'YYYY-MM-DD'))
INTO (
    PARTITION p_202601,
    PARTITION p_max
);
```

### 오래된 파티션 삭제 (2024년 12월)
```sql
ALTER TABLE QUALITY_INSPECTION DROP PARTITION p_202412;
```

### 파티션 데이터 아카이빙
```sql
-- 1. 아카이브 테이블 생성
CREATE TABLE QUALITY_INSPECTION_ARCHIVE AS
SELECT * FROM QUALITY_INSPECTION PARTITION (p_202412);

-- 2. 파티션 삭제
ALTER TABLE QUALITY_INSPECTION DROP PARTITION p_202412;
```

## 성능 최적화

### Partition Pruning (파티션 제거)
Oracle은 WHERE 절의 조건에 따라 불필요한 파티션을 자동으로 제외합니다.

```sql
-- 2024년 12월 PASS 데이터만 스캔 (다른 파티션은 스캔 안 함)
SELECT * FROM QUALITY_INSPECTION
WHERE INSPECTION_DATE BETWEEN TO_DATE('2024-12-01', 'YYYY-MM-DD') 
                          AND TO_DATE('2024-12-31', 'YYYY-MM-DD')
  AND RESULT = 'PASS';
```

### 실행 계획 확인
```sql
EXPLAIN PLAN FOR
SELECT * FROM QUALITY_INSPECTION
WHERE INSPECTION_DATE >= TO_DATE('2024-12-01', 'YYYY-MM-DD')
  AND RESULT = 'PASS';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

## DMS 마이그레이션 고려사항

### PostgreSQL 변환
PostgreSQL은 Composite Partition을 지원하지만 문법이 다릅니다:

```sql
-- PostgreSQL 17 버전
CREATE TABLE quality_inspection (
    inspection_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    inspection_date DATE NOT NULL,
    result VARCHAR(20) NOT NULL,
    defect_count INTEGER DEFAULT 0,
    inspector_name VARCHAR(100),
    notes TEXT,
    PRIMARY KEY (inspection_id, inspection_date, result)
) PARTITION BY RANGE (inspection_date);

-- 파티션 생성
CREATE TABLE quality_inspection_202412 PARTITION OF quality_inspection
FOR VALUES FROM ('2024-12-01') TO ('2025-01-01')
PARTITION BY LIST (result);

CREATE TABLE quality_inspection_202412_pass PARTITION OF quality_inspection_202412
FOR VALUES IN ('PASS');

CREATE TABLE quality_inspection_202412_fail PARTITION OF quality_inspection_202412
FOR VALUES IN ('FAIL');

CREATE TABLE quality_inspection_202412_pending PARTITION OF quality_inspection_202412
FOR VALUES IN ('PENDING');
```

### DMS 변환 규칙
- Oracle CLOB → PostgreSQL TEXT
- Oracle NUMBER(19) → PostgreSQL BIGINT
- Oracle DATE → PostgreSQL DATE
- Partition 구조는 수동 재생성 필요

## 비즈니스 활용

1. **품질 관리**: 제품별 불량률 추적
2. **검사자 평가**: 검사자별 실적 분석
3. **트렌드 분석**: 월별 품질 추이 파악
4. **재작업 관리**: FAIL 데이터 기반 재작업 지시
5. **규정 준수**: 품질 검사 이력 보관 (법적 요구사항)

## 참고 자료

- [Oracle Partitioning Guide](https://docs.oracle.com/en/database/oracle/oracle-database/19/vldbg/partition-concepts.html)
- [PostgreSQL Partitioning](https://www.postgresql.org/docs/17/ddl-partitioning.html)
