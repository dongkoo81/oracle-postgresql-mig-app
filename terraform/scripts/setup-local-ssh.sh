#!/bin/bash
set -e

echo "=== VSCode Remote SSH Setup for Private EC2 Instances ==="
echo ""

# Check if terraform output exists
if [ ! -f "../terraform.tfstate" ]; then
    echo "Error: terraform.tfstate not found. Please run 'terraform apply' first."
    exit 1
fi

# Get outputs from terraform
cd ..
REGION=$(terraform output -raw aws_region 2>/dev/null || echo "ap-northeast-2")
ON_PREM_APP_ID=$(terraform output -raw on_premises_app_instance_id)
CLOUD_APP_ID=$(terraform output -raw cloud_app_instance_id)
ORACLE_ID=$(terraform output -raw onprem_oracle_instance_id)
PRIVATE_KEY_PATH=$(terraform output -raw private_key_path)
# Convert to absolute path
PRIVATE_KEY_PATH=$(cd "$(dirname "$PRIVATE_KEY_PATH")" && pwd)/$(basename "$PRIVATE_KEY_PATH")

echo "Terraform outputs retrieved:"
echo "  Region: $REGION"
echo "  On-Premises App Instance ID: $ON_PREM_APP_ID"
echo "  Cloud App Instance ID: $CLOUD_APP_ID"
echo "  Oracle Instance ID: $ORACLE_ID"
echo "  Private Key Path: $PRIVATE_KEY_PATH"
echo ""

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if Session Manager Plugin is installed
if ! command -v session-manager-plugin &> /dev/null; then
    echo "Error: AWS Session Manager Plugin is not installed."
    echo "Install it from: https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html"
    exit 1
fi

# Create SSH config
SSH_CONFIG="$HOME/.ssh/config"
BACKUP_CONFIG="$HOME/.ssh/config.backup.$(date +%Y%m%d_%H%M%S)"

# Backup existing config
if [ -f "$SSH_CONFIG" ]; then
    echo "Backing up existing SSH config to: $BACKUP_CONFIG"
    cp "$SSH_CONFIG" "$BACKUP_CONFIG"
fi

# Append new configuration
echo "" >> "$SSH_CONFIG"
echo "# Oracle PostgreSQL Migration Project - Private EC2 via SSM" >> "$SSH_CONFIG"
echo "# Generated on $(date)" >> "$SSH_CONFIG"
echo "" >> "$SSH_CONFIG"

# On-Premises App
cat >> "$SSH_CONFIG" << EOF
Host on-premises-app
    HostName $ON_PREM_APP_ID
    User ec2-user
    IdentityFile $PRIVATE_KEY_PATH
    ProxyCommand sh -c "aws ssm start-session --target %h --document-name AWS-StartSSHSession --parameters 'portNumber=%p' --region $REGION"
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null

EOF

# Cloud App
cat >> "$SSH_CONFIG" << EOF
Host cloud-app
    HostName $CLOUD_APP_ID
    User ec2-user
    IdentityFile $PRIVATE_KEY_PATH
    ProxyCommand sh -c "aws ssm start-session --target %h --document-name AWS-StartSSHSession --parameters 'portNumber=%p' --region $REGION"
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null

EOF

# Oracle
cat >> "$SSH_CONFIG" << EOF
Host onprem-oracle
    HostName $ORACLE_ID
    User ec2-user
    IdentityFile $PRIVATE_KEY_PATH
    ProxyCommand sh -c "aws ssm start-session --target %h --document-name AWS-StartSSHSession --parameters 'portNumber=%p' --region $REGION"
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null

EOF

echo "✅ SSH config updated successfully!"
echo ""
echo "=== How to use ==="
echo ""
echo "1. Terminal SSH:"
echo "   ssh on-premises-app"
echo "   ssh cloud-app"
echo "   ssh onprem-oracle"
echo ""
echo "2. VSCode Remote-SSH:"
echo "   - Press F1 or Cmd+Shift+P"
echo "   - Select 'Remote-SSH: Connect to Host...'"
echo "   - Choose: on-premises-app, cloud-app, or onprem-oracle"
echo ""
echo "3. Direct SSM Session (no SSH):"
echo "   aws ssm start-session --target $ON_PREM_APP_ID --region $REGION"
echo "   aws ssm start-session --target $CLOUD_APP_ID --region $REGION"
echo "   aws ssm start-session --target $ORACLE_ID --region $REGION"
echo ""
echo "Note: Make sure your AWS credentials are configured properly."
