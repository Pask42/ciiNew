#!/bin/bash

echo "🔧 Correction des dépendances Lombok dans le projet..."

# Ajouter Lombok au module cii-validator
cat > cii-validator/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cii.messaging</groupId>
        <artifactId>cii-messaging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>cii-validator</artifactId>
    <packaging>jar</packaging>

    <name>CII Validator</name>
    <description>XSD and business rules validation for CII messages</description>

    <dependencies>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>xerces</groupId>
            <artifactId>xercesImpl</artifactId>
        </dependency>
        <dependency>
            <groupId>net.sf.saxon</groupId>
            <artifactId>Saxon-HE</artifactId>
        </dependency>
        <dependency>
            <groupId>com.helger.cii</groupId>
            <artifactId>ph-cii-d16b</artifactId>
        </dependency>
        <dependency>
            <groupId>com.helger.cii</groupId>
            <artifactId>ph-cii-d20b</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <!-- ADD LOMBOK DEPENDENCY -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

# Vérifier également les autres modules
echo "Vérification des dépendances Lombok dans cii-service..."

# Ajouter Lombok au module cii-service
cat > cii-service/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cii.messaging</groupId>
        <artifactId>cii-messaging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>cii-service</artifactId>
    <packaging>jar</packaging>

    <name>CII Service</name>
    <description>Business logic and orchestration for CII messaging</description>

    <dependencies>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-reader</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-writer</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-validator</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <!-- ADD LOMBOK DEPENDENCY -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

# Ajouter Lombok au module cii-writer
cat > cii-writer/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cii.messaging</groupId>
        <artifactId>cii-messaging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>cii-writer</artifactId>
    <packaging>jar</packaging>

    <name>CII Writer</name>
    <description>Java to XML generation for CII messages</description>

    <dependencies>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>jakarta.xml.bind</groupId>
            <artifactId>jakarta.xml.bind-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mustangproject</groupId>
            <artifactId>library</artifactId>
        </dependency>
        <dependency>
            <groupId>com.helger.cii</groupId>
            <artifactId>ph-cii-d16b</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <!-- ADD LOMBOK DEPENDENCY -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

# Ajouter Lombok au module cii-reader
cat > cii-reader/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cii.messaging</groupId>
        <artifactId>cii-messaging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>cii-reader</artifactId>
    <packaging>jar</packaging>

    <name>CII Reader</name>
    <description>XML to Java parsing for CII messages</description>

    <dependencies>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>jakarta.xml.bind</groupId>
            <artifactId>jakarta.xml.bind-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mustangproject</groupId>
            <artifactId>library</artifactId>
        </dependency>
        <dependency>
            <groupId>xerces</groupId>
            <artifactId>xercesImpl</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <!-- ADD LOMBOK DEPENDENCY -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

echo "✅ Dépendances Lombok ajoutées à tous les modules!"

# Alternative : Si les problèmes persistent, on peut aussi créer les classes sans Lombok
echo ""
echo "Si les problèmes persistent, vous pouvez exécuter :"
echo "  ./remove-lombok-annotations.sh"
echo "pour remplacer les annotations Lombok par du code Java standard."