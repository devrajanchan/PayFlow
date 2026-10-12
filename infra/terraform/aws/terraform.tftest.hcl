mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["us-west-2a", "us-west-2b"]
    }
  }
}

run "learning_environment_has_expected_shape" {
  command = plan

  variables {
    cluster_endpoint_public_access_cidrs = ["192.0.2.10/32"]
  }

  assert {
    condition     = length(aws_subnet.public) == 2
    error_message = "The learning VPC must use two public subnets across availability zones."
  }

  assert {
    condition     = aws_eks_node_group.payflow.scaling_config[0].desired_size == 1 && aws_eks_node_group.payflow.scaling_config[0].max_size == 1
    error_message = "The learning node group must stay at one node to limit cost."
  }

  assert {
    condition     = length(aws_ecr_repository.service) == 3
    error_message = "Each PayFlow service must have an ECR repository."
  }

  assert {
    condition     = toset(aws_eks_cluster.payflow.vpc_config[0].public_access_cidrs) == toset(var.cluster_endpoint_public_access_cidrs)
    error_message = "The EKS API endpoint must use the caller-provided CIDR allowlist."
  }
}

run "rejects_invalid_endpoint_cidr" {
  command = plan

  variables {
    cluster_endpoint_public_access_cidrs = ["not-a-cidr"]
  }

  expect_failures = [
    var.cluster_endpoint_public_access_cidrs,
  ]
}