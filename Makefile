.PHONY: all build test up down restart logs status clean help

# Default target
all: build

# ── Maven Targets ──
build:
	mvn clean package -DskipTests

test:
	mvn clean verify

clean:
	mvn clean

# ── Docker Compose Infrastructure Targets ──
up:
	docker compose up -d

down:
	docker compose down -v

restart:
	docker compose restart

logs:
	docker compose logs -f

status:
	docker compose ps

help:
	@echo "DisasterReliefNet Build and Orchestration Tooling"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build    - Compile all modules and produce executable jars (skips tests)"
	@echo "  test     - Run surefire unit tests, failsafe integration tests, and JaCoCo coverage"
	@echo "  up       - Spin up all backing infrastructure (PostgreSQL, Kafka KRaft, Redis, Keycloak, Prometheus, Grafana)"
	@echo "  down     - Tear down all infrastructure containers and clean volumes"
	@echo "  logs     - Stream live docker logs from all containers"
	@echo "  status   - Show status of infrastructure services"
	@echo "  clean    - Remove target build artifacts"
