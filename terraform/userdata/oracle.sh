#!/bin/bash
set -e

# Install SSM Agent for RHEL
dnf install -y https://s3.ap-northeast-2.amazonaws.com/amazon-ssm-ap-northeast-2/latest/linux_amd64/amazon-ssm-agent.rpm

# Start and enable SSM Agent
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

echo "SSM Agent installed and started"
