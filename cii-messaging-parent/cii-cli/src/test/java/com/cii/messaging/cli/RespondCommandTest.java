package com.cii.messaging.cli;

import com.cii.messaging.reader.OrderResponseReader;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.model.orderresponse.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RespondCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void genereOrderResponseDansFichierSpecifie() throws Exception {
        Path input = copierEchantillon("/order-sample.xml");
        Path output = tempDir.resolve("ordersp.xml");

        int exitCode = new CommandLine(new RespondCommand()).execute(
                input.toString(),
                "--output", output.toString(),
                "--ack-code", "42",
                "--issue-date", "20240305120000"
        );

        assertThat(exitCode).isZero();
        assertThat(output).exists();

        OrderResponse response = lireOrderResponse(output);
        assertThat(response.getExchangedDocument().getID().getValue()).isEqualTo("ORDRSP-ORD-2024-001");
        assertThat(response.getExchangedDocument().getStatusCode().getValue()).isEqualTo("42");
        assertThat(response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem()).hasSize(2);
        assertThat(response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().get(0)
                .getSpecifiedLineTradeDelivery().getAgreedQuantity().getValue()).isEqualByComparingTo("100");
    }

    @Test
    void genereFichierParDefautDansMemeDossier() throws Exception {
        Path input = copierEchantillon("/order-sample.xml");

        int exitCode = new CommandLine(new RespondCommand()).execute(
                input.toString(),
                "--issue-date", "20240305120000"
        );

        assertThat(exitCode).isZero();
        Path expected = input.getParent().resolve("order-sample-ordersp.xml");
        assertThat(expected).exists();
    }

    @Test
    void renvoieErreurSiFichierManquant() {
        Path missing = tempDir.resolve("missing.xml");

        int exitCode = new CommandLine(new RespondCommand()).execute(missing.toString());

        assertThat(exitCode).isNotZero();
    }

    private Path copierEchantillon(String resource) throws IOException, URISyntaxException {
        Path source = Path.of(getClass().getResource(resource).toURI());
        Path target = tempDir.resolve(Path.of(resource).getFileName());
        Files.copy(source, target);
        return target;
    }

    private OrderResponse lireOrderResponse(Path path) throws CIIReaderException {
        return new OrderResponseReader().read(path.toFile());
    }
}
