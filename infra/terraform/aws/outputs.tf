output "cluster_name" {
  description = "EKS cluster name for configuring kubectl."
  value       = aws_eks_cluster.payflow.name
}

output "cluster_region" {
  description = "AWS region containing the EKS cluster."
  value       = var.aws_region
}

output "ecr_repository_urls" {
  description = "ECR image repository URLs, keyed by PayFlow service name."
  value       = { for service, repository in aws_ecr_repository.service : service => repository.repository_url }
}

output "configure_kubectl_command" {
  description = "Command to configure kubectl for this EKS cluster."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.payflow.name}"
}