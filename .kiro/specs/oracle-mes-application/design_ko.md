# 설계 문서

## 개요

Oracle XE 기반 MES 애플리케이션은 Spring Boot 3.x를 사용하여 구축되며, Oracle 19c의 모든 특화 기능을 활용합니다. 이 시스템은 제품, 생산 주문, 재고, 생산 이력을 관리하며, 향후 AWS DMS를 통한 PostgreSQL 마이그레이션 테스트를 위해 다양한 Oracle 전용 기능을 의도적으로 사용합니다.

### 기술 스택

- **Backend Framework**: Spring Boot 3.2.x
- **Database**: Oracle 19c (10.1.5.18, Instance: oracle19c)
- **Database Accounts**: system/system, sys/sys, mesuser/mespass
- **Connection Pool**: HikariCP
- **ORM**: JPA (Hibernate 6.x)
- **Query Builder**: QueryDSL 5.x
- **SQL Mapper**: MyBatis 3.5.x
- **Template Engine**: Thymeleaf 3.1.x
- **Build Tool**: Gradle 8.x
- **Java Version**: Java 17

### 아키텍처 원칙

1. **계층화 아키텍처**: Controller → Service → Repository 계층 분리
2. **다중 데이터 액세스**: JPA, QueryDSL, MyBatis, JDBC Template 혼용 사용
3. **Oracle 기능 극대화**: 가능한 모든 Oracle 특화 기능 활용
4. **DMS 마이그레이션 준비**: PostgreSQL 변환 시 이슈가 발생하도록 의도적 설계

## 아키텍처

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    웹 브라우저                           │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP
┌────────────────────▼────────────────────────────────────┐
│              Spring Boot 애플리케이션                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │         프레젠테이션 계층                         │  │
│  │  ┌──────────────┐  ┌──────────────────────────┐ │  │
│  │  │ Thymeleaf    │  │   REST Controllers       │ │  │
│  │  │ Controllers  │  │   (@RestController)      │ │  │
│  │  └──────────────┘  └──────────────────────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │            서비스 계층                            │  │
│  │  - 비즈니스 로직                                  │  │
│  │  - 트랜잭션 관리 (@Transactional)                │  │
│  │  - 저장 프로시저 호출                             │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │         데이터 액세스 계층                        │  │
│  │  ┌──────┐ ┌─────────┐ ┌────────┐ ┌───────────┐ │  │
│  │  │ JPA  │ │QueryDSL │ │MyBatis │ │   JDBC    │ │  │
│  │  │Repos │ │ Repos   │ │Mappers │ │ Template  │ │  │
│  │  └──────┘ └─────────┘ └────────┘ └───────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │            HikariCP 커넥션 풀                     │  │
│  │  - 최소: 5, 최대: 20 연결                         │  │
│  │  - 누수 감지: 60초                                │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ JDBC
┌────────────────────▼────────────────────────────────────┐
│         Oracle 19c (10.1.5.18, Instance: oracle19c)     │
│  - Accounts: system/system, sys/sys, mesuser/mespass   │
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

## 데이터 모델

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

## 주요 컴포넌트

### 1. JPA 엔티티 예시

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
}
```

### 2. QueryDSL Repository 예시

```java
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
        // 동적 쿼리 구성
    }
}
```

### 3. MyBatis Mapper XML 예시

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

### 4. Service 계층 예시

```java
@Service
@Transactional
public class ProductionOrderService {
    private final ProductionOrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public ProductionOrderDto createOrder(CreateOrderRequest request) {
        // 1. JPA로 주문 생성
        ProductionOrder order = new ProductionOrder();
        order = orderRepository.save(order);
        
        // 2. Stored Procedure 호출 (총액 계산)
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("CALCULATE_ORDER_TOTAL");
        
        return convertToDto(order);
    }
}
```

### 5. REST Controller 예시

```java
@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    private final ProductService productService;
    
    @GetMapping
    public Page<ProductDto> getProducts(Pageable pageable) {
        return productService.searchProducts(pageable);
    }
    
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

### 6. Thymeleaf Controller 예시

```java
@Controller
@RequestMapping("/products")
public class ProductWebController {
    private final ProductService productService;
    
    @GetMapping
    public String listProducts(Model model, Pageable pageable) {
        Page<ProductDto> products = productService.searchProducts(pageable);
        model.addAttribute("products", products);
        return "products/list";
    }
    
    @PostMapping
    public String createProduct(@Valid @ModelAttribute CreateProductRequest request,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "products/form";
        }
        productService.createProduct(request);
        return "redirect:/products";
    }
}
```

## 정확성 속성 (Correctness Properties)

프로젝트는 Property-Based Testing을 사용하여 33개의 정확성 속성을 검증합니다.

### 주요 속성 예시

**Property 1**: 시퀀스 생성 ID는 고유하고 양수여야 함
- 모든 엔티티 삽입에 대해 생성된 ID는 양수이고 테이블 내에서 고유해야 함

**Property 2**: 주문 생성 시 이력 레코드 트리거
- 생산 주문 생성 시 일치하는 ORDER_ID를 가진 생산 이력 레코드가 자동으로 생성되어야 함

**Property 5**: 계층 쿼리 완전성
- 생산 이력 트리에 대해 CONNECT BY 쿼리는 올바른 부모-자식 관계로 계층의 모든 노드를 반환해야 함

**Property 12**: CLOB 라운드트립
- 최대 4GB의 텍스트 콘텐츠를 CLOB 컬럼에 저장하고 검색하면 동일한 텍스트를 반환해야 함

**Property 23**: 다중 테이블 트랜잭션 원자성
- 상세 정보가 있는 생산 주문에 대해 삽입 중 오류가 발생하면 PRODUCTION_ORDER 및 ORDER_DETAIL 테이블 모두 새 레코드가 없어야 함

## 오류 처리

### 예외 계층

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

### 전역 예외 핸들러

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
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

## 테스트 전략

### 이중 테스트 접근법

- **Unit Tests**: 특정 예제, 엣지 케이스, 설정 검증
- **Property Tests**: 모든 입력에 대해 성립해야 하는 보편적 속성 검증

### Property-Based Testing 예시

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
```

## 배포

### 애플리케이션 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//10.1.5.18:1521/oracle19c
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      leak-detection-threshold: 60000
  
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect
    hibernate:
      ddl-auto: validate
    show-sql: true

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

## DMS 마이그레이션 고려사항

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

### DMS GenAI 테스트 대상

1. **MyBatis XML**: CONNECT BY, Analytic Functions, Hints
2. **JPA Native Query**: @Query 어노테이션의 Oracle SQL
3. **JDBC Template**: Java 문자열 리터럴의 Oracle SQL
