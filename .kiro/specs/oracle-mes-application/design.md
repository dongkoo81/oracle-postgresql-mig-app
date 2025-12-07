# Design Document

## Overview

Oracle XE 기반 MES 애플리케이션은 Spring Boot 3.x를 사용하여 구축되며, Oracle 19c의 모든 특화 기능을 활용합니다. 이 시스템은 제품, 생산 주문, 재고, 생산 이력을 관리하며, 향후 AWS DMS를 통한 PostgreSQL 마이그레이션 테스트를 위해 다양한 Oracle 전용 기능을 의도적으로 사용합니다.

### 기술 스택

- **Backend Framework**: Spring Boot 3.2.x
- **Database**: Oracle 19c EE (ec2)
- **Connection Pool**: HikariCP
- **ORM**: JPA (Hibernate 6.x)
- **Query Builder**: QueryDSL 5.x
- **SQL Mapper**: MyBatis 3.5.x
- **Template Engine**: Thymeleaf 3.1.x
- **Build Tool**: Gradle 8.x
- **Java Version**: Java 17

### 아키텍처 원칙

1. **Layered Architecture**: Controller → Service → Repository 계층 분리
2. **Multiple Data Access**: JPA, QueryDSL, MyBatis, JDBC Template 혼용 사용
3. **Oracle Feature Maximization**: 가능한 모든 Oracle 특화 기능 활용
4. **DMS Migration Ready**: PostgreSQL 변환 시 이슈가 발생하도록 의도적 설계

## Architecture

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Web Browser                          │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP
┌────────────────────▼────────────────────────────────────┐
│              Spring Boot Application                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Presentation Layer                       │  │
│  │  ┌──────────────┐  ┌──────────────────────────┐ │  │
│  │  │ Thymeleaf    │  │   REST Controllers       │ │  │
│  │  │ Controllers  │  │   (@RestController)      │ │  │
│  │  └──────────────┘  └──────────────────────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │            Service Layer                         │  │
│  │  - Business Logic                                │  │
│  │  - Transaction Management (@Transactional)       │  │
│  │  - Stored Procedure Calls                        │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Data Access Layer                        │  │
│  │  ┌──────┐ ┌─────────┐ ┌────────┐ ┌───────────┐ │  │
│  │  │ JPA  │ │QueryDSL │ │MyBatis │ │   JDBC    │ │  │
│  │  │Repos │ │ Repos   │ │Mappers │ │ Template  │ │  │
│  │  └──────┘ └─────────┘ └────────┘ └───────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │            HikariCP Connection Pool              │  │
│  │  - Min: 5, Max: 20 connections                   │  │
│  │  - Leak Detection: 60s                           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ JDBC
┌────────────────────▼────────────────────────────────────┐
│              Oracle XE 21c (Docker)                     │
│  - Sequences, Triggers, Stored Procedures               │
│  - CLOB, BLOB, BFILE, XMLType                          │
│  - Materialized Views, Database Links                   │
└─────────────────────────────────────────────────────────┘
```

### 패키지 구조

```
com.example.mes
├── config/                    # 설정 클래스
│   ├── DataSourceConfig      # HikariCP, JPA 설정
│   ├── MyBatisConfig         # MyBatis 설정
│   └── QueryDslConfig        # QueryDSL 설정
├── domain/                    # 도메인 모델
│   ├── product/
│   │   ├── entity/           # JPA 엔티티
│   │   ├── repository/       # Repository 인터페이스
│   │   ├── service/          # 비즈니스 로직
│   │   └── controller/       # REST & Web 컨트롤러
│   ├── order/
│   ├── inventory/
│   ├── history/
│   └── document/
├── common/                    # 공통 컴포넌트
│   ├── exception/            # 예외 처리
│   ├── dto/                  # DTO 클래스
│   └── util/                 # 유틸리티
└── MesApplication.java       # 메인 클래스
```

## Components and Interfaces

### 1. Entity Layer (JPA)

모든 테이블에 대한 JPA 엔티티를 정의합니다.

**Product Entity 예시**:
```java
@Entity
@Table(name = "PRODUCT")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_ID")
    private Long productId;
    
    @Column(name = "PRODUCT_CODE", unique = true, nullable = false)
    private String productCode;
    
    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;
    
    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;
    
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private Inventory inventory;
    
    @OneToMany(mappedBy = "product")
    private List<ProductDocument> documents;
    
    // getters, setters
}
```

### 2. Repository Layer

**JPA Repository**:
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(String productCode);
    List<Product> findByIsActive(String isActive);
}
```

**QueryDSL Repository**:
```java
public interface ProductQueryDslRepository {
    Page<Product> searchProducts(ProductSearchCriteria criteria, Pageable pageable);
}

@Repository
public class ProductQueryDslRepositoryImpl implements ProductQueryDslRepository {
    private final JPAQueryFactory queryFactory;
    
    @Override
    public Page<Product> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();
        
        if (criteria.getProductName() != null) {
            builder.and(product.productName.contains(criteria.getProductName()));
        }
        // ... 동적 쿼리 구성
    }
}
```

**MyBatis Mapper**:
```java
@Mapper
public interface ProductionHistoryMapper {
    List<ProductionHistoryDto> findHierarchy(Long orderId);
    List<ProductionHistoryDto> findWithAnalyticFunctions(LocalDate startDate, LocalDate endDate);
}
```

**MyBatis XML**:
```xml
<select id="findHierarchy" resultType="ProductionHistoryDto">
    SELECT /*+ INDEX(ph IDX_HISTORY_ORDER) */
           HISTORY_ID, ORDER_ID, PARENT_ID, PROCESS_NAME,
           LEVEL as depth
    FROM PRODUCTION_HISTORY
    START WITH ORDER_ID = #{orderId} AND PARENT_ID IS NULL
    CONNECT BY PRIOR HISTORY_ID = PARENT_ID
    ORDER SIBLINGS BY HISTORY_ID
</select>
```

### 3. Service Layer

```java
@Service
@Transactional
public class ProductionOrderService {
    private final ProductionOrderRepository orderRepository;
    private final OrderDetailRepository detailRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public ProductionOrderDto createOrder(CreateOrderRequest request) {
        // 1. JPA로 주문 생성
        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(generateOrderNo());
        order = orderRepository.save(order);
        
        // 2. 상세 저장
        for (OrderDetailRequest detail : request.getDetails()) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            detailRepository.save(orderDetail);
        }
        
        // 3. Stored Procedure 호출 (총액 계산)
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("CALCULATE_ORDER_TOTAL")
            .declareParameters(
                new SqlParameter("p_order_id", Types.NUMERIC),
                new SqlOutParameter("p_total", Types.NUMERIC)
            );
        
        Map<String, Object> result = jdbcCall.execute(order.getOrderId());
        
        return convertToDto(order);
    }
}
```

### 4. Controller Layer

**REST Controller**:
```java
@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    private final ProductService productService;
    
    @GetMapping
    public Page<ProductDto> getProducts(
        @RequestParam(required = false) String name,
        Pageable pageable) {
        return productService.searchProducts(name, pageable);
    }
    
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

**Thymeleaf Controller**:
```java
@Controller
@RequestMapping("/products")
public class ProductWebController {
    private final ProductService productService;
    
    @GetMapping
    public String listProducts(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20) Pageable pageable,
        Model model) {
        Page<ProductDto> products = productService.searchProducts(name, pageable);
        model.addAttribute("products", products);
        model.addAttribute("searchName", name);
        return "products/list";
    }
    
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new CreateProductRequest());
        return "products/form";
    }
    
    @PostMapping
    public String createProduct(
        @Valid @ModelAttribute CreateProductRequest request,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "products/form";
        }
        productService.createProduct(request);
        redirectAttributes.addFlashAttribute("message", "제품이 등록되었습니다.");
        return "redirect:/products";
    }
}
```

### 5. Thymeleaf Templates

**레이아웃 구조**:
```
templates/
├── layout/
│   ├── default.html          # 기본 레이아웃
│   └── fragments/
│       ├── header.html       # 헤더 (네비게이션)
│       └── footer.html       # 푸터
├── products/
│   ├── list.html            # 제품 목록
│   └── form.html            # 제품 등록/수정
├── orders/
│   ├── list.html            # 주문 목록
│   └── form.html            # 주문 생성
└── inventory/
    └── list.html            # 재고 현황
```

**제품 목록 템플릿 예시**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/default}">
<head>
    <title>제품 관리</title>
</head>
<body>
<div layout:fragment="content">
    <h1>제품 목록</h1>
    
    <form th:action="@{/products}" method="get" class="search-form">
        <input type="text" name="name" th:value="${searchName}" placeholder="제품명 검색">
        <button type="submit">검색</button>
    </form>
    
    <a th:href="@{/products/new}" class="btn-primary">신규 등록</a>
    
    <table class="table">
        <thead>
            <tr>
                <th>제품코드</th>
                <th>제품명</th>
                <th>카테고리</th>
                <th>단가</th>
                <th>상태</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="product : ${products.content}">
                <td th:text="${product.productCode}"></td>
                <td th:text="${product.productName}"></td>
                <td th:text="${product.category}"></td>
                <td th:text="${#numbers.formatDecimal(product.unitPrice, 0, 'COMMA', 2, 'POINT')}"></td>
                <td th:text="${product.isActive == 'Y' ? '사용' : '미사용'}"></td>
            </tr>
        </tbody>
    </table>
    
    <!-- 페이징 -->
    <div th:if="${products.totalPages > 1}">
        <ul class="pagination">
            <li th:each="i : ${#numbers.sequence(0, products.totalPages - 1)}">
                <a th:href="@{/products(page=${i}, name=${searchName})}" 
                   th:text="${i + 1}"
                   th:classappend="${i == products.number} ? 'active'"></a>
            </li>
        </ul>
    </div>
</div>
</body>
</html>
```

## Data Models

### 데이터베이스 스키마

8개의 테이블로 구성되며, 각 테이블은 Oracle 특화 기능을 활용합니다.

### 테이블 관계도

```
PRODUCT (제품)
  ├─ 1:1 → INVENTORY (재고)
  ├─ 1:1 → PRODUCT_SPEC (스펙 - XMLType)
  ├─ 1:N → PRODUCT_DOCUMENT (문서 - LOB)
  └─ 1:N → ORDER_DETAIL (주문상세)

PRODUCTION_ORDER (생산주문)
  ├─ 1:N → ORDER_DETAIL (주문상세)
  └─ 1:N → PRODUCTION_HISTORY (생산이력 - 계층구조)

PRODUCTION_HISTORY (생산이력)
  └─ 자기참조 (PARENT_ID) - CONNECT BY 테스트용

DAILY_SUMMARY (일일집계)
  └─ Materialized View 소스
```

### 주요 엔티티 상세

#### 1. Product (제품)
- **PK**: PRODUCT_ID (Sequence 생성)
- **UK**: PRODUCT_CODE
- **관계**: Inventory (1:1), ProductDocument (1:N), ProductSpec (1:1)

#### 2. ProductionOrder (생산주문)
- **PK**: ORDER_ID (Sequence 생성)
- **UK**: ORDER_NO
- **특징**: 
  - NOTES 컬럼 (CLOB)
  - Trigger로 PRODUCTION_HISTORY 자동 생성
- **관계**: OrderDetail (1:N), ProductionHistory (1:N)

#### 3. ProductionHistory (생산이력)
- **PK**: HISTORY_ID
- **FK**: ORDER_ID, PARENT_ID (자기참조)
- **특징**: CONNECT BY 계층 쿼리 테스트용

#### 4. ProductDocument (제품문서)
- **PK**: DOCUMENT_ID
- **FK**: PRODUCT_ID
- **특징**: 
  - DOC_CONTENT (CLOB)
  - DOC_FILE (BLOB)
  - EXTERNAL_FILE (BFILE)

#### 5. ProductSpec (제품스펙)
- **PK**: SPEC_ID
- **FK**: PRODUCT_ID (UK)
- **특징**: SPEC_XML (XMLTYPE)

#### 6. Inventory (재고)
- **PK**: INVENTORY_ID
- **FK**: PRODUCT_ID (UK)
- **특징**: Optimistic Locking (@Version)

#### 7. OrderDetail (주문상세)
- **PK**: DETAIL_ID
- **FK**: ORDER_ID, PRODUCT_ID

#### 8. DailySummary (일일집계)
- **PK**: SUMMARY_ID
- **UK**: SUMMARY_DATE
- **특징**: Materialized View 소스

### Oracle 특화 기능 매핑

| Oracle 기능 | 사용 위치 | 목적 |
|------------|----------|------|
| Sequence | 모든 PK | 자동 증가 ID 생성 |
| Trigger | PRODUCTION_ORDER | 주문 생성 시 이력 자동 기록 |
| CLOB | PRODUCTION_ORDER.NOTES, PRODUCT_DOCUMENT.DOC_CONTENT | 대용량 텍스트 |
| BLOB | PRODUCT_DOCUMENT.DOC_FILE | 바이너리 파일 |
| BFILE | PRODUCT_DOCUMENT.EXTERNAL_FILE | 외부 파일 참조 |
| XMLTYPE | PRODUCT_SPEC.SPEC_XML | XML 문서 저장 |
| CONNECT BY | PRODUCTION_HISTORY | 계층 쿼리 |
| Hint | 복잡한 조회 쿼리 | 성능 최적화 |
| Stored Procedure | CALCULATE_ORDER_TOTAL | 주문 총액 계산 |
| Stored Function | CHECK_PRODUCT_AVAILABLE | 제품 가용성 확인 |
| Database Link | 자기 자신 참조 | 마이그레이션 테스트 |
| Materialized View | DAILY_SUMMARY | 집계 데이터 |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

많은 요구사항이 설정 검증이나 예제 테스트에 해당하므로, 핵심 비즈니스 로직과 데이터 무결성에 집중하는 속성들을 선별합니다.

**중복 제거**:
- 2.1과 4.5는 모두 시퀀스 사용을 검증 → 2.1로 통합
- 6.2와 9.5는 모두 CONNECT BY 쿼리 검증 → 하나의 속성으로 통합
- 10.2와 2.4는 BLOB 저장 검증 → 10.2로 통합
- 10.3과 2.5는 BFILE 저장 검증 → 10.3으로 통합
- 11.1과 11.5는 XML 검증 → 하나의 속성으로 통합

**통합 가능한 속성**:
- 4.2와 4.3은 타입 매핑 검증 → 하나의 round-trip 속성으로 통합
- 11.2, 11.3, 11.4는 XML round-trip → 하나의 속성으로 통합

### Correctness Properties

Property 1: Sequence-generated IDs are unique and positive
*For any* entity insertion across all tables, the generated ID should be a positive number and unique within that table
**Validates: Requirements 2.1**

Property 2: Order creation triggers history record
*For any* production order creation, a corresponding production history record should be automatically created with matching ORDER_ID
**Validates: Requirements 2.2**

Property 3: BLOB storage round-trip
*For any* binary file data, storing it in a BLOB column and retrieving it should return identical byte content
**Validates: Requirements 2.4, 10.2**

Property 4: BFILE reference storage
*For any* external file reference, storing directory and filename in BFILE should allow retrieval of the same reference
**Validates: Requirements 2.5, 10.3**

Property 5: Hierarchical query completeness
*For any* production history tree, a CONNECT BY query should return all nodes in the hierarchy with correct parent-child relationships
**Validates: Requirements 2.7, 6.2, 9.5**

Property 6: Stored procedure calculation correctness
*For any* production order with details, calling the CALCULATE_ORDER_TOTAL stored procedure should set TOTAL_AMOUNT equal to the sum of all LINE_AMOUNT values
**Validates: Requirements 2.9, 13.1**

Property 7: Stored function availability check
*For any* product, the CHECK_PRODUCT_AVAILABLE stored function should return true if and only if the product's IS_ACTIVE flag is 'Y' and inventory quantity is greater than zero
**Validates: Requirements 2.10, 13.2**

Property 8: JPA type mapping round-trip
*For any* entity with NUMBER, DATE, and TIMESTAMP columns, saving and loading the entity should preserve all numeric and temporal values exactly
**Validates: Requirements 4.2, 4.3**

Property 9: QueryDSL dynamic query correctness
*For any* combination of search criteria, a QueryDSL query should return only entities that match all specified predicates
**Validates: Requirements 5.2, 5.5**

Property 10: QueryDSL projection mapping
*For any* entity query with projections, the resulting DTO should contain values that match the corresponding entity fields
**Validates: Requirements 5.4**

Property 11: MyBatis analytic function correctness
*For any* dataset queried with ROW_NUMBER, the row numbers should be sequential starting from 1 with no gaps or duplicates within each partition
**Validates: Requirements 6.3**

Property 12: CLOB round-trip
*For any* text content up to 4GB, storing it in a CLOB column and retrieving it should return identical text
**Validates: Requirements 6.4, 10.1, 10.4**

Property 13: MyBatis batch insert atomicity
*For any* collection of records, a batch insert should either insert all records or none if an error occurs
**Validates: Requirements 6.5**

Property 14: JDBC Template stored procedure OUT parameters
*For any* stored procedure call with OUT parameters, the returned values should match the procedure's output
**Validates: Requirements 8.2, 13.5**

Property 15: JDBC Template type mapping
*For any* result set from a JDBC query, RowMapper should correctly map Oracle types to Java types preserving all values
**Validates: Requirements 8.3**

Property 16: JDBC Template batch operation atomicity
*For any* collection of SQL statements, batchUpdate should either execute all statements or none if an error occurs
**Validates: Requirements 8.4**

Property 17: REST API validation
*For any* product creation request with missing required fields, the API should return a validation error and not insert any record
**Validates: Requirements 9.1**

Property 18: Order number generation uniqueness
*For any* two production orders created, they should have different ORDER_NO values generated from the sequence
**Validates: Requirements 9.2**

Property 19: Optimistic locking prevents concurrent updates
*For any* inventory record, if two concurrent updates attempt to modify it, one should succeed and the other should fail with an optimistic locking exception
**Validates: Requirements 9.4**

Property 20: BLOB retrieval access
*For any* BLOB data stored, retrieving it should provide access as either a byte array or InputStream with identical content
**Validates: Requirements 10.5**

Property 21: XML validation rejects malformed input
*For any* malformed XML string, attempting to store it in an XMLTYPE column should fail with a validation error
**Validates: Requirements 11.1, 11.5**

Property 22: XML round-trip preservation
*For any* well-formed XML document, storing it in XMLTYPE and retrieving it should return semantically equivalent XML
**Validates: Requirements 11.2, 11.3, 11.4**

Property 23: Multi-table transaction atomicity
*For any* production order with details, if an error occurs during insertion, both PRODUCTION_ORDER and ORDER_DETAIL tables should have no new records
**Validates: Requirements 12.1, 12.3**

Property 24: Trigger participation in transaction
*For any* production order creation that is rolled back, the trigger-generated PRODUCTION_HISTORY record should also be rolled back
**Validates: Requirements 12.2**

Property 25: Transaction propagation
*For any* service method call chain with @Transactional, all database operations should participate in the same transaction
**Validates: Requirements 12.4**

Property 26: Database error logging
*For any* SQL exception, the log should contain the error code and message
**Validates: Requirements 14.1**

Property 27: Constraint violation error messages
*For any* constraint violation, the API response should contain a meaningful error message identifying the constraint
**Validates: Requirements 14.3**

Property 28: LOB operation error logging
*For any* LOB operation failure, the log should contain specific LOB-related error information
**Validates: Requirements 14.4**

Property 29: Product list pagination
*For any* set of products, the product list page should display them in pages with correct page numbers and navigation
**Validates: Requirements 15.2**

Property 30: Form submission validation and redirect
*For any* valid product creation form submission, the system should validate inputs, create the product, and redirect to the product list
**Validates: Requirements 15.3**

Property 31: Order rendering with product information
*For any* production order, the order detail view should display all related product information correctly
**Validates: Requirements 15.4**

Property 32: Inventory color-coded warnings
*For any* inventory with quantity below a threshold, the display should show a color-coded warning
**Validates: Requirements 15.6**

Property 33: Form validation error display
*For any* invalid form input, the same page should be redisplayed with error messages and field highlighting
**Validates: Requirements 15.7**

## Error Handling

### Exception Hierarchy

```
MesException (RuntimeException)
├── DatabaseException
│   ├── SequenceGenerationException
│   ├── TriggerExecutionException
│   ├── StoredProcedureException
│   └── LobOperationException
├── ValidationException
│   ├── ProductValidationException
│   ├── OrderValidationException
│   └── XmlValidationException
├── ConcurrencyException
│   └── OptimisticLockException
└── ConfigurationException
    ├── ConnectionPoolException
    └── DataSourceException
```

### Error Handling Strategy

1. **Controller Level**:
   - `@ExceptionHandler` for REST API exceptions
   - Return appropriate HTTP status codes (400, 404, 409, 500)
   - Return structured error responses with error codes

2. **Service Level**:
   - Catch specific SQL exceptions
   - Transform to domain exceptions
   - Log with context information

3. **Repository Level**:
   - Let JPA/MyBatis exceptions propagate
   - Add context in service layer

4. **Global Exception Handler**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONCURRENT_UPDATE", "Data was modified by another user"));
    }
    
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSqlException(SQLException ex) {
        log.error("SQL Error: Code={}, Message={}", ex.getErrorCode(), ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("DATABASE_ERROR", "Database operation failed"));
    }
}
```

### Logging Strategy

1. **SLF4J with Logback**:
   - INFO: Business operations (order created, product updated)
   - WARN: Recoverable errors (validation failures, optimistic lock conflicts)
   - ERROR: System errors (SQL exceptions, connection pool exhaustion)

2. **Structured Logging**:
```java
log.info("Order created: orderId={}, orderNo={}, customerId={}", 
    order.getOrderId(), order.getOrderNo(), order.getCustomerId());

log.error("SQL Exception: errorCode={}, sqlState={}, message={}", 
    ex.getErrorCode(), ex.getSQLState(), ex.getMessage(), ex);
```

3. **Performance Logging**:
   - Log slow queries (> 1 second)
   - Log connection pool statistics on exhaustion
   - Log LOB operation sizes and durations

## Testing Strategy

### Dual Testing Approach

이 프로젝트는 **Unit Testing**과 **Property-Based Testing**을 모두 사용합니다:

- **Unit Tests**: 특정 예제, 엣지 케이스, 설정 검증
- **Property Tests**: 모든 입력에 대해 성립해야 하는 보편적 속성 검증

두 가지 테스트 방식은 상호 보완적입니다:
- Unit tests는 구체적인 버그를 잡아냅니다
- Property tests는 일반적인 정확성을 검증합니다

### Property-Based Testing

**프레임워크**: JUnit-Quickcheck 또는 jqwik

**설정**:
- 각 property test는 최소 100회 반복 실행
- 각 property test는 디자인 문서의 correctness property를 명시적으로 참조
- 태그 형식: `**Feature: oracle-mes-application, Property {number}: {property_text}**`

**예시**:
```java
/**
 * Feature: oracle-mes-application, Property 1: Sequence-generated IDs are unique and positive
 */
@Property(tries = 100)
void sequenceGeneratedIdsAreUniqueAndPositive(@ForAll("validProducts") Product product) {
    Product saved = productRepository.save(product);
    
    assertThat(saved.getProductId()).isPositive();
    
    Product saved2 = productRepository.save(createAnotherProduct());
    assertThat(saved2.getProductId()).isNotEqualTo(saved.getProductId());
}

@Provide
Arbitrary<Product> validProducts() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofLength(10),
        Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100),
        Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("999999.99"))
    ).as((code, name, price) -> {
        Product p = new Product();
        p.setProductCode(code);
        p.setProductName(name);
        p.setUnitPrice(price);
        return p;
    });
}
```

### Unit Testing

**프레임워크**: JUnit 5, Mockito, Spring Boot Test

**테스트 범위**:
1. **Configuration Tests**: HikariCP 설정, MyBatis 매퍼 등록 확인
2. **Entity Tests**: JPA 엔티티 매핑, 관계 설정 확인
3. **Repository Tests**: 기본 CRUD, 커스텀 쿼리 동작 확인
4. **Service Tests**: 비즈니스 로직, 트랜잭션 동작 확인
5. **Controller Tests**: REST API 엔드포인트, 요청/응답 검증
6. **Integration Tests**: 전체 플로우, 데이터베이스 통합 테스트

**예시**:
```java
@SpringBootTest
@Transactional
class ProductServiceTest {
    
    @Autowired
    private ProductService productService;
    
    @Test
    void shouldCreateProductWithSequenceId() {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductCode("TEST001");
        request.setProductName("Test Product");
        
        ProductDto created = productService.createProduct(request);
        
        assertThat(created.getProductId()).isNotNull();
        assertThat(created.getProductId()).isPositive();
    }
    
    @Test
    void shouldRejectDuplicateProductCode() {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductCode("DUP001");
        request.setProductName("Duplicate Test");
        
        productService.createProduct(request);
        
        assertThatThrownBy(() -> productService.createProduct(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Product code already exists");
    }
}
```

### Test Data Management

**Testcontainers for Oracle XE**:
```java
@Testcontainers
@SpringBootTest
class IntegrationTestBase {
    
    @Container
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withInitScript("init-test-schema.sql");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracle::getJdbcUrl);
        registry.add("spring.datasource.username", oracle::getUsername);
        registry.add("spring.datasource.password", oracle::getPassword());
    }
}
```

### Test Coverage Goals

- **Line Coverage**: 80% 이상
- **Branch Coverage**: 70% 이상
- **Property Tests**: 모든 correctness properties 구현
- **Integration Tests**: 주요 비즈니스 플로우 커버

## Deployment

### Docker Compose 구성

```yaml
version: '3.8'

services:
  oracle-xe:
    image: gvenzl/oracle-xe:21-slim
    container_name: mes-oracle-xe
    ports:
      - "1521:1521"
    environment:
      ORACLE_PASSWORD: oracle
      APP_USER: mesuser
      APP_USER_PASSWORD: mespass
    volumes:
      - ./sql/oracle:/docker-entrypoint-initdb.d
      - oracle-data:/opt/oracle/oradata
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 30s
      timeout: 10s
      retries: 5

  mes-app:
    build: .
    container_name: mes-application
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:oracle:thin:@//oracle-xe:1521/XEPDB1
      SPRING_DATASOURCE_USERNAME: mesuser
      SPRING_DATASOURCE_PASSWORD: mespass
    depends_on:
      oracle-xe:
        condition: service_healthy

volumes:
  oracle-data:
```

### Application Configuration

**application.yml**:
```yaml
spring:
  application:
    name: mes-application
  
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/XEPDB1
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      connection-test-query: SELECT 1 FROM DUAL
  
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
  
  thymeleaf:
    cache: false
    prefix: classpath:/templates/
    suffix: .html

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.mes.domain.*.entity
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
    default-statement-timeout: 30

logging:
  level:
    com.example.mes: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.zaxxer.hikari: DEBUG
```

### Build Configuration

**build.gradle**:
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Oracle
    implementation 'com.oracle.database.jdbc:ojdbc11:23.3.0.23.09'
    implementation 'com.oracle.database.jdbc:ucp:23.3.0.23.09'
    
    // MyBatis
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    
    // Utilities
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:oracle-xe:1.19.3'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'net.jqwik:jqwik:1.8.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## DMS Migration Considerations

### Oracle 기능별 PostgreSQL 변환 예상 이슈

| Oracle 기능 | PostgreSQL 대응 | DMS 자동 변환 | 수동 작업 필요 |
|------------|----------------|--------------|--------------|
| Sequence | Sequence | ✓ | 문법 차이 |
| Trigger | Trigger | ✓ | 문법 완전히 다름 |
| CLOB | TEXT | ✓ | - |
| BLOB | BYTEA | ✓ | - |
| BFILE | VARCHAR | ✗ | 파일 경로만 저장 |
| XMLTYPE | XML | ✓ | 함수 차이 |
| CONNECT BY | WITH RECURSIVE | ✗ | 완전 재작성 |
| Hint | pg_hint_plan | ✗ | 확장 설치 필요 |
| Stored Procedure | Function | 부분 | 문법 차이 |
| Database Link | postgres_fdw | ✗ | 완전히 다른 방식 |
| Materialized View | Materialized View | ✓ | 문법 차이 |

### DMS GenAI 테스트 대상

1. **MyBatis XML**: CONNECT BY, Analytic Functions, Hints
2. **JPA Native Query**: @Query 어노테이션의 Oracle SQL
3. **JDBC Template**: Java 문자열 리터럴의 Oracle SQL

### 마이그레이션 후 검증 항목

1. 모든 Property Tests 재실행
2. 데이터 무결성 검증
3. 성능 비교 (쿼리 실행 시간)
4. 트랜잭션 동작 확인
5. LOB 데이터 검증
