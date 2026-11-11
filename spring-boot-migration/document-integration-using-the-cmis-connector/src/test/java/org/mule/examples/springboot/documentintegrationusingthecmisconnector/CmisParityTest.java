package org.mule.examples.springboot.documentintegrationusingthecmisconnector;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP parity for {@code /cmis}. The CMIS repository (the OpenKM demo server) cannot be reproduced locally, so the
 * typed {@link CmisClient} is replaced by a recording fake that answers like OpenKM (object id = folder path + file
 * name, which is what the Mule IT's {@code {"id":"/okm:root/pic} prefix shows). {@code expectedCmisCall} lists the
 * connector call the Mule flow makes.
 */
@Import(CmisParityTest.FakeRepository.class)
class CmisParityTest extends AbstractParityTest {

    static final String FIXTURES = "parity/document-integration-using-the-cmis-connector";
    static final List<Map<String, String>> CALLS = new CopyOnWriteArrayList<>();

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        CALLS.clear();
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        assertThat(CALLS).as("[%s] CMIS calls", fixture.name()).hasSize(1);
        Map<String, List<String>> call = new java.util.HashMap<>();
        CALLS.get(0).forEach((k, v) -> call.put(k, List.of(v)));
        FixtureExtras.assertSideEffect(fixture, "CMIS call",
                FixtureExtras.raw(FIXTURES, fixture.name()).path("expectedCmisCall"), call, new byte[0]);
        // The response id is the id the repository returned for this very document.
        assertThat(spring.bodyAsString()).isEqualTo("{\"id\":\"" + CALLS.get(0).get("Folder-Path") + CALLS.get(0).get("File-Name") + "\"}");
    }

    @TestConfiguration
    static class FakeRepository {

        @Bean
        @Primary
        CmisClient recordingCmisClient() {
            return (folderPath, filename, content, mimeType, objectType, versioningState) -> {
                CALLS.add(Map.of(
                        "Folder-Path", folderPath,
                        "File-Name", filename,
                        "Mime-Type", mimeType,
                        "Object-Type", objectType,
                        "Versioning-State", versioningState,
                        "Content-SHA-256", sha256(content)));
                return folderPath + filename;
            };
        }
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
