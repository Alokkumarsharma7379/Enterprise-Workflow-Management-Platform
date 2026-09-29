package com.example.workflow.task;
import com.example.workflow.common.ApiException;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
class TaskWorkflowTest {
    @TestFactory Stream<DynamicTest> everyTransitionIsExplicit() {
        Set<String> allowed=Set.of("TODO:IN_PROGRESS","TODO:CANCELLED","IN_PROGRESS:TODO","IN_PROGRESS:IN_REVIEW","IN_PROGRESS:CANCELLED","IN_REVIEW:IN_PROGRESS","IN_REVIEW:DONE","IN_REVIEW:CANCELLED");
        return Arrays.stream(TaskStatus.values()).flatMap(from -> Arrays.stream(TaskStatus.values()).map(to -> DynamicTest.dynamicTest(from+" -> "+to,() -> {
            if (from==to || allowed.contains(from+":"+to)) assertDoesNotThrow(() -> TaskWorkflow.validate(from,to));
            else assertThrows(ApiException.class,() -> TaskWorkflow.validate(from,to));
        })));
    }
}
