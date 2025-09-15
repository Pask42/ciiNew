<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
    xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
    xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/">
    <svrl:schematron-output>
      <xsl:apply-templates select="rsm:CrossIndustryInvoice"/>
    </svrl:schematron-output>
  </xsl:template>
  <xsl:template match="rsm:CrossIndustryInvoice">
    <!-- Warning if invoice ID missing -->
    <xsl:if test="not(rsm:ExchangedDocument/ram:ID)">
      <svrl:successful-report test="rsm:ExchangedDocument/ram:ID" location="/rsm:CrossIndustryInvoice">
        <svrl:text>Invoice ID is missing</svrl:text>
      </svrl:successful-report>
    </xsl:if>
    <!-- Error if no line items -->
    <xsl:if test="not(rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem)">
      <svrl:failed-assert test="rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem" location="/rsm:CrossIndustryInvoice">
        <svrl:text>At least one line item required</svrl:text>
      </svrl:failed-assert>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
