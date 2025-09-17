package com.cii.messaging.writer.generation;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.OrderWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utilitaire dédié à la génération de messages ORDERS au format XML.
 */
public final class OrderGenerator {

    private OrderGenerator() {
        // utilitaire
    }

    /**
     * Génère un fichier ORDERS XML depuis l'objet métier fourni et retourne un message de confirmation.
     *
     * @param commande     représentation métier de la commande
     * @param cheminSortie chemin du fichier à générer
     * @return un message décrivant l'emplacement du fichier généré
     * @throws IOException        si le chemin est invalide ou si l'écriture échoue au niveau du système de fichiers
     * @throws CIIWriterException si la sérialisation JAXB échoue
     */
    public static String genererOrders(ObjetCommande commande, String cheminSortie)
            throws IOException, CIIWriterException {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(cheminSortie, "cheminSortie");

        if (cheminSortie.isBlank()) {
            throw new IllegalArgumentException("Le chemin de sortie ne peut pas être vide");
        }

        Order order = Objects.requireNonNull(commande.toOrder(),
                "L'objet commande doit fournir une instance Order valide");

        Path outputPath = Path.of(cheminSortie);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
            throw new IOException("Le chemin de sortie correspond à un répertoire : " + cheminSortie);
        }

        OrderWriter writer = new OrderWriter();
        writer.write(order, outputPath.toFile());

        return "Fichier ORDERS généré avec succès : " + outputPath.toAbsolutePath();
    }
}
