package org.mule.examples.springboot.muleexpressionlanguagebasics;

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * The MEL expressions of greeting.xml rewritten as SpEL. Variables mirror the MEL context: {@code #username} and
 * {@code #age} hold the extracted values (query parameter, XPath text or JSON field).
 */
@Component
public class GreetingExpressions {

    /** {@code Hello #[...username]} */
    static final String GREETING = "'Hello ' + #username";
    /** {@code #[...username == empty]}: MVEL's {@code empty} literal matches null and blank (whitespace-only) strings. */
    static final String USERNAME_EMPTY = "#username == null || #username.trim().isEmpty()";
    /** {@code #[username], #[age], #[age > 18]} */
    static final String SUMMARY = "#username + ', ' + #age + ', ' + #isAdult";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Expression greeting = parser.parseExpression(GREETING);
    private final Expression usernameEmpty = parser.parseExpression(USERNAME_EMPTY);
    private final Expression summary = parser.parseExpression(SUMMARY);

    public String greeting(String username) {
        return greeting.getValue(context(Map.of("username", String.valueOf(username))), String.class);
    }

    public boolean usernameEmpty(String username) {
        SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
        ctx.setVariable("username", username);
        return Boolean.TRUE.equals(usernameEmpty.getValue(ctx, Boolean.class));
    }

    /** {@code username, age, age > 18} with MEL's String-to-number coercion for the comparison. */
    public String summary(Object username, Object age) {
        return summary.getValue(context(Map.of(
                "username", String.valueOf(username),
                "age", String.valueOf(age),
                "isAdult", isAdult(age))), String.class);
    }

    /** MEL/DataWeave coerce the String operand of {@code age > 18} to a number. */
    public static boolean isAdult(Object age) {
        if (age == null) {
            return false;
        }
        return new java.math.BigDecimal(age.toString().trim()).compareTo(java.math.BigDecimal.valueOf(18)) > 0;
    }

    private static SimpleEvaluationContext context(Map<String, Object> variables) {
        SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        variables.forEach(ctx::setVariable);
        return ctx;
    }
}
