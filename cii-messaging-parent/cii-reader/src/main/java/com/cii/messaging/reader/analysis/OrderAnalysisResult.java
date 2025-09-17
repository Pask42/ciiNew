package com.cii.messaging.reader.analysis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Représente une vue synthétique d'une commande CII et de ses lignes.
 */
public final class OrderAnalysisResult {

    private final String orderId;
    private final String issueDate;
    private final String buyerName;
    private final String buyerIdentifier;
    private final String buyerReference;
    private final String sellerName;
    private final String currency;
    private final int lineCount;
    private final List<OrderLineSummary> lines;

    public OrderAnalysisResult(
            String orderId,
            String issueDate,
            String buyerName,
            String buyerIdentifier,
            String buyerReference,
            String sellerName,
            String currency,
            int lineCount,
            List<OrderLineSummary> lines) {
        this.orderId = orderId;
        this.issueDate = issueDate;
        this.buyerName = buyerName;
        this.buyerIdentifier = buyerIdentifier;
        this.buyerReference = buyerReference;
        this.sellerName = sellerName;
        this.currency = currency;
        this.lineCount = lineCount;
        this.lines = Collections.unmodifiableList(Objects.requireNonNull(lines, "lines"));
    }

    public String getOrderId() {
        return orderId;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerIdentifier() {
        return buyerIdentifier;
    }

    public String getBuyerReference() {
        return buyerReference;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getCurrency() {
        return currency;
    }

    public int getLineCount() {
        return lineCount;
    }

    public List<OrderLineSummary> getLines() {
        return lines;
    }

    /**
     * Retourne un message multi-lignes prêt à afficher décrivant la commande.
     */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Commande");
        if (orderId != null && !orderId.isBlank()) {
            sb.append(' ').append(orderId);
        }
        sb.append('\n');

        if (issueDate != null && !issueDate.isBlank()) {
            sb.append("  Date d'émission : ").append(issueDate).append('\n');
        }
        if ((buyerName != null && !buyerName.isBlank()) || (buyerIdentifier != null && !buyerIdentifier.isBlank())) {
            sb.append("  Acheteur : ");
            if (buyerName != null && !buyerName.isBlank()) {
                sb.append(buyerName);
            }
            if (buyerIdentifier != null && !buyerIdentifier.isBlank()) {
                if (buyerName != null && !buyerName.isBlank()) {
                    sb.append(" (ID : ").append(buyerIdentifier).append(')');
                } else {
                    sb.append(buyerIdentifier);
                }
            }
            sb.append('\n');
        }
        if (buyerReference != null && !buyerReference.isBlank()) {
            sb.append("  Référence acheteur : ").append(buyerReference).append('\n');
        }
        if (sellerName != null && !sellerName.isBlank()) {
            sb.append("  Vendeur : ").append(sellerName).append('\n');
        }
        if (currency != null && !currency.isBlank()) {
            sb.append("  Devise : ").append(currency).append('\n');
        }
        sb.append("  Nombre de lignes : ").append(lineCount).append('\n');

        for (OrderLineSummary line : lines) {
            sb.append("    - Ligne ");
            if (line.getLineId() != null && !line.getLineId().isBlank()) {
                sb.append(line.getLineId());
            } else {
                sb.append('?');
            }
            sb.append(" : ");
            if (line.getProductName() != null && !line.getProductName().isBlank()) {
                sb.append(line.getProductName());
            } else if (line.getProductIdentifier() != null && !line.getProductIdentifier().isBlank()) {
                sb.append(line.getProductIdentifier());
            } else {
                sb.append("Article sans nom");
            }
            if (line.getQuantity() != null) {
                sb.append(" — Quantité : ").append(line.getQuantity());
                if (line.getQuantityUnit() != null && !line.getQuantityUnit().isBlank()) {
                    sb.append(' ').append(line.getQuantityUnit());
                }
            }
            if (line.getNetPrice() != null) {
                sb.append(" — Prix unitaire : ")
                        .append(line.getNetPrice());
                if (line.getNetPriceCurrency() != null && !line.getNetPriceCurrency().isBlank()) {
                    sb.append(' ').append(line.getNetPriceCurrency());
                }
            }
            if (line.getLineTotal() != null) {
                sb.append(" — Total ligne : ")
                        .append(line.getLineTotal());
                String currencyCode = line.getLineTotalCurrency() != null && !line.getLineTotalCurrency().isBlank()
                        ? line.getLineTotalCurrency()
                        : currency;
                if (currencyCode != null && !currencyCode.isBlank()) {
                    sb.append(' ').append(currencyCode);
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPrettyString();
    }

    /**
     * Résumé d'une ligne de commande.
     */
    public static final class OrderLineSummary {
        private final String lineId;
        private final String productIdentifier;
        private final String productName;
        private final BigDecimal quantity;
        private final String quantityUnit;
        private final BigDecimal netPrice;
        private final String netPriceCurrency;
        private final BigDecimal lineTotal;
        private final String lineTotalCurrency;

        public OrderLineSummary(
                String lineId,
                String productIdentifier,
                String productName,
                BigDecimal quantity,
                String quantityUnit,
                BigDecimal netPrice,
                String netPriceCurrency,
                BigDecimal lineTotal,
                String lineTotalCurrency) {
            this.lineId = lineId;
            this.productIdentifier = productIdentifier;
            this.productName = productName;
            this.quantity = quantity;
            this.quantityUnit = quantityUnit;
            this.netPrice = netPrice;
            this.netPriceCurrency = netPriceCurrency;
            this.lineTotal = lineTotal;
            this.lineTotalCurrency = lineTotalCurrency;
        }

        public String getLineId() {
            return lineId;
        }

        public String getProductIdentifier() {
            return productIdentifier;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public String getQuantityUnit() {
            return quantityUnit;
        }

        public BigDecimal getNetPrice() {
            return netPrice;
        }

        public String getNetPriceCurrency() {
            return netPriceCurrency;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }

        public String getLineTotalCurrency() {
            return lineTotalCurrency;
        }
    }
}
