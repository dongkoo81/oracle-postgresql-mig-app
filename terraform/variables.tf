variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "oracle-postgresql"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "146.168.0.0/16"
}

variable "availability_zones" {
  description = "Availability Zones"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2b", "ap-northeast-2c"]
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "r6i.xlarge"
}

variable "db_instance_class" {
  description = "Aurora instance class"
  type        = string
  default     = "db.r6i.xlarge"
}

variable "db_master_username" {
  description = "Aurora master username"
  type        = string
  default     = "postgres"
}

variable "db_master_password" {
  description = "Aurora master password"
  type        = string
  default     = "PostgresPass123!"
  sensitive   = true
}

variable "db_name" {
  description = "Initial database name"
  type        = string
  default     = "mesdb"
}

variable "oracle_ami_id" {
  description = "Oracle EE 19c AMI ID"
  type        = string
  default     = "ami-05b733ff1080c095f"
}
