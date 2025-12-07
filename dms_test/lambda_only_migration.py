"""
Lambda만 사용한 Oracle → PostgreSQL 마이그레이션 (전처리 포함)

트리거: EventBridge (스케줄) - 예: 매 5분마다
처리: Oracle 조회 → 전처리 → PostgreSQL 적재
"""

import json
import cx_Oracle
import psycopg2
from datetime import datetime

def lambda_handler(event, context):
    """
    Lambda 함수 메인
    """
    
    print("=== Migration Started ===")
    
    # 1. Oracle 연결
    oracle_conn = connect_oracle()
    
    # 2. 데이터 조회 (증분 로드)
    rows = fetch_oracle_data(oracle_conn)
    print(f"Fetched {len(rows)} rows from Oracle")
    
    # 3. 전처리
    processed_rows = preprocess_data(rows)
    print(f"Preprocessed {len(processed_rows)} rows")
    
    # 4. PostgreSQL 적재
    load_to_postgresql(processed_rows)
    print(f"Loaded {len(processed_rows)} rows to PostgreSQL")
    
    # 5. 연결 종료
    oracle_conn.close()
    
    return {
        'statusCode': 200,
        'body': json.dumps(f'Migrated {len(processed_rows)} rows')
    }


def connect_oracle():
    """
    Oracle 연결
    """
    return cx_Oracle.connect(
        user='dmstest',
        password='dmstest123',
        dsn='10.1.5.18:1521/oracle19c'
    )


def fetch_oracle_data(conn):
    """
    Oracle에서 데이터 조회 (증분 로드)
    """
    cursor = conn.cursor()
    
    # 마지막 마이그레이션 시간 조회 (PostgreSQL에서)
    last_migration_time = get_last_migration_time()
    
    # 증분 데이터만 조회
    if last_migration_time:
        query = """
            SELECT 
                product_id,
                product_code,
                product_name,
                is_active,
                created_date_str,
                created_date,
                price_str,
                price,
                metadata_json,
                tags,
                created_at
            FROM test_product
            WHERE created_at > :last_time
            ORDER BY created_at
        """
        cursor.execute(query, last_time=last_migration_time)
    else:
        # 전체 로드
        query = """
            SELECT 
                product_id,
                product_code,
                product_name,
                is_active,
                created_date_str,
                created_date,
                price_str,
                price,
                metadata_json,
                tags,
                created_at
            FROM test_product
            ORDER BY created_at
        """
        cursor.execute(query)
    
    rows = cursor.fetchall()
    cursor.close()
    
    return rows


def preprocess_data(rows):
    """
    데이터 전처리
    """
    processed = []
    
    for row in rows:
        # 케이스 1: Y/N → Boolean
        is_active = True if row[3] == 'Y' else False if row[3] == 'N' else None
        
        # 케이스 2: 날짜 문자열 → Date (이미 DATE 타입이면 그대로)
        created_date = row[5]
        
        # 케이스 3: 금액 문자열 → Numeric
        price_str = row[6] or ''
        price = float(price_str.replace(',', '').replace('$', '').replace('₩', '').strip() or '0')
        
        # 케이스 4: JSON 문자열 (그대로)
        metadata_json = row[8]
        
        # 케이스 5: 구분자 문자열 → 배열
        tags = row[9].split(',') if row[9] else []
        
        # 케이스 6: 밀리초 → 초 단위
        created_at = row[10]
        if created_at:
            created_at = created_at.replace(microsecond=0)
        
        processed.append({
            'product_id': row[0],
            'product_code': row[1],
            'product_name': row[2],
            'is_active': is_active,
            'created_date': created_date,
            'price': price,
            'metadata_json': metadata_json,
            'tags': tags,
            'created_at': created_at
        })
    
    return processed


def load_to_postgresql(rows):
    """
    PostgreSQL에 데이터 적재
    """
    conn = psycopg2.connect(
        host='target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com',
        port=5432,
        database='target',
        user='dmstest',
        password='dmstest123'
    )
    
    cursor = conn.cursor()
    
    for row in rows:
        # 배열을 PostgreSQL 형식으로 변환
        tags_array = row['tags']
        
        cursor.execute("""
            INSERT INTO dmstest.test_product (
                product_id, product_code, product_name,
                is_active, created_date, price,
                metadata_json, tags, created_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (product_id) DO UPDATE SET
                product_code = EXCLUDED.product_code,
                product_name = EXCLUDED.product_name,
                is_active = EXCLUDED.is_active,
                created_date = EXCLUDED.created_date,
                price = EXCLUDED.price,
                metadata_json = EXCLUDED.metadata_json,
                tags = EXCLUDED.tags,
                created_at = EXCLUDED.created_at
        """, (
            row['product_id'],
            row['product_code'],
            row['product_name'],
            row['is_active'],
            row['created_date'],
            row['price'],
            row['metadata_json'],
            tags_array,
            row['created_at']
        ))
    
    conn.commit()
    
    # 마지막 마이그레이션 시간 업데이트
    if rows:
        update_last_migration_time(rows[-1]['created_at'])
    
    cursor.close()
    conn.close()


def get_last_migration_time():
    """
    마지막 마이그레이션 시간 조회
    """
    try:
        conn = psycopg2.connect(
            host='target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com',
            port=5432,
            database='target',
            user='dmstest',
            password='dmstest123'
        )
        cursor = conn.cursor()
        cursor.execute("""
            SELECT MAX(created_at) FROM dmstest.test_product
        """)
        result = cursor.fetchone()
        cursor.close()
        conn.close()
        return result[0] if result else None
    except:
        return None


def update_last_migration_time(timestamp):
    """
    마지막 마이그레이션 시간 업데이트 (메타데이터 테이블)
    """
    # 실제로는 별도 메타데이터 테이블에 저장
    pass


# Lambda Layer 필요 패키지:
# - cx_Oracle
# - psycopg2-binary
