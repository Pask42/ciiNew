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

Generated XML now declares the canonical CII prefixes (`rsm`, `ram`, `udt`, `qdt`) so that CLI outputs match UNECE interoperability requirements.

```bash
# Générer une réponse acceptée pour order-sample.xml et l’écrire dans target/order-response.xml
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  respond --ack-code AP --response-id-prefix ORDRSP- \
  --output target/order-response.xml cii-samples/src/main/resources/samples/order-sample.xml
```

### Purpose codes officiels UNECE

L’option `--ack-code` supporte également la table **UN/CEFACT Message Function/Purpose Code**. Voici la liste
complète des valeurs officielles à utiliser dans vos ORDER_RESPONSE (`ORDRSP`) :

- **1 – Annulation** : Message annulant une transmission précédente pour une transaction donnée.
- **2 – Ajout** : Message contenant des éléments à ajouter.
- **3 – Suppression** : Message contenant des éléments à supprimer.
- **4 – Modification** : Message contenant des éléments à modifier.
- **5 – Remplacement** : Message remplaçant un message précédent.
- **6 – Confirmation** : Confirmation des détails d’une transmission précédente, si requis contractuellement.
- **7 – Duplicata** : Double d’un message déjà généré.
- **8 – Statut** : Le message référencé indique un statut.
- **9 – Original** : Première transmission relative à une transaction donnée.
- **10 – Non trouvé** : Numéro de référence non enregistré.
- **11 – Réponse** : Réponse à un message ou document précédent.
- **12 – Non traité** : Message reçu mais pas encore traité.
- **13 – Demande** : Message référencé représentant une demande.
- **14 – Notification préalable** : Notification anticipée d’informations à venir.
- **15 – Rappel** : Transmission répétée d’un message en guise de rappel.
- **16 – Proposition** : Contenu constituant une proposition.
- **17 – Annulé, à réémettre** : Transaction annulée ; une nouvelle transmission suivra.
- **18 – Réémission** : Nouvelle émission d’un message précédent.
- **19 – Modification initiée par le vendeur** : Modification transmise par l’acheteur mais initiée par le vendeur.
- **20 – Remplacer uniquement l’en-tête** : Remplacement limité à la section en-tête.
- **21 – Remplacer les détails et le résumé** : Remplacement des lignes détaillées et du résumé.
- **22 – Transmission finale** : Dernier message d’une série pour la transaction.
- **23 – Transaction en attente** : Message à ne pas traiter jusqu’à nouvel ordre.
- **24 – Instruction de livraison** : Instructions de livraison à court terme.
- **25 – Prévision** : Prévisions à long terme.
- **26 – Instruction + Prévision** : Combinaison des fonctions 24 et 25.
- **27 – Non accepté** : Message signalant un refus d’acceptation.
- **28 – Accepté, avec modification dans l’en-tête** : Accepté mais en-tête modifié.
- **29 – Accepté sans modification** : Accepté intégralement.
- **30 – Accepté, modification dans les détails** : Accepté mais lignes détaillées modifiées.
- **31 – Copie** : Copie d’un message original déjà envoyé.
- **32 – Approbation** : Autorisation d’exécuter le message référencé.
- **33 – Modification de l’en-tête** : Mise à jour de la section en-tête.
- **34 – Accepté avec amendement** : Accepté mais amendé.
- **35 – Retransmission** : Transmission identique d’un message précédent.
- **36 – Modification des détails** : Mise à jour des lignes détaillées uniquement.
- **37 – Annulation d’un débit** : Annule un débit enregistré.
- **38 – Annulation d’un crédit** : Annule un crédit enregistré.
- **39 – Réversion d’une annulation** : Annule une annulation précédente.
- **40 – Demande de suppression** : Demande de suppression de la transaction référencée.
- **41 – Ordre de clôture / fin** : Dernier message d’une série d’appels de livraison.
- **42 – Confirmation par un moyen spécifique** : Confirmation d’une transaction par un autre canal (ex. téléphone).
- **43 – Transmission complémentaire** : Données électroniques venant compléter un autre canal.
- **44 – Accepté sans réserve** : Acceptation sans aucune réserve.

- **45 – Accepté avec réserves** : Acceptation sous réserve.
- **46 – Provisoire** : Contenu provisoire.
- **47 – Définitif** : Contenu définitif.
- **48 – Accepté, contenu rejeté** : Message reçu mais non traitable (ex. raisons légales).
- **49 – Litige résolu** : Litige résolu.
- **50 – Retrait** : Retrait d’un message précédemment approuvé.
- **51 – Autorisation** : Autorisation d’un message ou d’une transaction.
- **52 – Amendement proposé** : Modification suggérée par l’expéditeur.
- **53 – Test** : Message de test.
- **54 – Extrait** : Sous-ensemble du message original.
- **55 – Notification uniquement** : Informations utilisables à des fins d’analyse seulement.
- **56 – Avis d’écritures comptabilisées** : Confirmation d’écritures passées au grand livre.
- **57 – Avis d’éléments en attente de comptabilisation** : Éléments en attente d’enregistrement.
- **58 – Préavis d’éléments nécessitant des informations supplémentaires** : Éléments nécessitant des informations additionnelles.
- **59 – Éléments préavisés** : Éléments simplement préavisés.
- **60 – Aucune action depuis le dernier message** : Aucun changement depuis la dernière transmission.
- **61 – Planning complet** : Message représentant un planning complet.
- **62 – Mise à jour du planning** : Mise à jour d’un planning existant.
- **63 – Non accepté, provisoire** : Non accepté sous réserve de confirmation.
- **64 – Vérification** : Message transmis pour vérification.
- **65 – Litige non résolu** : Litige toujours ouvert.
- **66 – Levée de garantie d’opération** : Informations sur la levée d’une opération garantie.
- **67 – Fin d’une garantie d’opération** : Informations sur la fin d’une opération garantie.
- **68 – Début d’une garantie d’opération** : Informations sur le début d’une opération garantie.
- **69 – Nouvelle déclaration de données** : Nouvelles données déclaratives liées à une garantie.
- **70 – Déclaration amendée** : Déclaration modifiée.
- **71 – Refus du début d’une garantie d’opération** : Refus de démarrer l’opération garantie.
- **72 – Informations sur les scellés (Début)** : Informations sur les scellés au début de l’opération.
- **73 – Informations sur les scellés (Fin)** : Informations sur les scellés à la fin de l’opération.

### AMAZON – LineStatusCode pour OrderResponse

Pour les réponses aux commandes **AMAZON**, le chemin XML
`CrossIndustryOrderResponse/SupplyChainTradeTransaction/IncludedSupplyChainTradeLineItem/AssociatedDocumentLineDocument/LineStatusCode`
doit véhiculer des codes spécifiques pour indiquer le statut de chaque ligne lors de l’envoi d’un message **OrderResponse (ORDRSP)** :

- **3 – Changement** : La ligne est modifiée par rapport à la demande initiale.
- **5 – Accepté** : La ligne est acceptée telle que demandée.
- **10 – Non trouvé** : La ligne référencée n’a pas été identifiée dans la commande.


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
