# DMS 전처리 케이스 상세 설명

## 케이스 1: Y/N → Boolean 변환

### 배경
레거시 시스템에서 boolean 값을 CHAR(1)로 저장하는 경우가 많음
- 'Y' = Yes = True
- 'N' = No = False
- 'A' = Active, 'I' = Inactive 등 다양한 변형 존재

### Oracle 구조
```sql
is_active CHAR(1) DEFAULT 'Y'
CHECK (is_active IN ('Y', 'N'))
```

### DMS 자동 변환
```sql
-- PostgreSQL
is_active char(1) DEFAULT 'Y'
-- 문자 그대로 복사
```

### 문제점
```sql
-- PostgreSQL에서 boolean 쿼리 불가
SELECT * FROM product WHERE is_active = true;  -- ❌ 에러

-- 문자열로 비교해야 함
SELECT * FROM product WHERE is_active = 'Y';   -- ✓ 가능
```

### Glue ETL 전처리
```python
.withColumn(
    "is_active",
    when(col("IS_ACTIVE") == "Y", True)
    .when(col("IS_ACTIVE") == "N", False)
    .otherwise(None)
)
```

### PostgreSQL 결과
```sql
is_active boolean
-- true/false로 저장

SELECT * FROM product WHERE is_active = true;  -- ✓ 정상 동작
```

---

## 케이스 2: 날짜 문자열 → DATE 변환

### 배경
외부 시스템 연동 시 날짜를 문자열로 받는 경우
- CSV 파일 import
- API 응답 (JSON)
- 레거시 시스템 인터페이스

### Oracle 구조
```sql
created_date_str VARCHAR2(20)  -- '2025-12-03', '2025/12/03', '03-DEC-2025' 등
created_date DATE              -- 실제 날짜 타입
```

**샘플 데이터**:
```
created_date_str     | created_date
---------------------|-------------
'2025-12-03'         | 2025-12-03
'2025/12/03'         | 2025-12-03
'03-DEC-2025'        | 2025-12-03
'20251203'           | 2025-12-03
'12/03/2025'         | 2025-12-03 (미국 형식)
```

### DMS 자동 변환
```sql
-- PostgreSQL
created_date_str varchar(20)  -- 문자열 그대로
created_date timestamp         -- DATE → timestamp 변환
```

### 문제점
```sql
-- 문자열 날짜는 정렬/비교 불가
SELECT * FROM product 
WHERE created_date_str > '2025-12-01'  -- ❌ 문자열 비교 (잘못된 결과)

-- 다양한 형식 혼재 시 처리 불가
'2025-12-03' vs '03-DEC-2025'  -- 같은 날짜지만 문자열로는 다름
```

### Glue ETL 전처리
```python
# 다양한 날짜 형식 통일
.withColumn(
    "created_date",
    coalesce(
        to_date(col("CREATED_DATE_STR"), "yyyy-MM-dd"),
        to_date(col("CREATED_DATE_STR"), "yyyy/MM/dd"),
        to_date(col("CREATED_DATE_STR"), "dd-MMM-yyyy"),
        to_date(col("CREATED_DATE_STR"), "yyyyMMdd"),
        to_date(col("CREATED_DATE_STR"), "MM/dd/yyyy")
    )
)
```

### PostgreSQL 결과
```sql
created_date date
-- 모든 형식이 date 타입으로 통일

SELECT * FROM product 
WHERE created_date > '2025-12-01'  -- ✓ 정상 날짜 비교
```

---

## 케이스 3: 금액 문자열 → Numeric 변환

### 배경
사용자 입력이나 외부 시스템에서 금액을 문자열로 받는 경우
- 쉼표 포함: '1,234,567.89'
- 통화 기호 포함: '$2,500.00', '₩1,000,000'
- 공백 포함: '1 234 567.89'

### Oracle 구조
```sql
price_str VARCHAR2(50)   -- 문자열 금액
price NUMBER(15,2)       -- 실제 숫자
```

**샘플 데이터**:
```
price_str          | price
-------------------|------------
'1,234,567.89'     | 1234567.89
'$2,500.00'        | 2500.00
'₩1,000,000'       | 1000000.00
'999999'           | 999999.00
'1 234.56'         | 1234.56
```

### DMS 자동 변환
```sql
-- PostgreSQL
price_str varchar(50)    -- 문자열 그대로
price numeric(15,2)      -- 숫자는 변환됨
```

### 문제점
```sql
-- 문자열 금액은 계산 불가
SELECT SUM(price_str) FROM product;  -- ❌ 에러

-- 쉼표/기호 제거 필요
SELECT price_str::numeric FROM product;  -- ❌ 에러 (쉼표 때문에)
```

### Glue ETL 전처리
```python
# 숫자가 아닌 모든 문자 제거 (소수점 제외)
.withColumn(
    "price",
    regexp_replace(col("PRICE_STR"), "[^0-9.]", "").cast("decimal(15,2)")
)
```

### PostgreSQL 결과
```sql
price numeric(15,2)
-- 순수 숫자로 저장

SELECT SUM(price) FROM product;  -- ✓ 정상 계산
```

---

## 케이스 4: JSON 문자열 → JSONB 변환

### 배경
NoSQL 데이터를 RDBMS에 저장하는 경우
- API 응답 저장
- 동적 속성 (EAV 패턴 대체)
- 메타데이터 저장

### Oracle 구조
```sql
metadata_json CLOB
-- JSON 문자열로 저장
```

**샘플 데이터**:
```json
{"category": "electronics", "brand": "Samsung", "warranty": 24}
{"category": "furniture", "brand": "IKEA", "dimensions": {"width": 100, "height": 200}}
{"category": "clothing", "brand": "Nike", "sizes": ["S", "M", "L", "XL"]}
```

### DMS 자동 변환
```sql
-- PostgreSQL
metadata_json text
-- 문자열 그대로
```

### 문제점
```sql
-- JSON 쿼리 불가
SELECT * FROM product 
WHERE metadata_json->>'category' = 'electronics';  -- ❌ 에러 (text 타입)

-- 인덱싱 불가
CREATE INDEX idx_category ON product ((metadata_json->>'category'));  -- ❌ 에러
```

### Glue ETL 전처리
```python
# JSON 문자열을 그대로 전달 (PostgreSQL에서 자동 파싱)
.withColumn(
    "metadata_json",
    col("METADATA_JSON")
)
```

### PostgreSQL 결과
```sql
metadata_json jsonb
-- JSONB 타입으로 저장

-- JSON 쿼리 가능
SELECT * FROM product 
WHERE metadata_json->>'category' = 'electronics';  -- ✓ 정상 동작

-- GIN 인덱스 생성 가능
CREATE INDEX idx_metadata ON product USING GIN (metadata_json);

-- 중첩 쿼리 가능
SELECT * FROM product 
WHERE metadata_json->'dimensions'->>'width' = '100';
```

---

## 케이스 5: 구분자 문자열 → 배열 변환

### 배경
다대다 관계를 단순하게 저장하는 경우
- 태그 (tag1, tag2, tag3)
- 카테고리 (전자제품, 가전, 스마트폰)
- 권한 (read, write, delete)

### Oracle 구조
```sql
tags VARCHAR2(500)
-- 쉼표로 구분된 문자열
```

**샘플 데이터**:
```
tags
-----------------------
'tag1,tag2,tag3'
'electronics,samsung'
'furniture,wood,modern'
'sale,new,popular'
```

### DMS 자동 변환
```sql
-- PostgreSQL
tags varchar(500)
-- 문자열 그대로
```

### 문제점
```sql
-- 특정 태그 검색 어려움
SELECT * FROM product 
WHERE tags LIKE '%tag1%';  -- ⚠️ 'tag10', 'tag11'도 검색됨

-- 태그 개수 계산 불가
SELECT COUNT(*) FROM product WHERE ...  -- 태그별 집계 불가
```

### Glue ETL 전처리
```python
# 쉼표로 분리하여 배열 생성
.withColumn(
    "tags",
    split(col("TAGS"), ",")
)
```

### PostgreSQL 결과
```sql
tags text[]
-- 배열로 저장

-- 배열 연산 가능
SELECT * FROM product 
WHERE 'tag1' = ANY(tags);  -- ✓ 정확한 검색

-- 배열 함수 사용
SELECT 
    product_code,
    array_length(tags, 1) AS tag_count,
    tags[1] AS first_tag
FROM product;

-- GIN 인덱스 생성
CREATE INDEX idx_tags ON product USING GIN (tags);

-- 배열 확장 (unnest)
SELECT product_code, unnest(tags) AS tag
FROM product;
```

---

## 케이스 6: 밀리초 → 초 단위 변환 (신규)

### 배경
Oracle TIMESTAMP는 밀리초(또는 마이크로초)까지 저장
PostgreSQL에서 초 단위만 필요한 경우 (로그, 이력 데이터)

### Oracle 구조
```sql
created_at TIMESTAMP(6)  -- 마이크로초까지 저장
updated_at TIMESTAMP(3)  -- 밀리초까지 저장
```

**샘플 데이터**:
```
created_at                      | updated_at
--------------------------------|-------------------------
2025-12-03 10:05:30.123456      | 2025-12-03 10:05:30.123
2025-12-03 10:05:31.987654      | 2025-12-03 10:05:31.987
2025-12-03 10:05:32.000001      | 2025-12-03 10:05:32.000
```

### DMS 자동 변환
```sql
-- PostgreSQL
created_at timestamp(6)  -- 마이크로초 그대로
updated_at timestamp(3)  -- 밀리초 그대로
```

### 문제점
```sql
-- 불필요한 정밀도로 저장 공간 낭비
-- 비교 시 밀리초 차이로 인한 불일치
SELECT * FROM product 
WHERE created_at = '2025-12-03 10:05:30';  -- ❌ 밀리초 때문에 매칭 안 됨
```

### Glue ETL 전처리
```python
# 초 단위로 절삭
.withColumn(
    "created_at",
    date_trunc("second", col("CREATED_AT"))
)
.withColumn(
    "updated_at",
    date_trunc("second", col("UPDATED_AT"))
)
```

### PostgreSQL 결과
```sql
created_at timestamp(0)  -- 초 단위만
updated_at timestamp(0)  -- 초 단위만

-- 결과
created_at           | updated_at
---------------------|--------------------
2025-12-03 10:05:30  | 2025-12-03 10:05:30
2025-12-03 10:05:31  | 2025-12-03 10:05:31
2025-12-03 10:05:32  | 2025-12-03 10:05:32

-- 정확한 비교 가능
SELECT * FROM product 
WHERE created_at = '2025-12-03 10:05:30';  -- ✓ 정상 매칭
```

---

## 전체 케이스 요약

| 케이스 | Oracle | DMS 변환 | Glue 전처리 | PostgreSQL 최종 |
|--------|--------|----------|-------------|----------------|
| 1 | CHAR(1) 'Y'/'N' | char(1) | boolean 변환 | boolean |
| 2 | VARCHAR2 날짜 | varchar | date 파싱 | date |
| 3 | VARCHAR2 금액 | varchar | 숫자 추출 | numeric |
| 4 | CLOB JSON | text | JSON 파싱 | jsonb |
| 5 | VARCHAR2 태그 | varchar | 배열 분리 | text[] |
| 6 | TIMESTAMP(6) | timestamp(6) | 초 단위 절삭 | timestamp(0) |

---

## DMS vs Glue ETL 비교

### DMS만 사용
```
장점:
✅ 빠름 (실시간 CDC 가능)
✅ 저렴 (시간당 과금)
✅ 설정 간단

단점:
❌ 타입 매핑만 가능
❌ 비즈니스 로직 변환 불가
❌ 데이터 정제 불가
```

### Glue ETL 추가
```
장점:
✅ 복잡한 변환 가능
✅ 데이터 정제/클렌징
✅ 유연한 로직 구현

단점:
❌ 느림 (배치 처리)
❌ 비쌈 (DPU 과금)
❌ 실시간 CDC 불가
```

---

## 실무 권장 전략

### 1. 단순 마이그레이션
```
DMS만 사용
→ 애플리케이션 코드에서 처리
```

### 2. 데이터 정제 필요
```
DMS (Full Load) → S3 → Glue ETL → PostgreSQL
→ 한 번만 실행
```

### 3. 지속적 동기화
```
DMS (CDC) → PostgreSQL (원본 그대로)
+ PostgreSQL View/Function으로 변환
```

### 4. 하이브리드
```
DMS (단순 테이블) + Glue ETL (복잡한 테이블)
→ 테이블별로 다른 전략
```

---

## 작성 정보

- 작성일: 2025-12-03
- 케이스: 6개 (Y/N, 날짜, 금액, JSON, 배열, 밀리초)
