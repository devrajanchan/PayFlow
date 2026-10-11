variable "aws_region" {
  description = "AWS region for PayFlow resources."
  type        = string
  default     = "us-west-2"
}

variable "project_name" {
  description = "Short name used to identify PayFlow resources."
  type        = string
  default     = "payflow"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. Confirm the version is offered in aws_region before applying."
  type        = string
  default     = "1.34"
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "IPv4 CIDR ranges allowed to reach the public Kubernetes API endpoint. Supply your public IP as a /32."
  type        = list(string)

  validation {
    condition = length(var.cluster_endpoint_public_access_cidrs) > 0 && alltrue([
      for cidr in var.cluster_endpoint_public_access_cidrs : can(cidrhost(cidr, 0))
    ])
    error_message = "Provide at least one valid IPv4 CIDR, preferably your current public IP with a /32 mask."
  }
}

variable "vpc_cidr" {
  description = "IPv4 address range for the PayFlow VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "node_instance_type" {
  description = "EC2 instance type for the single learning node."
  type        = string
  default     = "t3.small"
}

variable "tags" {
  description = "Additional tags applied to AWS resources."
  type        = map(string)
  default = {
    Project   = "PayFlow"
    ManagedBy = "Terraform"
  }
}