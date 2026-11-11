package org.mule.examples.springboot.munitshorttutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** JUnit 5 port of munit-short-tutorial/src/test/munit/production-code-test-suite.xml (unit tests). */
class ProductionCodeTestSuiteTest {

    @Test
    @DisplayName("doc-test-exampleFlow2Test1: payload_1 calls exampleSub_Flow1 once")
    void exampleFlow2Test1() {
        SubFlows subFlows = spy(new SubFlows());
        new ExampleFlow2(subFlows).process(new FlowEvent("payload_1"));
        verify(subFlows, times(1)).exampleSubFlow1(any());
        verify(subFlows, never()).exampleSubFlow2(any());
    }

    @Test
    @DisplayName("doc-test-exampleFlow2Test2: payload_2 calls exampleSub_Flow2 once")
    void exampleFlow2Test2() {
        SubFlows subFlows = spy(new SubFlows());
        new ExampleFlow2(subFlows).process(new FlowEvent("payload_2"));
        verify(subFlows, times(1)).exampleSubFlow2(any());
        verify(subFlows, never()).exampleSubFlow1(any());
    }

    @Test
    @DisplayName("doc-test-exampleFlow-unit-Test_1: mocked exampleFlow2 returning my_variable=var_value_1")
    void exampleFlowUnitTest1() {
        assertThat(runWithMockedFlow2("var_value_1")).isEqualTo("response_payload_1");
    }

    @Test
    @DisplayName("doc-test-exampleFlow-unit-Test_2: mocked exampleFlow2 returning my_variable=var_value_2")
    void exampleFlowUnitTest2() {
        assertThat(runWithMockedFlow2("var_value_2")).isEqualTo("response_payload_2");
    }

    private static Object runWithMockedFlow2(String myVariable) {
        ExampleFlow2 flow2 = mock(ExampleFlow2.class);
        when(flow2.process(any())).thenAnswer(inv -> {
            FlowEvent returned = new FlowEvent(null);
            returned.setFlowVar(SubFlows.MY_VARIABLE, myVariable);
            return returned;
        });
        return new ExampleFlow(flow2).process(new FlowEvent(null)).getPayload();
    }
}
