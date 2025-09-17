package com.cii.messaging.writer.generation;

import com.cii.messaging.model.invoice.Invoice;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.InvoiceWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utilitaire dédié à la génération de messages INVOICE au format XML.
 */
public final class InvoiceGenerator {

    private InvoiceGenerator() {
        // utilitaire
    }

    /**
     * Génère un fichier INVOICE XML depuis l'objet métier fourni et retourne un message de confirmation.
     *
     * @param invoice      représentation métier de la facture
     * @param cheminSortie chemin du fichier à générer
     * @return un message décrivant l'emplacement du fichier généré
     * @throws IOException        si le chemin est invalide ou si l'écriture échoue au niveau du système de fichiers
     * @throws CIIWriterException si la sérialisation JAXB échoue
     */
    public static String genererInvoice(ObjetInvoice invoice, String cheminSortie)
            throws IOException, CIIWriterException {
        Objects.requireNonNull(invoice, "invoice");
        Objects.requireNonNull(cheminSortie, "cheminSortie");

        if (cheminSortie.isBlank()) {
            throw new IllegalArgumentException("Le chemin de sortie ne peut pas être vide");
        }

        Invoice invoiceMessage = Objects.requireNonNull(invoice.toInvoice(),
                "L'objet invoice doit fournir une instance Invoice valide");

        Path outputPath = Path.of(cheminSortie);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
            throw new IOException("Le chemin de sortie correspond à un répertoire : " + cheminSortie);
        }

        InvoiceWriter writer = new InvoiceWriter();
        writer.write(invoiceMessage, outputPath.toFile());

        return "Fichier INVOICE généré avec succès : " + outputPath.toAbsolutePath();
    }
}
