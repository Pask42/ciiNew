package com.cii.messaging.writer.generation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Configuration immuable dédiée à la génération d'un message ORDER_RESPONSE à partir d'un ORDER.
 */
public final class OrderResponseGenerationOptions {

    private final String responseId;
    private final String responseIdPrefix;
    private final String acknowledgementCode;
    private final String documentTypeCode;
    private final LocalDateTime issueDateTime;
    private final Clock clock;

    private OrderResponseGenerationOptions(Builder builder) {
        this.responseId = builder.responseId;
        this.responseIdPrefix = builder.responseIdPrefix;
        this.acknowledgementCode = builder.acknowledgementCode;
        this.documentTypeCode = builder.documentTypeCode;
        this.issueDateTime = builder.issueDateTime;
        this.clock = builder.clock;
    }

    /**
     * Crée un jeu d'options avec les valeurs par défaut.
     *
     * @return les options avec horloge système et identifiants par défaut
     */
    public static OrderResponseGenerationOptions defaults() {
        return builder().build();
    }

    /**
     * Démarre la construction d'options personnalisées.
     *
     * @return un builder initialisé avec les valeurs par défaut
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Identifiant explicite du document ORDER_RESPONSE à générer.
     *
     * @return identifiant fixe ou {@code null} si un préfixe doit être appliqué
     */
    public String getResponseId() {
        return responseId;
    }

    /**
     * Préfixe utilisé pour dériver l'identifiant lorsque {@link #getResponseId()} est nul.
     *
     * @return préfixe non nul
     */
    public String getResponseIdPrefix() {
        return responseIdPrefix;
    }

    /**
     * Code d'accusé de réception à positionner dans le document (ex. {@code AP}).
     *
     * @return code d'accusé de réception non nul
     */
    public String getAcknowledgementCode() {
        return acknowledgementCode;
    }

    /**
     * Code de type de document à positionner (ex. {@code 231} pour ORDER_RESPONSE).
     *
     * @return code non nul
     */
    public String getDocumentTypeCode() {
        return documentTypeCode;
    }

    /**
     * Date d'émission explicite. Si {@code null}, l'horloge est utilisée.
     *
     * @return date d'émission souhaitée ou {@code null}
     */
    public LocalDateTime getIssueDateTime() {
        return issueDateTime;
    }

    /**
     * Horloge utilisée pour dériver la date d'émission lorsque {@link #getIssueDateTime()} est nulle.
     *
     * @return horloge non nulle
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Builder fluide pour {@link OrderResponseGenerationOptions}.
     */
    public static final class Builder {
        private String responseId;
        private String responseIdPrefix = "ORDRSP-";
        private String acknowledgementCode = "AP";
        private String documentTypeCode = "231";
        private LocalDateTime issueDateTime;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Définit un identifiant fixe pour la réponse.
         *
         * @param responseId identifiant souhaité
         * @return builder pour chaînage
         */
        public Builder withResponseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        /**
         * Personnalise le préfixe utilisé pour générer l'identifiant.
         *
         * @param responseIdPrefix préfixe non vide
         * @return builder pour chaînage
         */
        public Builder withResponseIdPrefix(String responseIdPrefix) {
            this.responseIdPrefix = Objects.requireNonNull(responseIdPrefix, "responseIdPrefix");
            return this;
        }

        /**
         * Personnalise le code d'accusé de réception.
         *
         * @param acknowledgementCode code non vide
         * @return builder pour chaînage
         */
        public Builder withAcknowledgementCode(String acknowledgementCode) {
            this.acknowledgementCode = Objects.requireNonNull(acknowledgementCode, "acknowledgementCode");
            return this;
        }

        /**
         * Personnalise le code de type de document.
         *
         * @param documentTypeCode code non vide
         * @return builder pour chaînage
         */
        public Builder withDocumentTypeCode(String documentTypeCode) {
            this.documentTypeCode = Objects.requireNonNull(documentTypeCode, "documentTypeCode");
            return this;
        }

        /**
         * Force la date/heure d'émission à utiliser.
         *
         * @param issueDateTime date explicite
         * @return builder pour chaînage
         */
        public Builder withIssueDateTime(LocalDateTime issueDateTime) {
            this.issueDateTime = issueDateTime;
            return this;
        }

        /**
         * Spécifie l'horloge pour les calculs de date par défaut.
         *
         * @param clock horloge non nulle
         * @return builder pour chaînage
         */
        public Builder withClock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Construit l'instance immuable.
         *
         * @return options configurées
         */
        public OrderResponseGenerationOptions build() {
            Objects.requireNonNull(responseIdPrefix, "responseIdPrefix");
            Objects.requireNonNull(acknowledgementCode, "acknowledgementCode");
            Objects.requireNonNull(documentTypeCode, "documentTypeCode");
            Objects.requireNonNull(clock, "clock");
            return new OrderResponseGenerationOptions(this);
        }
    }
}
