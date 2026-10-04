package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * A Spring Boot servlet application with the admin starter on the classpath (its auto-configuration is picked up
 * like in any application), started on a random port of a real embedded Tomcat, for tests that talk HTTP to it.
 */
public final class AdminTestApp {
    public static final String API = "/easy-extension-admin/easy-extension-api";

    private AdminTestApp() {
    }

    @MatcherParam
    public static class Param {
    }

    @ExtensionPoint
    public interface Pricing {
        String price();
    }

    public static class PricingDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Pricing {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class);
        }

        @Override
        public String price() {
            return "default price";
        }
    }

    /** Endpoints of the host application itself, unrelated to the admin. */
    @RestController
    public static class HostController {
        @GetMapping("/host/ok")
        public String ok() {
            return "ok";
        }

        @GetMapping("/host/forbidden")
        public String forbidden() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not for you");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class App {
        @Bean
        DefaultExtensionContext<Param> extensionContext() throws Exception {
            DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
            context.registerExtensionPoint(Pricing.class);
            context.registerMatcherParamClass(Param.class);
            context.addExtensionPointDefaultImplementation(new PricingDefault());
            return context;
        }

        @Bean
        HostController hostController() {
            return new HostController();
        }
    }

    /** Starts the application on a random port; {@code properties} are {@code key=value} pairs. */
    public static ConfigurableApplicationContext start(String... properties) {
        String[] all = new String[properties.length + 2];
        all[0] = "server.port=0";
        all[1] = "spring.main.banner-mode=off";
        System.arraycopy(properties, 0, all, 2, properties.length);
        SpringApplication application = new SpringApplicationBuilder(App.class).web(WebApplicationType.SERVLET).properties(all).build();
        return application.run();
    }

    /** The status and the interesting parts of what came back. */
    public record Reply(int status, String body, String allowOrigin) {
    }

    /** Sends the request line exactly as given: {@code rawPathAndQuery} is neither normalized nor encoded. */
    public static Reply send(ConfigurableApplicationContext context, String method, String rawPathAndQuery, String... headers)
            throws IOException, InterruptedException {
        int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPathAndQuery))
                .method(method, HttpRequest.BodyPublishers.noBody());
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    public static Reply get(ConfigurableApplicationContext context, String rawPathAndQuery, String... headers) throws IOException, InterruptedException {
        return send(context, "GET", rawPathAndQuery, headers);
    }
}
