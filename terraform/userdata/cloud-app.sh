#!/bin/bash
set -e

# Update system
dnf update -y

# Install git
dnf install -y git

# Install Java 17 (for Spring Boot)
dnf install -y java-17-amazon-corretto-devel

# Install PostgreSQL client
dnf install -y postgresql16

# Clone project repository
cd /home/ec2-user
git clone https://github.com/dongkoo81/oracle-postgresql-migration.git

# Set ownership
chown -R ec2-user:ec2-user /home/ec2-user/oracle-postgresql-migration

# Verify installations
git --version
java -version
psql --version

echo "Installation completed successfully!"
echo "Project cloned to: /home/ec2-user/oracle-postgresql-migration"
