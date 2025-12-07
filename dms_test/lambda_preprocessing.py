"""
Lambda Function: DMS S3 데이터 전처리 후 PostgreSQL 적재

트리거: S3 PUT 이벤트
입력: DMS가 S3에 저장한 Parquet/CSV 파일
출력: PostgreSQL에 전처리된 데이터 적재
"""

import json
import boto3
import psycopg2
import pandas as pd
from io import BytesIO
from datetime import datetime

s3 = boto3.client('s3')

def lambda_handler(event, context):
    """
    S3 이벤트로 트리거되는 Lambda 함수
    """
    
    # 1. S3 이벤트에서 파일 정보 추출
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = event['Records'][0]['s3']['object']['key']
    
    print(f"Processing file: s3://{bucket}/{key}")
    
    # 2. S3에서 파일 읽기
    response = s3.get_object(Bucket=bucket, Key=key)
    
    # Parquet 파일 읽기
    if key.endswith('.parquet'):
        df = pd.read_parquet(BytesIO(response['Body'].read()))
    # CSV 파일 읽기
    elif key.endswith('.csv'):
        df = pd.read_csv(BytesIO(response['Body'].read()))
    else:
        print(f"Unsupported file format: {key}")
        return
    
    print(f"Loaded {len(df)} rows")
    
    # 3. 전처리 수행
    df_processed = preprocess_data(df)
    
    # 4. PostgreSQL에 적재
    load_to_postgresql(df_processed)
    
    return {
        'statusCode': 200,
        'body': json.dumps(f'Processed {len(df_processed)} rows')
    }


def preprocess_data(df):
    """
    데이터 전처리
    """
    
    # 케이스 1: Y/N → Boolean
    if 'is_active' in df.columns:
        df['is_active'] = df['is_active'].map({'Y': True, 'N': False})
    
    # 케이스 2: 날짜 문자열 → Date
    if 'created_date_str' in df.columns:
        df['created_date'] = pd.to_datetime(
            df['created_date_str'], 
            errors='coerce',
            infer_datetime_format=True
        ).dt.date
    
    # 케이스 3: 금액 문자열 → Numeric
    if 'price_str' in df.columns:
        df['price'] = df['price_str'].str.replace(r'[^0-9.]', '', regex=True).astype(float)
    
    # 케이스 4: JSON 문자열 → 그대로 (PostgreSQL에서 JSONB로 변환)
    # metadata_json은 그대로 유지
    
    # 케이스 5: 구분자 문자열 → 배열
    if 'tags' in df.columns:
        df['tags'] = df['tags'].str.split(',')
    
    # 케이스 6: 밀리초 → 초 단위
    if 'created_at' in df.columns:
        df['created_at'] = pd.to_datetime(df['created_at']).dt.floor('S')
    
    # 필요한 컬럼만 선택
    columns = [
        'product_id', 'product_code', 'product_name',
        'is_active', 'created_date', 'price', 
        'metadata_json', 'tags'
    ]
    df_processed = df[[col for col in columns if col in df.columns]]
    
    return df_processed


def load_to_postgresql(df):
    """
    PostgreSQL에 데이터 적재
    """
    
    # PostgreSQL 연결
    conn = psycopg2.connect(
        host='target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com',
        port=5432,
        database='target',
        user='dmstest',
        password='dmstest123'
    )
    
    cursor = conn.cursor()
    
    # 데이터 삽입
    for _, row in df.iterrows():
        # 배열을 PostgreSQL 형식으로 변환
        tags_array = '{' + ','.join(row['tags']) + '}' if isinstance(row['tags'], list) else None
        
        cursor.execute("""
            INSERT INTO dmstest.test_product (
                product_id, product_code, product_name,
                is_active, created_date, price,
                metadata_json, tags
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (product_id) DO UPDATE SET
                product_code = EXCLUDED.product_code,
                product_name = EXCLUDED.product_name,
                is_active = EXCLUDED.is_active,
                created_date = EXCLUDED.created_date,
                price = EXCLUDED.price,
                metadata_json = EXCLUDED.metadata_json,
                tags = EXCLUDED.tags
        """, (
            row['product_id'],
            row['product_code'],
            row['product_name'],
            row['is_active'],
            row['created_date'],
            row['price'],
            row['metadata_json'],
            tags_array
        ))
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"Loaded {len(df)} rows to PostgreSQL")


# Lambda Layer에 필요한 패키지:
# - pandas
# - psycopg2-binary
# - pyarrow (Parquet 읽기용)
