# CII Messaging System

Système Java 17 modulaire pour la lecture, l'écriture et la validation de messages **UN/CEFACT Cross Industry**.
Il couvre les flux ORDER, ORDERSP, DESADV et INVOICE et reste compatible avec ZUGFeRD, XRechnung et Factur-X.

## 📦 Modules

| Module | Rôle principal |
|--------|----------------|
| `cii-model` | Modèles de données (POJO) et schémas XSD embarqués |
| `cii-reader` | Parsing XML → objets Java |
| `cii-writer` | Génération Java → XML (INVOICE, DESADV, ORDERSP) |
| `cii-validator` | Validation XSD et règles métiers |
| `cii-service` | Orchestration et API de haut niveau |
| `cii-cli` | Interface en ligne de commande |
| `cii-samples` | Messages d'exemple et tests d'intégration |

## ✅ Prérequis

- Java 17+
- Maven 3.6+

## 🔨 Compilation

```bash
# Cloner le projet
git clone <url-du-repo>/cii-messaging-parent.git
cd cii-messaging-parent

# Construire tous les modules et lancer les tests
mvn clean install
```

Le JAR exécutable de la CLI est ensuite disponible dans `cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar`.
Pour ne construire que la CLI :

```bash
mvn -pl cii-cli -am package
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

> ⚠️ L'écriture de messages **ORDER** n'est pas encore implémentée dans `cii-writer`. Les lectures ORDER sont supportées et la génération pourra être ajoutée en développant un `OrderWriter`.

### Utilisation programmatique

```java
CIIMessagingService service = new CIIMessagingServiceImpl();

// Lecture
CIIMessage order = service.readMessage(new File("order.xml"));

// Génération d'une facture en réponse
CIIMessage invoice = service.createInvoiceResponse(order);
service.writeMessage(invoice, new File("invoice.xml"));

// Génération d'un avis d'expédition
CIIMessage desadv = service.createDespatchAdvice(order);
service.writeMessage(desadv, new File("desadv.xml"));

// Génération d'une réponse à commande
CIIMessage ordersp = service.createOrderResponse(order, OrderResponseType.ACCEPTED);
service.writeMessage(ordersp, new File("ordersp.xml"));
```

## 🔍 Validation

```bash
# Validation simple
java -jar cii-cli.jar validate invoice.xml

# Validation détaillée sur plusieurs fichiers
java -jar cii-cli.jar validate *.xml --verbose
```

## 📑 Schémas XSD

Les schémas nécessaires se trouvent dans `cii-model/src/main/resources/xsd/uncefact/data/standard/` :

- `CrossIndustryOrder_12p1.xsd`
- `CrossIndustryOrderResponse_12p1.xsd`
- `CrossIndustryDespatchAdvice_12p1.xsd`
- `CrossIndustryInvoice_13p1.xsd`
- `ReusableAggregateBusinessInformationEntity_20p0.xsd`
- `QualifiedDataType_20p0.xsd`
- `UnqualifiedDataType_20p0.xsd`

Ils proviennent des publications officielles **UN/CEFACT** : <https://service.unece.org/trade/uncefact/v1/>

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
