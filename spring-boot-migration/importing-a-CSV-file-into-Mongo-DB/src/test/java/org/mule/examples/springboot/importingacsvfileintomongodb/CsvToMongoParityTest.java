package org.mule.examples.springboot.importingacsvfileintomongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * File-triggered parity: each fixture's {@code request.body} is dropped into the polled directory as
 * {@code request.headers.File-Name}; {@code expected} describes the side effect the Mule flow produces: the documents
 * in the {@code customers} collection (body, without the generated {@code _id}) and the {@code Collection} /
 * {@code Input-File-Deleted} facts (headers). {@code given.existingCollection} pre-creates the collection to drive the
 * "otherwise" branch of the choice router.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
@Testcontainers
class CsvToMongoParityTest {

    static final String FIXTURES = "parity/importing-a-CSV-file-into-Mongo-DB";
    static final Path INPUT_DIR = tempDir("mongo-input");
    static final Path STAGING_DIR = tempDir("mongo-staging");

    @Container
    static final GenericContainer<?> MONGO = ParityContainers.mongo();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("database.host", MONGO::getHost);
        registry.add("database.port", () -> MONGO.getMappedPort(27017));
        registry.add("file.input-dir", INPUT_DIR::toString);
    }

    @Autowired
    MongoTemplate mongo;

    private final ObjectMapper mapper = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    private void runCase(ParityFixture fixture) throws Exception {
        mongo.getDb().drop();
        JsonNode given = FixtureExtras.raw(FIXTURES, fixture.name()).path("given");
        if (given.path("existingCollection").asBoolean(false)) {
            mongo.createCollection(CustomerImporter.COLLECTION);
        }

        String fileName = fixture.request().headers().get("File-Name");
        Path staged = STAGING_DIR.resolve(fileName);
        Files.writeString(staged, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Path target = INPUT_DIR.resolve(fileName);
        Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);

        await().atMost(Duration.ofSeconds(30)).until(() -> !Files.exists(target));

        List<Document> stored = mongo.findAll(Document.class, CustomerImporter.COLLECTION);
        assertThat(stored).allSatisfy(d -> assertThat(d.get("_id")).isInstanceOf(ObjectId.class));
        stored.forEach(d -> d.remove("_id"));
        byte[] body = mapper.writeValueAsBytes(stored);
        Map<String, List<String>> facts = Map.of(
                "Collection", List.of(mongo.collectionExists(CustomerImporter.COLLECTION) ? CustomerImporter.COLLECTION : ""),
                "Input-File-Deleted", List.of(String.valueOf(!Files.exists(target))));
        FixtureExtras.assertSideEffect(fixture, "customers collection",
                FixtureExtras.raw(FIXTURES, fixture.name()).path("expected"), facts, body);
    }

    private static Path tempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
