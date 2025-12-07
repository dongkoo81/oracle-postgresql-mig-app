#!/bin/bash
set -e

# Set hostname
hostnamectl set-hostname cloud-app
echo "127.0.0.1 cloud-app" >> /etc/hosts

# Wait for any existing dnf processes
while pgrep -x dnf > /dev/null; do
  sleep 2
done

# Update system
dnf update -y

# Install git
dnf install -y git

# Install Java 17 (for Spring Boot)
dnf install -y java-17-amazon-corretto-devel

# Install PostgreSQL client
dnf install -y postgresql16

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

echo "Installation completed successfully!"
echo "Project cloned to: /home/ec2-user/projects/oracle-postgresql-migration"
