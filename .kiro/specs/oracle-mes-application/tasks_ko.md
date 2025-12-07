# 구현 계획

## 데이터베이스 연결 정보
- **호스트**: 10.1.5.18 (EC2 Oracle 19c)
- **포트**: 1521
- **인스턴스명**: oracle19c
- **시스템 계정**: sys / sys (as sysdba)
- **앱 사용자**: mesuser / mespass (생성 필요)

- [ ] 1. 프로젝트 구조 및 의존성 설정
  - Gradle로 Spring Boot 프로젝트 생성
  - 모든 의존성으로 build.gradle 구성 (Spring Boot, Oracle JDBC, MyBatis, QueryDSL, Thymeleaf, jqwik)
  - 패키지 구조 설정 (config, domain, common)
  - HikariCP 및 데이터베이스 설정으로 application.yml 구성 (10.1.5.18:1521/oracle19c)
  - _요구사항: 3.1, 3.2, 3.3_

- [ ]* 1.1 구성에 대한 단위 테스트 작성
  - HikariCP 구성 값 테스트
  - 10.1.5.18:1521/oracle19c에 대한 데이터소스 연결 테스트
  - _요구사항: 3.1, 3.2, 3.3, 3.4_

- [ ] 2. 기존 Oracle 19c에 MES 사용자 및 스키마 생성
  - sys/sys as sysdba로 10.1.5.18:1521/oracle19c에 연결
  - 비밀번호 mespass로 사용자 mesuser 생성
  - 필요한 권한 부여 (CONNECT, RESOURCE, CREATE VIEW, CREATE MATERIALIZED VIEW 등)
  - _요구사항: 1.1, 1.2_

- [ ] 3. 데이터베이스 스키마 및 Oracle 객체 생성
  - 제약조건 및 인덱스가 있는 8개의 테이블 모두 생성
  - 모든 기본 키에 대한 시퀀스 생성
  - PRODUCTION_ORDER → PRODUCTION_HISTORY 트리거 생성
  - 저장 프로시저 CALCULATE_ORDER_TOTAL 생성
  - 저장 함수 CHECK_PRODUCT_AVAILABLE 생성
  - 루프백 데이터베이스 링크 생성
  - DAILY_SUMMARY에 대한 구체화된 뷰 생성
  - _요구사항: 1.3, 2.1, 2.2, 2.9, 2.10, 2.11, 2.12_

- [ ] 4. 모든 테이블에 대한 JPA 엔티티 구현
  - @SequenceGenerator가 있는 Product 엔티티 생성
  - NOTES에 대한 CLOB 매핑이 있는 ProductionOrder 엔티티 생성
  - 관계가 있는 OrderDetail 엔티티 생성
  - 관계가 있는 Inventory 엔티티 생성 (@Version으로 낙관적 잠금)
  - CLOB, BLOB, BFILE 매핑이 있는 ProductDocument 엔티티 생성
  - XMLTYPE 매핑이 있는 ProductSpec 엔티티 생성
  - 자기 참조가 있는 ProductionHistory 엔티티 생성
  - DailySummary 엔티티 생성
  - _요구사항: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 4.1 JPA 엔티티에 대한 단위 테스트 작성
  - 시퀀스 생성 테스트
  - 관계 매핑 테스트
  - CLOB/BLOB 매핑 테스트
  - _요구사항: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 5. JPA Repository 인터페이스 구현
  - 모든 엔티티에 대한 JpaRepository 확장
  - 커스텀 쿼리 메서드 정의 (findByProductCode, findByOrderDate 등)
  - _요구사항: 9.1, 9.2, 9.3_

- [ ] 6. QueryDSL 구성 및 Repository 구현
  - QueryDSL APT 프로세서 구성
  - Q-클래스 생성 확인
  - 동적 검색을 위한 QueryDSL Repository 구현
  - BooleanBuilder로 복잡한 술어 구현
  - DTO 프로젝션 구현
  - _요구사항: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 6.1 QueryDSL 쿼리에 대한 단위 테스트 작성
  - 동적 쿼리 구성 테스트
  - 조인 및 페치 조인 테스트
  - DTO 프로젝션 테스트
  - _요구사항: 5.2, 5.4, 5.5_

- [ ] 7. MyBatis 구성 및 Mapper 구현
  - MyBatis 구성 클래스 생성
  - 각 도메인에 대한 XML 매퍼 파일 생성
  - CONNECT BY 계층 쿼리 구현
  - 분석 함수 쿼리 구현 (ROW_NUMBER, RANK, LAG/LEAD)
  - Oracle 힌트가 있는 쿼리 구현
  - CLOB 결과 매핑 구성
  - 배치 삽입 매퍼 구현
  - _요구사항: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ]* 7.1 MyBatis 매퍼에 대한 단위 테스트 작성
  - CONNECT BY 쿼리 테스트
  - 분석 함수 테스트
  - CLOB 매핑 테스트
  - 배치 삽입 테스트
  - _요구사항: 6.2, 6.3, 6.4, 6.5_

- [ ] 8. JPA Native Query 구현
  - CONNECT BY가 있는 @Query 어노테이션 생성
  - Oracle 날짜 함수가 있는 네이티브 쿼리 작성
  - 힌트가 있는 네이티브 쿼리 작성
  - 분석 함수가 있는 네이티브 쿼리 작성
  - DUAL 테이블 참조 쿼리 작성
  - _요구사항: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 9. JDBC Template 구현
  - JDBC Template 구성
  - Oracle SQL이 있는 쿼리 메서드 구현
  - 저장 프로시저 호출을 위한 SimpleJdbcCall 구현
  - Oracle 타입 매핑을 위한 RowMapper 구현
  - 배치 업데이트 구현
  - _요구사항: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ]* 9.1 JDBC Template에 대한 단위 테스트 작성
  - 저장 프로시저 호출 테스트
  - RowMapper 테스트
  - 배치 업데이트 테스트
  - _요구사항: 8.2, 8.3, 8.4_

- [ ] 10. Service 계층 구현
  - 모든 도메인에 대한 Service 클래스 생성
  - @Transactional 어노테이션 구성
  - 비즈니스 로직 구현 (검증, 계산 등)
  - 저장 프로시저/함수 호출 통합
  - 다중 Repository 사용 (JPA, QueryDSL, MyBatis, JDBC)
  - _요구사항: 12.1, 12.2, 12.3, 12.4, 12.5, 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ]* 10.1 Service 계층에 대한 단위 테스트 작성
  - 트랜잭션 동작 테스트
  - 롤백 시나리오 테스트
  - 저장 프로시저 통합 테스트
  - _요구사항: 12.1, 12.2, 12.3, 12.4, 13.1, 13.2_

- [ ] 11. REST API Controller 구현
  - 제품 CRUD를 위한 ProductRestController 생성
  - 주문 관리를 위한 ProductionOrderRestController 생성
  - 재고 관리를 위한 InventoryRestController 생성
  - 요청 검증 구현 (@Valid)
  - 응답 DTO 매핑 구현
  - _요구사항: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ]* 11.1 REST API에 대한 단위 테스트 작성
  - 엔드포인트 테스트 (MockMvc)
  - 검증 테스트
  - 오류 응답 테스트
  - _요구사항: 9.1, 9.2, 9.3, 9.4_

- [ ] 12. LOB 처리 구현
  - CLOB 읽기/쓰기 유틸리티 구현
  - BLOB 업로드/다운로드 구현
  - BFILE 참조 처리 구현
  - 스트리밍 지원 구현
  - _요구사항: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ]* 12.1 LOB 처리에 대한 단위 테스트 작성
  - CLOB 라운드트립 테스트
  - BLOB 라운드트립 테스트
  - BFILE 참조 테스트
  - _요구사항: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 13. XMLType 처리 구현
  - XML 검증 유틸리티 구현
  - XMLType 읽기/쓰기 구현
  - XML 파싱 통합
  - _요구사항: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ]* 13.1 XMLType 처리에 대한 단위 테스트 작성
  - XML 검증 테스트
  - XML 라운드트립 테스트
  - 잘못된 형식의 XML 거부 테스트
  - _요구사항: 11.1, 11.2, 11.5_

- [ ] 14. 오류 처리 및 로깅 구현
  - 전역 예외 핸들러 생성 (@ControllerAdvice)
  - 커스텀 예외 클래스 정의
  - SQL 예외 로깅 구현
  - HikariCP 통계 로깅 구현
  - 느린 쿼리 로깅 구현
  - _요구사항: 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ]* 14.1 오류 처리에 대한 단위 테스트 작성
  - 예외 핸들러 테스트
  - 오류 응답 형식 테스트
  - 로깅 동작 테스트
  - _요구사항: 14.1, 14.3, 14.4_

- [ ] 15. Thymeleaf 웹 UI 구현
  - 레이아웃 템플릿 생성 (헤더, 푸터, 네비게이션)
  - 제품 목록 페이지 구현
  - 제품 생성/수정 양식 구현
  - 주문 목록 페이지 구현
  - 주문 생성 양식 구현
  - 재고 목록 페이지 구현
  - 페이징 구현
  - 양식 검증 및 오류 표시 구현
  - _요구사항: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8_

- [ ]* 15.1 웹 UI에 대한 통합 테스트 작성
  - 페이지 렌더링 테스트
  - 양식 제출 테스트
  - 검증 오류 표시 테스트
  - _요구사항: 15.2, 15.3, 15.7_

- [ ] 16. Property-Based 테스트 구현
  - jqwik 구성
  - 33개의 correctness properties에 대한 property 테스트 작성
  - 각 property에 대한 Arbitrary 생성기 구현
  - 최소 100회 반복으로 테스트 실행
  - _Design Document의 모든 Correctness Properties_

- [ ] 17. 통합 테스트 구현
  - Testcontainers 구성 (Oracle XE)
  - 전체 플로우 통합 테스트 작성
  - 트랜잭션 통합 테스트
  - 트리거 동작 테스트
  - _모든 요구사항_

- [ ] 18. 문서화 및 배포 준비
  - README.md 작성
  - API 문서 생성
  - 배포 가이드 작성
  - DMS 마이그레이션 가이드 작성
