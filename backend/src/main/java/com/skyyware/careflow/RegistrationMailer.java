package com.skyyware.careflow;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

@ApplicationScoped
public class RegistrationMailer {
    private final Mailer mailer;
    private final String host;
    private final String notifyTo;
    private final String from;

    public RegistrationMailer(
            Mailer mailer,
            @ConfigProperty(name = "quarkus.mailer.host", defaultValue = "") String host,
            @ConfigProperty(name = "app.registration.notify-to", defaultValue = "admin@stage.dev") String notifyTo,
            @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "admin@stage.dev") String from
    ) {
        this.mailer = mailer;
        this.host = host;
        this.notifyTo = notifyTo;
        this.from = from;
    }

    public RegistrationResponse register(RegistrationRequest request) {
        boolean sent = send(request);
        return new RegistrationResponse("received", sent, Instant.now());
    }

    private boolean send(RegistrationRequest request) {
        if (host == null || host.isBlank()) {
            return false;
        }

        try {
            mailer.send(Mail.withText(
                    notifyTo,
                    "[CareFlow] Access request from " + request.company(),
                    String.join("\n",
                            "CareFlow access request",
                            "",
                            "Name: " + request.name(),
                            "Email: " + request.email(),
                            "Company / team: " + request.company(),
                            "Submitted: " + Instant.now(),
                            "",
                            "Use case:",
                            request.useCase()
                    )
            ).setFrom(from).setReplyTo(request.email()));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
