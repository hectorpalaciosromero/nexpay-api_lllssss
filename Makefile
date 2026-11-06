.PHONY: help build up down restart logs test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Build

build: ## Build all microservices (Maven)
	mvn clean package -DskipTests

build-docker: build ## Build Docker images for all services
	docker compose build

# Run

up: ## Start all services (infra + microservices + observability)
	docker compose up -d
	@echo ""
	@echo "  NexPay API is starting..."
	@echo ""
	@echo "  API Gateway:   http://localhost:8080"
	@echo "  Swagger UI:    http://localhost:8081/swagger-ui.html"
	@echo "  Grafana:       http://localhost:3000  (admin/nexpay)"
	@echo "  Prometheus:    http://localhost:9090"
	@echo "  Zipkin:        http://localhost:9411"
	@echo "  Kibana:        http://localhost:5601"
	@echo "  LocalStack:    http://localhost:4566"
	@echo ""

up-infra: ## Start only infrastructure (DB, Redis, LocalStack, monitoring)
	docker compose up -d postgres-wallet postgres-payment redis localstack prometheus grafana zipkin elasticsearch kibana logstash

down: ## Stop all services
	docker compose down

restart: down up ## Restart all services

# Logs

logs: ## Tail all microservice logs
	docker compose logs -f api-gateway wallet-service payment-service notification-service

logs-wallet: ## Tail wallet-service logs
	docker compose logs -f wallet-service

logs-payment: ## Tail payment-service logs
	docker compose logs -f payment-service

logs-notification: ## Tail notification-service logs
	docker compose logs -f notification-service

# Testing

test: ## Run all tests
	mvn test

test-wallet: ## Run wallet-service tests
	mvn test -pl services/wallet-service

test-payment: ## Run payment-service tests
	mvn test -pl services/payment-service

test-coverage: ## Run tests with JaCoCo coverage report
	mvn test jacoco:report

# Utilities

clean: ## Clean build artifacts and Docker volumes
	mvn clean
	docker compose down -v --remove-orphans

aws-status: ## Check LocalStack AWS resources
	@echo "SQS Queues:"
	@aws --endpoint-url=http://localhost:4566 sqs list-queues --region us-east-1
	@echo "\nSNS Topics:"
	@aws --endpoint-url=http://localhost:4566 sns list-topics --region us-east-1
	@echo "\nS3 Buckets:"
	@aws --endpoint-url=http://localhost:4566 s3 ls --region us-east-1
	@echo "\nDynamoDB Tables:"
	@aws --endpoint-url=http://localhost:4566 dynamodb list-tables --region us-east-1

health: ## Check health of all services
	@echo "Wallet Service:"; curl -s http://localhost:8081/actuator/health | python3 -m json.tool 2>/dev/null || echo "DOWN"
	@echo "\nPayment Service:"; curl -s http://localhost:8082/actuator/health | python3 -m json.tool 2>/dev/null || echo "DOWN"
	@echo "\nNotification Service:"; curl -s http://localhost:8083/actuator/health | python3 -m json.tool 2>/dev/null || echo "DOWN"
