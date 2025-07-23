.PHONY: build run e2e_test integration_test dev clean

# Build application
build:
	docker-compose build

# Run application
run:
	docker-compose --profile app up

# Run end-to-end tests
e2e_test:
	docker-compose --profile e2e up --abort-on-container-exit

# Run integration tests (using the dev container)
integration_test:
	docker-compose --profile dev up -d
	docker-compose exec dev mvn test -Dgroups=it
	docker-compose --profile dev down

# Run all tests
test:
	docker-compose --profile dev up -d
	docker-compose exec dev mvn test
	docker-compose --profile dev down

# Clean up
clean:
	docker-compose down --remove-orphans
	docker-compose --profile app down --remove-orphans
	docker-compose --profile dev down --remove-orphans
	docker-compose --profile e2e down --remove-orphans
	mvn clean