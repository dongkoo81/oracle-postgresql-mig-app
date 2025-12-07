# 요구사항 문서

## 소개

Oracle 19c 기반 MES(Manufacturing Execution System) 애플리케이션 개발 프로젝트입니다. 이 시스템은 Oracle 19c의 모든 특화 기능(Partition, Sequence, Trigger, CLOB/BLOB/BFILE, XMLType, 계층 쿼리 등)을 활용하여 구현되며, 향후 AWS DMS를 통한 PostgreSQL 마이그레이션 시 다양한 변환 이슈를 경험하고 해결하는 것을 목표로 합니다.

**Oracle 연결 정보:**
- IP: 10.1.5.18
- Instance: oracle19c
- System Account: system/system
- SYS Account: sys/sys
- Application Account: mesuser/mespass

## 용어 정의

- **MES_System**: 생산 실행 시스템으로 제품, 주문, 재고, 생산 이력을 관리하는 애플리케이션
- **Oracle_19c**: Oracle Database 19c, 인스턴스명 oracle19c (10.1.5.18)
- **HikariCP**: Java 애플리케이션의 JDBC 커넥션 풀 라이브러리
- **DMS**: AWS Database Migration Service, 스키마 및 데이터 마이그레이션 도구
- **Spring_Boot**: Java 기반 웹 애플리케이션 프레임워크
- **JPA**: Java Persistence API, ORM 표준
- **MyBatis**: SQL 매퍼 프레임워크
- **QueryDSL**: 타입 안전한 쿼리 빌더 라이브러리
- **JDBC_Template**: Spring의 JDBC 추상화 레이어
- **DMS_GenAI**: AWS DMS의 GenAI 기반 애플리케이션 코드 SQL 변환 기능
- **Thymeleaf**: Spring Boot의 서버사이드 템플릿 엔진

## 요구사항

### 요구사항 1

**사용자 스토리:** 개발자로서, Docker에서 Oracle XE를 설정하여 로컬 머신에 Oracle을 직접 설치하지 않고도 MES 애플리케이션을 개발할 수 있어야 한다.

#### 인수 기준

1. Docker Compose 파일이 실행되면 MES_System은 포트 1521이 노출된 Oracle XE 컨테이너를 생성해야 한다
2. Oracle XE 컨테이너가 시작되면 MES_System은 사용자 생성 스크립트와 테이블 생성 스크립트를 자동으로 실행해야 한다
3. 데이터베이스 초기화가 완료되면 MES_System은 적절한 제약조건과 인덱스가 있는 8개의 테이블을 모두 생성해야 한다
4. Oracle XE 컨테이너가 실행 중이면 MES_System은 서비스 이름을 사용한 JDBC URL로 연결을 수락해야 한다
5. 헬스 체크가 구성되면 MES_System은 컨테이너를 정상으로 표시하기 전에 Oracle 리스너 상태를 확인해야 한다

### 요구사항 2

**사용자 스토리:** 개발자로서, 데이터베이스 스키마에서 Oracle 특화 기능을 사용하여 DMS를 통해 PostgreSQL로 마이그레이션할 때 다양한 변환 이슈를 경험할 수 있어야 한다.

#### 인수 기준

1. 레코드를 삽입할 때 MES_System은 모든 기본 키 생성에 Oracle 시퀀스를 사용해야 한다
2. 생산 주문이 생성되면 MES_System은 트리거를 통해 생산 이력 레코드를 자동으로 생성해야 한다
3. 대용량 텍스트 데이터를 저장할 때 MES_System은 NOTES 및 DOC_CONTENT 컬럼에 CLOB 타입을 사용해야 한다
4. 바이너리 파일을 저장할 때 MES_System은 DOC_FILE 컬럼에 BLOB 타입을 사용해야 한다
5. 외부 파일을 참조할 때 MES_System은 EXTERNAL_FILE 컬럼에 BFILE 타입을 사용해야 한다
6. 제품 사양을 저장할 때 MES_System은 SPEC_XML 컬럼에 XMLTYPE을 사용해야 한다
7. 생산 이력 계층을 쿼리할 때 MES_System은 PARENT_ID 자기 참조와 함께 CONNECT BY 쿼리를 지원해야 한다
8. Oracle 힌트를 사용할 때 MES_System은 복잡한 쿼리에 INDEX, FULL, PARALLEL 힌트를 포함해야 한다
9. 비즈니스 로직을 계산할 때 MES_System은 주문 금액 계산을 위해 저장 프로시저를 사용해야 한다
10. 데이터를 검증할 때 MES_System은 제품 가용성을 확인하기 위해 저장 함수를 사용해야 한다
11. 데이터베이스 링크 기능을 테스트할 때 MES_System은 마이그레이션 테스트를 위해 자기 자신을 가리키는 루프백 데이터베이스 링크를 생성해야 한다
12. 구체화된 뷰를 생성할 때 MES_System은 요약 데이터에 대한 새로 고침 일정을 정의해야 한다

### 요구사항 3

**사용자 스토리:** 개발자로서, HikariCP 커넥션 풀링을 사용하는 Spring Boot 애플리케이션을 구축하여 Oracle XE에 대한 데이터베이스 연결을 효율적으로 관리할 수 있어야 한다.

#### 인수 기준

1. Spring_Boot 애플리케이션이 시작되면 MES_System은 Oracle JDBC 드라이버로 HikariCP를 구성해야 한다
2. 커넥션 풀을 구성할 때 MES_System은 최소 유휴 연결을 5로, 최대 풀 크기를 20으로 설정해야 한다
3. 애플리케이션이 Oracle에 연결할 때 MES_System은 jdbc:oracle:thin:@//host:port/service 형식의 JDBC URL을 사용해야 한다
4. 커넥션 풀이 초기화되면 MES_System은 테스트 쿼리를 사용하여 연결을 검증해야 한다
5. 연결 누수가 발생하면 MES_System은 60초 후에 누수 경고를 감지하고 로깅해야 한다

### 요구사항 4

**사용자 스토리:** 개발자로서, 모든 MES 테이블에 대한 JPA 엔티티를 구현하여 기본 CRUD 작업에 객체 관계 매핑을 사용할 수 있어야 한다.

#### 인수 기준

1. JPA 엔티티를 정의할 때 MES_System은 적절한 어노테이션이 있는 8개의 테이블 모두에 대한 엔티티 클래스를 생성해야 한다
2. Oracle NUMBER 타입을 매핑할 때 MES_System은 적절한 Java 타입(ID는 Long, 금액은 BigDecimal)을 사용해야 한다
3. Oracle DATE 및 TIMESTAMP를 매핑할 때 MES_System은 각각 LocalDate 및 LocalDateTime을 사용해야 한다
4. 관계를 매핑할 때 MES_System은 적절한 페치 전략으로 @OneToMany 및 @ManyToOne 연관관계를 정의해야 한다
5. 시퀀스를 사용할 때 MES_System은 Oracle 시퀀스 이름으로 @SequenceGenerator를 구성해야 한다

### 요구사항 5

**사용자 스토리:** 개발자로서, 타입 안전한 동적 쿼리를 위해 QueryDSL을 사용하여 문자열 연결 없이 복잡한 검색 조건을 구축할 수 있어야 한다.

#### 인수 기준

1. QueryDSL을 구성할 때 MES_System은 모든 JPA 엔티티에 대한 Q-클래스를 생성해야 한다
2. 동적 검색 쿼리를 구축할 때 MES_System은 조건부 술어에 BooleanBuilder를 사용해야 한다
3. 테이블을 조인할 때 MES_System은 성능을 위해 페치 조인과 함께 QueryDSL 조인 구문을 사용해야 한다
4. 특정 컬럼을 프로젝션할 때 MES_System은 DTO 매핑을 위해 Projections를 사용해야 한다
5. 복잡한 필터링이 필요한 경우 MES_System은 and/or 연산을 사용하여 여러 술어를 결합해야 한다

### 요구사항 6

**사용자 스토리:** 개발자로서, 복잡한 Oracle 특화 쿼리를 위해 MyBatis를 사용하여 CONNECT BY 및 분석 함수와 같은 네이티브 SQL 기능을 활용할 수 있어야 한다.

#### 인수 기준

1. MyBatis를 구성할 때 MES_System은 각 도메인에 대한 XML 매퍼 파일을 등록해야 한다
2. 생산 이력 계층을 쿼리할 때 MES_System은 MyBatis XML 매퍼를 통해 CONNECT BY 쿼리를 실행해야 한다
3. 분석 함수를 사용할 때 MES_System은 MyBatis 쿼리에서 ROW_NUMBER, RANK, LAG/LEAD 윈도우 함수를 지원해야 한다
4. CLOB 데이터를 처리할 때 MES_System은 결과 맵에서 CLOB 컬럼을 String으로 적절히 매핑해야 한다
5. 배치 삽입을 실행할 때 MES_System은 성능을 위해 MyBatis 배치 실행기를 사용해야 한다
6. MyBatis에서 Oracle 힌트를 사용할 때 MES_System은 SELECT 문에 힌트 주석을 포함해야 한다

### 요구사항 7

**사용자 스토리:** 개발자로서, Oracle SQL과 함께 JPA Native Query를 사용하여 Java 어노테이션에 포함된 SQL의 DMS GenAI 변환을 테스트할 수 있어야 한다.

#### 인수 기준

1. @Query 어노테이션을 사용할 때 MES_System은 CONNECT BY가 있는 Oracle 특화 SQL 구문을 포함해야 한다
2. 네이티브 쿼리를 작성할 때 MES_System은 TO_DATE 및 TRUNC와 같은 Oracle 날짜 함수를 사용해야 한다
3. 힌트와 함께 쿼리할 때 MES_System은 @Query SQL 문자열에 Oracle 힌트 주석을 포함해야 한다
4. @Query에서 분석 함수를 사용할 때 MES_System은 PARTITION BY가 있는 OVER 절을 포함해야 한다
5. dual 테이블이 필요한 경우 MES_System은 네이티브 쿼리에서 Oracle DUAL 테이블을 참조해야 한다

### 요구사항 8

**사용자 스토리:** 개발자로서, Oracle SQL과 함께 JDBC Template을 사용하여 프로그래밍 방식의 SQL 실행에 대한 DMS GenAI 변환을 테스트할 수 있어야 한다.

#### 인수 기준

1. JDBC_Template을 통해 쿼리를 실행할 때 MES_System은 Java 문자열 리터럴에서 Oracle 특화 SQL 구문을 사용해야 한다
2. 저장 프로시저를 호출할 때 MES_System은 Oracle 프로시저 이름과 함께 SimpleJdbcCall을 사용해야 한다
3. 결과 집합을 처리할 때 MES_System은 RowMapper를 사용하여 Oracle 타입을 Java 타입으로 매핑해야 한다
4. 배치 작업을 실행할 때 MES_System은 Oracle SQL 문과 함께 batchUpdate를 사용해야 한다
5. 복잡한 쿼리가 필요한 경우 MES_System은 Oracle 함수로 SQL 문자열을 구성해야 한다

### 요구사항 9

**사용자 스토리:** 개발자로서, MES 작업을 위한 REST API를 구현하여 제품, 주문 및 재고에 대한 CRUD 작업을 수행할 수 있어야 한다.

#### 인수 기준

1. REST API가 제품 생성 요청을 받으면 MES_System은 필수 필드를 검증하고 PRODUCT 테이블에 삽입해야 한다
2. 생산 주문을 생성할 때 MES_System은 시퀀스를 사용하여 ORDER_NO를 생성하고 ORDER_DATE와 함께 삽입해야 한다
3. 날짜 범위로 주문을 쿼리할 때 MES_System은 성능을 위해 인덱싱된 ORDER_DATE 컬럼을 사용해야 한다
4. 재고를 업데이트할 때 MES_System은 동시 업데이트 충돌을 방지하기 위해 낙관적 잠금을 사용해야 한다
5. 생산 이력을 검색할 때 MES_System은 CONNECT BY 쿼리 결과를 사용하여 계층적 데이터를 반환해야 한다

### 요구사항 10

**사용자 스토리:** 개발자로서, 애플리케이션에서 LOB 타입(CLOB, BLOB, BFILE)을 처리하여 대용량 문서와 파일을 저장하고 검색할 수 있어야 한다.

#### 인수 기준

1. CLOB 콘텐츠가 있는 제품 문서를 저장할 때 MES_System은 최대 4GB의 텍스트 콘텐츠를 처리해야 한다
2. 바이너리 파일을 업로드할 때 MES_System은 파일 데이터를 BLOB 컬럼에 저장하고 메타데이터를 별도로 저장해야 한다
3. 외부 파일을 참조할 때 MES_System은 BFILE 디렉토리 및 파일 이름 참조를 저장해야 한다
4. JPA를 통해 CLOB 데이터를 검색할 때 MES_System은 대용량 텍스트 콘텐츠를 적절히 스트리밍해야 한다
5. BLOB 데이터를 읽을 때 MES_System은 바이트 배열 또는 InputStream 액세스를 제공해야 한다

### 요구사항 11

**사용자 스토리:** 개발자로서, XMLType 컬럼을 사용하여 XML 형식의 구조화된 제품 사양을 저장하고 쿼리할 수 있어야 한다.

#### 인수 기준

1. 제품 사양을 삽입할 때 MES_System은 XMLTYPE 컬럼에 저장하기 전에 XML 구조를 검증해야 한다
2. MyBatis를 통해 XML 데이터를 쿼리할 때 MES_System은 XMLTYPE을 String 표현으로 매핑해야 한다
3. XML 사양을 업데이트할 때 MES_System은 전체 XML 문서를 원자적으로 교체해야 한다
4. XML 파싱이 필요한 경우 MES_System은 Java XML 라이브러리를 사용하여 검색된 XML 문자열을 파싱해야 한다
5. XML을 저장할 때 MES_System은 올바른 형식의 XML 구조를 보장해야 한다

### 요구사항 12

**사용자 스토리:** 개발자로서, 트랜잭션 관리를 구현하여 여러 테이블 작업에서 데이터 일관성을 보장할 수 있어야 한다.

#### 인수 기준

1. 상세 정보가 있는 생산 주문을 생성할 때 MES_System은 단일 트랜잭션 내에서 PRODUCTION_ORDER 및 ORDER_DETAIL 테이블에 삽입해야 한다
2. 주문 생성 중에 트리거가 실행되면 MES_System은 트리거 생성 PRODUCTION_HISTORY 레코드를 동일한 트랜잭션에 포함해야 한다
3. 다중 테이블 삽입 중에 오류가 발생하면 MES_System은 모든 변경 사항을 롤백해야 한다
4. @Transactional 어노테이션을 사용할 때 MES_System은 모든 서비스 메서드에 트랜잭션 컨텍스트를 전파해야 한다
5. 읽기 전용 작업이 수행되는 경우 MES_System은 최적화를 위해 트랜잭션을 읽기 전용으로 표시해야 한다

### 요구사항 13

**사용자 스토리:** 개발자로서, Spring Boot에서 Oracle 저장 프로시저 및 함수를 호출하여 데이터베이스 계층에서 복잡한 비즈니스 로직을 실행할 수 있어야 한다.

#### 인수 기준

1. 주문 합계를 계산할 때 MES_System은 TOTAL_AMOUNT를 업데이트하는 저장 프로시저를 호출해야 한다
2. 제품 가용성을 확인할 때 MES_System은 부울 결과를 반환하는 저장 함수를 호출해야 한다
3. JPA를 사용할 때 MES_System은 프로시저 호출을 위해 @NamedStoredProcedureQuery를 구성해야 한다
4. MyBatis를 사용할 때 MES_System은 XML 매퍼 파일에서 저장 프로시저 호출을 매핑해야 한다
5. OUT 매개변수가 있는 경우 MES_System은 프로시저 호출에서 출력 값을 적절히 검색해야 한다

### 요구사항 14

**사용자 스토리:** 개발자로서, 적절한 오류 처리 및 로깅을 구현하여 개발 및 마이그레이션 테스트 중에 문제를 진단할 수 있어야 한다.

#### 인수 기준

1. 데이터베이스 오류가 발생하면 MES_System은 오류 코드 및 메시지를 포함한 SQL 예외 세부 정보를 로깅해야 한다
2. 커넥션 풀 고갈이 발생하면 MES_System은 HikariCP 풀 통계를 로깅해야 한다
3. 제약 조건 위반이 발생하면 MES_System은 API 클라이언트에 의미 있는 오류 메시지를 반환해야 한다
4. LOB 작업이 실패하면 MES_System은 특정 LOB 관련 오류 정보를 로깅해야 한다
5. 성능 문제가 감지되면 MES_System은 실행 시간과 함께 느린 쿼리 경고를 로깅해야 한다

### 요구사항 15

**사용자 스토리:** 사용자로서, 웹 인터페이스를 통해 MES 기능에 액세스하여 API 도구를 사용하지 않고도 제품, 주문 및 재고를 관리할 수 있어야 한다.

#### 인수 기준

1. Spring_Boot 애플리케이션이 시작되면 MES_System은 웹 엔드포인트에서 Thymeleaf 템플릿을 제공해야 한다
2. 제품 목록 페이지에 액세스할 때 MES_System은 페이징이 있는 HTML 테이블에 모든 제품을 표시해야 한다
3. 제품 생성 양식을 제출할 때 MES_System은 입력 필드를 검증하고 성공 시 제품 목록으로 리디렉션해야 한다
4. 생산 주문을 볼 때 MES_System은 관련 제품 정보와 함께 주문 세부 정보를 렌더링해야 한다
5. 생산 주문을 생성할 때 MES_System은 제품 선택 드롭다운 및 날짜 선택기가 있는 양식을 제공해야 한다
6. 재고를 볼 때 MES_System은 낮은 재고에 대한 색상 코드 경고와 함께 현재 재고 수준을 표시해야 한다
7. 양식 검증이 실패하면 MES_System은 필드 강조 표시와 함께 동일한 페이지에 오류 메시지를 표시해야 한다
8. 페이지 간을 탐색할 때 MES_System은 모든 뷰에서 일관된 탐색 메뉴를 제공해야 한다
