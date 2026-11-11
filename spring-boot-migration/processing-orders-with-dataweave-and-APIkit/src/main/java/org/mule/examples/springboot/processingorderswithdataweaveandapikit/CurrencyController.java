package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** currency.xml: APIkit router on /api/* for currency.raml; {@code get:/currencies} → parse-template currency.json. */
@RestController
public class CurrencyController {

    @GetMapping(path = "/api/currencies", produces = MediaType.APPLICATION_JSON_VALUE)
    public String currencies() throws IOException {
        return StreamUtils.copyToString(new ClassPathResource("currency.json").getInputStream(), StandardCharsets.UTF_8);
    }
}
