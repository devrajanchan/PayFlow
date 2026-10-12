# PayFlow AWS Terraform Learning Slice

This configuration describes an AWS VPC, an EKS cluster with one managed worker node, and one ECR repository per PayFlow service. It is a learning environment, not a production architecture. Creating the cluster and node incurs ongoing AWS charges. Nothing is created by `terraform init`, `fmt`, `validate`, `test`, or `plan`; `terraform apply` is the operation that provisions resources.

## Zero-Cost Gate

The current requirement is **no AWS charges**. Do not run `terraform apply` for this configuration under that requirement. EKS has a per-cluster control-plane charge, and the worker node, public IPv4 addresses, storage, and network usage can add charges. Free-tier offers or account credits are account- and time-dependent and are not a guarantee that this stack will cost $0. A budget alert is useful, but it is not a hard spending cap and may arrive after usage has accrued.

Keep using the local Docker Compose/Kubernetes setup and offline `terraform test` for now. A real AWS deployment is a separate, opt-in learning exercise only after you approve a maximum spend and check the AWS pricing calculator for your account, region, and planned runtime. I will not create AWS resources unless you explicitly authorize that cost-bearing step.

If you later decide to proceed with a spend limit, the required inputs are:

1. The AWS account you intend to use, with AWS CLI credentials configured locally through IAM Identity Center or another approved method. Never paste keys or tokens into chat or repository files.
2. The target AWS region.
3. Your current public IPv4 address in `/32` CIDR notation for the EKS API allowlist.
4. The maximum total spend you explicitly approve, including cluster runtime, node runtime, public IPv4, storage, and data transfer.
5. The planned start and destroy times. `terraform destroy` must be run after the exercise, and the account must be checked afterward for leftover resources.

If the maximum approved spend must remain exactly $0, do not deploy this EKS configuration to AWS. We can continue learning Terraform locally with plan tests and simulated providers instead.

## File Guide

| File | Purpose |
|---|---|
| `versions.tf` | Pins the Terraform CLI range and AWS provider major version, then configures the region-based provider. The generated `.terraform.lock.hcl` pins the exact provider build used. |
| `variables.tf` | Defines the region, cluster version, network range, node size, resource tags, and required public endpoint CIDR allowlist. Values that may differ by environment are inputs, not hard-coded credentials. |
| `main.tf` | Creates two public subnets across availability zones, internet routing, EKS IAM roles and policies, the cluster, its single-node managed node group, and three ECR repositories. |
| `outputs.tf` | Prints the cluster name, region, ECR URLs, and the `kubectl` configuration command after an apply. |
| `terraform.tfvars.example` | Shows the required input shape. Copy it to `terraform.tfvars` and replace the documentation-only IP address with your own public IPv4 address using a `/32` mask. |
| `terraform.tftest.hcl` | Runs plan-level tests with a mocked AWS provider, checking the subnet count, node count, ECR repository count, endpoint allowlist, and invalid CIDR rejection without AWS credentials. |
| `.terraform.lock.hcl` | Records the selected provider version and checksums for repeatable installs. Commit this file; do not commit `.terraform/` or state files. |

## Learning Workflow

1. Install Terraform and the AWS CLI. Configure AWS IAM Identity Center or another approved credential method; do not put access keys in Terraform files.
2. Configure a billing budget and alert, then confirm EKS and EC2 prices for your account and region.
3. Copy `terraform.tfvars.example` to `terraform.tfvars` and replace `203.0.113.10/32` with your current public IP in CIDR notation. The example address is reserved for documentation and will not reach the cluster.
4. From this directory, run:

   ```bash
   terraform init
   terraform fmt -check -recursive
   terraform validate
   terraform test
   aws sts get-caller-identity
   terraform plan
   ```

5. Review the plan and cost implications before deciding whether to run `terraform apply`. Do not apply this learning environment without an explicit decision to incur AWS charges.
6. If resources were applied, use `terraform destroy` as soon as the exercise is finished, then verify the resources are gone in AWS.

The `terraform test` step is safe to run before AWS access is configured. It uses a mock provider to evaluate plans and test our infrastructure intent without creating AWS resources.

Terraform uses local state for this first exercise. State can contain sensitive infrastructure metadata, so it is ignored by Git. Local state is suitable only for an individual learning exercise; shared or production environments need a secured remote backend with locking and access controls.

## Important Boundaries

- This configuration does not deploy the application workloads. The local Kubernetes manifests still reference local image names; a later deployment slice must build/tag/push images to these ECR repositories and update workload image references.
- PostgreSQL and Kafka are not provisioned on AWS in this slice. We should choose managed or self-hosted services separately, after comparing cost and operational trade-offs.
- The worker node runs in public subnets to avoid NAT Gateway hourly charges in this small exercise. This is a cost trade-off, not a production network design.
- The EKS API has public and private endpoints enabled, but the public endpoint is restricted to the CIDRs provided in `cluster_endpoint_public_access_cidrs`.
- One worker node is not highly available. The cluster and node are intentionally small for learning, but EKS control-plane and EC2 charges continue until the resources are destroyed.
- Never commit real `terraform.tfvars`, state files, AWS credentials, or kubeconfig data.

## Interview Questions

### What problem does Terraform solve here?

Terraform describes infrastructure as versioned configuration. A plan previews proposed changes before apply, while state maps the configuration to actual AWS resources. That makes the environment repeatable and reviewable, but state must be protected because it is part of Terraform's correctness model.

### Why are provider versions and the lock file both needed?

The version constraint defines which provider releases are acceptable. The lock file records the exact selected release and checksums, so another machine can install the same provider rather than silently resolving a different one.

### Why is the EKS API CIDR allowlist required input?

The Kubernetes API is an administrative control plane. Restricting its public endpoint to a known address reduces exposure; requiring the input prevents a permissive `0.0.0.0/0` default from being copied into an AWS environment unnoticed.

### Why use two subnets but one worker node?

EKS needs subnets in multiple availability zones for cluster networking and future load-balancer placement. A single worker reduces learning cost, but it is a single point of failure and is not a production availability target.

### Why use Terraform for infrastructure and Kubernetes for workloads?

Terraform owns cloud resources such as VPCs, EKS, IAM, and ECR. Kubernetes owns workload resources such as Deployments and Services. Keeping those responsibilities distinct makes infrastructure lifecycle and application rollout easier to reason about.