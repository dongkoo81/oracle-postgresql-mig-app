# Requirements Document

## Introduction

Oracle 19c 기반 MES(Manufacturing Execution System) 애플리케이션 개발 프로젝트입니다. 이 시스템은 Oracle 19c의 모든 특화 기능(Partition, Sequence, Trigger, CLOB/BLOB/BFILE, XMLType, 계층 쿼리 등)을 활용하여 구현되며, 향후 AWS DMS를 통한 PostgreSQL 마이그레이션 시 다양한 변환 이슈를 경험하고 해결하는 것을 목표로 합니다.

**Oracle 연결 정보:**
- IP: 10.1.5.18
- Instance: oracle19c
- System Account: system/system
- SYS Account: sys/sys
- Application Account: mesuser/mespass

## Glossary

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

## Requirements

### Requirement 1

**User Story:** As a developer, I want to set up Oracle XE in Docker, so that I can develop the MES application locally without installing Oracle directly on my machine.

#### Acceptance Criteria

1. WHEN the Docker Compose file is executed THEN the MES_System SHALL create an Oracle XE container with port 1521 exposed
2. WHEN the Oracle XE container starts THEN the MES_System SHALL automatically execute user creation scripts and table creation scripts
3. WHEN the database initialization completes THEN the MES_System SHALL have all 8 tables created with proper constraints and indexes
4. WHEN the Oracle XE container is running THEN the MES_System SHALL accept connections using JDBC URL with service name
5. WHERE health check is configured THEN the MES_System SHALL verify Oracle listener status before marking container as healthy

### Requirement 2

**User Story:** As a developer, I want to use Oracle-specific features in the database schema, so that I can experience various conversion issues when migrating to PostgreSQL via DMS.

#### Acceptance Criteria

1. WHEN inserting records THEN the MES_System SHALL use Oracle sequences for all primary key generation
2. WHEN a production order is created THEN the MES_System SHALL automatically create a production history record via trigger
3. WHEN storing large text data THEN the MES_System SHALL use CLOB type for NOTES and DOC_CONTENT columns
4. WHEN storing binary files THEN the MES_System SHALL use BLOB type for DOC_FILE column
5. WHEN referencing external files THEN the MES_System SHALL use BFILE type for EXTERNAL_FILE column
6. WHEN storing product specifications THEN the MES_System SHALL use XMLTYPE for SPEC_XML column
7. WHEN querying production history hierarchy THEN the MES_System SHALL support CONNECT BY queries with PARENT_ID self-reference
8. WHEN using Oracle hints THEN the MES_System SHALL include INDEX, FULL, and PARALLEL hints in complex queries
9. WHEN calculating business logic THEN the MES_System SHALL use stored procedures for order amount calculation
10. WHEN validating data THEN the MES_System SHALL use stored functions to check product availability
11. WHEN testing database link functionality THEN the MES_System SHALL create a loopback database link pointing to itself for migration testing
12. WHEN creating materialized views THEN the MES_System SHALL define refresh schedules for summary data

### Requirement 3

**User Story:** As a developer, I want to build a Spring Boot application with HikariCP connection pooling, so that I can efficiently manage database connections to Oracle XE.

#### Acceptance Criteria

1. WHEN the Spring_Boot application starts THEN the MES_System SHALL configure HikariCP with Oracle JDBC driver
2. WHEN configuring the connection pool THEN the MES_System SHALL set minimum idle connections to 5 and maximum pool size to 20
3. WHEN the application connects to Oracle THEN the MES_System SHALL use the JDBC URL format jdbc:oracle:thin:@//host:port/service
4. WHEN connection pool is initialized THEN the MES_System SHALL validate connections using a test query
5. WHERE connection leaks occur THEN the MES_System SHALL detect and log leak warnings after 60 seconds

### Requirement 4

**User Story:** As a developer, I want to implement JPA entities for all MES tables, so that I can use object-relational mapping for basic CRUD operations.

#### Acceptance Criteria

1. WHEN defining JPA entities THEN the MES_System SHALL create entity classes for all 8 tables with proper annotations
2. WHEN mapping Oracle NUMBER types THEN the MES_System SHALL use appropriate Java types (Long for IDs, BigDecimal for amounts)
3. WHEN mapping Oracle DATE and TIMESTAMP THEN the MES_System SHALL use LocalDate and LocalDateTime respectively
4. WHEN mapping relationships THEN the MES_System SHALL define @OneToMany and @ManyToOne associations with proper fetch strategies
5. WHEN using sequences THEN the MES_System SHALL configure @SequenceGenerator with Oracle sequence names

### Requirement 5

**User Story:** As a developer, I want to use QueryDSL for type-safe dynamic queries, so that I can build complex search conditions without string concatenation.

#### Acceptance Criteria

1. WHEN configuring QueryDSL THEN the MES_System SHALL generate Q-classes for all JPA entities
2. WHEN building dynamic search queries THEN the MES_System SHALL use BooleanBuilder for conditional predicates
3. WHEN joining tables THEN the MES_System SHALL use QueryDSL join syntax with fetch joins for performance
4. WHEN projecting specific columns THEN the MES_System SHALL use Projections for DTO mapping
5. WHERE complex filtering is required THEN the MES_System SHALL combine multiple predicates using and/or operations

### Requirement 6

**User Story:** As a developer, I want to use MyBatis for complex Oracle-specific queries, so that I can leverage native SQL features like CONNECT BY and analytic functions.

#### Acceptance Criteria

1. WHEN configuring MyBatis THEN the MES_System SHALL register XML mapper files for each domain
2. WHEN querying production history hierarchy THEN the MES_System SHALL execute CONNECT BY queries via MyBatis XML mapper
3. WHEN using analytic functions THEN the MES_System SHALL support ROW_NUMBER, RANK, and LAG/LEAD window functions in MyBatis queries
4. WHEN handling CLOB data THEN the MES_System SHALL properly map CLOB columns to String in result maps
5. WHEN executing batch inserts THEN the MES_System SHALL use MyBatis batch executor for performance
6. WHEN using Oracle hints in MyBatis THEN the MES_System SHALL include hint comments in SELECT statements

### Requirement 7

**User Story:** As a developer, I want to use JPA Native Queries with Oracle SQL, so that I can test DMS GenAI conversion of embedded SQL in Java annotations.

#### Acceptance Criteria

1. WHEN using @Query annotation THEN the MES_System SHALL include Oracle-specific SQL syntax with CONNECT BY
2. WHEN writing native queries THEN the MES_System SHALL use Oracle date functions like TO_DATE and TRUNC
3. WHEN querying with hints THEN the MES_System SHALL embed Oracle hint comments in @Query SQL strings
4. WHEN using analytic functions in @Query THEN the MES_System SHALL include OVER clauses with PARTITION BY
5. WHERE dual table is needed THEN the MES_System SHALL reference Oracle DUAL table in native queries

### Requirement 8

**User Story:** As a developer, I want to use JDBC Template with Oracle SQL, so that I can test DMS GenAI conversion of programmatic SQL execution.

#### Acceptance Criteria

1. WHEN executing queries via JDBC_Template THEN the MES_System SHALL use Oracle-specific SQL syntax in Java string literals
2. WHEN calling stored procedures THEN the MES_System SHALL use SimpleJdbcCall with Oracle procedure names
3. WHEN handling result sets THEN the MES_System SHALL map Oracle types to Java types using RowMapper
4. WHEN executing batch operations THEN the MES_System SHALL use batchUpdate with Oracle SQL statements
5. WHERE complex queries are needed THEN the MES_System SHALL construct SQL strings with Oracle functions

### Requirement 9

**User Story:** As a developer, I want to implement REST APIs for MES operations, so that I can perform CRUD operations on products, orders, and inventory.

#### Acceptance Criteria

1. WHEN the REST API receives a product creation request THEN the MES_System SHALL validate required fields and insert into PRODUCT table
2. WHEN creating a production order THEN the MES_System SHALL generate ORDER_NO using sequence and insert with ORDER_DATE
3. WHEN querying orders by date range THEN the MES_System SHALL use indexed ORDER_DATE column for performance
4. WHEN updating inventory THEN the MES_System SHALL use optimistic locking to prevent concurrent update conflicts
5. WHEN retrieving production history THEN the MES_System SHALL return hierarchical data using CONNECT BY query results

### Requirement 10

**User Story:** As a developer, I want to handle LOB types (CLOB, BLOB, BFILE) in the application, so that I can store and retrieve large documents and files.

#### Acceptance Criteria

1. WHEN storing product documents with CLOB content THEN the MES_System SHALL handle text content up to 4GB
2. WHEN uploading binary files THEN the MES_System SHALL store file data in BLOB column and metadata separately
3. WHEN referencing external files THEN the MES_System SHALL store BFILE directory and filename references
4. WHEN retrieving CLOB data via JPA THEN the MES_System SHALL properly stream large text content
5. WHEN reading BLOB data THEN the MES_System SHALL provide byte array or InputStream access

### Requirement 11

**User Story:** As a developer, I want to work with XMLType columns, so that I can store and query structured product specifications in XML format.

#### Acceptance Criteria

1. WHEN inserting product specifications THEN the MES_System SHALL validate XML structure before storing in XMLTYPE column
2. WHEN querying XML data via MyBatis THEN the MES_System SHALL map XMLTYPE to String representation
3. WHEN updating XML specifications THEN the MES_System SHALL replace entire XML document atomically
4. WHERE XML parsing is required THEN the MES_System SHALL use Java XML libraries to parse retrieved XML strings
5. WHEN storing XML THEN the MES_System SHALL ensure well-formed XML structure

### Requirement 12

**User Story:** As a developer, I want to implement transaction management, so that I can ensure data consistency across multiple table operations.

#### Acceptance Criteria

1. WHEN creating a production order with details THEN the MES_System SHALL insert into PRODUCTION_ORDER and ORDER_DETAIL tables within a single transaction
2. WHEN a trigger fires during order creation THEN the MES_System SHALL include trigger-generated PRODUCTION_HISTORY records in the same transaction
3. WHEN an error occurs during multi-table insert THEN the MES_System SHALL rollback all changes
4. WHEN using @Transactional annotation THEN the MES_System SHALL propagate transaction context to all service methods
5. WHERE read-only operations are performed THEN the MES_System SHALL mark transactions as read-only for optimization

### Requirement 13

**User Story:** As a developer, I want to call Oracle stored procedures and functions from Spring Boot, so that I can execute complex business logic in the database layer.

#### Acceptance Criteria

1. WHEN calculating order totals THEN the MES_System SHALL call a stored procedure that updates TOTAL_AMOUNT
2. WHEN checking product availability THEN the MES_System SHALL call a stored function that returns boolean result
3. WHEN using JPA THEN the MES_System SHALL configure @NamedStoredProcedureQuery for procedure calls
4. WHEN using MyBatis THEN the MES_System SHALL map stored procedure calls in XML mapper files
5. WHERE OUT parameters exist THEN the MES_System SHALL properly retrieve output values from procedure calls

### Requirement 14

**User Story:** As a developer, I want to implement proper error handling and logging, so that I can diagnose issues during development and migration testing.

#### Acceptance Criteria

1. WHEN database errors occur THEN the MES_System SHALL log SQL exception details including error code and message
2. WHEN connection pool exhaustion happens THEN the MES_System SHALL log HikariCP pool statistics
3. WHEN constraint violations occur THEN the MES_System SHALL return meaningful error messages to API clients
4. WHEN LOB operations fail THEN the MES_System SHALL log specific LOB-related error information
5. WHERE performance issues are detected THEN the MES_System SHALL log slow query warnings with execution time

### Requirement 15

**User Story:** As a user, I want to access MES functions through a web interface, so that I can manage products, orders, and inventory without using API tools.

#### Acceptance Criteria

1. WHEN the Spring_Boot application starts THEN the MES_System SHALL serve Thymeleaf templates at web endpoints
2. WHEN accessing the product list page THEN the MES_System SHALL display all products in an HTML table with pagination
3. WHEN submitting a product creation form THEN the MES_System SHALL validate input fields and redirect to the product list on success
4. WHEN viewing production orders THEN the MES_System SHALL render order details with related product information
5. WHEN creating a production order THEN the MES_System SHALL provide a form with product selection dropdown and date picker
6. WHEN viewing inventory THEN the MES_System SHALL display current stock levels with color-coded warnings for low stock
7. WHERE form validation fails THEN the MES_System SHALL display error messages on the same page with field highlighting
8. WHEN navigating between pages THEN the MES_System SHALL provide a consistent navigation menu across all views
