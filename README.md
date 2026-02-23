# MES Application - Aurora PostgreSQL Migration

Oracle 19c 기반 제조 실행 시스템(MES)을 AWS Aurora PostgreSQL로 마이그레이션한 프로젝트

## 📋 프로젝트 개요

- **목적**: Oracle MES 애플리케이션을 AWS DMS를 통해 Aurora PostgreSQL로 마이그레이션
- **원본 DB**: Oracle 19c (Docker)
- **타겟 DB**: AWS Aurora PostgreSQL 17
- **애플리케이션**: Spring Boot 3.2, Java 17

## 🏗️ 기술 스택

### 백엔드
- **프레임워크**: Spring Boot 3.2.0
- **언어**: Java 17 (Amazon Corretto)
- **빌드 도구**: Gradle 8.5
- **ORM**: JPA (Hibernate 6.3)
- **동적 쿼리**: QueryDSL 5.0
- **SQL 매퍼**: MyBatis 3.0
- **템플릿 엔진**: Thymeleaf 3.1

### 데이터베이스
- **개발**: Oracle 19c (Docker)
- **운영**: AWS Aurora PostgreSQL 17

## 🔄 마이그레이션 프로세스

### 1단계: 데이터 마이그레이션 (AWS DMS)
- Oracle 19c → Aurora PostgreSQL 17
- 테이블, 인덱스, 시퀀스 자동 변환
- Stored Procedure/Function 자동 변환 (일부 수동 수정 필요)

### 2단계: 애플리케이션 코드 수정

#### ✅ 설정 파일 변경

**build.gradle**
```gradle
// 변경 전
implementation 'com.oracle.database.jdbc:ojdbc11:23.3.0.23.09'

// 변경 후
implementation 'org.postgresql:postgresql:42.7.1'
```

**application.yml**
```yaml
# 변경 전 (Oracle)
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/ORCLPDB1
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      connection-test-query: SELECT 1 FROM DUAL
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect

# 변경 후 (PostgreSQL)
spring:
  datasource:
    url: jdbc:postgresql://apg17.cluster-xxx.ap-northeast-2.rds.amazonaws.com:5432/mesdb
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-test-query: SELECT 1
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

#### ✅ MyBatis XML 수정

**OrderMapper.xml** (4개 수정)
```xml
<!-- 1. Stored Procedure 호출 -->
변경 전: {CALL CALCULATE_ORDER_TOTAL(...)}
변경 후: CALL CALCULATE_ORDER_TOTAL(...)

<!-- 2. Stored Function 호출 -->
변경 전: SELECT CHECK_PRODUCT_AVAILABLE(...) FROM DUAL
변경 후: SELECT CHECK_PRODUCT_AVAILABLE(...)

<!-- 3. MERGE_INVENTORY -->
변경 전: {CALL MERGE_INVENTORY(...)}
변경 후: CALL MERGE_INVENTORY(...)
```

**HistoryMapper.xml** (CONNECT BY → WITH RECURSIVE)
```xml
<!-- 변경 전: Oracle CONNECT BY -->
SELECT * FROM PRODUCTION_HISTORY
WHERE ORDER_ID = #{orderId}
START WITH PARENT_ID IS NULL
CONNECT BY PRIOR HISTORY_ID = PARENT_ID

<!-- 변경 후: PostgreSQL WITH RECURSIVE -->
WITH RECURSIVE hierarchy AS (
    SELECT *, 1 as level
    FROM PRODUCTION_HISTORY
    WHERE ORDER_ID = #{orderId} AND PARENT_ID IS NULL
    UNION ALL
    SELECT ph.*, h.level + 1
    FROM PRODUCTION_HISTORY ph
    INNER JOIN hierarchy h ON ph.PARENT_ID = h.HISTORY_ID
    WHERE ph.ORDER_ID = #{orderId}
)
SELECT * FROM hierarchy ORDER BY level, PROCESS_DATE
```

#### ✅ Repository 구현체 수정

**ProductRepositoryImpl.java** (5개 메서드)

| 메서드 | Oracle | PostgreSQL |
|--------|--------|-----------|
| findProductsCreatedToday | `TRUNC(SYSDATE)` | `CURRENT_DATE` |
| findTopProductsByRownum | `ROWNUM <= :limit` | `LIMIT :limit` |
| getSequenceNextval | `SEQ.NEXTVAL FROM DUAL` | `NEXTVAL('seq')` |
| findProductsWithoutInventory | `MINUS` | `EXCEPT` |
| findProductsWithInventoryOldStyle | `(+)` Outer Join | `LEFT JOIN` |

#### ✅ Native Query 수정

**DailySummaryRepository.java**
```java
// 변경 전
@Query(value = "BEGIN DBMS_MVIEW.REFRESH('DAILY_SUMMARY', 'C'); END;", nativeQuery = true)

// 변경 후
@Query(value = "REFRESH MATERIALIZED VIEW DAILY_SUMMARY", nativeQuery = true)
```

#### ✅ Stored Procedure/Function

DMS가 자동 변환했으며, 다음 항목들이 PostgreSQL로 변환됨:
- `CALCULATE_ORDER_TOTAL` - NVL → COALESCE
- `CHECK_PRODUCT_AVAILABLE` - NVL → COALESCE
- `GET_PRODUCT_STATUS` - DECODE → CASE WHEN
- `MERGE_INVENTORY` - MERGE → INSERT ... ON CONFLICT (수동 수정)

## 📁 프로젝트 구조

```
src/main/java/com/autoever/mes/
├── MesApplication.java
├── config/                          # JPA, QueryDSL 설정
├── common/                          # 공통 컴포넌트
├── domain/
│   ├── product/                     # 제품 관리
│   ├── order/                       # 작업지시 관리
│   ├── quality/                     # 품질검사
│   ├── inventory/                   # 재고 관리
│   ├── history/                     # 생산 이력
│   ├── document/                    # 문서 (CLOB → TEXT)
│   ├── spec/                        # 제품 사양 (XMLType → XML)
│   └── test/                        # 기능 테스트 API
└── mapper/                          # MyBatis Mapper

src/main/resources/
├── application.yml
├── mapper/                          # MyBatis XML
├── templates/                       # Thymeleaf
└── static/
```

## 🚀 빌드 및 실행

### 사전 요구사항
- JDK 17 이상
- Gradle 8.5 이상
- Aurora PostgreSQL 접속 정보

### 빌드
```bash
./gradlew clean build -x test
```

### 실행
```bash
# 개발 모드
./gradlew bootRun

# JAR 실행
java -jar build/libs/mes-0.0.1-SNAPSHOT.jar

# 백그라운드 실행
nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 로그 실시간 확인
tail -f app.log
```

### 애플리케이션 종료 및 재시작
```bash
# 애플리케이션 종료
pkill -f mes-0.0.1-SNAPSHOT.jar

# 재빌드 및 재시작 (한 번에)
pkill -f mes-0.0.1-SNAPSHOT.jar && ./gradlew clean build -x test && nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 애플리케이션 접속
- **홈페이지**: http://localhost:8080
- **제품 관리**: http://localhost:8080/products
- **작업지시 관리**: http://localhost:8080/orders
- **품질검사**: http://localhost:8080/quality
- **기능 테스트**: http://localhost:8080/oracle-features

## 🧪 API 테스트

### 기본 REST API
```bash
# 제품 목록 조회
curl http://localhost:8080/api/products

# 주문 목록 조회
curl http://localhost:8080/api/orders

# 품질검사 조회
curl http://localhost:8080/api/quality
```

### PostgreSQL 기능 테스트 API
```bash
# 1. QueryDSL 동적 검색
curl "http://localhost:8080/api/test/oracle/querydsl/search?name=Engine"

# 2. Stored Function (재고 확인)
curl "http://localhost:8080/api/test/oracle/function/check-available?productId=1&requiredQty=10"

# 3. Stored Procedure (금액 계산)
curl -X POST "http://localhost:8080/api/test/oracle/procedure/calculate-total/1"

# 4. WITH RECURSIVE (계층 쿼리)
curl "http://localhost:8080/api/test/oracle/hierarchy/1"

# 5. TEXT 타입 (CLOB 대체)
curl -X POST "http://localhost:8080/api/test/oracle/clob/save?productId=1&content=TestDocument"

# 6. XML 타입
curl -X POST "http://localhost:8080/api/test/oracle/xml/save?productId=1&xmlContent=%3Cspec%3E%3C%2Fspec%3E"

# 7. Materialized View
curl "http://localhost:8080/api/test/oracle/materialized-view"

# 8. Materialized View Refresh
curl -X POST "http://localhost:8080/api/test/oracle/materialized-view/refresh"

# 9. CASE WHEN (DECODE 대체)
curl "http://localhost:8080/api/test/oracle/decode/product-status/1"

# 10. ON CONFLICT (MERGE 대체)
curl -X POST "http://localhost:8080/api/test/oracle/merge/inventory?productId=1&quantity=10"

# 11. CURRENT_DATE (SYSDATE 대체)
curl "http://localhost:8080/api/test/oracle/sysdate/today-products"

# 12. TO_DATE
curl "http://localhost:8080/api/test/oracle/to-date/search?startDate=2024-01-01&endDate=2026-12-31"

# 13. LIMIT (ROWNUM 대체)
curl "http://localhost:8080/api/test/oracle/rownum/top-products?limit=5"

# 14. NEXTVAL() (Sequence)
curl "http://localhost:8080/api/test/oracle/sequence/nextval?sequenceName=product_seq"

# 15. EXCEPT (MINUS 대체)
curl "http://localhost:8080/api/test/oracle/minus/products-without-inventory"

# 16. LEFT JOIN ((+) 대체)
curl "http://localhost:8080/api/test/oracle/outer-join/products-inventory"

# 17. Partition Table
curl "http://localhost:8080/api/test/oracle/partition/PASS"
```

## 📊 마이그레이션 결과

### 변경 불필요 항목 (DB 독립적)
- ✅ JPA Entity 클래스
- ✅ QueryDSL 동적 쿼리
- ✅ Service 레이어 비즈니스 로직
- ✅ Controller (REST API, 웹)
- ✅ Thymeleaf 템플릿
- ✅ DTO 클래스

### 변경 완료 항목
- ✅ build.gradle - JDBC 드라이버
- ✅ application.yml - 연결 정보, Dialect
- ✅ OrderMapper.xml - FROM DUAL 제거, {CALL} → CALL
- ✅ HistoryMapper.xml - CONNECT BY → WITH RECURSIVE
- ✅ ProductRepositoryImpl.java - 5개 메서드 (SYSDATE, ROWNUM, MINUS, (+), NEXTVAL)
- ✅ DailySummaryRepository.java - DBMS_MVIEW → REFRESH MATERIALIZED VIEW
- ✅ Stored Procedures/Functions - PostgreSQL로 변환 (DMS + 수동)

## 🔍 주요 변환 내역

| Oracle | PostgreSQL | 비고 |
|--------|-----------|------|
| `NUMBER(19)` | `BIGINT` | DMS 자동 변환 |
| `VARCHAR2(n)` | `VARCHAR(n)` | DMS 자동 변환 |
| `DATE` | `TIMESTAMP(0)` | DMS 자동 변환 |
| `CLOB` | `TEXT` | DMS 자동 변환 |
| `BLOB` | `BYTEA` | DMS 자동 변환 |
| `XMLType` | `XML` | DMS 자동 변환 |
| `SYSDATE` | `CURRENT_TIMESTAMP` | 코드 수정 |
| `ROWNUM` | `LIMIT` | 코드 수정 |
| `MINUS` | `EXCEPT` | 코드 수정 |
| `(+)` Outer Join | `LEFT JOIN` | 코드 수정 |
| `CONNECT BY` | `WITH RECURSIVE` | 코드 수정 |
| `MERGE` | `INSERT ... ON CONFLICT` | DB + 코드 수정 |
| `NVL()` | `COALESCE()` | DMS 자동 변환 |
| `DECODE()` | `CASE WHEN` | DMS 자동 변환 |
| `FROM DUAL` | 제거 | 코드 수정 |
| `SEQ.NEXTVAL` | `NEXTVAL('seq')` | 코드 수정 |
| `DBMS_MVIEW.REFRESH` | `REFRESH MATERIALIZED VIEW` | 코드 수정 |

## 📝 데이터베이스 구조

### 테이블 목록 (9개)
1. **PRODUCT** - 제품 정보
2. **PRODUCTION_ORDER** - 작업지시
3. **ORDER_DETAIL** - 작업지시 상세
4. **INVENTORY** - 재고 (Optimistic Lock)
5. **PRODUCTION_HISTORY** - 생산 이력 (계층 구조)
6. **PRODUCT_DOCUMENT** - 문서 (TEXT)
7. **PRODUCT_SPEC** - 제품 사양 (XML)
8. **QUALITY_INSPECTION** - 품질검사 (Partition Table)
9. **DAILY_SUMMARY** - 일일 요약 (Materialized View)

### Stored Procedures/Functions (4개)
1. **CALCULATE_ORDER_TOTAL** - 주문 금액 계산
2. **CHECK_PRODUCT_AVAILABLE** - 재고 확인
3. **GET_PRODUCT_STATUS** - 제품 상태 조회
4. **MERGE_INVENTORY** - 재고 UPSERT

## 🎯 마이그레이션 체크리스트

- [x] AWS DMS로 데이터 마이그레이션
- [x] JDBC 드라이버 변경 (ojdbc → postgresql)
- [x] Hibernate Dialect 변경
- [x] MyBatis XML 수정 (FROM DUAL, {CALL})
- [x] CONNECT BY → WITH RECURSIVE 변환
- [x] Native Query 수정 (SYSDATE, ROWNUM, MINUS, (+))
- [x] Materialized View Refresh 문법 변경
- [x] Stored Procedure/Function 검증
- [x] 통합 테스트 실행
- [x] API 엔드포인트 테스트

## 🔗 관련 저장소

- **원본 (Oracle)**: https://github.com/dongkoo81/oracle-postgresql-migration
- **마이그레이션 (PostgreSQL)**: https://github.com/dongkoo81/oracle-postgresql-mig-app

## 📌 참고 사항

### PostgreSQL 특이사항
- **Materialized View**: 자동 갱신 기능 없음 (수동 REFRESH 필요)
- **Partition Table**: `PARTITION()` 문법 지원 (자동 파티션 프루닝)
- **Sequence**: `NEXTVAL('seq_name')` 함수 형태로 호출
- **TO_DATE**: 날짜만 반환 (시간 00:00:00)

### 성능 최적화
- Connection Pool: HikariCP (기본 설정)
- QueryDSL: 동적 쿼리 최적화
- Partition Pruning: WHERE 조건으로 자동 활성화

## 📧 문의

프로젝트 관련 문의사항은 GitHub Issues를 이용해주세요.
