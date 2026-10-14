# PayFlow Local Kubernetes Learning Lab

This lab runs the PayFlow services with PostgreSQL and a single-node Apache Kafka broker in KRaft mode on a local Kubernetes cluster created with `kind`. It uses Docker Desktop resources on your Mac and creates no AWS resources. A local cluster teaches Kubernetes Deployments, Services, ConfigMaps, Secrets, probes, resource requests/limits, image rollout, logs, and port forwarding. It does not behave identically to EKS: AWS IAM integration, cloud load balancers, managed storage, VPC networking, and EKS operations need separate cloud practice.

## Prerequisites

- Docker Desktop running. Allocate about 6 GB of memory and at least 4 CPUs to its Linux VM for the full stack; actual usage varies by workload.
- Java 25 for building the services.
- `kubectl` and `kind`. On macOS with Homebrew, install `kind` using `brew install kind`.
- The default local PostgreSQL password is for learning only. Do not reuse it outside this local sandbox.

## Run The Lab

From the repository root:

```bash
kind create cluster --name payflow
docker compose -f infra/docker-compose.yml build payment-intake payment-processor payment-notification
kind load docker-image infra-payment-intake:latest infra-payment-processor:latest infra-payment-notification:latest --name payflow
kubectl apply -f infra/kubernetes/namespace.yaml
kubectl apply -k infra/kubernetes
kubectl get pods -n payflow --watch
```

The compose file is used here only as a convenient image builder. Its containers are not started; PostgreSQL, Kafka, and the three applications run as Kubernetes workloads. `kind load docker-image` copies those local image tags into the kind node so Kubernetes does not need a registry. The applications wait in init containers until PostgreSQL and Kafka become reachable; Kafka becomes a Service endpoint only after its readiness probe succeeds.

Check each workload and its probes:

```bash
kubectl get deployments,services,pods -n payflow
kubectl rollout status deployment/payment-intake -n payflow
kubectl rollout status deployment/payment-processor -n payflow
kubectl rollout status deployment/payment-notification -n payflow
kubectl logs -n payflow deployment/payment-intake
kubectl logs -n payflow deployment/payment-processor
kubectl logs -n payflow deployment/payment-notification
```

Expose the intake service to your Mac in a separate terminal:

```bash
kubectl port-forward -n payflow service/payment-intake 8081:8081
```

Then use the existing `POST /payments` example in the root README. The HTTP request reaches intake; Kafka carries the accepted event to the processor; the processor persists the ledger result and publishes the processed event; notification consumes it. Use the pod logs to follow that event path.

To inspect Kafka topics in Kafdrop, use a second terminal:

```bash
kubectl port-forward -n payflow service/kafdrop 9000:9000
```

Open `http://localhost:9000` in your browser. The UI is a local Kafka metadata/topic browser; it is not an application business dashboard.

To rebuild a changed service, rebuild its image, load it into kind again, then restart that deployment. For example:

```bash
docker compose -f infra/docker-compose.yml build payment-intake
kind load docker-image infra-payment-intake:latest --name payflow
kubectl rollout restart deployment/payment-intake -n payflow
kubectl rollout status deployment/payment-intake -n payflow
```

To stop and remove the whole local cluster:

```bash
kind delete cluster --name payflow
```

## Terraform Without AWS Resources

Terraform's mocked tests are the fully offline routine:

```bash
terraform -chdir=infra/terraform/aws validate
terraform -chdir=infra/terraform/aws test
```

`terraform test` uses the mocked AWS provider to check the planned resource shape and input validation without AWS credentials or cloud resource creation. A normal `terraform plan` against the real AWS provider is different: it may need AWS credentials and make read/API calls to refresh remote state or evaluate data sources. It does not create the planned resources, but it is not an offline simulator. Never run `terraform apply` for this project while the no-AWS-charge requirement remains in effect.

## Resource Budget And Trade-Offs

The three Spring services each use a 256 MiB Java heap cap and a 512 MiB Kubernetes memory limit. PostgreSQL and Kafka also have explicit requests and limits. These are local development settings, not production sizing recommendations. JVM memory includes native memory beyond the heap, so the pod limit is intentionally higher than `-Xmx`.

All local Kubernetes data is ephemeral because PostgreSQL uses `emptyDir`; deleting the kind cluster deletes the database. This avoids leaving persistent local volumes behind, but it is not a durability design. For durable local iteration, use Docker Compose's named volume or create a PersistentVolumeClaim deliberately.

## Interview Questions

### Why does this lab use Kafka KRaft while Docker Compose uses ZooKeeper?

Kafka's KRaft mode stores cluster metadata in Kafka's own quorum rather than requiring ZooKeeper. This lab has one broker and one controller to keep the local resource footprint down. The Compose environment remains unchanged as a separate setup. Neither single-node configuration provides production availability.

### Why use kind if the target cloud is EKS?

Kind provides a disposable local Kubernetes cluster for learning the core Kubernetes API and deployment workflow without paying for an always-on control plane. The same standard Deployments, Services, probes, and resource declarations are useful on EKS, but cloud integrations and managed-service behavior are not reproduced locally.

### Why load images into kind instead of pushing to ECR?

The app images are built locally and copied directly to the kind node, so the exercise needs no registry account or image transfer. In a cloud release pipeline we would tag, scan, and push immutable images to ECR, then deploy those image references.

### What does a readiness probe do differently from a liveness probe?

Readiness determines whether a pod should receive service traffic. Liveness determines whether Kubernetes should restart a stuck container. A startup probe gives a slow-starting Spring application time to initialize before liveness checks begin.

### Why set both a JVM heap cap and Kubernetes memory limits?

The heap cap bounds Java-managed memory, while the container limit bounds the whole process, including metaspace, thread stacks, direct buffers, and native libraries. The container limit must leave headroom above `-Xmx` or the kernel may terminate the process.

### How is this cost-free but still real experience?

The Kubernetes control plane and pods are real processes running locally in Docker, and Terraform tests exercise actual configuration and plan assertions with a provider mock. It teaches workflows and resource modeling, but it does not claim that AWS services or AWS billing behavior were exercised.