# Get latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# On-Premises App EC2 (Private Subnet)
resource "aws_instance" "on_premises_app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.private[0].id
  vpc_security_group_ids = [aws_security_group.main.id]
  key_name               = aws_key_pair.ec2_key.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = file("${path.module}/userdata/on-premises-app.sh")

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = {
    Name = "on-premises-app"
  }
}

# Cloud App EC2 (Private Subnet)
resource "aws_instance" "cloud_app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.private[1].id
  vpc_security_group_ids = [aws_security_group.main.id]
  key_name               = aws_key_pair.ec2_key.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = file("${path.module}/userdata/cloud-app.sh")

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = {
    Name = "cloud-app"
  }
}

# On-Premises Oracle EE 19c EC2 (Private Subnet)
resource "aws_instance" "onprem_oracle" {
  ami                    = var.oracle_ami_id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.private[2].id
  vpc_security_group_ids = [aws_security_group.main.id]
  key_name               = aws_key_pair.ec2_key.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = file("${path.module}/userdata/oracle.sh")

  root_block_device {
    volume_size = 200
    volume_type = "gp3"
  }

  tags = {
    Name = "onprem-oracle-ee-19c"
  }
}
