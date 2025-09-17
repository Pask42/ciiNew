package com.cii.messaging.writer.generation;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.DesadvWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utilitaire dédié à la génération de messages DESADV au format XML.
 */
public final class DesadvGenerator {

    private DesadvGenerator() {
        // utilitaire
    }

    /**
     * Génère un fichier DESADV XML depuis l'objet métier fourni et retourne un message de confirmation.
     *
     * @param desadv       représentation métier de l'avis d'expédition
     * @param cheminSortie chemin du fichier à générer
     * @return un message décrivant l'emplacement du fichier généré
     * @throws IOException        si le chemin est invalide ou si l'écriture échoue au niveau du système de fichiers
     * @throws CIIWriterException si la sérialisation JAXB échoue
     */
    public static String genererDesadv(ObjetDesadv desadv, String cheminSortie)
            throws IOException, CIIWriterException {
        Objects.requireNonNull(desadv, "desadv");
        Objects.requireNonNull(cheminSortie, "cheminSortie");

        if (cheminSortie.isBlank()) {
            throw new IllegalArgumentException("Le chemin de sortie ne peut pas être vide");
        }

        DespatchAdvice despatchAdvice = Objects.requireNonNull(desadv.toDespatchAdvice(),
                "L'objet desadv doit fournir une instance DespatchAdvice valide");

        Path outputPath = Path.of(cheminSortie);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
            throw new IOException("Le chemin de sortie correspond à un répertoire : " + cheminSortie);
        }

        DesadvWriter writer = new DesadvWriter();
        writer.write(despatchAdvice, outputPath.toFile());

        return "Fichier DESADV généré avec succès : " + outputPath.toAbsolutePath();
    }
}
