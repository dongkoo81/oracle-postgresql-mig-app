# Lambda 전처리 설정 가이드

## 아키텍처

```
┌─────────┐     ┌─────┐     ┌────────┐     ┌──────────┐     ┌────────────┐
│ Oracle  │ --> │ DMS │ --> │   S3   │ --> │  Lambda  │ --> │ PostgreSQL │
└─────────┘     └─────┘     └────────┘     └──────────┘     └────────────┘
                              (Parquet)      (전처리)
```

---

## 1단계: DMS S3 타겟 설정

### DMS 타겟 엔드포인트 생성
```bash
aws dms create-endpoint \
  --endpoint-identifier dms-s3-target \
  --endpoint-type target \
  --engine-name s3 \
  --s3-settings '{
    "BucketName": "dms-preprocessing-bucket",
    "DataFormat": "parquet",
    "CompressionType": "gzip",
    "EnableStatistics": true,
    "ParquetVersion": "parquet-2-0"
  }' \
  --region ap-northeast-2
```

### S3 버킷 구조
```
s3://dms-preprocessing-bucket/
├── dmstest/
│   └── test_product/
│       ├── LOAD00000001.parquet
│       ├── LOAD00000002.parquet
│       └── ...
```

---

## 2단계: Lambda Layer 생성 (의존성 패키지)

### 로컬에서 패키지 준비
```bash
# 디렉토리 생성
mkdir -p lambda-layer/python

# 패키지 설치
pip install \
  pandas \
  psycopg2-binary \
  pyarrow \
  -t lambda-layer/python/

# ZIP 파일 생성
cd lambda-layer
zip -r lambda-layer.zip python/
```

### Lambda Layer 생성
```bash
aws lambda publish-layer-version \
  --layer-name dms-preprocessing-dependencies \
  --zip-file fileb://lambda-layer.zip \
  --compatible-runtimes python3.11 \
  --region ap-northeast-2
```

---

## 3단계: Lambda 함수 생성

### IAM Role 생성
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::dms-preprocessing-bucket",
        "arn:aws:s3:::dms-preprocessing-bucket/*"
      ]
    },
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
zip lambda-function.zip lambda_preprocessing.py

# Lambda 함수 생성
aws lambda create-function \
  --function-name dms-preprocessing \
  --runtime python3.11 \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-dms-role \
  --handler lambda_preprocessing.lambda_handler \
  --zip-file fileb://lambda-function.zip \
  --timeout 300 \
  --memory-size 1024 \
  --layers arn:aws:lambda:ap-northeast-2:ACCOUNT_ID:layer:dms-preprocessing-dependencies:1 \
  --vpc-config SubnetIds=subnet-xxx,SecurityGroupIds=sg-xxx \
  --region ap-northeast-2
```

### 환경 변수 설정
```bash
aws lambda update-function-configuration \
  --function-name dms-preprocessing \
  --environment Variables='{
    "DB_HOST":"target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com",
    "DB_PORT":"5432",
    "DB_NAME":"target",
    "DB_USER":"dmstest",
    "DB_PASSWORD":"dmstest123"
  }' \
  --region ap-northeast-2
```

---

## 4단계: S3 이벤트 트리거 설정

### S3 버킷에 Lambda 트리거 추가
```bash
aws s3api put-bucket-notification-configuration \
  --bucket dms-preprocessing-bucket \
  --notification-configuration '{
    "LambdaFunctionConfigurations": [
      {
        "LambdaFunctionArn": "arn:aws:lambda:ap-northeast-2:ACCOUNT_ID:function:dms-preprocessing",
        "Events": ["s3:ObjectCreated:*"],
        "Filter": {
          "Key": {
            "FilterRules": [
              {
                "Name": "prefix",
                "Value": "dmstest/test_product/"
              },
              {
                "Name": "suffix",
                "Value": ".parquet"
              }
            ]
          }
        }
      }
    ]
  }'
```

### Lambda에 S3 권한 부여
```bash
aws lambda add-permission \
  --function-name dms-preprocessing \
  --statement-id s3-trigger \
  --action lambda:InvokeFunction \
  --principal s3.amazonaws.com \
  --source-arn arn:aws:s3:::dms-preprocessing-bucket \
  --region ap-northeast-2
```

---

## 5단계: DMS 태스크 실행

### DMS 태스크 생성
```bash
aws dms create-replication-task \
  --replication-task-identifier oracle-to-s3 \
  --source-endpoint-arn arn:aws:dms:...:endpoint:source-oracle \
  --target-endpoint-arn arn:aws:dms:...:endpoint:dms-s3-target \
  --replication-instance-arn arn:aws:dms:...:rep:... \
  --migration-type full-load \
  --table-mappings file://table-mappings.json \
  --region ap-northeast-2
```

### table-mappings.json
```json
{
  "rules": [
    {
      "rule-type": "selection",
      "rule-id": "1",
      "rule-name": "1",
      "object-locator": {
        "schema-name": "DMSTEST",
        "table-name": "TEST_PRODUCT"
      },
      "rule-action": "include"
    }
  ]
}
```

---

## 실행 흐름

### 1. DMS가 데이터 추출
```
Oracle DMSTEST.TEST_PRODUCT → DMS
```

### 2. S3에 Parquet 저장
```
DMS → s3://dms-preprocessing-bucket/dmstest/test_product/LOAD00000001.parquet
```

### 3. S3 이벤트 발생
```
S3 PUT 이벤트 → Lambda 트리거
```

### 4. Lambda 전처리
```python
# Lambda 함수 실행
1. S3에서 Parquet 읽기
2. 전처리 수행
   - Y/N → boolean
   - 날짜 문자열 → date
   - 금액 문자열 → numeric
   - 태그 → 배열
3. PostgreSQL에 적재
```

### 5. PostgreSQL 확인
```sql
SELECT * FROM dmstest.test_product;
-- 전처리된 데이터 확인
```

---

## Lambda vs Glue ETL 비교

| 항목 | Lambda | Glue ETL |
|------|--------|----------|
| **비용** | $0.20/100만 요청 + 실행 시간 | $0.44/DPU-Hour |
| **속도** | 빠름 (초 단위) | 느림 (분 단위) |
| **데이터 크기** | < 10MB (메모리 제한) | GB ~ TB |
| **타임아웃** | 15분 | 무제한 |
| **실시간** | ✅ 가능 | ❌ 배치만 |
| **복잡도** | 간단 | 복잡 |

---

## 예상 비용 (월 1GB 데이터)

### Lambda
```
요청: 1,000회/월
실행 시간: 평균 10초
메모리: 1024MB

비용: ~$0.50/월
```

### Glue ETL
```
DPU: 2 DPU
실행 시간: 10분/일

비용: ~$26/월
```

**Lambda가 50배 저렴!**

---

## 제약 사항

### Lambda 제약
```
❌ 파일 크기 > 10MB (메모리 부족)
❌ 처리 시간 > 15분 (타임아웃)
❌ 복잡한 조인/집계
```

### 해결 방법
```
→ 파일을 작게 분할 (DMS 설정)
→ 대용량은 Glue ETL 사용
→ 복잡한 로직은 Step Functions + Lambda
```

---

## 권장 사항

### Lambda 사용
```
✅ 파일 < 10MB
✅ 간단한 전처리
✅ 실시간 처리
✅ 비용 최소화
```

### Glue ETL 사용
```
✅ 파일 > 100MB
✅ 복잡한 변환
✅ 배치 처리
✅ 안정성 우선
```

---

## 작성 정보

- 작성일: 2025-12-03
- 방식: DMS → S3 → Lambda → PostgreSQL
- 비용: Lambda가 Glue보다 50배 저렴



## 실시간 처리 흐름

1. Oracle에 데이터 INSERT
   ↓ (수초 내)
2. DMS CDC가 변경 감지
   ↓ (수초 내)
3. DMS가 S3에 파일 저장
   ↓ (즉시)
4. S3 PUT 이벤트 발생
   ↓ (밀리초)
5. Lambda 자동 실행
   ↓ (수초)
6. PostgreSQL에 적재 완료
