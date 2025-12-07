output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public Subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private Subnet IDs"
  value       = aws_subnet.private[*].id
}

output "security_group_id" {
  description = "Security Group ID"
  value       = aws_security_group.main.id
}

output "nat_gateway_id" {
  description = "NAT Gateway ID"
  value       = aws_nat_gateway.main.id
}

output "key_pair_name" {
  description = "EC2 Key Pair Name"
  value       = aws_key_pair.ec2_key.key_name
}

output "private_key_path" {
  description = "Private Key File Path"
  value       = local_file.private_key.filename
}

output "on_premises_app_instance_id" {
  description = "On-Premises App EC2 Instance ID"
  value       = aws_instance.on_premises_app.id
}

output "on_premises_app_private_ip" {
  description = "On-Premises App EC2 Private IP"
  value       = aws_instance.on_premises_app.private_ip
}

output "cloud_app_instance_id" {
  description = "Cloud App EC2 Instance ID"
  value       = aws_instance.cloud_app.id
}

output "cloud_app_private_ip" {
  description = "Cloud App EC2 Private IP"
  value       = aws_instance.cloud_app.private_ip
}

output "onprem_oracle_instance_id" {
  description = "On-Premises Oracle EC2 Instance ID"
  value       = aws_instance.onprem_oracle.id
}

output "onprem_oracle_private_ip" {
  description = "On-Premises Oracle EC2 Private IP"
  value       = aws_instance.onprem_oracle.private_ip
}

output "ssm_connect_commands" {
  description = "SSM Session Manager Connect Commands"
  value = {
    on_premises_app = "aws ssm start-session --target ${aws_instance.on_premises_app.id} --region ${var.aws_region}"
    cloud_app       = "aws ssm start-session --target ${aws_instance.cloud_app.id} --region ${var.aws_region}"
    onprem_oracle   = "aws ssm start-session --target ${aws_instance.onprem_oracle.id} --region ${var.aws_region}"
  }
}

output "ec2_instances_summary" {
  description = "EC2 Instances Summary"
  value = {
    on_premises_app = {
      instance_id = aws_instance.on_premises_app.id
      private_ip  = aws_instance.on_premises_app.private_ip
      subnet_id   = aws_instance.on_premises_app.subnet_id
    }
    cloud_app = {
      instance_id = aws_instance.cloud_app.id
      private_ip  = aws_instance.cloud_app.private_ip
      subnet_id   = aws_instance.cloud_app.subnet_id
    }
    onprem_oracle = {
      instance_id = aws_instance.onprem_oracle.id
      private_ip  = aws_instance.onprem_oracle.private_ip
      subnet_id   = aws_instance.onprem_oracle.subnet_id
    }
  }
}

output "aurora_cluster_endpoint" {
  description = "Aurora Cluster Writer Endpoint"
  value       = aws_rds_cluster.aurora.endpoint
}

output "aurora_cluster_reader_endpoint" {
  description = "Aurora Cluster Reader Endpoint"
  value       = aws_rds_cluster.aurora.reader_endpoint
}

output "aurora_cluster_id" {
  description = "Aurora Cluster ID"
  value       = aws_rds_cluster.aurora.id
}

output "aurora_database_name" {
  description = "Aurora Database Name"
  value       = aws_rds_cluster.aurora.database_name
}

output "aurora_master_username" {
  description = "Aurora Master Username"
  value       = aws_rds_cluster.aurora.master_username
  sensitive   = true
}

output "aurora_connection_string" {
  description = "Aurora Connection String"
  value       = "postgresql://${var.db_master_username}:${var.db_master_password}@${aws_rds_cluster.aurora.endpoint}:5432/${var.db_name}"
  sensitive   = true
}
