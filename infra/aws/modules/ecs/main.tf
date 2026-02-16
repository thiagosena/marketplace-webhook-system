# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-cluster"
  }
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "marketplace" {
  name              = "/ecs/${var.project_name}-marketplace-service"
  retention_in_days = 3

  tags = {
    Name = "${var.project_name}-marketplace-logs"
  }
}

resource "aws_cloudwatch_log_group" "receiver" {
  name              = "/ecs/${var.project_name}-receiver-service"
  retention_in_days = 3

  tags = {
    Name = "${var.project_name}-receiver-logs"
  }
}

# ECS Task Execution Role
resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.project_name}-${var.environment}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })

  tags = {
    Name = "${var.project_name}-${var.environment}-ecs-task-execution-role"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ECS Task Role
resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-${var.environment}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })

  tags = {
    Name = "${var.project_name}-${var.environment}-ecs-task-role"
  }
}

# Security Groups
resource "aws_security_group" "alb_marketplace" {
  name        = "${var.project_name}-${var.environment}-alb-marketplace-sg"
  description = "Security group for Marketplace ALB"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
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
    Name = "${var.project_name}-${var.environment}-alb-marketplace-sg"
  }
}

resource "aws_security_group" "alb_receiver" {
  name        = "${var.project_name}-${var.environment}-alb-receiver-sg"
  description = "Security group for Receiver ALB"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
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
    Name = "${var.project_name}-${var.environment}-alb-receiver-sg"
  }
}

resource "aws_security_group" "ecs" {
  name        = "${var.project_name}-${var.environment}-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    description     = "From Marketplace ALB"
    from_port       = var.marketplace_container_port
    to_port         = var.marketplace_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_marketplace.id]
  }

  ingress {
    description     = "From Receiver ALB"
    from_port       = var.receiver_container_port
    to_port         = var.receiver_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_receiver.id]
  }

  ingress {
    description = "Inter-service communication"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-ecs-sg"
  }
}

# Application Load Balancers
resource "aws_lb" "marketplace" {
  name               = "${var.project_name}-${var.environment}-marketplace-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_marketplace.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name = "${var.project_name}-${var.environment}-marketplace-alb"
  }
}

resource "aws_lb" "receiver" {
  name               = "${var.project_name}-${var.environment}-receiver-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_receiver.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name = "${var.project_name}-${var.environment}-receiver-alb"
  }
}

# Target Groups
resource "aws_lb_target_group" "marketplace" {
  name        = "${var.project_name}-${var.environment}-marketplace-tg"
  port        = var.marketplace_container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Name = "${var.project_name}-${var.environment}-marketplace-tg"
  }
}

resource "aws_lb_target_group" "receiver" {
  name        = "${var.project_name}-${var.environment}-receiver-tg"
  port        = var.receiver_container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Name = "${var.project_name}-${var.environment}-receiver-tg"
  }
}

# Listeners
resource "aws_lb_listener" "marketplace" {
  load_balancer_arn = aws_lb.marketplace.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.marketplace.arn
  }
}

resource "aws_lb_listener" "receiver" {
  load_balancer_arn = aws_lb.receiver.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.receiver.arn
  }
}

# Task Definitions
resource "aws_ecs_task_definition" "marketplace" {
  family                   = "${var.project_name}-marketplace-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.marketplace_cpu
  memory                   = var.marketplace_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "marketplace-service"
    image = "${var.marketplace_ecr_repository}:latest"

    portMappings = [{
      containerPort = var.marketplace_container_port
      protocol      = "tcp"
    }]

    environment = [
      {
        name  = "SERVER_PORT"
        value = tostring(var.marketplace_container_port)
      },
      {
        name  = "DATABASE_URL"
        value = "jdbc:postgresql://${var.marketplace_db_host}/${var.marketplace_db_name}"
      },
      {
        name  = "DATABASE_USER"
        value = var.db_username
      },
      {
        name  = "DATABASE_PASSWORD"
        value = var.db_password
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.marketplace.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.marketplace_container_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = {
    Name = "${var.project_name}-marketplace-service"
  }
}

resource "aws_ecs_task_definition" "receiver" {
  family                   = "${var.project_name}-receiver-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.receiver_cpu
  memory                   = var.receiver_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "receiver-service"
    image = "${var.receiver_ecr_repository}:latest"

    portMappings = [{
      containerPort = var.receiver_container_port
      protocol      = "tcp"
    }]

    environment = [
      {
        name  = "SERVER_PORT"
        value = tostring(var.receiver_container_port)
      },
      {
        name  = "DATABASE_URL"
        value = "jdbc:postgresql://${var.receiver_db_host}/${var.receiver_db_name}"
      },
      {
        name  = "DATABASE_USER"
        value = var.db_username
      },
      {
        name  = "DATABASE_PASSWORD"
        value = var.db_password
      },
      {
        name  = "MARKETPLACE_SERVICE_URL"
        value = "http://${aws_lb.marketplace.dns_name}"
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.receiver.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.receiver_container_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = {
    Name = "${var.project_name}-receiver-service"
  }
}

# ECS Services
resource "aws_ecs_service" "marketplace" {
  name            = "${var.project_name}-marketplace-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.marketplace.arn
  desired_count   = var.marketplace_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.marketplace.arn
    container_name   = "marketplace-service"
    container_port   = var.marketplace_container_port
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  depends_on = [
    aws_lb_listener.marketplace,
    aws_iam_role_policy_attachment.ecs_task_execution
  ]

  tags = {
    Name = "${var.project_name}-marketplace-service"
  }
}

resource "aws_ecs_service" "receiver" {
  name            = "${var.project_name}-receiver-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.receiver.arn
  desired_count   = var.receiver_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.receiver.arn
    container_name   = "receiver-service"
    container_port   = var.receiver_container_port
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  depends_on = [
    aws_lb_listener.receiver,
    aws_iam_role_policy_attachment.ecs_task_execution,
    aws_ecs_service.marketplace
  ]

  tags = {
    Name = "${var.project_name}-receiver-service"
  }
}

# Data source for current region
data "aws_region" "current" {}
