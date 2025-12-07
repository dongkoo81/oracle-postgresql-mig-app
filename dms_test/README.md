# DMS 전처리 테스트 가이드

## 테스트 목적

DMS에서 자동 변환이 어려운 데이터를 Glue ETL로 전처리 후 PostgreSQL에 적재

## 전처리 케이스

| 케이스 | Oracle | PostgreSQL | 전처리 내용 |
|--------|--------|------------|------------|
| 1 | CHAR(1) 'Y'/'N' | boolean | Y→true, N→false |
| 2 | VARCHAR2 날짜 문자열 | date | 다양한 형식 통일 |
| 3 | VARCHAR2 금액 문자열 | numeric | 쉼표, $ 제거 |
| 4 | CLOB JSON 문자열 | jsonb | JSON 파싱 |
| 5 | VARCHAR2 구분자 태그 | text[] | 배열 변환 |

---

## 실행 순서

### 1. Oracle 테스트 환경 구성

```bash
# Oracle 접속
ssh -i dk.pem ec2-user@10.1.5.18

# system 계정으로 실행
sqlplus system/system@localhost:1521/oracle19c @/path/to/01_oracle_test_setup.sql
```

**생성 내용**:
- 사용자: dmstest/dmstest123
- 테이블: test_product (3건 샘플 데이터)

---

### 2. PostgreSQL 테스트 환경 구성

```bash
# PostgreSQL 접속
psql -h target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com -U postgres -d target

# 스크립트 실행
\i /path/to/02_postgresql_test_setup.sql
```

**생성 내용**:
- 사용자: dmstest/dmstest123
- 스키마: dmstest
- 테이블: test_product (전처리 후 구조)

---

### 3. Glue Connection 생성

#### Oracle Connection
```
Name: oracle-dmstest-connection
Type: JDBC
URL: jdbc:oracle:thin:@10.1.5.18:1521/oracle19c
Username: dmstest
Password: dmstest123
VPC: (Oracle이 있는 VPC)
Subnet: (Private subnet)
Security Group: (Oracle 접근 가능한 SG)
```

#### PostgreSQL Connection
```
Name: postgresql-dmstest-connection
Type: JDBC
URL: jdbc:postgresql://target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432/target
Username: dmstest
Password: dmstest123
VPC: (Aurora가 있는 VPC)
Subnet: (Private subnet)
Security Group: (Aurora 접근 가능한 SG)
```

**Connection 테스트**:
- AWS Glue Console → Connections → Test connection

---

### 4. Glue ETL Job 생성

#### Job 설정
```
Name: dms-preprocessing-test
Type: Spark
Glue version: 4.0
Language: Python 3
Script: 03_glue_etl_preprocessing.py 업로드
Connections: oracle-dmstest-connection, postgresql-dmstest-connection
IAM Role: AWSGlueServiceRole (S3, Glue 권한 필요)
Worker type: G.1X
Number of workers: 2
```

#### Job 실행
```bash
# AWS CLI로 실행
aws glue start-job-run --job-name dms-preprocessing-test --region ap-northeast-2

# 또는 Console에서 "Run job" 클릭
```

---

### 5. 결과 확인

#### PostgreSQL에서 확인
```sql
-- dmstest 계정으로 접속
psql -h target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com -U dmstest -d target

-- 데이터 확인
SELECT * FROM dmstest.test_product;

-- 전처리 결과 확인
SELECT 
    product_code,
    is_active,              -- boolean (true/false)
    created_date,           -- date
    price,                  -- numeric
    metadata_json,          -- jsonb
    tags                    -- text[]
FROM dmstest.test_product;

-- JSONB 쿼리 테스트
SELECT 
    product_code,
    metadata_json->>'category' AS category,
    metadata_json->>'brand' AS brand
FROM dmstest.test_product;

-- 배열 쿼리 테스트
SELECT 
    product_code,
    tags,
    array_length(tags, 1) AS tag_count
FROM dmstest.test_product;
```

---

## 예상 결과

### Oracle 원본 데이터
```
PRODUCT_ID | PRODUCT_CODE | IS_ACTIVE | CREATED_DATE_STR | PRICE_STR      | TAGS
-----------|--------------|-----------|------------------|----------------|------------------
1          | PROD001      | Y         | 2025-12-03       | 1,234,567.89   | tag1,tag2,tag3
2          | PROD002      | N         | 2025/12/03       | $2,500.00      | tag4,tag5
3          | PROD003      | Y         | 03-DEC-2025      | 999999         | tag6
```

### PostgreSQL 전처리 후 데이터
```
product_id | product_code | is_active | created_date | price      | tags
-----------|--------------|-----------|--------------|------------|------------------
1          | PROD001      | true      | 2025-12-03   | 1234567.89 | {tag1,tag2,tag3}
2          | PROD002      | false     | 2025-12-03   | 2500.00    | {tag4,tag5}
3          | PROD003      | true      | 2025-12-03   | 999999.00  | {tag6}
```

---

## 전처리 로직 설명

### 1. Y/N → Boolean
```python
when(col("IS_ACTIVE") == "Y", True)
.when(col("IS_ACTIVE") == "N", False)
```

### 2. 날짜 문자열 → Date
```python
to_date(col("CREATED_DATE"), "yyyy-MM-dd")
```

### 3. 금액 문자열 → Numeric
```python
regexp_replace(col("PRICE_STR"), "[^0-9.]", "").cast("decimal(15,2)")
# 쉼표, $, 기타 문자 제거 후 숫자로 변환
```

### 4. JSON 문자열 → JSONB
```python
col("METADATA_JSON")
# PostgreSQL에서 자동으로 JSONB로 변환
```

### 5. 구분자 태그 → 배열
```python
split(col("TAGS"), ",")
# 쉼표로 분리하여 배열 생성
```

---

## DMS vs Glue ETL 비교

| 항목 | DMS | Glue ETL |
|------|-----|----------|
| Y/N → Boolean | ❌ 불가 | ✅ 가능 |
| 날짜 형식 통일 | ❌ 불가 | ✅ 가능 |
| 금액 문자열 정제 | ❌ 불가 | ✅ 가능 |
| JSON → JSONB | ⚠️ text로만 | ✅ JSONB로 변환 |
| 구분자 → 배열 | ❌ 불가 | ✅ 가능 |
| 속도 | 빠름 | 느림 |
| 비용 | 저렴 | 비쌈 (DPU 과금) |

---

## 정리

### DMS만 사용
- 단순 데이터 타입 변환 (VARCHAR → text, NUMBER → numeric)
- 빠르고 저렴

### Glue ETL 추가 사용
- 복잡한 비즈니스 로직 변환
- 데이터 정제/클렌징
- 느리고 비싸지만 유연함

---

## 다음 단계

1. ✅ 전처리 테스트 완료
2. ⬜ DMS + Glue 조합 테스트
3. ⬜ 성능 비교 (DMS vs Glue)
4. ⬜ 비용 분석

---

## 작성 정보

- 작성일: 2025-12-03
- 환경: AWS ap-northeast-2 (Seoul)
- Oracle: 10.1.5.18:1521/oracle19c
- PostgreSQL: target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com
