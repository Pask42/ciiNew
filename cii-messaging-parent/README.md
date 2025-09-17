# CII Messaging System

Système Java 21 modulaire pour la lecture, l'écriture et la validation de messages **UN/CEFACT Cross Industry**.
Il couvre les flux ORDER, ORDER_RESPONSE, DESPATCH_ADVICE (DESADV) et INVOICE et reste compatible avec ZUGFeRD, XRechnung et Factur-X.

## 📦 Modules

| Module | Rôle principal |
|--------|----------------|
| `cii-model` | Modèles de données (POJO) et schémas XSD embarqués |
| `cii-reader` | Parsing XML → objets Java |
| `cii-writer` | Génération Java → XML |
| `cii-validator` | Validation XSD et règles métiers |
| `cii-cli` | Interface en ligne de commande |
| `cii-samples` | Fichiers XML d'exemple |

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

Les modules `cii-reader` et `cii-writer` exposent des *readers* / *writers* JAXB pour chaque
flux. Les modèles (`Order`, `DespatchAdvice`, `Invoice`, …) proviennent de `cii-model` et sont
alimentés par les schémas UN/CEFACT embarqués. Les fichiers d'exemple se trouvent dans
`cii-samples/src/main/resources/samples/` et constituent une excellente base pour vos tests.

#### Lire un ORDER depuis un fichier

```java
import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.OrderReader;
import java.nio.file.Path;

Path orderXml = Path.of("cii-samples/src/main/resources/samples/order-sample.xml");
Order order = new OrderReader().read(orderXml.toFile());
```

#### Modifier et réécrire un ORDER

Les types simples (ID, texte, code, montant, quantité…) proposent des accesseurs `setValue`,
`setUnitCode`, `setCurrencyID`, etc. Il suffit de mettre à jour les nœuds métiers puis de
marshaller via le writer dédié.

```java
import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.writer.OrderWriter;
import com.cii.messaging.unece.order.HeaderTradeAgreementType;
import com.cii.messaging.unece.order.LineTradeDeliveryType;
import com.cii.messaging.unece.order.QuantityType;
import com.cii.messaging.unece.order.SupplyChainTradeLineItemType;
import java.math.BigDecimal;
import java.nio.file.Path;

Order order = new OrderReader()
        .read(Path.of("cii-samples/src/main/resources/samples/order-sample.xml").toFile());

order.getExchangedDocument().getID().setValue("ORD-2024-002");
order.getExchangedDocument().getIssueDateTime().getDateTimeString().setValue("20240215103000");
order.getExchangedDocument().getIssueDateTime().getDateTimeString().setFormat("102");

HeaderTradeAgreementType agreement = order.getSupplyChainTradeTransaction()
        .getApplicableHeaderTradeAgreement();
agreement.getBuyerReference().setValue("BUY-REF-2024-002");
agreement.getSellerTradeParty().getName().setValue("Seller Company GmbH (mise à jour)");

SupplyChainTradeLineItemType firstLine = order.getSupplyChainTradeTransaction()
        .getIncludedSupplyChainTradeLineItem().get(0);
firstLine.getSpecifiedTradeProduct().getName().get(0)
        .setValue("Industrial Widget Type A+");

LineTradeDeliveryType delivery = firstLine.getSpecifiedLineTradeDelivery();
QuantityType requested = delivery.getRequestedQuantity();
requested.setValue(new BigDecimal("120"));
requested.setUnitCode("EA");

new OrderWriter().write(order, Path.of("target/order-generated.xml").toFile());
```

#### Générer un avis d'expédition (DESADV)

Pour créer un message d'expédition complet, instanciez un `DespatchAdvice`, remplissez les
agrégats requis (contexte, document échangé, transaction et lignes) puis sérialisez avec
`DesadvWriter`.

```java
import com.cii.messaging.model.despatchadvice.DespatchAdvice;
import com.cii.messaging.writer.DesadvWriter;
import com.cii.messaging.unece.despatchadvice.*;
import java.math.BigDecimal;
import java.nio.file.Path;

DespatchAdvice advice = new DespatchAdvice();

ExchangedDocumentContextType ctx = new ExchangedDocumentContextType();
DocumentContextParameterType guideline = new DocumentContextParameterType();
IDType guidelineId = new IDType();
guidelineId.setValue("urn:factur-x:despatchadvice:1p0");
guideline.setID(guidelineId);
ctx.getGuidelineSpecifiedDocumentContextParameter().add(guideline);
advice.setExchangedDocumentContext(ctx);

ExchangedDocumentType doc = new ExchangedDocumentType();
IDType docId = new IDType();
docId.setValue("DES-2024-001");
doc.setID(docId);
DocumentCodeType docType = new DocumentCodeType();
docType.setValue("351"); // Code DESADV (UNCL1001)
doc.setTypeCode(docType);
DateTimeType issue = new DateTimeType();
DateTimeType.DateTimeString issueString = new DateTimeType.DateTimeString();
issueString.setFormat("102");
issueString.setValue("20240215120000");
issue.setDateTimeString(issueString);
doc.setIssueDateTime(issue);
advice.setExchangedDocument(doc);

SupplyChainTradeTransactionType tx = new SupplyChainTradeTransactionType();
SupplyChainTradeLineItemType line = new SupplyChainTradeLineItemType();
DocumentLineDocumentType lineDoc = new DocumentLineDocumentType();
IDType lineId = new IDType();
lineId.setValue("1");
lineDoc.setLineID(lineId);
line.setAssociatedDocumentLineDocument(lineDoc);

TradeProductType product = new TradeProductType();
TextType productName = new TextType();
productName.setValue("Palette A");
product.getName().add(productName);
line.setSpecifiedTradeProduct(product);

LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
QuantityType qty = new QuantityType();
qty.setUnitCode("EA");
qty.setValue(new BigDecimal("10"));
lineDelivery.setRequestedQuantity(qty);
line.setSpecifiedLineTradeDelivery(lineDelivery);

tx.getIncludedSupplyChainTradeLineItem().add(line);
advice.setSupplyChainTradeTransaction(tx);

new DesadvWriter().write(advice, Path.of("target/desadv-generated.xml").toFile());
```

#### Mettre à jour une facture (INVOICE)

Vous pouvez appliquer la même logique aux factures : charger le modèle depuis le fichier,
modifier les montants ou libellés puis écrire la sortie XML.

```java
import com.cii.messaging.model.invoice.Invoice;
import com.cii.messaging.reader.InvoiceReader;
import com.cii.messaging.writer.InvoiceWriter;
import com.cii.messaging.unece.invoice.AmountType;
import com.cii.messaging.unece.invoice.SupplyChainTradeLineItemType;
import java.math.BigDecimal;
import java.nio.file.Path;

Invoice invoice = new InvoiceReader()
        .read(Path.of("cii-samples/src/main/resources/samples/invoice-sample.xml").toFile());

invoice.getExchangedDocument().getID().setValue("INV-2024-002");

AmountType due = invoice.getSupplyChainTradeTransaction()
        .getApplicableHeaderTradeSettlement()
        .getSpecifiedTradeSettlementHeaderMonetarySummation()
        .getDuePayableAmount();
due.setValue(new BigDecimal("19000.00"));
due.setCurrencyID("EUR");

SupplyChainTradeLineItemType invoiceLine = invoice.getSupplyChainTradeTransaction()
        .getIncludedSupplyChainTradeLineItem().get(0);
invoiceLine.getSpecifiedTradeProduct().getName().get(0)
        .setValue("Industrial Widget Type A (version facturée)");

new InvoiceWriter().write(invoice, Path.of("target/invoice-generated.xml").toFile());
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
  <artifactId>cii-validator</artifactId>
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

```bash
# Valider une commande
java -jar cii-cli.jar validate cii-samples/src/main/resources/samples/order-sample.xml

# Valider une facture
java -jar cii-cli.jar validate cii-samples/src/main/resources/samples/invoice-sample.xml
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
