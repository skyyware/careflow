package com.skyyware.careflow;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CareFlowResourceTest {
    @Test
    void exposesOperationalStatus() {
        given()
            .when().get("/api/status")
            .then()
            .statusCode(200)
            .body("status", equalTo("ready"))
            .body("openCases", equalTo(3))
            .body("eventStream", containsString("kafka-ready"));
    }

    @Test
    void sortsCriticalCasesFirst() {
        given()
            .when().get("/api/cases")
            .then()
            .statusCode(200)
            .body("size()", equalTo(3))
            .body("[0].priority", equalTo("Critical"))
            .body("[0].patientCode", equalTo("CF-3091"));
    }

    @Test
    void addsClinicalNotesToCases() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "author": "QA Reviewer",
                  "body": "Synthetic note created during automated test."
                }
                """)
            .when().post("/api/cases/case-discharge-1024/notes")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("author", equalTo("QA Reviewer"));

        given()
            .when().get("/api/cases/case-discharge-1024")
            .then()
            .statusCode(200)
            .body("notes.size()", greaterThan(1))
            .body("timeline[0].type", equalTo("NOTE_ADDED"));
    }

    @Test
    void validatesClinicalNotes() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"author\":\"\",\"body\":\"\"}")
            .when().post("/api/cases/case-discharge-1024/notes")
            .then()
            .statusCode(400);
    }
}
