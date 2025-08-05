.PHONY: all build test clean run-cli

all: build

build:
	@echo "Building CII Messaging System..."
	@mvn clean install

test:
	@echo "Running tests..."
	@mvn test

clean:
	@echo "Cleaning..."
	@mvn clean
	@rm -rf dist/

run-cli:
	@java -jar dist/cii-cli.jar $(ARGS)

generate-docs:
	@echo "Generating JavaDoc..."
	@mvn javadoc:aggregate
