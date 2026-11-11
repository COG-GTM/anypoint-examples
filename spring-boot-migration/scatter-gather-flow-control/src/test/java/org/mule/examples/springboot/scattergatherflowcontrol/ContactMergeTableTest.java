package org.mule.examples.springboot.scattergatherflowcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

/**
 * Table-driven aggregation parity: the same (route A, route B) inputs through the Mule ContactMerge logic as
 * asserted by the original ContactsMergeTest, and the scatter-gather flow end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ContactMergeTableTest {

    @MockBean
    AggregationReportMailer mailer;

    @Autowired
    ScatterGatherGateway gateway;

    static Stream<Arguments> merges() {
        return Stream.of(
                // original ContactsMergeTest: A = 0..1, B = 1..2
                Arguments.of(contacts("SomeName_", 0, 1), contacts("SomeName_", 1, 2), List.of(
                        merged("SomeName_0", "some.email.0@fakemail.com", "0", ""),
                        merged("SomeName_1", "some.email.1@fakemail.com", "1", "1"),
                        merged("SomeName_2", "some.email.2@fakemail.com", "", "2"))),
                // the static sources of scatter-gather.xml
                Arguments.of(ContactSources.sourceA(), ContactSources.sourceB(), List.of(
                        merged("vlado", "vlado@email.com", "1", "2"),
                        merged("michal", "michal@email.com", "2", ""),
                        merged("peter", "peter@email.com", "", "1"))),
                // blank e-mails never match
                Arguments.of(List.of(contact("1", "a", "")), List.of(contact("9", "b", "")), List.of(
                        merged("a", "", "1", ""),
                        merged("b", "", "", "9"))),
                // matching is case-sensitive (String.equals)
                Arguments.of(List.of(contact("1", "a", "X@e.com")), List.of(contact("2", "a", "x@e.com")), List.of(
                        merged("a", "X@e.com", "1", ""),
                        merged("a", "x@e.com", "", "2"))));
    }

    @ParameterizedTest
    @MethodSource("merges")
    void mergesLikeMule(List<Map<String, String>> a, List<Map<String, String>> b, List<Map<String, String>> expected) {
        assertThat(ContactMerge.mergeList(a, b)).isEqualTo(expected);
    }

    @Test
    void aggregatorOrdersRoutesBySequenceNotArrival() {
        SimpleMessageGroup group = new SimpleMessageGroup("g");
        group.add(MessageBuilder.withPayload(ContactSources.sourceB()).setSequenceNumber(2).setSequenceSize(2).build());
        group.add(MessageBuilder.withPayload(ContactSources.sourceA()).setSequenceNumber(1).setSequenceSize(2).build());
        assertThat(ContactMergeAggregationStrategy.aggregate(group))
                .isEqualTo(ContactMerge.mergeList(ContactSources.sourceA(), ContactSources.sourceB()));
    }

    @Test
    void aggregatorRequiresBothRoutes() {
        SimpleMessageGroup group = new SimpleMessageGroup("g");
        group.add(MessageBuilder.withPayload(ContactSources.sourceA()).setSequenceNumber(1).setSequenceSize(2).build());
        assertThatThrownBy(() -> ContactMergeAggregationStrategy.aggregate(group))
                .hasMessage("Data from at least one source was not able to be obtained correctly.");
    }

    @Test
    void flowProducesTheMuleCsvReport() {
        org.mockito.Mockito.when(mailer.send(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> i.getArgument(0));
        assertThat(gateway.run("x")).isEqualTo("Id,Name,Email\n,vlado,vlado@email.com\n,michal,michal@email.com\n,peter,peter@email.com\n");
    }

    private static List<Map<String, String>> contacts(String prefix, int start, int end) {
        return Stream.iterate(start, i -> i <= end, i -> i + 1)
                .map(i -> contact(Integer.toString(i), prefix + i, "some.email." + i + "@fakemail.com")).toList();
    }

    private static Map<String, String> contact(String id, String name, String email) {
        Map<String, String> c = new HashMap<>();
        c.put("Id", id);
        c.put("Name", name);
        c.put("Email", email);
        return c;
    }

    private static Map<String, String> merged(String name, String email, String idInA, String idInB) {
        return Map.of("Name", name, "Email", email, "IDInA", idInA, "IDInB", idInB);
    }
}
