# Implementation Plan

## Database Connection Info
- **Host**: 10.1.5.18 (EC2 Oracle 19c)
- **Port**: 1521
- **Service Name**: oracle19c
- **System Account**: system / system 
- **App User**: mesuser / mespass

## Phase 1: Infrastructure Setup

- [x] 1. Set up project structure and dependencies
  - [x] Create Spring Boot project with Gradle
  - [x] Configure build.gradle with all dependencies (Spring Boot, Oracle JDBC, MyBatis, QueryDSL, Thymeleaf, jqwik)
  - [x] Set up package structure (config, domain, common)
  - [x] Configure application.yml with HikariCP and database settings (10.1.5.18:1521)

- [ ] 1.1 Write unit tests for configuration
  - [ ] Test HikariCP configuration values
  - [ ] Test datasource connection to 10.1.5.18

- [x] 2. Create MES user and schema on existing Oracle 19c
  - [x] Create sql/01_create_user.sql script
  - [x] Create user mesuser with password mespass
  - [x] Grant necessary privileges

- [x] 3. Create database schema and Oracle objects
  - [x] Create all 8 tables with constraints and indexes (sql/schema/02_create_tables.sql)
  - [x] Create sequences for all primary keys
  - [x] Create trigger for PRODUCTION_ORDER → PRODUCTION_HISTORY (sql/procedures/03_create_procedures.sql)
  - [x] Create stored procedure CALCULATE_ORDER_TOTAL
  - [x] Create stored function CHECK_PRODUCT_AVAILABLE
  - [x] Create sample data script (sql/data/04_insert_sample_data.sql)

## Phase 2: Domain Implementation

- [x] 4. Implement JPA entities for all tables
  - [x] Product entity with @SequenceGenerator
  - [x] ProductionOrder entity with CLOB mapping for NOTES
  - [x] OrderDetail entity with relationships
  - [x] Inventory entity with optimistic locking
  - [x] ProductionHistory entity with hierarchical structure
  - [x] ProductDocument entity with CLOB/BLOB
  - [x] ProductSpec entity with XML support
  - [x] DailySummary entity

- [x] 5. Implement Product domain (완료)
  - [x] ProductRepository (JPA)
  - [x] ProductService
  - [x] ProductRestController (REST API)
  - [x] ProductWebController (Thymeleaf)
  - [x] Thymeleaf templates (list.html, form.html)
  - [x] CSS styling

- [ ] 6. Implement Order domain
  - [x] ProductionOrderRepository (JPA)
  - [x] OrderDetailRepository (JPA)
  - [ ] OrderService
  - [ ] OrderRestController (REST API)
  - [ ] OrderWebController (Thymeleaf)
  - [ ] Thymeleaf templates

- [ ] 7. Implement Inventory domain
  - [x] InventoryRepository (JPA)
  - [ ] InventoryService
  - [ ] InventoryRestController (REST API)
  - [ ] InventoryWebController (Thymeleaf)
  - [ ] Thymeleaf templates

- [ ] 8. Implement History domain
  - [x] ProductionHistoryRepository (JPA)
  - [ ] HistoryService (with CONNECT BY hierarchical query)
  - [ ] HistoryRestController (REST API)
  - [ ] HistoryWebController (Thymeleaf)
  - [ ] Thymeleaf templates

- [ ] 9. Implement Document domain
  - [x] ProductDocumentRepository (JPA)
  - [ ] DocumentService (CLOB/BLOB handling)
  - [ ] DocumentRestController (REST API)
  - [ ] File upload/download functionality

- [ ] 10. Implement Spec domain
  - [x] ProductSpecRepository (JPA)
  - [ ] SpecService (XML handling)
  - [ ] SpecRestController (REST API)
  - [ ] XML validation

## Phase 3: Advanced Features

- [ ] 11. Implement MyBatis mappers
  - [ ] Create mapper XML files in resources/mapper
  - [ ] Complex queries using MyBatis
  - [ ] Stored procedure calls

- [ ] 12. Implement QueryDSL queries
  - [ ] Generate Q-classes
  - [ ] Complex search queries
  - [ ] Dynamic filtering

- [ ] 13. Oracle-specific features
  - [ ] Database link usage
  - [ ] Materialized view refresh
  - [ ] CONNECT BY hierarchical queries
  - [ ] XMLType operations

## Phase 4: Testing & Documentation

- [ ] 14. Write unit tests
  - [ ] Entity tests
  - [ ] Repository tests
  - [ ] Service tests
  - [ ] Controller tests

- [ ] 15. Write integration tests
  - [ ] Database integration tests
  - [ ] API integration tests
  - [ ] End-to-end tests

- [ ] 16. AWS DMS preparation
  - [ ] Document Oracle-specific features
  - [ ] Create PostgreSQL migration plan
  - [ ] Test data migration scenarios
  