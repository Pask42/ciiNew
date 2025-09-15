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
