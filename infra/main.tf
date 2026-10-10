terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
  }

  backend "s3" {}
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  description = "AWS region for the Lambda function and container registry."
  type        = string
  default     = "us-east-1"
}

variable "image_uri" {
  description = "ECR image URI, including the tag, for the Spring Boot application."
  type        = string
}

variable "create_lambda" {
  description = "Create the function. The first apply only creates the registry so the image can be pushed."
  type        = bool
  default     = true
}

variable "mongodb_uri" {
  description = "MongoDB connection string passed to the function as MONGODB_URI_TICKERTAPE."
  type        = string
  sensitive   = true
}

resource "aws_ecr_repository" "app" {
  name                 = "tickertape-spring"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep the five most recent images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 5
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_iam_role" "lambda" {
  name = "tickertape-spring-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "app" {
  count = var.create_lambda ? 1 : 0

  function_name = "tickertape-spring"
  role          = aws_iam_role.lambda.arn
  package_type  = "Image"
  image_uri     = var.image_uri
  architectures = ["x86_64"]
  memory_size   = 1024
  timeout       = 60

  environment {
    variables = {
      PORT                   = "8080"
      AWS_LWA_PORT           = "8080"
      AWS_LWA_ASYNC_INIT     = "true"
      APP_SYNC_ON_STARTUP    = "false"
      MONGODB_URI_TICKERTAPE = var.mongodb_uri
    }
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_logs]
}

resource "aws_lambda_function_url" "app" {
  count = var.create_lambda ? 1 : 0

  function_name      = aws_lambda_function.app[0].function_name
  authorization_type = "NONE"
}

resource "aws_lambda_permission" "function_url" {
  count = var.create_lambda ? 1 : 0

  statement_id           = "FunctionURLAllowPublicAccess"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.app[0].function_name
  principal              = "*"
  function_url_auth_type = "NONE"
}

output "function_url" {
  description = "Public URL for the stocks demo."
  value       = one(aws_lambda_function_url.app[*].function_url)
}

output "ecr_repository_url" {
  description = "ECR repository that stores the application image."
  value       = aws_ecr_repository.app.repository_url
}
