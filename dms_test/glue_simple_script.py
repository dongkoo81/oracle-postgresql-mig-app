import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from pyspark.sql.functions import *
from awsglue.dynamicframe import DynamicFrame

args = getResolvedOptions(sys.argv, ['JOB_NAME'])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

# 1. Oracle 읽기
oracle_df = glueContext.create_dynamic_frame.from_options(
    connection_type="oracle",
    connection_options={
        "useConnectionProperties": "true",
        "dbtable": "MESUSER.TEST_PRODUCT",
        "connectionName": "oracle-mesuser-connection"
    }
).toDF()

print("=== Oracle Data ===")
oracle_df.show()

# 2. 전처리
df_processed = oracle_df \
    .withColumn("is_active", 
        when(col("IS_ACTIVE") == "Y", True)
        .when(col("IS_ACTIVE") == "N", False)
        .otherwise(None)) \
    .withColumn("created_date", 
        to_date(col("CREATED_DATE"), "yyyy-MM-dd")) \
    .withColumn("price", 
        regexp_replace(col("PRICE_STR"), "[^0-9.]", "").cast("decimal(15,2)")) \
    .withColumn("metadata_json", 
        col("METADATA_JSON")) \
    .withColumn("tags", 
        split(col("TAGS"), ",")) \
    .withColumn("created_at", 
        date_trunc("second", col("CREATED_AT"))) \
    .select(
        col("PRODUCT_ID").alias("product_id"),
        col("PRODUCT_CODE").alias("product_code"),
        col("PRODUCT_NAME").alias("product_name"),
        col("is_active"),
        col("created_date"),
        col("price"),
        col("metadata_json"),
        col("tags"),
        col("created_at")
    )

print("=== Processed Data ===")
df_processed.show()

# 3. PostgreSQL 적재
dyf_processed = DynamicFrame.fromDF(df_processed, glueContext, "processed")

glueContext.write_dynamic_frame.from_options(
    frame=dyf_processed,
    connection_type="postgresql",
    connection_options={
        "useConnectionProperties": "true",
        "dbtable": "mesuser.test_product",
        "connectionName": "postgresql-mesuser-connection"
    }
)

print("=== Completed ===")

job.commit()
