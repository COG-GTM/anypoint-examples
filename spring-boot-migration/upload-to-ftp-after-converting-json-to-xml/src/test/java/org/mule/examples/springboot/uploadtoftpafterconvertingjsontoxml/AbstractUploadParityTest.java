package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import com.fasterxml.jackson.databind.JsonNode;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;

import java.util.Map;

/**
 * Replays every fixture against {@code POST /} and then checks the side effect of the Mule
 * {@code ftp:outbound-endpoint}: the file named in {@code expectedUpload.path} exists on the server with the
 * expected content. Subclasses provide the server (FTP like the Mule app, or SFTP).
 */
abstract class AbstractUploadParityTest extends AbstractParityTest {

    static final String FIXTURES = "parity/upload-to-ftp-after-converting-json-to-xml";

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    /** Content of {@code path} (as written by Mule, e.g. {@code /Upload/muleExample.xml}) on the server. */
    protected abstract byte[] readRemoteFile(String path) throws Exception;

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        JsonNode upload = FixtureExtras.raw(FIXTURES, fixture.name()).path("expectedUpload");
        try {
            byte[] content = readRemoteFile(upload.path("path").asText());
            FixtureExtras.assertSideEffect(fixture, "uploaded file " + upload.path("path").asText(), upload,
                    Map.of(), content);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("[" + fixture.name() + "] uploaded file not readable: " + e, e);
        }
    }
}
