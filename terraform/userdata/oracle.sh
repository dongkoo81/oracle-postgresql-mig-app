#!/bin/bash
set -e

# Set hostname
hostnamectl set-hostname onprem-oracle-ee-19c
echo "127.0.0.1 onprem-oracle-ee-19c" >> /etc/hosts

# Install SSM Agent for RHEL
dnf install -y https://s3.ap-northeast-2.amazonaws.com/amazon-ssm-ap-northeast-2/latest/linux_amd64/amazon-ssm-agent.rpm

# Start and enable SSM Agent
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

# Get private IP address
PRIVATE_IP=$(hostname -I | awk '{print $1}')

# Wait for Oracle to be ready (max 5 minutes)
for i in {1..30}; do
  if su - oracle -c "sqlplus -s / as sysdba <<< 'SELECT 1 FROM DUAL;' > /dev/null 2>&1"; then
    break
  fi
  sleep 10
done

# Update listener.ora with new private IP
LISTENER_ORA="/app/oracle/product/19.0.0/dbhome_1/network/admin/listener.ora"
if [ -f "$LISTENER_ORA" ]; then
  su - oracle -c "sed -i 's/(HOST = [^)]*)(PORT = 1521)/(HOST = $PRIVATE_IP)(PORT = 1521)/' $LISTENER_ORA"
  
  # Restart listener
  su - oracle -c "lsnrctl stop"
  su - oracle -c "lsnrctl start"
  
  echo "Listener.ora updated with IP: $PRIVATE_IP"
fi

echo "SSM Agent installed and Oracle listener configured"
