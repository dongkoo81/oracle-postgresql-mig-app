# DB Subnet Group
resource "aws_db_subnet_group" "aurora" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name        = "${var.project_name}-db-subnet-group"
    auto-delete = "no"
  }
}

# Aurora Cluster Parameter Group
resource "aws_rds_cluster_parameter_group" "aurora" {
  name   = "cloud-aurora-postgresql-pg17"
  family = "aurora-postgresql17"

  tags = {
    Name        = "cloud-aurora-postgresql-pg17"
    auto-delete = "no"
  }
}

# Aurora DB Parameter Group
resource "aws_db_parameter_group" "aurora" {
  name   = "cloud-aurora-postgresql-instance-pg17"
  family = "aurora-postgresql17"

  tags = {
    Name        = "cloud-aurora-postgresql-instance-pg17"
    auto-delete = "no"
  }
}

# Aurora PostgreSQL Cluster
resource "aws_rds_cluster" "aurora" {
  cluster_identifier              = "cloud-aurora-postgresql"
  engine                          = "aurora-postgresql"
  engine_version                  = "17.5"
  database_name                   = var.db_name
  master_username                 = var.db_master_username
  master_password                 = var.db_master_password
  db_subnet_group_name            = aws_db_subnet_group.aurora.name
  db_cluster_parameter_group_name = aws_rds_cluster_parameter_group.aurora.name
  vpc_security_group_ids          = [aws_security_group.main.id]
  
  skip_final_snapshot       = true
  backup_retention_period   = 7
  preferred_backup_window   = "03:00-04:00"
  preferred_maintenance_window = "mon:04:00-mon:05:00"

  tags = {
    Name        = "cloud-aurora-postgresql"
    auto-delete = "no"
  }
}

# Aurora PostgreSQL Instance (Writer)
resource "aws_rds_cluster_instance" "aurora_writer" {
  identifier              = "cloud-aurora-postgresql-writer"
  cluster_identifier      = aws_rds_cluster.aurora.id
  instance_class          = var.db_instance_class
  engine                  = aws_rds_cluster.aurora.engine
  engine_version          = aws_rds_cluster.aurora.engine_version
  db_parameter_group_name = aws_db_parameter_group.aurora.name
  publicly_accessible     = false

  tags = {
    Name        = "cloud-aurora-postgresql-writer"
    auto-delete = "no"
  }
}
