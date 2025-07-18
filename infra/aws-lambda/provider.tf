terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  required_version = ">= 0.13"
}

provider "aws" {
  region  = "ap-southeast-1"
  profile = "default"
}