.PHONY: all build test clean run-cli

all: build

build:
    @echo "Construction du système de messagerie CII..."
	@mvn clean install

test:
    @echo "Exécution des tests..."
	@mvn test

clean:
    @echo "Nettoyage..."
	@mvn clean
	@rm -rf dist/

run-cli:
	@java -jar dist/cii-cli.jar $(ARGS)

generate-docs:
    @echo "Génération de la JavaDoc..."
	@mvn javadoc:aggregate
