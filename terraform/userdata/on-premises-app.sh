#!/bin/bash
set -e

# Set hostname
hostnamectl set-hostname on-premises-app
echo "127.0.0.1 on-premises-app" >> /etc/hosts

# Update system
dnf update -y

# Install git
dnf install -y git

# Install Java 17 (for Spring Boot)
dnf install -y java-17-amazon-corretto-devel

# Install PostgreSQL client
dnf install -y postgresql16

# Install Oracle Instant Client dependencies
dnf install -y libaio libnsl wget unzip

# Download and install Oracle Instant Client
cd /tmp
wget https://download.oracle.com/otn_software/linux/instantclient/1923000/instantclient-basic-linux.x64-19.23.0.0.0dbru.zip
wget https://download.oracle.com/otn_software/linux/instantclient/1923000/instantclient-sqlplus-linux.x64-19.23.0.0.0dbru.zip

unzip -o instantclient-basic-linux.x64-19.23.0.0.0dbru.zip -d /opt/oracle
unzip -o instantclient-sqlplus-linux.x64-19.23.0.0.0dbru.zip -d /opt/oracle

# Set Oracle environment variables
echo "export ORACLE_HOME=/opt/oracle/instantclient_19_23" >> /etc/profile.d/oracle.sh
echo "export LD_LIBRARY_PATH=\$ORACLE_HOME:\$LD_LIBRARY_PATH" >> /etc/profile.d/oracle.sh
echo "export PATH=\$ORACLE_HOME:\$PATH" >> /etc/profile.d/oracle.sh

# Apply environment variables
source /etc/profile.d/oracle.sh

# Create symlink for sqlplus
ln -sf /opt/oracle/instantclient_19_23/sqlplus /usr/local/bin/sqlplus

# Clone project repository
mkdir -p /home/ec2-user/projects
cd /home/ec2-user/projects
git clone https://github.com/dongkoo81/oracle-postgresql-migration.git

# Set ownership
chown -R ec2-user:ec2-user /home/ec2-user/projects

# Verify installations
git --version
java -version
psql --version
sqlplus -v

echo "Installation completed successfully!"
echo "Project cloned to: /home/ec2-user/projects/oracle-postgresql-migration"
