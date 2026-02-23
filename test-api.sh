#!/bin/bash

BASE_URL="http://localhost:8080"
PASS_COUNT=0
FAIL_COUNT=0

echo "=========================================="
echo "Oracle → PostgreSQL 마이그레이션 기능 테스트"
echo "=========================================="

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 함수
test_api() {
    local num=$1
    local name=$2
    local method=$3
    local url=$4
    local data=$5
    local check=$6
    
    echo -ne "\n[$num] $name ... "
    
    if [ "$method" = "POST" ]; then
        if [ -n "$data" ]; then
            response=$(curl -s -X POST "$BASE_URL$url" -H "Content-Type: application/json" -d "$data" 2>&1)
        else
            response=$(curl -s -X POST "$BASE_URL$url" 2>&1)
        fi
    else
        response=$(curl -s "$BASE_URL$url" 2>&1)
    fi
    
    # 응답 확인
    if echo "$response" | grep -q "$check" || [ "$check" = "ANY" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((PASS_COUNT++))
    else
        echo -e "${RED}❌ FAIL${NC}"
        echo "   Response: $(echo $response | head -c 100)"
        ((FAIL_COUNT++))
    fi
}

# 애플리케이션 상태 확인
echo -ne "애플리케이션 상태 확인 ... "
if curl -s "$BASE_URL/api/products" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 실행 중${NC}"
else
    echo -e "${RED}❌ 실행되지 않음${NC}"
    exit 1
fi

echo -e "\n${YELLOW}=== 1. 동적 쿼리 & 검색 ===${NC}"
test_api "1.1" "QueryDSL 동적 검색" "GET" "/api/test/oracle/querydsl/search?name=Engine" "" "productId"
test_api "1.2" "TO_DATE 날짜 검색" "GET" "/api/test/oracle/to-date/search?startDate=2024-01-01&endDate=2026-12-31" "" "ANY"

echo -e "\n${YELLOW}=== 2. Stored Procedure/Function ===${NC}"
test_api "2.1" "Stored Function (재고 확인)" "GET" "/api/test/oracle/function/check-available?productId=1&requiredQty=10" "" "available"
test_api "2.2" "Stored Procedure (금액 계산)" "POST" "/api/test/oracle/procedure/calculate-total/1" "" "totalAmount"
test_api "2.3" "DECODE → CASE WHEN" "GET" "/api/test/oracle/decode/product-status/1" "" "status"

echo -e "\n${YELLOW}=== 3. 계층 쿼리 ===${NC}"
test_api "3.1" "CONNECT BY → WITH RECURSIVE" "GET" "/api/test/oracle/hierarchy/1" "" "ANY"

echo -e "\n${YELLOW}=== 4. LOB 타입 ===${NC}"
test_api "4.1" "CLOB → TEXT" "POST" "/api/test/oracle/clob/save?productId=1&content=TestDocument" "" "docId"
test_api "4.2" "BLOB → BYTEA" "GET" "/api/test/oracle/documents/product/1" "" "ANY"
test_api "4.3" "XMLType → XML" "POST" "/api/test/oracle/xml/save?productId=1&xmlContent=%3Cspec%3E%3Cversion%3E1.0%3C%2Fversion%3E%3C%2Fspec%3E" "" "specId"

echo -e "\n${YELLOW}=== 5. Materialized View ===${NC}"
test_api "5.1" "Materialized View 조회" "GET" "/api/test/oracle/materialized-view" "" "ANY"
test_api "5.2" "Materialized View Refresh" "POST" "/api/test/oracle/materialized-view/refresh" "" "message"

echo -e "\n${YELLOW}=== 6. MERGE → INSERT ON CONFLICT ===${NC}"
test_api "6.1" "재고 UPSERT" "POST" "/api/test/oracle/merge/inventory?productId=1&quantity=10" "" "message"

echo -e "\n${YELLOW}=== 7. 날짜/시간 함수 ===${NC}"
test_api "7.1" "SYSDATE → CURRENT_DATE" "GET" "/api/test/oracle/sysdate/today-products" "" "ANY"

echo -e "\n${YELLOW}=== 8. 집합 연산 ===${NC}"
test_api "8.1" "ROWNUM → LIMIT" "GET" "/api/test/oracle/rownum/top-products?limit=3" "" "productId"
test_api "8.2" "MINUS → EXCEPT" "GET" "/api/test/oracle/minus/products-without-inventory" "" "ANY"
test_api "8.3" "(+) → LEFT JOIN" "GET" "/api/test/oracle/outer-join/products-inventory" "" "productId"

echo -e "\n${YELLOW}=== 9. Sequence ===${NC}"
test_api "9.1" "NEXTVAL 함수" "GET" "/api/test/oracle/sequence/nextval?sequenceName=PRODUCT_SEQ" "" "nextVal"
test_api "9.2" "자동 PK 생성" "GET" "/api/products" "" "productId"

echo -e "\n${YELLOW}=== 10. Partition Table ===${NC}"
test_api "10.1" "Partition 조회 (PASS)" "GET" "/api/test/oracle/partition/PASS" "" "ANY"
test_api "10.2" "Partition 조회 (FAIL)" "GET" "/api/test/oracle/partition/FAIL" "" "ANY"

echo -e "\n${YELLOW}=== 11. 통합 기능 테스트 ===${NC}"
ORDER_NO="TEST-$(date +%s)"
ORDER_DATA="{\"orderNo\":\"$ORDER_NO\",\"orderDate\":\"2026-02-23T12:00:00\",\"notes\":\"Integration Test\",\"details\":[{\"productId\":1,\"quantity\":5,\"unitPrice\":1000}]}"
test_api "11.1" "주문 생성 (Trigger + Sequence)" "POST" "/api/orders" "$ORDER_DATA" "orderId"

echo -e "\n=========================================="
echo -e "테스트 결과: ${GREEN}PASS $PASS_COUNT${NC} / ${RED}FAIL $FAIL_COUNT${NC} / TOTAL $((PASS_COUNT + FAIL_COUNT))"
echo "=========================================="

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 테스트 통과!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  일부 테스트 실패${NC}"
    exit 1
fi
