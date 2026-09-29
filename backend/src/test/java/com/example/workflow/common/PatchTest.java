package com.example.workflow.common;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PatchTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void distinguishesOmissionFromNull() throws Exception {
        var patch=new Patch(json.readTree("{\"assigneeId\":null}"),"assigneeId","dueDate");
        assertTrue(patch.has("assigneeId")); assertNull(patch.uuid("assigneeId")); assertFalse(patch.has("dueDate"));
    }
    @Test void rejectsMassAssignment() throws Exception { var body=json.readTree("{\"reporterId\":\"x\"}"); assertThrows(ApiException.class,() -> new Patch(body,"title","version")); }
    @Test void rejectsStaleVersion() throws Exception { var patch=new Patch(json.readTree("{\"version\":0}"),"version"); assertThrows(ApiException.class,() -> patch.version(1)); }
    @Test void rejectsNullRequiredField() throws Exception { var patch=new Patch(json.readTree("{\"title\":null}"),"title"); assertThrows(ApiException.class,() -> patch.text("title",200,false)); }
}
