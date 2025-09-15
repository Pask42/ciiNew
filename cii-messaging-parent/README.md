# CII Messaging System

Système Java 21 modulaire pour la lecture, l'écriture et la validation de messages **UN/CEFACT Cross Industry**.
Il couvre les flux ORDER, ORDERSP, DESADV et INVOICE et reste compatible avec ZUGFeRD, XRechnung et Factur-X.

## 📦 Modules

| Module | Rôle principal |
|--------|----------------|
| `cii-model` | Modèles de données (POJO) et schémas XSD embarqués |
| `cii-reader` | Parsing XML → objets Java |
| `cii-writer` | Génération Java → XML |
| `cii-validator` | Validation XSD et règles métiers |

## ✅ Prérequis

- Java 21+
- Maven 3.6+

## 🔨 Compilation

```bash
# Cloner le projet
git clone <url-du-repo>/cii-messaging-parent.git
cd cii-messaging-parent

# Construire tous les modules et lancer les tests
mvn clean install
```

### Choix de la version UN/CEFACT

Les schémas XSD sont chargés depuis `src/main/resources/xsd/<version>/...`.
La version est déterminée par le paramètre `unece.version` (par défaut `D23B`).

Exemples :

```bash
# Utiliser la version par défaut (D23B)
mvn clean install

# Forcer la version D24A
mvn -Dunece.version=D24A clean install

# Ou via variable d'environnement
UNECE_VERSION=D24A mvn clean install
```

Pour ajouter une nouvelle version (ex. `D24A`), déposez simplement les XSD dans
`cii-model/src/main/resources/xsd/D24A/uncefact/data/standard/` puis construisez avec
`-Dunece.version=D24A`.
Les XSD officiels sont disponibles sur [le site de l'UNECE](https://unece.org/trade/uncefact/xml-schemas).

### Utilisation programmatique

```java
InvoiceReader reader = new InvoiceReader();
Invoice invoice = reader.read(new File("invoice.xml"));

InvoiceWriter writer = new InvoiceWriter();
writer.write(invoice, new File("invoice-out.xml"));
```

### Chargement manuel des schémas

```java
Schema schema = UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd");
// Le chargeur résout automatiquement le suffixe spécifique à la version
```

## 🚀 Déploiement

### Utilisation de la CLI

```bash
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar --help
```

### Utilisation comme bibliothèque Maven

```xml
<dependency>
  <groupId>com.cii.messaging</groupId>
  <artifactId>cii-service</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 🤖 Scripts

### `scripts/build.sh`
Build Maven complet (tests ignorés) et copie du JAR de la CLI dans `dist/cii-cli.jar`.

```bash
./scripts/build.sh
```

### `scripts/run-cli.sh`
Wrapper pour lancer la CLI depuis `dist`. À utiliser après le build.

```bash
./scripts/run-cli.sh --help
```

### `scripts/validate-all.sh`
Valide tous les fichiers XML d'un répertoire via la CLI. Dépend de `dist/cii-cli.jar` généré par le build.

```bash
./scripts/validate-all.sh cii-samples/src/main/resources/samples
```

## 📝 Exemples d'utilisation

### Lecture d'un message

```bash
# ORDER
java -jar cii-cli.jar parse cii-samples/src/main/resources/samples/order-sample.xml

# INVOICE
java -jar cii-cli.jar parse cii-samples/src/main/resources/samples/invoice-sample.xml
```

### Génération de messages avec la CLI

```bash
# Générer une facture (INVOICE) à partir d'une commande
java -jar cii-cli.jar generate INVOICE \
  --from-order cii-samples/src/main/resources/samples/order-sample.xml \
  --output invoice.xml

# Générer un avis d'expédition (DESADV)
java -jar cii-cli.jar generate DESADV \
  --from-order cii-samples/src/main/resources/samples/order-sample.xml \
  --output desadv.xml

# Générer une réponse à commande (ORDERSP)
java -jar cii-cli.jar generate ORDERSP \
  --from-order cii-samples/src/main/resources/samples/order-sample.xml \
  --output ordersp.xml
```

## 📑 Schémas XSD

Les schémas nécessaires se trouvent dans `cii-model/src/main/resources/xsd/VERSION/` :

- `CrossIndustryOrder.xsd`
- `CrossIndustryOrderResponse.xsd`
- `CrossIndustryDespatchAdvice.xsd`
- `CrossIndustryInvoice.xsd`

Ils proviennent des publications officielles **UN/CEFACT** : <https://unece.org/trade/uncefact/mainstandards>

## 🧪 Tests

```bash
mvn test
```

## 📚 Ressources utiles

- [UN/CEFACT](https://unece.org/trade/uncefact)
- [ZUGFeRD](https://www.zugferd.org/)
- [EN 16931](https://www.en16931.eu/)

## 🤝 Contribution

Les contributions sont les bienvenues ! Forkez le dépôt et ouvrez une Pull Request.

## 📄 Licence

Projet distribué sous licence MIT. Voir le fichier `LICENSE`.
