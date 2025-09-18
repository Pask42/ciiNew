# Système de messagerie CII

Boîte à outils modulaire en Java 21 pour lire, écrire et valider les messages **UN/CEFACT Cross Industry**.
Elle couvre les flux ORDER, ORDER_RESPONSE (ORDERSP), DESPATCH_ADVICE (DESADV) et INVOICE tout en restant
compatible avec les profils ZUGFeRD, XRechnung et Factur-X.

## 📦 Modules

| Module | Rôle principal |
|--------|----------------|
| `cii-model` | Modèles de données (POJOs) et schémas XSD UNECE embarqués |
| `cii-reader` | Conversion d'un XML vers des objets Java fortement typés |
| `cii-writer` | Génération d'objets Java vers XML |
| `cii-validator` | Validation XSD et règles métier |
| `cii-cli` | Outils en ligne de commande |
| `cii-samples` | Charges utiles XML d'exemple |

## ✅ Prérequis techniques

- Java 21 ou supérieur
- Maven 3.6 ou supérieur
- Pour exécuter la CLI, assurez-vous que `$JAVA_HOME` est défini et que l'exécutable `java` est accessible dans le `PATH`

## 🔨 Compilation et tests

```bash
# Cloner le projet
git clone <repository-url>/cii-messaging-parent.git
cd cii-messaging-parent

# Construire tous les modules et lancer l'ensemble des tests
mvn clean install
```

### Compiler uniquement le module CLI

```bash
mvn -pl cii-cli -am clean package
```

Le build de la CLI produit deux artefacts dans `cii-cli/target/` :

- `cii-cli-<version>.jar` : archive fine qui s'appuie sur la résolution des dépendances Maven
- `cii-cli-<version>-jar-with-dependencies.jar` : jar exécutable contenant toutes les dépendances

Lancer la CLI directement via l'archive d'assemblage :

```bash
java -jar cii-cli/target/cii-cli-<version>-jar-with-dependencies.jar --help
```

Ou l'exécuter avec Maven sans générer l'archive :

```bash
mvn -pl cii-cli -am exec:java -Dexec.args="--help"
```

### Exécuter les tests

```bash
mvn -pl cii-cli test
```

## 🌐 Sélection de la version de schéma UNECE

Les schémas sont chargés depuis `src/main/resources/xsd/<version>/...` et pilotés par la propriété
`unece.version` (valeur par défaut : `D23B`).

```bash
# Utiliser la version par défaut (D23B)
mvn clean install

# Forcer la version D24A
mvn -Dunece.version=D24A clean install

# Ou la définir via une variable d'environnement
UNECE_VERSION=D24A mvn clean install
```

La même propriété contrôle le schéma par défaut utilisé par la commande de validation de la CLI. Vous pouvez
toujours la surcharger à l'exécution avec l'option `--schema-version`.

## 🛠️ Référence de la CLI (`cii-cli`)

### Options globales

| Option | Description |
|--------|-------------|
| `-l, --log-level <LEVEL>` | Remplace le niveau racine Logback (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`) |
| `-c, --config <FILE>` | Charge les options depuis un fichier properties contenant une entrée `log.level` |

Si aucune option n'est fournie, la CLI recherche `cii-cli.properties` dans le répertoire courant, puis sur le
classpath. Un fichier de configuration minimal ressemble à :

```properties
# cii-cli.properties
log.level=DEBUG
```

### Commande `parse`

Analyse un message CII et le restitue sous forme de synthèse lisible ou de JSON complet.

| Paramètre ou option | Description | Défaut |
|---------------------|-------------|--------|
| `INPUT` (paramètre) | Chemin du fichier XML à analyser | — |
| `-o, --output <FILE>` | Chemin de sortie optionnel. S'il est omis, la synthèse est envoyée sur STDOUT | — |
| `--format <FORMAT>` | Format de sortie : `SUMMARY` (synthèse lisible) ou `JSON` (payload complet) | `SUMMARY` |

Pour les messages ORDER, la synthèse exploite `OrderAnalyzer` et expose les informations métier essentielles
(identifiant, parties, dates, lignes). Les autres types de message annoncent la classe détectée.

### Commande `validate`

Valide un document CII contre les schémas UNECE et les règles métier.

| Paramètre ou option | Description | Défaut |
|---------------------|-------------|--------|
| `INPUT` (paramètre) | Chemin du fichier XML à valider | — |
| `--schema-version <VERSION>` | Version UNECE explicite (`D23B`, `D24A`, …) | `SchemaVersion.getDefault()` (propriété système `unece.version`, puis `UNECE_VERSION`, sinon `D23B`) |
| `--fail-on-warning` | Considère les avertissements comme des erreurs (code retour non nul) | Désactivé |

Le validateur affiche un résumé concis (validité, nombre d'erreurs, schéma utilisé, temps d'exécution) et liste
chaque erreur et avertissement.

## 🧪 Exemples en ligne de commande

En supposant que l'archive d'assemblage a été générée (`mvn -pl cii-cli -am clean package`) :

```bash
# Afficher une synthèse du document ORDER fourni
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse cii-samples/src/main/resources/samples/order-sample.xml

# Produire une représentation JSON d'un message ORDER
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --format JSON --output target/order.json cii-samples/src/main/resources/samples/order-sample.xml

# Valider un document avec le schéma par défaut
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-sample.xml

# Valider contre D24A et échouer dès qu'un avertissement survient
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate --schema-version D24A --fail-on-warning cii-samples/src/main/resources/samples/order-valid.xml

# Lancer une analyse avec un niveau de log verbeux défini sur la ligne de commande
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --log-level DEBUG cii-samples/src/main/resources/samples/order-sample.xml

# Fournir le niveau de log via un fichier de configuration
cat <<'PROPS' > cii-cli.properties
log.level=DEBUG
PROPS
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-valid.xml
```

## 💻 Utilisation programmatique

Les modules `cii-reader` et `cii-writer` exposent des lecteurs et rédacteurs JAXB pour chaque flux supporté. Les
modèles (`Order`, `DespatchAdvice`, `Invoice`, …) sont fournis par `cii-model` avec les schémas UNECE.
Des documents XML d'exemple sont disponibles sous `cii-samples/src/main/resources/samples/` et constituent un point de départ pratique pour les tests.

### Utilitaires de lecture, d'écriture et de validation
