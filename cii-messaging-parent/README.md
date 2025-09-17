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

#### Fonctions de lecture, d'écriture et de validation

- **Lecture** : utilisez `CIIReaderFactory` pour obtenir dynamiquement le reader approprié à un
  fichier XML ou instanciez directement `OrderReader`, `InvoiceReader`, etc. Chaque reader retourne
  un objet métier fortement typé (`Order`, `Invoice`, `DespatchAdvice`, …) prêt à être manipulé.
- **Écriture** : les writers (`OrderWriter`, `OrderResponseWriter`, `DesadvWriter`, `InvoiceWriter`)
  convertissent vos objets Java vers un XML conforme. Les utilitaires `OrderGenerator`,
  `DesadvGenerator` et `InvoiceGenerator` offrent une façade pratique autour des writers lorsque vos
  objets métier implémentent respectivement `ObjetCommande`, `ObjetDesadv` ou `ObjetInvoice`.
- **Validation** : `XmlValidator.validerFichierXML(xml, xsd)` assure la conformité d'un document
  vis-à-vis d'un schéma XSD et produit un message synthétique listant les erreurs détectées.
  Pour des validations multi-règles (XSD + règles métier), combinez `XmlValidator` avec les
  implémentations de `CIIValidator` fournies par le module `cii-validator`.

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

### Lecture d'un message via la CLI

```bash
mvn -pl cii-cli -am package
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse \
  cii-samples/src/main/resources/samples/order-sample.xml

java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse \
  cii-samples/src/main/resources/samples/invoice-sample.xml --format JSON
```

### Validation rapide via la CLI

```bash
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate \
  cii-samples/src/main/resources/samples/order-sample.xml

java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate \
  cii-samples/src/main/resources/samples/invoice-sample.xml
```

### Commandes Java prêtes à l'emploi

Compilez au préalable les modules nécessaires (une seule fois) :

```bash
mvn -pl cii-reader,cii-writer,cii-cli -am package
```

#### Lire un ORDER

```bash
java -cp cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  com.cii.messaging.cli.CIIMessagingCLI parse \
  cii-samples/src/main/resources/samples/order-sample.xml
```

#### Générer un ORDERS

```bash
java --source 21 --class-path "cii-reader/target/classes:cii-writer/target/classes:cii-model/target/classes" - <<'EOF' \
  cii-samples/src/main/resources/samples/order-sample.xml \
  target/orders-from-cli.xml
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.writer.generation.ObjetCommande;
import com.cii.messaging.writer.generation.OrderGenerator;
import java.nio.file.Path;

public class GenerateOrdersCli {
    public static void main(String[] args) throws Exception {
        ObjetCommande commande = () -> new OrderReader().read(Path.of(args[0]).toFile());
        System.out.println(OrderGenerator.genererOrders(commande, args[1]));
    }
}
EOF
```

#### Générer un DESADV

```bash
java --source 21 --class-path "cii-writer/target/classes:cii-model/target/classes" - <<'EOF' \
  target/desadv-from-cli.xml
import com.cii.messaging.model.despatchadvice.DespatchAdvice;
import com.cii.messaging.unece.despatchadvice.*;
import com.cii.messaging.writer.generation.DesadvGenerator;
import com.cii.messaging.writer.generation.ObjetDesadv;
import java.math.BigDecimal;

public class GenerateDesadvCli {
    public static void main(String[] args) throws Exception {
        ObjetDesadv desadv = () -> {
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
            docId.setValue("DES-2024-CLI");
            doc.setID(docId);
            DocumentCodeType docType = new DocumentCodeType();
            docType.setValue("351");
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

            LineTradeDeliveryType delivery = new LineTradeDeliveryType();
            QuantityType qty = new QuantityType();
            qty.setUnitCode("EA");
            qty.setValue(new BigDecimal("10"));
            delivery.setRequestedQuantity(qty);
            line.setSpecifiedLineTradeDelivery(delivery);

            tx.getIncludedSupplyChainTradeLineItem().add(line);
            advice.setSupplyChainTradeTransaction(tx);

            return advice;
        };

        System.out.println(DesadvGenerator.genererDesadv(desadv, args[0]));
    }
}
EOF
```

#### Générer une INVOICE

```bash
java --source 21 --class-path "cii-reader/target/classes:cii-writer/target/classes:cii-model/target/classes" - <<'EOF' \
  cii-samples/src/main/resources/samples/invoice-sample.xml \
  target/invoice-from-cli.xml
import com.cii.messaging.reader.InvoiceReader;
import com.cii.messaging.writer.generation.InvoiceGenerator;
import com.cii.messaging.writer.generation.ObjetInvoice;
import java.nio.file.Path;

public class GenerateInvoiceCli {
    public static void main(String[] args) throws Exception {
        ObjetInvoice invoice = () -> new InvoiceReader().read(Path.of(args[0]).toFile());
        System.out.println(InvoiceGenerator.genererInvoice(invoice, args[1]));
    }
}
EOF
```

#### Valider un fichier XML

```bash
java -cp cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  com.cii.messaging.cli.CIIMessagingCLI validate target/orders-from-cli.xml
```

## 📑 Schémas XSD

Les schémas officiels **UN/CEFACT** sont embarqués dans le module `cii-model` pour chaque version
prise en charge (`D23B`, `D24A`, …). Ils se trouvent sous `cii-model/src/main/resources/xsd/<VERSION>/`
et sont chargés automatiquement par `UneceSchemaLoader`.

Chaque schéma garantit la structure XML des flux suivants :

- `CrossIndustryOrder.xsd` : commandes **ORDER/ORDERS** ;
- `CrossIndustryOrderResponse.xsd` : réponses de commande **ORDER_RESPONSE** ;
- `CrossIndustryDespatchAdvice.xsd` : avis d'expédition **DESADV** ;
- `CrossIndustryInvoice.xsd` : factures **INVOICE**.

Les writers s'appuient sur ces XSD pour générer des documents conformes et `XmlValidator`
les utilise pour contrôler vos fichiers. Si vous ajoutez une nouvelle version, déposez les XSD dans
le dossier correspondant puis indiquez la propriété Maven `-Dunece.version=<VERSION>` lors de la
construction. Les schémas les plus récents sont disponibles sur le site de l'UNECE :
<https://unece.org/trade/uncefact/mainstandards>.

## 🧪 Tests unitaires avec Maven

Exécutez l'ensemble de la suite via la commande standard :

```bash
mvn test
```

Quelques variantes utiles :

- Lancer les tests d'un module précis : `mvn -pl cii-reader test` ;
- Cibler une classe de test : `mvn -Dtest=OrderReaderTest -pl cii-reader test` ;
- Exécuter les tests tout en reconstruisant les artefacts : `mvn clean verify`.

Tous les modules s'appuient sur **Surefire**. Veillez à avoir Java 21 dans votre environnement avant
de lancer les tests.

## 📚 Ressources utiles

- [UN/CEFACT](https://unece.org/trade/uncefact)
- [ZUGFeRD](https://www.zugferd.org/)
- [EN 16931](https://www.en16931.eu/)

## 🤝 Contribution

Les contributions sont les bienvenues ! Forkez le dépôt et ouvrez une Pull Request.

## 📄 Licence

Projet distribué sous licence MIT. Voir le fichier `LICENSE`.
