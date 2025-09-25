# Système de messagerie CII

Boîte à outils modulaire Java 21 pour lire, écrire et valider les messages **UN/CEFACT Cross Industry**.
Elle couvre les flux ORDER, ORDER_RESPONSE (ORDERSP), DESPATCH_ADVICE (DESADV) et INVOICE tout en restant
compatible avec les profils ZUGFeRD, XRechnung et Factur-X.

## 📦 Modules

| Module | Responsabilité principale |
|--------|---------------------------|
| `cii-model` | Modèles de données (POJO) et schémas XSD UNECE embarqués |
| `cii-reader` | Analyse d’XML vers des objets Java fortement typés |
| `cii-writer` | Génération d’objets Java vers XML |
| `cii-validator` | Validation XSD et règles métier |
| `cii-cli` | Outils en ligne de commande |
| `cii-samples` | Charges utiles XML d’exemple |

## ✅ Prérequis techniques

- Java 21 ou version ultérieure
- Maven 3.6 ou version ultérieure
- Pour exécuter le CLI, vérifiez que `$JAVA_HOME` est défini et que l’exécutable `java` est disponible dans votre `PATH`

## 🔨 Compilation et tests

```bash
# Cloner le projet
git clone <repository-url>/cii-messaging-parent.git
cd cii-messaging-parent

# Construire tous les modules et exécuter l’ensemble de la suite de tests
mvn clean install
```

### Construire uniquement le module CLI

```bash
mvn -pl cii-cli -am clean package
```

La construction du CLI produit deux artefacts dans `cii-cli/target/` :

- `cii-cli-<version>.jar` : JAR fin qui s’appuie sur la résolution des dépendances Maven
- `cii-cli-<version>-jar-with-dependencies.jar` : JAR exécutable embarquant toutes les dépendances

Lancez le CLI directement depuis le JAR assemblé :

```bash
java -jar cii-cli/target/cii-cli-<version>-jar-with-dependencies.jar --help
```

Ou exécutez-le avec Maven sans créer explicitement le JAR :

```bash
mvn -pl cii-cli -am exec:java -Dexec.args="--help"
```

### Exécuter les tests

```bash
mvn -pl cii-cli test
```

## 🌐 Sélection de la version de schéma UNECE

Les schémas sont chargés depuis `src/main/resources/xsd/<version>/...` et pilotés par la propriété
`unece.version` (par défaut `D23B`).

```bash
# Utiliser la version par défaut (D23B)
mvn clean install

# Forcer la version D24A
mvn -Dunece.version=D24A clean install

# Ou configurer via une variable d’environnement
UNECE_VERSION=D24A mvn clean install
```

La même propriété détermine le schéma par défaut utilisé par la commande de validation du CLI. Vous pouvez encore
le surcharger à chaque exécution avec l’option `--schema-version`.

## 🛠️ Référence CLI (`cii-cli`)

### Options globales

| Option | Description |
|--------|-------------|
| `-l, --log-level <LEVEL>` | Redéfinit le niveau Logback racine (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`) |
| `-c, --config <FILE>` | Charge les options depuis un fichier properties contenant une entrée `log.level` |

Si aucune option n’est fournie, le CLI recherche `cii-cli.properties` dans le répertoire courant, puis sur le
classpath. Un fichier de configuration minimal ressemble à ceci :

```properties
# cii-cli.properties
log.level=DEBUG
```

### Commande `parse`

Analyse un message CII et le restitue sous forme de synthèse ou de JSON.

| Paramètre ou option | Description | Valeur par défaut |
|---------------------|-------------|-------------------|
| `INPUT` (paramètre) | Chemin vers le fichier XML à analyser | — |
| `-o, --output <FILE>` | Chemin optionnel où écrire le rendu. Si omis, la synthèse est affichée sur la sortie standard | — |
| `--format <FORMAT>` | Format de sortie : `SUMMARY` (synthèse lisible) ou `JSON` (payload complet) | `SUMMARY` |

Pour les messages ORDER, la synthèse s’appuie sur `OrderAnalyzer` et contient les informations métier
essentielles (identifiant du document, parties, dates et lignes). Les autres types de message signalent la
classe détectée.

### Commande `validate`

Valide un document CII selon les schémas UNECE et les règles métier.

| Paramètre ou option | Description | Valeur par défaut |
|---------------------|-------------|-------------------|
| `INPUT` (paramètre) | Chemin vers le fichier XML à valider | — |
| `--schema-version <VERSION>` | Version UNECE explicite (`D23B`, `D24A`, …) | `SchemaVersion.getDefault()` (propriété système `unece.version`, puis `UNECE_VERSION`, sinon `D23B`) |
| `--fail-on-warning` | Considère les avertissements comme des erreurs fatales (code retour non nul) | Désactivé |

Le validateur affiche un résumé concis (validité, nombre d’erreurs, bundle de schémas utilisé, temps d’exécution)
et liste chaque erreur et avertissement individuellement.

### Commande `respond`

Génère automatiquement un ORDER_RESPONSE (ORDERSP) à partir d’un ORDER existant.

| Paramètre ou option | Description | Valeur par défaut |
|---------------------|-------------|-------------------|
| `INPUT` (paramètre) | Fichier ORDER XML source | — |
| `-o, --output <FILE>` | Fichier ORDER_RESPONSE à produire | `<INPUT>-ordersp.xml` dans le même dossier |
| `--response-id <ID>` | Identifiant explicite du document ORDER_RESPONSE | Préfixe + ID de l’ORDER |
| `--response-id-prefix <PREFIX>` | Préfixe utilisé pour générer l’ID si aucun n’est fourni | `ORDRSP-` |
| `--ack-code <CODE>` | Code d’accusé de réception UNECE (1–51, ex. `29`=Accepté, `42`=Rejeté) | `29` |
| `--issue-date <yyyyMMddHHmmss>` | Date d’émission forcée | Date courante |

La commande lit le message ORDER, reconstruit les entêtes (parties, montants, lignes) et produit un ORDER_RESPONSE
cohérent avec les quantités demandées.

```bash
# Générer une réponse acceptée pour order-sample.xml et l’écrire dans target/order-response.xml
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  respond --ack-code AP --response-id-prefix ORDRSP- \
  --output target/order-response.xml cii-samples/src/main/resources/samples/order-sample.xml
```

### Codes d'accusé de réception UNECE

L’option `--ack-code` exploite les codes **UN/CEFACT Message function** . Pour une lecture plus rapide,
les valeurs sont regroupées par thématique et présentées sous forme de listes courtes. Chaque entrée indique le
code numérique, l’intitulé officiel en français et des conseils d’usage pour un ORDER_RESPONSE (`ORDRSP`).

# 📑 Codes de statut et disponibilité des documents (UNECE / CIOP-CIOR)

## ✅ Acceptation & Validation
- **1 – Accepté**  
  Le document spécifié est accepté.  
- **3 – Accepté sous condition**  
  Le document spécifié est accepté sous certaines conditions.  
- **10 – Document valide**  
  Le document est actuellement valide.  
- **44 – Accepté avec avertissements**  
  Le document est accepté mais a généré des avertissements.  
- **49 – Accepté sous conditions**  
  Le document est accepté avec conditions et poursuivi sauf contestation.  

---

## ❌ Rejet & Invalidité
- **8 – Rejeté**  
  Le document spécifié est rejeté.  
- **11 – Document non disponible**  
  Le document spécifié n’est pas disponible.  
- **16 – Document non joint**  
  Le document n’est pas ou ne peut pas être joint.  
- **35 – Document incomplet**  
  Le document est incomplet.  
- **42 – En erreur**  
  Le document est erroné.  
- **50 – Rejeté, pas de traitement ultérieur**  
  Le document est rejeté, une clarification est nécessaire.  
- **41 – Validité révoquée**  
  La validité du document a été révoquée.  
- **40 – Validité suspendue**  
  La validité du document est suspendue.  

---

## 📦 Transmission & Acheminement
- **2 – Accompagne les marchandises**  
  Le document accompagne physiquement les biens.  
- **4 – Arrivera par message EDI séparé**  
  Le document/message sera transmis via un autre message EDI.  
- **6 – Arrivera par moyens manuels**  
  Le document/message ne sera pas envoyé par EDI.  
- **7 – À établir et envoyer**  
  Demande de génération et transmission du document/message.  
- **9 – À imprimer**  
  Le document doit être imprimé.  
- **17 – Avec les marchandises**  
  Le document n’est pas joint à la déclaration en douane mais aux biens.  
- **18 – Joint, à retourner après visa**  
  Le document est joint mais devra être restitué après validation douane.  
- **19 – Document demandé**  
  Une demande a été déposée pour ce document.  
- **20 – Reçu pour expédition**  
  Document valide à partir de la réception de la cargaison.  
- **21 – Embarqué à bord**  
  Document valide à partir du chargement sur le navire.  
- **30 – Inclus dans la première transmission**  
  Documents joints à la première transmission.  
- **31 – Inclus dans la seconde transmission**  
  Documents joints à la seconde transmission.  
- **32 – Non requis (dispense accordée)**  
  Document non nécessaire, dispense émise.  
- **33 – Déjà en possession du destinataire**  
  Document déjà disponible auprès du destinataire du message.  
- **34 – Conservé par l’expéditeur**  
  Document détenu par l’expéditeur ou son représentant.  
- **36 – Déjà soumis**  
  Document déjà transmis.  
- **37 – Document complet**  
  Le document est complet.  
- **51 – Arrivera par voie électronique**  
  Le document/message sera transmis électroniquement.  

---

## 🏛️ Douanes & Réglementation
- **12 – Document épuisé par la déclaration (joint)**  
  Déclaration en douane ayant consommé le document, lequel est joint.  
- **13 – Document non épuisé (joint)**  
  Déclaration douane n’a pas épuisé le document, déjà déposé en douane.  
- **14 – Document épuisé (préalablement déposé)**  
  Document consommé, non joint mais déjà en douane.  
- **15 – Document non épuisé (préalablement déposé)**  
  Document encore valide pour futures déclarations, déjà en douane.  

---

## ⚙️ Statut du traitement
- **22 – Statut 0**  
  Message au statut 0.  
- **23 – Statut 1**  
  Message au statut 1.  
- **24 – Statut 2**  
  Message au statut 2.  
- **25 – Message en cours de développement**  
  Message encore en élaboration.  
- **26 – Non freté**  
  Document ne contient pas les frais de transport.  
- **27 – Freté**  
  Document inclut les frais de transport.  
- **28 – Archivé**  
  Document/message archivé.  
- **29 – Provisoire**  
  Document/message sans statut officiel.  
- **38 – Finalisé**  
  Document terminé et officialisé.  
- **39 – En attente**  
  Document/message suspendu jusqu’à nouvel ordre.  
- **43 – Reçu**  
  Document reçu.  
- **45 – En cours de traitement**  
  Document en cours de traitement.  
- **46 – En attente de réponse (sous requête)**  
  Document bloqué dans l’attente d’une réponse.  
- **47 – Payé**  
  Document réglé.  
- **48 – Accusé de réception**  
  Document reconnu et soumis pour traitement.  


## 🧪 Exemples en ligne de commande

En supposant que le JAR assemblé a été construit (`mvn -pl cii-cli -am clean package`) :

```bash
# Afficher une synthèse du document ORDER fourni
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse cii-samples/src/main/resources/samples/order-sample.xml

# Produire une représentation JSON d’un message ORDER
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --format JSON --output target/order.json cii-samples/src/main/resources/samples/order-sample.xml

# Valider un document avec le schéma par défaut
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-sample.xml

# Valider avec D24A et échouer immédiatement si des avertissements sont présents
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate --schema-version D24A --fail-on-warning cii-samples/src/main/resources/samples/order-valid.xml

# Lancer une analyse avec un niveau de log verbeux fourni en ligne de commande
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --log-level DEBUG cii-samples/src/main/resources/samples/order-sample.xml

# Fournir le niveau de log via un fichier de configuration
cat <<'PROPS' > cii-cli.properties
log.level=DEBUG
PROPS
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-valid.xml
```

## 💻 Utilisation programmatique

Les modules `cii-reader` et `cii-writer` exposent des lecteurs et écrivains JAXB pour chaque flux supporté. Les
modèles (`Order`, `DespatchAdvice`, `Invoice`, …) sont fournis par `cii-model` avec les schémas UNECE.
Des documents XML d’exemple sont disponibles dans `cii-samples/src/main/resources/samples/` et constituent un
point de départ pratique pour les tests.

### Utilitaires de lecture, d’écriture et de validation

- **Lecture** : utilisez `CIIReaderFactory` pour détecter le lecteur approprié à partir d’un fichier XML, ou
  instanciez directement `OrderReader`, `InvoiceReader` et classes similaires. Chaque lecteur renvoie un objet
  métier fortement typé prêt à être traité.
- **Écriture** : les écrivains (`OrderWriter`, `OrderResponseWriter`, `DesadvWriter`, `InvoiceWriter`) transforment
  vos objets Java en XML conforme aux schémas. Les classes utilitaires `OrderGenerator`, `DesadvGenerator` et
  `InvoiceGenerator` fournissent une façade lorsque vos objets métier implémentent respectivement
  `ObjetCommande`, `ObjetDesadv` ou `ObjetInvoice`.
- **Validation** : `XmlValidator.validerFichierXML(xml, xsd)` vérifie la conformité vis-à-vis d’un schéma XSD et
  renvoie un rapport structuré. Combinez `XmlValidator` avec les implémentations de `CIIValidator` présentes dans
  `cii-validator` pour appliquer des règles métier supplémentaires.

### Lire un ORDER depuis un fichier

```java
import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.OrderReader;
import java.nio.file.Path;

Path orderXml = Path.of("cii-samples/src/main/resources/samples/order-sample.xml");
Order order = new OrderReader().read(orderXml.toFile());
```

### Modifier et réécrire un ORDER

Les types simples (ID, texte, codes, montants, quantités, …) exposent des accesseurs comme `setValue`,
`setUnitCode` et `setCurrencyID`. Mettez à jour les nœuds concernés puis marshaller l’objet via l’écrivain dédié.

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
agreement.getSellerTradeParty().getName().setValue("Seller Company GmbH (updated)");

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

### Générer manuellement des fichiers ORDERS, ORDER_RESPONSE, DESADV et INVOICE

Les utilitaires du module `cii-writer` vous permettent de produire un XML complet à partir de vos objets métier sans
avoir à manipuler directement les API JAXB :

```java
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.generation.DesadvGenerator;
import com.cii.messaging.writer.generation.InvoiceGenerator;
import com.cii.messaging.writer.generation.ObjetCommande;
import com.cii.messaging.writer.generation.ObjetDesadv;
import com.cii.messaging.writer.generation.ObjetInvoice;
import com.cii.messaging.writer.generation.OrderGenerator;

ObjetCommande commande = () -> orderInstanceConstruiteAvecVosDonnées();
String ordreGenere = OrderGenerator.genererOrders(commande, "target/orders-from-domain.xml");

ObjetDesadv avis = () -> desadvInstanceConstruiteAvecVosDonnées();
String desadvGenere = DesadvGenerator.genererDesadv(avis, "target/desadv-from-domain.xml");

ObjetInvoice facture = () -> invoiceInstanceConstruiteAvecVosDonnées();
String factureGeneree = InvoiceGenerator.genererInvoice(facture, "target/invoice-from-domain.xml");
```

Chaque générateur crée les dossiers parents manquants et renvoie un message de confirmation. Pour ORDER_RESPONSE
(`ORDERSP`), instanciez directement `OrderResponseWriter` en construisant l’objet `OrderResponse` correspondant.

### Générer un ORDER_RESPONSE (ORDERSP)

```java
import com.cii.messaging.model.orderresponse.OrderResponse;
import com.cii.messaging.writer.OrderResponseWriter;
import com.cii.messaging.unece.orderresponse.*;
import java.math.BigDecimal;
import java.nio.file.Path;

OrderResponse response = new OrderResponse();

ExchangedDocumentContextType ctx = new ExchangedDocumentContextType();
DocumentContextParameterType guideline = new DocumentContextParameterType();
IDType guidelineId = new IDType();
guidelineId.setValue("urn:factur-x:orderresponse:1p0");
guideline.setID(guidelineId);
ctx.getGuidelineSpecifiedDocumentContextParameter().add(guideline);
response.setExchangedDocumentContext(ctx);

ExchangedDocumentType doc = new ExchangedDocumentType();
IDType docId = new IDType();
docId.setValue("ORDRSP-2024-001");
doc.setID(docId);
DocumentCodeType docType = new DocumentCodeType();
docType.setValue("231"); // Code UNCL1001 pour une réponse de commande
doc.setTypeCode(docType);
DateTimeType issue = new DateTimeType();
DateTimeType.DateTimeString issueString = new DateTimeType.DateTimeString();
issueString.setFormat("102");
issueString.setValue("20240216100000");
issue.setDateTimeString(issueString);
doc.setIssueDateTime(issue);
response.setExchangedDocument(doc);

SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();

HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
TradePartyType seller = new TradePartyType();
TextType sellerName = new TextType();
sellerName.setValue("Seller Company GmbH");
seller.setName(sellerName);
agreement.setSellerTradeParty(seller);

TradePartyType buyer = new TradePartyType();
TextType buyerName = new TextType();
buyerName.setValue("Buyer Corp");
buyer.setName(buyerName);
agreement.setBuyerTradeParty(buyer);
transaction.setApplicableHeaderTradeAgreement(agreement);

HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
TradePartyType shipTo = new TradePartyType();
TextType shipToName = new TextType();
shipToName.setValue("Buyer Warehouse");
shipTo.setName(shipToName);
delivery.setShipToTradeParty(shipTo);
transaction.setApplicableHeaderTradeDelivery(delivery);

HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
TradeSettlementHeaderMonetarySummationType settlementSum = new TradeSettlementHeaderMonetarySummationType();
AmountType grandTotal = new AmountType();
grandTotal.setCurrencyID("EUR");
grandTotal.setValue(new BigDecimal("150.00"));
settlementSum.getGrandTotalAmount().add(grandTotal);
settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(settlementSum);
transaction.setApplicableHeaderTradeSettlement(settlement);

SupplyChainTradeLineItemType line = new SupplyChainTradeLineItemType();
DocumentLineDocumentType lineDoc = new DocumentLineDocumentType();
IDType lineId = new IDType();
lineId.setValue("1");
lineDoc.setLineID(lineId);
line.setAssociatedDocumentLineDocument(lineDoc);

TradeProductType product = new TradeProductType();
TextType productName = new TextType();
productName.setValue("Article confirmé");
product.getName().add(productName);
line.setSpecifiedTradeProduct(product);

LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
QuantityType agreed = new QuantityType();
agreed.setUnitCode("EA");
agreed.setValue(new BigDecimal("10"));
lineDelivery.setAgreedQuantity(agreed);
line.setSpecifiedLineTradeDelivery(lineDelivery);

transaction.getIncludedSupplyChainTradeLineItem().add(line);
response.setSupplyChainTradeTransaction(transaction);

new OrderResponseWriter().write(response, Path.of("target/ordersp-generated.xml").toFile());
```

### Générer un DESPATCH_ADVICE (DESADV)

Créez un avis d’expédition complet en instanciant un `DespatchAdvice`, en remplissant les agrégats requis
(contexte, document échangé, transaction et lignes), puis sérialisez-le avec `DesadvWriter`.

Vous pouvez également vous appuyer sur `DesadvGenerator.genererDesadv(() -> desadv, "...")` pour partir directement de
vos objets métier.

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

LineTradeDeliveryType delivery = new LineTradeDeliveryType();
QuantityType qty = new QuantityType();
qty.setUnitCode("EA");
qty.setValue(new BigDecimal("10"));
delivery.setRequestedQuantity(qty);
line.setSpecifiedLineTradeDelivery(delivery);

tx.getIncludedSupplyChainTradeLineItem().add(line);
advice.setSupplyChainTradeTransaction(tx);

new DesadvWriter().write(advice, Path.of("target/desadv-generated.xml").toFile());
```

### Générer une INVOICE

```java
import com.cii.messaging.model.invoice.Invoice;
import com.cii.messaging.writer.InvoiceWriter;
import com.cii.messaging.unece.invoice.*;
import java.math.BigDecimal;
import java.nio.file.Path;

Invoice invoice = new Invoice();

ExchangedDocumentContextType ctx = new ExchangedDocumentContextType();
DocumentContextParameterType guideline = new DocumentContextParameterType();
IDType guidelineId = new IDType();
guidelineId.setValue("urn:factur-x:invoice:1p0");
guideline.setID(guidelineId);
ctx.getGuidelineSpecifiedDocumentContextParameter().add(guideline);
invoice.setExchangedDocumentContext(ctx);

ExchangedDocumentType doc = new ExchangedDocumentType();
IDType docId = new IDType();
docId.setValue("INV-2024-001");
doc.setID(docId);
DocumentCodeType docType = new DocumentCodeType();
docType.setValue("380"); // Code UNCL1001 pour une facture commerciale
doc.setTypeCode(docType);
DateTimeType issue = new DateTimeType();
DateTimeType.DateTimeString issueString = new DateTimeType.DateTimeString();
issueString.setFormat("102");
issueString.setValue("20240216123000");
issue.setDateTimeString(issueString);
doc.setIssueDateTime(issue);
invoice.setExchangedDocument(doc);

SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();

HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
TradePartyType seller = new TradePartyType();
TextType sellerName = new TextType();
sellerName.setValue("Seller Company GmbH");
seller.setName(sellerName);
agreement.setSellerTradeParty(seller);

TradePartyType buyer = new TradePartyType();
TextType buyerName = new TextType();
buyerName.setValue("Buyer Corp");
buyer.setName(buyerName);
agreement.setBuyerTradeParty(buyer);
transaction.setApplicableHeaderTradeAgreement(agreement);

HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
TradePartyType shipTo = new TradePartyType();
TextType shipToName = new TextType();
shipToName.setValue("Buyer Warehouse");
shipTo.setName(shipToName);
delivery.setShipToTradeParty(shipTo);
transaction.setApplicableHeaderTradeDelivery(delivery);

HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
TradeSettlementHeaderMonetarySummationType settlementSum = new TradeSettlementHeaderMonetarySummationType();
AmountType lineTotal = new AmountType();
lineTotal.setCurrencyID("EUR");
lineTotal.setValue(new BigDecimal("500.00"));
settlementSum.getLineTotalAmount().add(lineTotal);
AmountType grandTotal = new AmountType();
grandTotal.setCurrencyID("EUR");
grandTotal.setValue(new BigDecimal("500.00"));
settlementSum.getGrandTotalAmount().add(grandTotal);
settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(settlementSum);
transaction.setApplicableHeaderTradeSettlement(settlement);

SupplyChainTradeLineItemType line = new SupplyChainTradeLineItemType();
DocumentLineDocumentType lineDoc = new DocumentLineDocumentType();
IDType lineId = new IDType();
lineId.setValue("1");
lineDoc.setLineID(lineId);
line.setAssociatedDocumentLineDocument(lineDoc);

TradeProductType product = new TradeProductType();
TextType productName = new TextType();
productName.setValue("Widget A");
product.getName().add(productName);
line.setSpecifiedTradeProduct(product);

LineTradeAgreementType lineAgreement = new LineTradeAgreementType();
TradePriceType price = new TradePriceType();
AmountType unitPrice = new AmountType();
unitPrice.setCurrencyID("EUR");
unitPrice.setValue(new BigDecimal("50.00"));
price.getChargeAmount().add(unitPrice);
lineAgreement.setNetPriceProductTradePrice(price);
line.setSpecifiedLineTradeAgreement(lineAgreement);

LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
QuantityType billedQuantity = new QuantityType();
billedQuantity.setUnitCode("EA");
billedQuantity.setValue(new BigDecimal("10"));
lineDelivery.setRequestedQuantity(billedQuantity);
line.setSpecifiedLineTradeDelivery(lineDelivery);

LineTradeSettlementType lineSettlement = new LineTradeSettlementType();
TradeSettlementLineMonetarySummationType lineSum = new TradeSettlementLineMonetarySummationType();
AmountType lineAmount = new AmountType();
lineAmount.setCurrencyID("EUR");
lineAmount.setValue(new BigDecimal("500.00"));
lineSum.getLineTotalAmount().add(lineAmount);
lineSettlement.setSpecifiedTradeSettlementLineMonetarySummation(lineSum);
line.setSpecifiedLineTradeSettlement(lineSettlement);

transaction.getIncludedSupplyChainTradeLineItem().add(line);
invoice.setSupplyChainTradeTransaction(transaction);

new InvoiceWriter().write(invoice, Path.of("target/invoice-generated.xml").toFile());
```

Comme pour les autres flux, `InvoiceGenerator.genererInvoice(() -> invoice, "...")` peut produire le fichier final à
partir d’un objet métier existant.

### Charger manuellement les schémas

```java
Schema schema = UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd");
// Le chargeur résout automatiquement le suffixe spécifique à la version
```

## 🤖 Scripts

- `scripts/build.sh` : construction Maven complète (tests ignorés) et copie du JAR CLI dans `dist/cii-cli.jar`
- `scripts/run-cli.sh` : wrapper pour lancer le CLI depuis `dist` (exécutez d’abord le script de build)
- `scripts/validate-all.sh` : valide tous les fichiers XML d’un répertoire via le CLI en utilisant `dist/cii-cli.jar`

## 📑 Schémas XSD

Les schémas officiels **UN/CEFACT** sont livrés avec le module `cii-model` pour chaque version supportée (`D23B`,
`D24A`, …). Ils se trouvent dans `cii-model/src/main/resources/xsd/<VERSION>/` et sont chargés automatiquement par
`UneceSchemaLoader`.

Chaque schéma garantit la structure XML pour les flux suivants :

- `CrossIndustryOrder.xsd` : commandes (**ORDER/ORDERS**)
- `CrossIndustryOrderResponse.xsd` : réponses de commande (**ORDER_RESPONSE**)
- `CrossIndustryDespatchAdvice.xsd` : avis d’expédition (**DESADV**)
- `CrossIndustryInvoice.xsd` : factures (**INVOICE**)

Les écrivains s’appuient sur ces XSD pour produire des documents conformes et `XmlValidator` les utilise pour
valider les fichiers. Pour ajouter une nouvelle version, déposez les fichiers XSD dans le dossier correspondant et
défissez la propriété Maven `-Dunece.version=<VERSION>` lors de la compilation. Les derniers schémas sont
disponibles sur le site UNECE : <https://unece.org/trade/uncefact/mainstandards>.

## 🧪 Exécuter les tests avec Maven

Lancez l’ensemble de la suite avec la commande standard :

```bash
mvn test
```

Variantes utiles :

- Exécuter les tests d’un module spécifique : `mvn -pl cii-reader test`
- Cibler une seule classe de test : `mvn -Dtest=OrderReaderTest -pl cii-reader test`
- Exécuter les tests tout en reconstruisant les artefacts : `mvn clean verify`

Chaque module s’appuie sur Surefire. Assurez-vous que Java 21 est disponible dans votre environnement avant de
lancer les tests.

## 📚 Ressources utiles

- [UN/CEFACT](https://unece.org/trade/uncefact)
- [ZUGFeRD](https://www.zugferd.org/)
- [EN 16931](https://www.en16931.eu/)
