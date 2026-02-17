resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-${var.environment}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-rds-sg"
  }
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-${var.environment}-postgres"
  engine         = "postgres"
  engine_version = "18.2"
  instance_class = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 2
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.marketplace_db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  skip_final_snapshot       = true
  final_snapshot_identifier = "${var.project_name}-${var.environment}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = {
    Name = "${var.project_name}-${var.environment}-postgres"
  }

  lifecycle {
    ignore_changes = [final_snapshot_identifier]
  }
}

# Key pair para o bastion
resource "aws_key_pair" "bastion" {
  key_name   = "${var.project_name}-${var.environment}-bastion-key"
  public_key = var.bastion_public_key

  tags = {
    Name = "${var.project_name}-${var.environment}-bastion-key"
  }
}

# Security group para bastion temporário
resource "aws_security_group" "bastion" {
  name        = "${var.project_name}-${var.environment}-bastion-sg"
  description = "Security group for temporary bastion host"
  vpc_id      = var.vpc_id

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-bastion-sg"
  }
}

# Permitir conexão do bastion ao RDS
resource "aws_security_group_rule" "rds_from_bastion" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bastion.id
  security_group_id        = aws_security_group.rds.id
  description              = "PostgreSQL from bastion"
}

# Buscar AMI mais recente do Amazon Linux 2023
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

# EC2 bastion temporário para criar o database
resource "aws_instance" "bastion" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.micro"
  subnet_id     = var.public_subnet_ids[0]
  key_name      = aws_key_pair.bastion.key_name

  vpc_security_group_ids = [aws_security_group.bastion.id]

  user_data = <<-EOF
              #!/bin/bash
              dnf install -y postgresql15
              EOF

  tags = {
    Name = "${var.project_name}-${var.environment}-bastion-temp"
  }

  depends_on = [aws_db_instance.main]
}

# Criar database receiver via bastion
resource "null_resource" "create_receiver_db" {
  depends_on = [aws_instance.bastion, aws_security_group_rule.rds_from_bastion]

  provisioner "remote-exec" {
    inline = [
      "until pg_isready -h ${aws_db_instance.main.address} -p ${aws_db_instance.main.port} -U ${var.db_username}; do sleep 5; done",
      "PGPASSWORD='${var.db_password}' psql -h ${aws_db_instance.main.address} -p ${aws_db_instance.main.port} -U ${var.db_username} -d ${var.marketplace_db_name} -c \"SELECT 1 FROM pg_database WHERE datname = '${var.receiver_db_name}'\" | grep -q 1 || PGPASSWORD='${var.db_password}' psql -h ${aws_db_instance.main.address} -p ${aws_db_instance.main.port} -U ${var.db_username} -d ${var.marketplace_db_name} -c \"CREATE DATABASE ${var.receiver_db_name};\""
    ]

    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = var.bastion_private_key
      host        = aws_instance.bastion.public_ip
    }
  }

  # Destruir o bastion após criar o database
  provisioner "local-exec" {
    when    = destroy
    command = "echo 'Bastion will be destroyed with terraform destroy'"
  }
}