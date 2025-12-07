# Lambda만 사용한 마이그레이션 설정

## 아키텍처

```
┌──────────────┐
│ EventBridge  │ (매 5분마다 트리거)
└──────┬───────┘
       ↓
┌──────────────┐
│   Lambda     │ (Oracle 조회 + 전처리)
└──────┬───────┘
       ↓
┌──────────────┐
│ PostgreSQL   │ (적재)
└──────────────┘
```

**DMS 없음! Lambda만 사용**

---

## 1단계: Lambda Layer 생성

### cx_Oracle 설치 (Oracle 클라이언트)
```bash
# Oracle Instant Client 다운로드 필요
mkdir -p lambda-layer/python

# cx_Oracle 설치
pip install cx_Oracle -t lambda-layer/python/

# psycopg2 설치
pip install psycopg2-binary -t lambda-layer/python/

# ZIP 생성
cd lambda-layer
zip -r lambda-layer.zip python/
```

### Lambda Layer 생성
```bash
aws lambda publish-layer-version \
  --layer-name oracle-postgresql-layer \
  --zip-file fileb://lambda-layer.zip \
  --compatible-runtimes python3.11 \
  --region ap-northeast-2
```

---

## 2단계: Lambda 함수 생성

### IAM Role
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
```

### Lambda 함수 생성
```bash
# 함수 코드 ZIP
zip lambda-function.zip lambda_only_migration.py

# Lambda 생성
aws lambda create-function \
  --function-name oracle-to-postgresql-migration \
  --runtime python3.11 \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-migration-role \
  --handler lambda_only_migration.lambda_handler \
  --zip-file fileb://lambda-function.zip \
  --timeout 300 \
  --memory-size 1024 \
  --layers arn:aws:lambda:ap-northeast-2:ACCOUNT_ID:layer:oracle-postgresql-layer:1 \
  --vpc-config SubnetIds=subnet-xxx,SecurityGroupIds=sg-xxx \
  --region ap-northeast-2
```

---

## 3단계: EventBridge 스케줄 설정

### EventBridge Rule 생성
```bash
aws events put-rule \
  --name oracle-migration-schedule \
  --schedule-expression "rate(5 minutes)" \
  --region ap-northeast-2
```

### Lambda 타겟 추가
```bash
aws events put-targets \
  --rule oracle-migration-schedule \
  --targets "Id"="1","Arn"="arn:aws:lambda:ap-northeast-2:ACCOUNT_ID:function:oracle-to-postgresql-migration" \
  --region ap-northeast-2
```

### Lambda 권한 부여
```bash
aws lambda add-permission \
  --function-name oracle-to-postgresql-migration \
  --statement-id eventbridge-trigger \
  --action lambda:InvokeFunction \
  --principal events.amazonaws.com \
  --source-arn arn:aws:events:ap-northeast-2:ACCOUNT_ID:rule/oracle-migration-schedule \
  --region ap-northeast-2
```

---

## 실행 흐름

### 1. EventBridge 트리거 (매 5분)
```
EventBridge → Lambda 실행
```

### 2. Lambda 실행
```python
1. Oracle 연결
2. 마지막 마이그레이션 시간 이후 데이터 조회
3. 전처리 수행
   - Y/N → boolean
   - 날짜 문자열 → date
   - 금액 문자열 → numeric
   - 태그 → 배열
   - 밀리초 → 초
4. PostgreSQL 적재
5. 마지막 마이그레이션 시간 업데이트
```

### 3. 증분 로드
```sql
-- Oracle 쿼리
SELECT * FROM test_product
WHERE created_at > :last_migration_time
ORDER BY created_at
```

---

## 장단점

### 장점
```
✅ 간단 (Lambda 하나만)
✅ 저렴 (DMS 비용 없음)
✅ 전처리 가능
✅ VPC 내부 통신 (보안)
```

### 단점
```
❌ 실시간 CDC 불가 (스케줄만)
❌ 대용량 처리 어려움 (메모리 제한)
❌ 증분 로드 직접 구현
❌ 에러 핸들링 직접 구현
```

---

## DMS vs Lambda 비교

| 항목 | DMS | Lambda만 |
|------|-----|----------|
| **실시간 CDC** | ✅ | ❌ (스케줄) |
| **대용량** | ✅ | ❌ (< 1GB) |
| **전처리** | ❌ | ✅ |
| **비용** | $$ | $ |
| **복잡도** | 중간 | 낮음 |
| **안정성** | 높음 | 중간 |

---

## 비용 비교 (월 1GB 데이터)

### DMS
```
인스턴스: dms.t3.micro
시간: 24시간 × 30일

비용: ~$13/월
```

### Lambda만
```
실행: 8,640회/월 (5분마다)
실행 시간: 평균 10초
메모리: 1024MB

비용: ~$2/월
```

**Lambda가 6배 저렴!**

---

## 사용 시나리오

### Lambda만 사용
```
✅ 소량 데이터 (< 1GB)
✅ 배치 처리 (5분~1시간 간격)
✅ 전처리 필요
✅ 비용 최소화
```

### DMS 사용
```
✅ 대용량 데이터 (> 1GB)
✅ 실시간 CDC 필요
✅ 안정성 중요
✅ 관리 편의성
```

### DMS + Lambda
```
✅ 대용량 + 전처리
✅ 실시간 + 전처리
✅ 최고 성능
```

---

## 테스트 방법

### 1. Lambda 수동 실행
```bash
aws lambda invoke \
  --function-name oracle-to-postgresql-migration \
  --region ap-northeast-2 \
  response.json

cat response.json
```

### 2. CloudWatch Logs 확인
```bash
aws logs tail /aws/lambda/oracle-to-postgresql-migration \
  --follow \
  --region ap-northeast-2
```

### 3. PostgreSQL 확인
```sql
SELECT * FROM dmstest.test_product
ORDER BY created_at DESC
LIMIT 10;
```

---

## 권장 사항

### 현재 프로젝트 (MES)
```
데이터: 8개 테이블, < 1GB
요구사항: 배치 처리 가능

→ Lambda만 사용 권장
→ 가장 간단하고 저렴
```

### 실시간 필요 시
```
→ DMS + Lambda
→ 또는 DMS만 사용 (전처리는 PostgreSQL에서)
```

---

## 작성 정보

- 작성일: 2025-12-03
- 방식: Lambda만 사용 (DMS 없음)
- 비용: ~$2/월 (DMS 대비 6배 저렴)
