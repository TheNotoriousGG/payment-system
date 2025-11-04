.PHONY: help all infra build-services build-person-service build-individuals-api start-apps start stop restart clean deep-clean logs infra-logs status test-person-service test-person-service-all rebuild

DOCKER_COMPOSE = docker compose

NEXUS_URL = http://localhost:8082
KEYCLOAK_URL = http://localhost:9000

INFRA_SERVICES = nexus keycloak person-postgres prometheus grafana tempo loki alloy
APP_SERVICES = person-service individuals-api

GREEN = \033[0;32m
YELLOW = \033[0;33m
BLUE = \033[0;34m
RED = \033[0;31m
NC = \033[0m

.DEFAULT_GOAL := help

##@ General

help: ## Display this help message
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@echo "$(GREEN)Payment System - Makefile Commands$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@awk 'BEGIN {FS = ":.*##"; printf "\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(YELLOW)%-25s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""

##@ Development Workflow

all: infra build-services start-apps ## Complete setup: start infrastructure, build and start apps
	@echo "$(GREEN)✓ All services are up and running!$(NC)"
	@$(MAKE) --no-print-directory status

##@ Infrastructure

infra: ## Start infrastructure services (Nexus, Keycloak, PostgreSQL, monitoring)
	@echo "$(BLUE)→ Starting infrastructure services...$(NC)"
	@$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
	@echo "$(YELLOW)⏳ Waiting for Nexus to be ready...$(NC)"
	@until curl -sf $(NEXUS_URL)/service/rest/v1/status > /dev/null 2>&1; do \
		printf "."; sleep 3; \
	done
	@echo "\n$(GREEN)✓ Nexus is ready!$(NC)"
	@echo "$(YELLOW)⏳ Waiting for Keycloak to be ready...$(NC)"
	@until curl -sf $(KEYCLOAK_URL)/health/ready > /dev/null 2>&1; do \
		printf "."; sleep 3; \
	done
	@echo "\n$(GREEN)✓ Keycloak is ready!$(NC)"
	@echo "$(GREEN)✓ Infrastructure is ready!$(NC)"

infra-logs: ## Show logs from infrastructure services
	@$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

infra-stop: ## Stop infrastructure services
	@echo "$(YELLOW)→ Stopping infrastructure services...$(NC)"
	@$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)
	@echo "$(GREEN)✓ Infrastructure stopped$(NC)"

##@ Application Services

build-services: build-person-service build-individuals-api ## Build all application services

build-person-service: ## Build person-service Docker image
	@echo "$(BLUE)→ Building person-service...$(NC)"
	@$(DOCKER_COMPOSE) build person-service --no-cache
	@echo "$(GREEN)✓ person-service built successfully$(NC)"

build-individuals-api: ## Build individuals-api Docker image
	@echo "$(BLUE)→ Building individuals-api...$(NC)"
	@$(DOCKER_COMPOSE) build individuals-api --no-cache
	@echo "$(GREEN)✓ individuals-api built successfully$(NC)"

start-apps: ## Start application services
	@echo "$(BLUE)→ Starting application services...$(NC)"
	@$(DOCKER_COMPOSE) up -d $(APP_SERVICES)
	@echo "$(GREEN)✓ Application services started$(NC)"

##@ Container Management

start: ## Start all services (without building)
	@echo "$(BLUE)→ Starting all services...$(NC)"
	@$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)✓ All services started$(NC)"

stop: ## Stop all services
	@echo "$(YELLOW)→ Stopping all services...$(NC)"
	@$(DOCKER_COMPOSE) down
	@echo "$(GREEN)✓ All services stopped$(NC)"

restart: stop start ## Restart all services

status: ## Show status of all services
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@echo "$(GREEN)Service Status$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@$(DOCKER_COMPOSE) ps

##@ Logging

logs: ## Show logs from all services
	@$(DOCKER_COMPOSE) logs -f --tail=200

logs-person: ## Show logs from person-service
	@$(DOCKER_COMPOSE) logs -f --tail=200 person-service

logs-individuals: ## Show logs from individuals-api
	@$(DOCKER_COMPOSE) logs -f --tail=200 individuals-api

logs-nexus: ## Show logs from Nexus
	@$(DOCKER_COMPOSE) logs -f --tail=200 nexus

logs-keycloak: ## Show logs from Keycloak
	@$(DOCKER_COMPOSE) logs -f --tail=200 keycloak

##@ Testing

test-person-service: ## Run integration tests for person-service
	@echo "$(BLUE)→ Running person-service integration tests...$(NC)"
	@cd person-service && ./gradlew test --tests IndividualRestControllerV1IT
	@echo "$(GREEN)✓ Tests completed$(NC)"

test-person-service-all: ## Run all tests for person-service
	@echo "$(BLUE)→ Running all person-service tests...$(NC)"
	@cd person-service && ./gradlew test
	@echo "$(GREEN)✓ All tests completed$(NC)"

##@ Cleanup

clean: stop ## Stop services and remove containers
	@echo "$(YELLOW)→ Cleaning up containers...$(NC)"
	@$(DOCKER_COMPOSE) rm -f
	@rm -rf ./person-service/build
	@echo "$(GREEN)✓ Cleanup completed$(NC)"

deep-clean: clean ## Deep clean: remove containers, volumes, and build artifacts
	@echo "$(RED)→ Deep cleaning (removing volumes)...$(NC)"
	@docker volume rm payment-system_nexus-data 2>/dev/null || true
	@docker volume rm payment-system_tempo_data 2>/dev/null || true
	@docker volume ls -qf dangling=true | xargs -r docker volume rm 2>/dev/null || true
	@echo "$(GREEN)✓ Deep cleanup completed$(NC)"

rebuild: clean all ## Clean and rebuild everything

##@ Quick Access URLs

urls: ## Show service URLs
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@echo "$(GREEN)Service URLs$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"
	@echo "$(YELLOW)Nexus:$(NC)              http://localhost:8082 (admin/admin)"
	@echo "$(YELLOW)Keycloak:$(NC)           http://localhost:8080 (admin/admin)"
	@echo "$(YELLOW)Person Service:$(NC)     http://localhost:8092"
	@echo "$(YELLOW)Individuals API:$(NC)    http://localhost:8081"
	@echo "$(YELLOW)Grafana:$(NC)            http://localhost:3000 (admin/admin)"
	@echo "$(YELLOW)Prometheus:$(NC)         http://localhost:9090"
	@echo "$(YELLOW)Tempo:$(NC)              http://localhost:3200"
	@echo "$(YELLOW)Loki:$(NC)               http://localhost:3100"
	@echo "$(BLUE)════════════════════════════════════════════════════════════════$(NC)"