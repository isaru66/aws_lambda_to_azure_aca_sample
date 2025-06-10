variable "resource_prefix" {
  description = "Prefix for all resources"
  type        = string
}


variable "lambda_function_name" {
  description = "Name of the Lambda function"
  default = "aws_lambda_java_hello_world"
  type        = string 
}