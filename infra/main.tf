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
  region = "ap-south-1"
}

variable "package_path" {
  description = "Path to the function zip (run.sh and app.jar)."
  type        = string
}

variable "mongodb_uri" {
  description = "MongoDB connection string passed to the function as MONGODB_URI_TICKERTAPE."
  type        = string
  sensitive   = true
}

resource "aws_iam_role" "lambda" {
  name = "tickertape-spring-lambda-ap-south-1"

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
  function_name = "moneyctrl"
  role          = aws_iam_role.lambda.arn
  runtime       = "java21"
  handler       = "run.sh"
  filename      = var.package_path
  source_code_hash = filebase64sha256(var.package_path)
  architectures = ["x86_64"]
  memory_size   = 1024
  timeout       = 60
  layers = [
    "arn:aws:lambda:ap-south-1:753240598075:layer:LambdaAdapterLayerX86:30"
  ]

  environment {
    variables = {
      AWS_LAMBDA_EXEC_WRAPPER = "/opt/bootstrap"
      AWS_LWA_PORT            = "8080"
      AWS_LWA_ASYNC_INIT      = "true"
      APP_SYNC_ON_STARTUP     = "false"
      MONGODB_URI_TICKERTAPE  = var.mongodb_uri
    }
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_logs]
}

resource "aws_lambda_function_url" "app" {
  function_name      = aws_lambda_function.app.function_name
  authorization_type = "NONE"
}

resource "aws_lambda_permission" "function_url" {
  statement_id           = "FunctionURLAllowPublicAccess"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.app.function_name
  principal              = "*"
  function_url_auth_type = "NONE"
}

output "function_url" {
  description = "Public URL for the stocks demo."
  value       = aws_lambda_function_url.app.function_url
}
