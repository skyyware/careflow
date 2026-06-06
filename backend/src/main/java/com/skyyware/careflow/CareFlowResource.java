package com.skyyware.careflow;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CareFlowResource {
    private final CareFlowStore store;
    private final RegistrationMailer registrationMailer;

    public CareFlowResource(CareFlowStore store, RegistrationMailer registrationMailer) {
        this.store = store;
        this.registrationMailer = registrationMailer;
    }

    @GET
    @Path("/status")
    public PlatformStatus status() {
        return store.status();
    }

    @GET
    @Path("/cases")
    public List<CareCase> cases() {
        return store.cases();
    }

    @GET
    @Path("/cases/{caseId}")
    public CareCase careCase(@PathParam("caseId") String caseId) {
        return store.findCase(caseId).orElseThrow(() -> new NotFoundException(new CareCaseNotFoundException(caseId).getMessage()));
    }

    @POST
    @Path("/cases/{caseId}/notes")
    public ClinicalNote addNote(@PathParam("caseId") String caseId, @Valid CreateNoteRequest request) {
        try {
            return store.addNote(caseId, request);
        } catch (CareCaseNotFoundException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }

    @GET
    @Path("/events")
    public List<CareEvent> events() {
        return store.events();
    }

    @POST
    @Path("/registrations")
    public RegistrationResponse register(@Valid RegistrationRequest request) {
        return registrationMailer.register(request);
    }
}
