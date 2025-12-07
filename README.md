# Oracle MES Application

Oracle 19c 기반 제조 실행 시스템(MES) 애플리케이션

## 프로젝트 개요

- **목적**: Oracle 특화 기능을 활용한 MES 시스템 구축 및 AWS DMS를 통한 PostgreSQL 마이그레이션 테스트
- **기술 스택**: Spring Boot 3.2, Oracle 19c, JPA, QueryDSL, MyBatis, Thymeleaf

## 데이터베이스 설정

### 연결 정보
- **호스트**: 10.1.5.18
- **포트**: 1521
- **서비스명**: oracle19c
- **사용자**: mesuser / mespass

### 데이터베이스 초기화

1. **사용자 생성** (system 계정으로 실행)
```bash
sqlplus system/system@10.1.5.18:1521/oracle19c @sql/01_create_user.sql
```

2. **테이블 생성** (mesuser 계정으로 실행)
```bash
sqlplus mesuser/mespass@10.1.5.18:1521/oracle19c @sql/schema/02_create_tables.sql
```

3. **프로시저, 트리거, Materialized View 생성**
```bash
sqlplus mesuser/mespass@10.1.5.18:1521/oracle19c @sql/procedures/03_create_procedures.sql
```

4. **샘플 데이터 삽입**
```bash
sqlplus mesuser/mespass@10.1.5.18:1521/oracle19c @sql/data/04_insert_sample_data.sql
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
- Oracle 19c 데이터베이스 (10.1.5.18:1521/oracle19c)

### 빌드
```bash
# 프로젝트 디렉토리로 이동
cd /home/ec2-user/projects/autoever

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


# 또는 JAR 파일로
nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 프로세스 확인
ps aux | grep java

# 로그 확인
tail -f app.log

# 종료
pkill -f "gradlew bootRun"
# 또는
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

## Oracle 특화 기능

- **Sequence**: 모든 PK 자동 생성
- **Trigger**: 주문 생성 시 이력 자동 기록
- **CLOB**: 대용량 텍스트 (NOTES, DOC_CONTENT)
- **BLOB**: 바이너리 파일 (DOC_FILE)
- **BFILE**: 외부 파일 참조 (EXTERNAL_FILE)
- **XMLType**: XML 문서 (SPEC_XML)
- **CONNECT BY**: 계층 쿼리 (PRODUCTION_HISTORY)
- **Stored Procedure**: CALCULATE_ORDER_TOTAL
- **Stored Function**: CHECK_PRODUCT_AVAILABLE
- **Materialized View**: DAILY_SUMMARY (REFRESH ON DEMAND)
- **Partition Table**: QUALITY_INSPECTION (Range + List Composite Partition)

### Oracle 특화 기능 UI (http://localhost:8080/oracle-features)

| 기능 | 화면 메뉴 | 설명 |
|------|----------|------|
| QueryDSL 동적 검색 | 📦 제품 검색 | 제품명, 가격 범위로 동적 검색 |
| Stored Function | 📊 재고 가용성 확인 | 제품별 재고 충분 여부 체크 |
| Stored Procedure | 💰 주문 금액 계산 | 주문 상세 기반 총액 자동 계산 |
| CONNECT BY | 🔗 생산 이력 조회 | 계층 구조 이력 표시 |
| CLOB | 📄 문서 관리 | 대용량 텍스트, 문서 유형별 템플릿 제공 |
| XMLType | 📋 제품 사양 | XML 검증, 제품별 사양 템플릿 |
| Materialized View | 📈 일일 주문 요약 | 날짜별 주문 통계, 수동 Refresh |

### Oracle 특화 기능 테스트 API

개발자용 직접 API 호출 (Postman, curl 등):

```bash
# 1. Stored Procedure - 주문 금액 계산
curl -X POST http://localhost:8080/api/test/oracle/procedure/calculate-total/1

# 2. Stored Function - 재고 가용성 체크
curl "http://localhost:8080/api/test/oracle/function/check-available?productId=1&requiredQty=10"

# 3. CONNECT BY - 계층 쿼리
curl http://localhost:8080/api/test/oracle/hierarchy/1

# 4. QueryDSL - 동적 검색
curl "http://localhost:8080/api/test/oracle/querydsl/search?name=Engine&minPrice=100000&maxPrice=200000"

# 5. CLOB - 대용량 텍스트 저장
curl -X POST "http://localhost:8080/api/test/oracle/clob/save?productId=1&content=Large%20text%20content"

# 6. XMLType - XML 저장
curl -X POST "http://localhost:8080/api/test/oracle/xml/save?productId=1&xmlContent=<spec><version>1.0</version></spec>"

# 7. Materialized View - 조회
curl http://localhost:8080/api/test/oracle/materialized-view

# 8. Materialized View - Refresh
curl -X POST http://localhost:8080/api/test/oracle/materialized-view/refresh

# 9. Partition Table - 파티션별 조회 (PASS)
curl http://localhost:8080/api/test/oracle/partition/PASS

# 10. Partition Table - 파티션별 조회 (FAIL)
curl http://localhost:8080/api/test/oracle/partition/FAIL

# 11. Partition Table - 파티션별 조회 (PENDING)
curl http://localhost:8080/api/test/oracle/partition/PENDING
```

**권장 사용 방법:**
- **일반 사용자**: `http://localhost:8080/oracle-features` 화면 사용
- **개발자/테스트**: 위 API 직접 호출

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
- `http://localhost:8080/products` - 제품 관리 (Sequence, QueryDSL, JPA)
- `http://localhost:8080/orders` - 작업지시 관리 (Stored Procedure, Trigger, CLOB)
- `http://localhost:8080/quality` - 품질검사 이력 (파티션 테이블 - Range + List Composite)
- `http://localhost:8080/oracle-features` - Oracle 특화 기능 (실제 UI)

---

## Oracle 특화 기능 테스트 API별 사용 테이블 정리

### 1. POST /api/test/oracle/procedure/calculate-total/{orderId}
   - **Stored Procedure 테스트**
   - 사용 테이블:
     - PRODUCTION_ORDER (주문 정보)
     - ORDER_DETAIL (주문 상세)

### 2. GET /api/test/oracle/function/check-available
   - **Stored Function 테스트**
   - 사용 테이블:
     - INVENTORY (재고 정보)
     - PRODUCT (제품 정보)

### 3. GET /api/test/oracle/hierarchy/{orderId}
   - **CONNECT BY 계층 쿼리 테스트**
   - 사용 테이블:
     - PRODUCTION_HISTORY (생산 이력 - 자기 참조)

### 4. GET /api/test/oracle/querydsl/search
   - **QueryDSL 동적 쿼리 테스트**
   - 사용 테이블:
     - PRODUCT (제품 검색)

### 5. POST /api/test/oracle/clob/save
   - **CLOB 저장 테스트**
   - 사용 테이블:
     - PRODUCT_DOCUMENT (DOC_CONTENT 컬럼 - CLOB)

### 6. POST /api/test/oracle/xml/save
   - **XML 저장 테스트**
   - 사용 테이블:
     - PRODUCT_SPEC (SPEC_XML 컬럼 - CLOB/XMLType)

### 7. GET /api/test/oracle/materialized-view
   - **Materialized View 조회 테스트**
   - 사용 테이블:
     - DAILY_SUMMARY (일일 요약 - Materialized View)

### 8. GET /api/test/oracle/partition/{result}
   - **Partition Table 조회 테스트**
   - 사용 테이블:
     - QUALITY_INSPECTION (품질 검사 이력 - Range + List Composite Partition)

---

## 구현 완료 현황

### ✅ 전체 기능 구현 완료
총 9개 테이블, 8개 Oracle 특화 기능 모두 구현 및 테스트 완료

### 테이블 목록
1. ✅ PRODUCT - Sequence, QueryDSL, JPA
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
2. ✅ QueryDSL - 동적 검색 쿼리
3. ✅ Stored Procedure - CALCULATE_ORDER_TOTAL
4. ✅ Stored Function - CHECK_PRODUCT_AVAILABLE
5. ✅ Trigger - 주문 생성 시 이력 자동 기록
6. ✅ CONNECT BY - 계층 구조 쿼리
7. ✅ CLOB - 4GB 대용량 텍스트
8. ✅ XMLType - XML 데이터 저장 및 검증
9. ✅ Materialized View - REFRESH ON DEMAND
10. ✅ Partition Table - Range + List Composite Partition


