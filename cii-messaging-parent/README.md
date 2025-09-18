# CII Messaging System

Modular Java 21 toolkit for reading, writing, and validating **UN/CEFACT Cross Industry** messages.
It covers ORDER, ORDER_RESPONSE (ORDERSP), DESPATCH_ADVICE (DESADV), and INVOICE flows while remaining
compatible with ZUGFeRD, XRechnung, and Factur-X profiles.

## 📦 Modules

| Module | Primary responsibility |
|--------|------------------------|
| `cii-model` | Data models (POJOs) and embedded UNECE XSD schemas |
| `cii-reader` | Parsing XML to strongly typed Java objects |
| `cii-writer` | Generating Java objects to XML |
| `cii-validator` | XSD validation and business rules |
| `cii-cli` | Command-line tooling |
| `cii-samples` | Sample XML payloads |

## ✅ Technical prerequisites

- Java 21 or newer
- Maven 3.6 or newer
- For CLI execution ensure `$JAVA_HOME` is set and the `java` executable is available on your `PATH`

## 🔨 Build and test

```bash
# Clone the project
git clone <repository-url>/cii-messaging-parent.git
cd cii-messaging-parent

# Build every module and run the full test suite
mvn clean install
```

### Build the CLI module only

```bash
mvn -pl cii-cli -am clean package
```

The CLI build produces two artifacts under `cii-cli/target/`:

- `cii-cli-<version>.jar`: thin jar that relies on Maven dependency resolution
- `cii-cli-<version>-jar-with-dependencies.jar`: executable jar bundled with every dependency

Run the CLI directly from the assembly jar:

```bash
java -jar cii-cli/target/cii-cli-<version>-jar-with-dependencies.jar --help
```

Or execute it with Maven without creating the jar explicitly:

```bash
mvn -pl cii-cli -am exec:java -Dexec.args="--help"
```

### Run tests

```bash
mvn -pl cii-cli test
```

## 🌐 Selecting the UNECE schema version

Schemas are loaded from `src/main/resources/xsd/<version>/...` and controlled by the `unece.version`
property (defaults to `D23B`).

```bash
# Use the default version (D23B)
mvn clean install

# Force version D24A
mvn -Dunece.version=D24A clean install

# Or configure via environment variable
UNECE_VERSION=D24A mvn clean install
```

The same property controls the default schema used by the CLI validation command. You can still override
it per execution with the `--schema-version` option.

## 🛠️ CLI reference (`cii-cli`)

### Global options

| Option | Description |
|--------|-------------|
| `-l, --log-level <LEVEL>` | Override the root Logback level (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`) |
| `-c, --config <FILE>` | Load options from a properties file containing a `log.level` entry |

If no options are provided the CLI looks for `cii-cli.properties` in the working directory, then on the
classpath. A minimal configuration file looks like:

```properties
# cii-cli.properties
log.level=DEBUG
```

### `parse` command

Analyse a CII message and render it as a high-level summary or as JSON.

| Parameter or option | Description | Default |
|---------------------|-------------|---------|
| `INPUT` (parameter) | Path to the XML file to analyse | — |
| `-o, --output <FILE>` | Optional path where the rendered output will be written. If omitted, the summary is printed to STDOUT | — |
| `--format <FORMAT>` | Output format: `SUMMARY` (human-readable synthesis) or `JSON` (full payload) | `SUMMARY` |

For ORDER messages, the summary relies on `OrderAnalyzer` and contains key business information such as document
ID, parties, dates, and line items. Other message types report the detected message class.

### `validate` command

Validate a CII document against UNECE schemas and business rules.

| Parameter or option | Description | Default |
|---------------------|-------------|---------|
| `INPUT` (parameter) | Path to the XML file to validate | — |
| `--schema-version <VERSION>` | Explicit UNECE version (`D23B`, `D24A`, …) | `SchemaVersion.getDefault()` (system property `unece.version`, then `UNECE_VERSION`, otherwise `D23B`) |
| `--fail-on-warning` | Treat validation warnings as fatal errors (non-zero exit code) | Disabled |

The validator prints a concise summary (validity, number of errors, schema bundle used, execution time) and lists
each individual error and warning.

## 🧪 Command-line examples

Assuming the assembly jar has been built (`mvn -pl cii-cli -am clean package`):

```bash
# Display a summary of the provided ORDER document
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse cii-samples/src/main/resources/samples/order-sample.xml

# Produce a JSON representation of an ORDER message
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --format JSON --output target/order.json cii-samples/src/main/resources/samples/order-sample.xml

# Validate a document with the default schema
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-sample.xml

# Validate against D24A and fail fast when warnings are present
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate --schema-version D24A --fail-on-warning cii-samples/src/main/resources/samples/order-valid.xml

# Run a parse with verbose logging supplied on the command line
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar parse --log-level DEBUG cii-samples/src/main/resources/samples/order-sample.xml

# Provide the log level through a configuration file
cat <<'PROPS' > cii-cli.properties
log.level=DEBUG
PROPS
java -jar cii-cli/target/cii-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar validate cii-samples/src/main/resources/samples/order-valid.xml
```

## 💻 Programmatic usage

The `cii-reader` and `cii-writer` modules expose JAXB readers and writers for every supported flow. Models
(`Order`, `DespatchAdvice`, `Invoice`, …) are provided by `cii-model` together with the UNECE schemas.
Sample XML documents live under `cii-samples/src/main/resources/samples/` and provide a convenient starting point for tests.

### Reading, writing, and validation utilities

- **Reading**: use `CIIReaderFactory` to detect the appropriate reader based on an XML file, or instantiate
  `OrderReader`, `InvoiceReader`, and similar classes directly. Each reader returns a strongly typed business object ready to process.
- **Writing**: writers (`OrderWriter`, `OrderResponseWriter`, `DesadvWriter`, `InvoiceWriter`) turn your Java objects
  into schema-compliant XML. Helper classes `OrderGenerator`, `DesadvGenerator`, and `InvoiceGenerator` provide
  a façade when your domain objects implement `ObjetCommande`, `ObjetDesadv`, or `ObjetInvoice` respectively.
- **Validation**: `XmlValidator.validerFichierXML(xml, xsd)` checks conformity against an XSD schema and returns
  a structured report. Combine `XmlValidator` with the `CIIValidator` implementations found in `cii-validator`
  to apply additional business rules.

### Read an ORDER from a file

```java
import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.OrderReader;
import java.nio.file.Path;

Path orderXml = Path.of("cii-samples/src/main/resources/samples/order-sample.xml");
Order order = new OrderReader().read(orderXml.toFile());
```

### Modify and rewrite an ORDER

Simple types (ID, text, codes, amounts, quantities, …) expose setters such as `setValue`, `setUnitCode`, and
`setCurrencyID`. Update the relevant nodes and marshal the object through the dedicated writer.

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

### Generate a DESPATCH_ADVICE (DESADV)

Create a complete shipping notice by instantiating a `DespatchAdvice`, populating the required aggregates
(context, exchanged document, transaction, and lines), then serialise it with `DesadvWriter`.

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
docType.setValue("351"); // DESADV code (UNCL1001)
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

### Load schemas manually

```java
Schema schema = UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd");
// The loader automatically resolves the version specific suffix
```

## 🤖 Scripts

- `scripts/build.sh`: full Maven build (tests skipped) and copies the CLI jar into `dist/cii-cli.jar`
- `scripts/run-cli.sh`: wrapper to launch the CLI from `dist` (run the build script first)
- `scripts/validate-all.sh`: validates every XML file in a directory through the CLI using `dist/cii-cli.jar`

## 📑 XSD schemas

Official **UN/CEFACT** schemas ship with the `cii-model` module for each supported version (`D23B`, `D24A`, …).
They live under `cii-model/src/main/resources/xsd/<VERSION>/` and are loaded automatically by `UneceSchemaLoader`.

Each schema guarantees the XML structure for the following flows:

- `CrossIndustryOrder.xsd`: orders (**ORDER/ORDERS**)
- `CrossIndustryOrderResponse.xsd`: order responses (**ORDER_RESPONSE**)
- `CrossIndustryDespatchAdvice.xsd`: shipping notices (**DESADV**)
- `CrossIndustryInvoice.xsd`: invoices (**INVOICE**)

Writers rely on these XSDs to produce compliant documents and `XmlValidator` uses them to validate files. To add
a new version, drop the XSD files under the corresponding folder and set the Maven property `-Dunece.version=<VERSION>`
during the build. The latest schemas are available from the UNECE website: <https://unece.org/trade/uncefact/mainstandards>.

## 🧪 Running tests with Maven

Run the full suite with the standard command:

```bash
mvn test
```

Useful variants:

- Run the tests of a specific module: `mvn -pl cii-reader test`
- Target a single test class: `mvn -Dtest=OrderReaderTest -pl cii-reader test`
- Execute tests while rebuilding artifacts: `mvn clean verify`

Every module relies on Surefire. Ensure Java 21 is available in your environment before running the tests.

## 📚 Useful resources

- [UN/CEFACT](https://unece.org/trade/uncefact)
- [ZUGFeRD](https://www.zugferd.org/)
- [EN 16931](https://www.en16931.eu/)
