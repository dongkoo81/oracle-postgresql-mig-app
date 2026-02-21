# Oracle MES Application

Oracle 19c 기반 제조 실행 시스템(MES) 애플리케이션

## 프로젝트 개요

- **목적**: Oracle 특화 기능을 활용한 MES 시스템 구축 및 AWS DMS를 통한 PostgreSQL 마이그레이션 테스트
- **데이터베이스**: Oracle 19c (Docker)
- **애플리케이션**: Spring Boot 3.2, Java 17

## 기술 스택

### 백엔드
- **프레임워크**: Spring Boot 3.2.0
- **언어**: Java 17 (Amazon Corretto)
- **빌드 도구**: Gradle 8.5
- **ORM**: JPA (Hibernate 6.3)
- **동적 쿼리**: QueryDSL 5.0
- **SQL 매퍼**: MyBatis 3.0
- **템플릿 엔진**: Thymeleaf 3.1

### 데이터베이스
- **Oracle 19c**: 개발/테스트 환경
- **PostgreSQL**: 마이그레이션 타겟

### 마이그레이션 시 애플리케이션 변경 사항

| 항목 | Oracle | PostgreSQL | 변경 필요 |
|------|--------|-----------|----------|
| **JPA/Hibernate** | OracleDialect | PostgreSQLDialect | ✅ 설정 변경 |
| **QueryDSL** | 동일 | 동일 | ❌ 변경 불필요 |
| **MyBatis** | Oracle 문법 | PostgreSQL 문법 | ✅ XML 수정 |
| **JDBC Driver** | ojdbc8 | postgresql | ✅ 의존성 변경 |
| **Sequence 호출** | `@GeneratedValue` | `@GeneratedValue` | ❌ 변경 불필요 |
| **페이징** | ROWNUM | LIMIT/OFFSET | ✅ Native Query 수정 |

## 데이터베이스 설정

### 사전 준비: SQL*Plus 설치 (EC2 환경)

Docker 외부(EC2)에서 Oracle에 접속하려면 SQL*Plus 설치 필요:

```bash
# 1. yum-utils 설치
sudo yum install -y yum-utils

# 2. Oracle 저장소 추가
sudo yum-config-manager --add-repo=https://yum.oracle.com/repo/OracleLinux/OL8/oracle/instantclient/x86_64

# 3. Oracle GPG 키 가져오기
sudo rpm --import https://yum.oracle.com/RPM-GPG-KEY-oracle-ol8

# 4. Oracle Instant Client 설치
sudo yum install -y oracle-instantclient19.30-basic oracle-instantclient19.30-sqlplus

# 5. 환경 변수 설정
echo 'export PATH=/usr/lib/oracle/19.30/client64/bin:$PATH' >> ~/.bashrc
echo 'export LD_LIBRARY_PATH=/usr/lib/oracle/19.30/client64/lib:$LD_LIBRARY_PATH' >> ~/.bashrc
source ~/.bashrc

# 6. 설치 확인
sqlplus -v
```

### 연결 정보
- **호스트**: localhost (Docker 환경)
- **포트**: 1521
- **서비스명**: ORCLPDB1 (Docker 환경)
- **사용자**: mesuser / mespass

### 데이터베이스 초기화

1. **사용자 생성** (system 계정으로 실행)
```bash
sqlplus system/system@localhost:1521/ORCLPDB1 @sql/01_create_user.sql
```

2. **테이블 생성** (mesuser 계정으로 실행)
```bash
sqlplus mesuser/mespass@localhost:1521/ORCLPDB1 @sql/schema/02_create_tables.sql
```

3. **프로시저, 트리거, Materialized View 생성**
```bash
sqlplus mesuser/mespass@localhost:1521/ORCLPDB1 @sql/procedures/03_create_procedures.sql
```

4. **샘플 데이터 삽입**
```bash
sqlplus mesuser/mespass@localhost:1521/ORCLPDB1 @sql/data/04_insert_sample_data.sql
```

## 프로젝트 구조

```
autoever/
├── src/
│   ├── main/
│   │   ├── java/com/autoever/mes/
│   │   │   ├── config/              # 설정 클래스
│   │   │   ├── domain/              # 도메인 모델
│   │   │   │   ├── product/
│   │   │   │   ├── order/
│   │   │   │   ├── inventory/
│   │   │   │   ├── history/
│   │   │   │   └── document/
│   │   │   └── common/              # 공통 컴포넌트
│   │   └── resources/
│   │       ├── mapper/              # MyBatis XML
│   │       ├── templates/           # Thymeleaf 템플릿
│   │       └── application.yml
│   └── test/
├── sql/                             # SQL 스크립트
│   ├── schema/
│   ├── data/
│   └── procedures/
└── build.gradle
```

## 빌드 및 실행

### 사전 요구사항
- JDK 17 이상
- Gradle 8.5 이상
- Oracle 19c 데이터베이스 (localhost:1521/ORCLPDB1)

### Java 설치 (Amazon Linux)

```bash
# Amazon Corretto 17 설치
sudo yum install -y java-17-amazon-corretto-devel

# 설치 확인
java -version
```

### 빌드
```bash
# 프로젝트 디렉토리로 이동
cd /home/ec2-user/project/oracle-postgresql-migration

# 빌드 (테스트 제외)
./gradlew clean build -x test

# 빌드 (테스트 포함)
./gradlew clean build
```

### 실행

#### 1. Gradle로 직접 실행 (개발 모드)
```bash
./gradlew bootRun
```

#### 2. JAR 파일로 실행 (프로덕션)
```bash
# 빌드
./gradlew clean build -x test

# 실행
java -jar build/libs/mes-0.0.1-SNAPSHOT.jar
```

#### 3. 백그라운드 실행

```bash
# JAR 파일로 백그라운드 실행
nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 프로세스 확인
ps aux | grep java

# 로그 확인
tail -f app.log

# 종료
pkill -f "mes-0.0.1-SNAPSHOT.jar"
```

### 애플리케이션 접속
- **홈페이지**: http://localhost:8080
- **제품 관리**: http://localhost:8080/products
- **작업지시 관리**: http://localhost:8080/orders
- **Oracle 특화 기능**: http://localhost:8080/oracle-features

### 포트 변경
`src/main/resources/application.yml` 파일에서 포트 변경 가능:
```yaml
server:
  port: 8080  # 원하는 포트로 변경
```

## Oracle 특화 기능 (총 20개)

### 구현된 기능 목록

1. **Sequence** - 모든 PK 자동 생성 + NEXTVAL 직접 호출
2. **Trigger** - 주문 생성 시 이력 자동 기록
3. **CLOB** - 대용량 텍스트 (NOTES, DOC_CONTENT)
4. **BLOB** - 바이너리 파일 (DOC_FILE)
5. **XMLType** - XML 문서 (SPEC_XML)
6. **CONNECT BY** - 계층 쿼리 (PRODUCTION_HISTORY)
7. **Stored Procedure** - CALCULATE_ORDER_TOTAL, MERGE_INVENTORY
8. **Stored Function** - CHECK_PRODUCT_AVAILABLE, GET_PRODUCT_STATUS, GET_TOP_PRODUCTS
9. **Materialized View** - DAILY_SUMMARY (REFRESH ON DEMAND)
10. **Partition Table** - QUALITY_INSPECTION (Range + List Composite Partition)
11. **NVL** - NULL 값 처리 (프로시저/함수 내)
12. **DECODE** - 조건부 값 반환 (GET_PRODUCT_STATUS)
13. **ROWNUM** - 페이징 처리 (직접 사용 + Hibernate 자동)
14. **MERGE** - UPSERT 작업 (MERGE_INVENTORY)
15. **DUAL** - 함수 호출용 더미 테이블
16. **SYSDATE** - 현재 날짜/시간 (Native Query)
17. **TO_DATE** - 날짜 변환 및 검색
18. **MINUS** - 집합 연산 (차집합)
19. **(+) Outer Join** - 구식 Outer Join 문법
20. **QueryDSL** - 동적 쿼리 생성

### 화면별 Oracle 특화 기능

| 화면 | URL | 포함된 Oracle 기능 (개수) |
|------|-----|-------------------|
| 제품 관리 | http://localhost:8080/products | Sequence (2개) |
| 작업지시 관리 | http://localhost:8080/orders | Stored Procedure, Trigger, CLOB, NVL (4개) |
| 품질검사 이력 | http://localhost:8080/quality | Partition Table, ROWNUM (2개) |
| Oracle 특화 기능 | http://localhost:8080/oracle-features | Stored Function, CONNECT BY, XMLType, Materialized View, MERGE, DECODE, DUAL (8개) |

---

## PostgreSQL 마이그레이션 시 예상 문제점

### 1. Oracle 전용 함수

| Oracle | PostgreSQL | 변경 필요도 |
|--------|-----------|-----------|
| `NVL(col, 0)` | `COALESCE(col, 0)` | 🔴 높음 |
| `DECODE(col, val1, res1, val2, res2)` | `CASE WHEN col = val1 THEN res1 WHEN col = val2 THEN res2 END` | 🔴 높음 |
| `SYSDATE` | `CURRENT_TIMESTAMP` | 🟡 중간 |
| `TO_DATE(str, fmt)` | `TO_TIMESTAMP(str, fmt)` | 🟡 중간 |
| `ROWNUM` | `ROW_NUMBER() OVER()` 또는 `LIMIT/OFFSET` | 🔴 높음 |

### 2. 데이터 타입

| Oracle | PostgreSQL | 변경 필요도 |
|--------|-----------|-----------|
| `NUMBER(19)` | `BIGINT` 또는 `NUMERIC(19)` | 🟡 중간 |
| `VARCHAR2(n)` | `VARCHAR(n)` | 🟢 낮음 |
| `CLOB` | `TEXT` | 🟡 중간 |
| `BLOB` | `BYTEA` | 🟡 중간 |
| `DATE` (시간 포함) | `TIMESTAMP` | 🟡 중간 |
| `XMLType` | `XML` | 🟢 낮음 |

### 3. Sequence 문법

**Oracle:**
```sql
SELECT PRODUCT_SEQ.NEXTVAL FROM DUAL;
```

**PostgreSQL:**
```sql
SELECT NEXTVAL('product_seq');
-- 또는
SELECT NEXTVAL('product_seq'::regclass);
```

### 4. MERGE 문

**Oracle:**
```sql
MERGE INTO inventory i
USING (SELECT 1 AS product_id, 100 AS quantity FROM DUAL) src
ON (i.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET i.quantity = src.quantity
WHEN NOT MATCHED THEN INSERT VALUES (src.product_id, src.quantity);
```

**PostgreSQL:**
```sql
INSERT INTO inventory (product_id, quantity)
VALUES (1, 100)
ON CONFLICT (product_id)
DO UPDATE SET quantity = EXCLUDED.quantity;
```

### 5. 계층 쿼리 (CONNECT BY)

**Oracle:**
```sql
SELECT * FROM production_history
START WITH parent_id IS NULL
CONNECT BY PRIOR history_id = parent_id;
```

**PostgreSQL:**
```sql
WITH RECURSIVE hierarchy AS (
    SELECT * FROM production_history WHERE parent_id IS NULL
    UNION ALL
    SELECT ph.* FROM production_history ph
    INNER JOIN hierarchy h ON ph.parent_id = h.history_id
)
SELECT * FROM hierarchy;
```

### 6. DUAL 테이블

**Oracle:**
```sql
SELECT GET_PRODUCT_STATUS(1) FROM DUAL;
```

**PostgreSQL:**
```sql
SELECT GET_PRODUCT_STATUS(1);  -- FROM 절 불필요
```

### 7. Materialized View

**Oracle:**
```sql
CREATE MATERIALIZED VIEW daily_summary
BUILD IMMEDIATE
REFRESH COMPLETE ON DEMAND
AS SELECT ...;

-- Refresh
EXEC DBMS_MVIEW.REFRESH('DAILY_SUMMARY', 'C');
```

**PostgreSQL:**
```sql
CREATE MATERIALIZED VIEW daily_summary
AS SELECT ...;

-- Refresh
REFRESH MATERIALIZED VIEW daily_summary;
```

### 8. Partition Table

**Oracle:**
```sql
PARTITION BY RANGE (inspection_date)
SUBPARTITION BY LIST (result)
```

**PostgreSQL:**
```sql
PARTITION BY RANGE (inspection_date);
-- Subpartition은 별도 테이블로 구현 필요
```

### 9. Stored Procedure/Function

**Oracle:**
```sql
CREATE OR REPLACE PROCEDURE proc_name(p_in IN NUMBER, p_out OUT NUMBER) AS
BEGIN
    ...
END;
/
```

**PostgreSQL:**
```sql
CREATE OR REPLACE FUNCTION proc_name(p_in INTEGER, OUT p_out INTEGER)
RETURNS INTEGER AS $$
BEGIN
    ...
END;
$$ LANGUAGE plpgsql;
```

### 10. 마이그레이션 난이도 요약

| 기능 | 난이도 | 비고 |
|------|--------|------|
| Sequence | 🟢 낮음 | 문법만 변경 |
| CLOB/BLOB | 🟡 중간 | TEXT/BYTEA로 변경 |
| Stored Procedure/Function | 🔴 높음 | 문법 및 로직 재작성 |
| Trigger | 🟡 중간 | 문법 변경 |
| CONNECT BY | 🔴 높음 | WITH RECURSIVE로 재작성 |
| Materialized View | 🟢 낮음 | 문법 약간 변경 |
| Partition Table | 🟡 중간 | Subpartition 재설계 필요 |
| NVL/DECODE | 🔴 높음 | 모든 쿼리 수정 필요 |
| ROWNUM | 🔴 높음 | 페이징 로직 재작성 |
| MERGE | 🟡 중간 | ON CONFLICT로 변경 |

### 11. 애플리케이션 레이어 마이그레이션 체크리스트

#### ✅ 변경 불필요 (DB 독립적)
- **Spring Boot**: 프레임워크 코드 변경 없음
- **JPA Entity**: `@Entity`, `@Table`, `@Column` 등 어노테이션 유지
- **QueryDSL**: 동적 쿼리 코드 변경 없음 (Dialect만 변경)
- **Service/Controller**: 비즈니스 로직 변경 없음
- **Thymeleaf**: 템플릿 코드 변경 없음

#### ⚠️ 설정 변경 필요
- **application.yml**
  ```yaml
  # Before (Oracle)
  spring:
    datasource:
      url: jdbc:oracle:thin:@//localhost:1521/ORCLPDB1
      driver-class-name: oracle.jdbc.OracleDriver
    jpa:
      database-platform: org.hibernate.dialect.Oracle12cDialect
  
  # After (PostgreSQL)
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/mesdb
      driver-class-name: org.postgresql.Driver
    jpa:
      database-platform: org.hibernate.dialect.PostgreSQLDialect
  ```

- **build.gradle**
  ```gradle
  # Before (Oracle)
  implementation 'com.oracle.database.jdbc:ojdbc8:21.9.0.0'
  
  # After (PostgreSQL)
  implementation 'org.postgresql:postgresql:42.7.1'
  ```

#### 🔴 코드 수정 필요
- **MyBatis XML**: Oracle 전용 함수 사용 시 (NVL, DECODE, ROWNUM 등)
- **Native Query**: `@Query` 어노테이션에서 Oracle SQL 사용 시
- **Stored Procedure 호출**: MyBatis Mapper에서 프로시저 호출 부분

#### 📋 테스트 항목
1. **단위 테스트**: Repository, Service 레이어 테스트
2. **통합 테스트**: 전체 트랜잭션 플로우 테스트
3. **성능 테스트**: 페이징, 대용량 데이터 조회
4. **기능 테스트**: 각 화면별 CRUD 동작 확인

---
| ROWNUM | 🔴 높음 | 페이징 로직 재작성 |
| MERGE | 🟡 중간 | ON CONFLICT로 변경 |

---

## API 엔드포인트

### REST API
- `GET /api/products` - 제품 목록 조회
- `POST /api/products` - 제품 생성
- `GET /api/orders` - 주문 목록 조회
- `POST /api/orders` - 주문 생성
- `GET /api/quality` - 품질검사 목록 조회
- `GET /api/quality/result/{result}` - 결과별 품질검사 조회 (PASS/FAIL/PENDING)
- `POST /api/quality` - 품질검사 생성

### 웹 UI
- `http://localhost:8080` - 홈페이지
- `http://localhost:8080/products` - 제품 관리 (Sequence, JPA)
- `http://localhost:8080/orders` - 작업지시 관리 (Stored Procedure, Trigger, CLOB)
- `http://localhost:8080/quality` - 품질검사 이력 (파티션 테이블 - Range + List Composite)
- `http://localhost:8080/oracle-features` - Oracle 특화 기능 (실제 UI)

---

## 구현 완료 현황

### ✅ 전체 기능 구현 완료
총 9개 테이블, 14개 Oracle 특화 기능 모두 구현 및 테스트 완료

### 테이블 목록
1. ✅ PRODUCT - Sequence, JPA
2. ✅ PRODUCTION_ORDER - Stored Procedure, Trigger, CLOB
3. ✅ ORDER_DETAIL - Foreign Key
4. ✅ INVENTORY - Optimistic Lock (VERSION)
5. ✅ PRODUCTION_HISTORY - CONNECT BY (계층 쿼리)
6. ✅ PRODUCT_DOCUMENT - CLOB (대용량 텍스트)
7. ✅ PRODUCT_SPEC - XMLType
8. ✅ QUALITY_INSPECTION - Partition Table (Range + List Composite)
9. ✅ DAILY_SUMMARY - Materialized View

### Oracle 특화 기능
1. ✅ Sequence - 자동 증가 PK
2. ✅ Stored Procedure - CALCULATE_ORDER_TOTAL, MERGE_INVENTORY
3. ✅ Stored Function - CHECK_PRODUCT_AVAILABLE, GET_PRODUCT_STATUS, GET_TOP_PRODUCTS
4. ✅ Trigger - 주문 생성 시 이력 자동 기록
5. ✅ CONNECT BY - 계층 구조 쿼리
6. ✅ CLOB - 4GB 대용량 텍스트
7. ✅ XMLType - XML 데이터 저장 및 검증
8. ✅ Materialized View - REFRESH ON DEMAND
9. ✅ Partition Table - Range + List Composite Partition
10. ✅ NVL - NULL 값 처리 (CALCULATE_ORDER_TOTAL, CHECK_PRODUCT_AVAILABLE)
11. ✅ DECODE - 조건부 값 반환 (GET_PRODUCT_STATUS)
12. ✅ ROWNUM - 페이징 처리 (Hibernate 자동 사용)
13. ✅ MERGE - UPSERT 작업 (MERGE_INVENTORY)
14. ✅ DUAL - 함수 호출용 더미 테이블


