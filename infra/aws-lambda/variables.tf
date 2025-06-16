variable "resource_prefix" {
  description = "Prefix for all resources"
  type        = string
}

variable "lambda_function_name" {
  description = "Name of the Lambda function"
  default = "aws_lambda_s3_handler_java"
  type        = string 
}

variable "lambda_jar_filename" {
  description = "Name of the Lambda JAR file"
  default     = "aws-lambda-s3-handler-java-1.0-SNAPSHOT.jar"
  type        = string
}