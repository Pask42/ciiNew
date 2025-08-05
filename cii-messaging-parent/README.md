# CII Messaging System

Un système Java 17 professionnel et modulaire pour la gestion des messages CII (Cross Industry Invoice) conformes à la norme UN/CEFACT. Compatible avec ZUGFeRD, XRechnung et Factur-X.

## 🚀 Fonctionnalités principales

- **Génération de messages XML** : INVOICE, DESADV (avis d'expédition), ORDERSP (réponse à commande)
- **Parsing de messages** : Lecture et extraction de données des messages ORDER au format CII
- **Validation complète** :
  - Validation XSD contre les schémas officiels (D16B, D20B)
  - Validation des règles métier (Schematron, EN 16931)
- **Conversion de formats** : XML ↔ JSON pour faciliter l'intégration
- **Interface en ligne de commande** (CLI) pour une utilisation simple

## 📋 Prérequis

- Java 17 ou supérieur
- Maven 3.6 ou supérieur

## 🛠️ Installation

```bash
# Cloner le repository
git clone https://github.com/votre-repo/cii-messaging.git
cd cii-messaging

# Compiler le projet
mvn clean install

# Le JAR exécutable se trouve dans cii-cli/target/
cd cii-cli/target
```

## 🔧 Structure du projet

```
cii-messaging/
├── cii-model/      # Modèles de données (POJOs)
├── cii-reader/     # Parsing XML → Java
├── cii-writer/     # Génération Java → XML
├── cii-validator/  # Validation XSD & règles métier
├── cii-service/    # Logique d'orchestration
├── cii-cli/        # Interface ligne de commande
└── cii-samples/    # Exemples et tests d'intégration
```

## 💻 Utilisation de la CLI

### Aide générale

```bash
java -jar cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar --help
```

### Générer un message

#### Générer une facture (INVOICE) à partir d'une commande

```bash
java -jar cii-cli.jar generate INVOICE \
  --from-order order.xml \
  --output invoice.xml
```

#### Générer un avis d'expédition (DESADV)

```bash
java -jar cii-cli.jar generate DESADV \
  --from-order order.xml \
  --output desadv.xml
```

#### Générer une réponse à commande (ORDERSP)

```bash
java -jar cii-cli.jar generate ORDERSP \
  --from-order order.xml \
  --output ordersp.xml
```

#### Générer un message exemple

```bash
java -jar cii-cli.jar generate INVOICE \
  --output sample-invoice.xml \
  --sender MYCOMPANY001 \
  --receiver CUSTOMER001
```

### Parser un message

#### Afficher le résumé d'un message

```bash
java -jar cii-cli.jar parse order.xml
```

#### Extraire en JSON

```bash
java -jar cii-cli.jar parse order.xml \
  --format JSON \
  --output order.json
```

### Valider des messages

#### Validation simple

```bash
java -jar cii-cli.jar validate invoice.xml
```

#### Validation détaillée de plusieurs fichiers

```bash
java -jar cii-cli.jar validate *.xml --verbose
```

#### Validation avec un schéma spécifique

```bash
java -jar cii-cli.jar validate invoice.xml --schema D20B
```

### Convertir entre formats

#### XML vers JSON

```bash
java -jar cii-cli.jar convert invoice.xml \
  --to JSON \
  --output invoice.json
```

#### JSON vers XML

```bash
java -jar cii-cli.jar convert invoice.json \
  --to XML \
  --type INVOICE \
  --output invoice.xml
```

## 🔌 Utilisation programmatique

### Exemple simple

```java
import com.cii.messaging.service.*;
import com.cii.messaging.service.impl.*;
import com.cii.messaging.model.*;

public class Example {
    public static void main(String[] args) throws Exception {
        CIIMessagingService service = new CIIMessagingServiceImpl();
        
        // Lire une commande
        CIIMessage order = service.readMessage(new File("order.xml"));
        
        // Créer une facture à partir de la commande
        CIIMessage invoice = service.createInvoiceResponse(order);
        
        // Valider la facture
        ValidationResult result = service.validateMessage(invoice);
        if (result.isValid()) {
            // Écrire la facture
            service.writeMessage(invoice, new File("invoice.xml"));
        }
    }
}
```

### Création manuelle d'une facture

```java
CIIMessage invoice = CIIMessage.builder()
    .messageId("INV-2024-001")
    .messageType(MessageType.INVOICE)
    .creationDateTime(LocalDateTime.now())
    .senderPartyId("SELLER001")
    .receiverPartyId("BUYER001")
    .header(DocumentHeader.builder()
        .documentNumber("INV-2024-001")
        .documentDate(LocalDate.now())
        .currency("EUR")
        .build())
    .lineItems(Arrays.asList(
        LineItem.builder()
            .lineNumber("1")
            .productId("PROD001")
            .description("Widget Type A")
            .quantity(new BigDecimal("10"))
            .unitCode("EA")
            .unitPrice(new BigDecimal("100.00"))
            .lineAmount(new BigDecimal("1000.00"))
            .taxRate(new BigDecimal("20"))
            .build()
    ))
    .totals(TotalsInformation.builder()
        .lineTotalAmount(new BigDecimal("1000.00"))
        .taxTotalAmount(new BigDecimal("200.00"))
        .grandTotalAmount(new BigDecimal("1200.00"))
        .build())
    .build();
```

## 🧪 Tests

```bash
# Exécuter tous les tests
mvn test

# Tests d'intégration uniquement
mvn test -Dtest=CIIIntegrationTest

# Tests avec couverture
mvn test jacoco:report
```

## 📝 Exemples de messages

Des exemples de messages CII valides sont disponibles dans `cii-samples/src/main/resources/samples/` :

- `order-sample.xml` : Exemple de commande (ORDER)
- `invoice-sample.xml` : Exemple de facture (INVOICE)

## 🏗️ Architecture technique

### Dépendances principales

- **JAXB** : Marshalling/Unmarshalling XML
- **Mustang Project** : Support ZUGFeRD/CII
- **ph-cii** : Conversion CII ↔ UBL et validation
- **Saxon HE** : Validation Schematron
- **Picocli** : Interface CLI
- **Jackson** : Conversion JSON

### Modules Maven

| Module | Description | Dépendances principales |
|--------|-------------|------------------------|
| cii-model | POJOs et énumérations | JAXB, Lombok |
| cii-reader | Parsing XML → Java | Mustang, ph-cii |
| cii-writer | Génération Java → XML | Mustang, ph-cii |
| cii-validator | Validation XSD/Schematron | Xerces, Saxon |
| cii-service | Orchestration métier | Tous les modules |
| cii-cli | Interface ligne de commande | Picocli |

## 🔍 Validation

Le système implémente trois niveaux de validation :

1. **Validation XSD** : Conformité au schéma XML
2. **Validation Schematron** : Règles métier EN 16931
3. **Validation personnalisée** : Règles métier spécifiques

## 🌐 Compatibilité

- **Versions CII supportées** : D16B, D20B, D21B
- **Standards compatibles** :
  - ZUGFeRD 2.x
  - XRechnung
  - Factur-X
  - EN 16931

## 📚 Documentation supplémentaire

- [UN/CEFACT Standards](https://unece.org/trade/uncefact/introducing-uncefact)
- [Cross Industry Invoice](https://www.unece.org/cefact/codesfortrade/cii_index.html)
- [ZUGFeRD](https://www.zugferd.org/)
- [EN 16931](https://www.en16931.eu/)

## 🤝 Contribution

Les contributions sont les bienvenues ! Veuillez :

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## ⚠️ Notes importantes

- Les schémas XSD officiels doivent être placés dans les ressources appropriées
- La validation Schematron nécessite les fichiers XSLT compilés
- Pour une utilisation en production, configurez les logs appropriés via Logback

## 🚧 Roadmap

- [ ] Support API REST
- [ ] Interface web
- [ ] Support des signatures électroniques
- [ ] Intégration avec des ERP
- [ ] Support des pièces jointes PDF (ZUGFeRD hybrid)
- [ ] Validation étendue pour XRechnung
