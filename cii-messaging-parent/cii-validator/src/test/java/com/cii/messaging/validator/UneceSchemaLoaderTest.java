package com.cii.messaging.validator;

import org.junit.jupiter.api.Test;

import javax.xml.validation.Schema;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UneceSchemaLoaderTest {

    @Test
    void doitChargerSchemaInvoice() throws Exception {
        Schema schema = UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd");
        assertNotNull(schema);
    }
}

