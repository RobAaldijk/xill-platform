/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.xillio.xill.webservice.exceptions.BaseException;
import nl.xillio.xill.webservice.model.Runtime;
import nl.xillio.xill.webservice.model.WorkerFactory;
import nl.xillio.xill.webservice.services.WebService;
import nl.xillio.xill.webservice.types.WorkerID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static java.util.concurrent.TimeUnit.SECONDS;
import static nl.xillio.xill.webservice.IsValidUrlMatcher.isValidUrl;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class will run tests on the web api. It will also generate snippets that can be included
 * in asciidoc documentation.
 *
 * @author Thomas Biesaart
 */
@SpringBootTest
@TestPropertySource(locations="classpath:test.properties")
public class WebServiceIT extends AbstractTestNGSpringContextTests {
    private final ManualRestDocumentation restDocumentation = new ManualRestDocumentation("tmp-test");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private WebService webService;
    @Autowired
    WorkerFactory workerFactory;

    private MockMvc mockMvc;
    private Runtime runtime;

    @Value("${xill.xws.api.base.path}")
    private String basePath;

    @BeforeMethod
    public void setUp(Method method) throws BaseException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(this.restDocumentation)).build();
        this.restDocumentation.beforeTest(getClass(), method.getName());
        runtime = mock(Runtime.class);
        doNothing().when(runtime).compile(any(), any());
    }

    @AfterMethod
    public void tearDown() {
        this.restDocumentation.afterTest();

        webService.releaseAllWorkers();
    }

    private WorkerID allocateWorker(final String robotFQN) throws BaseException {
        return webService.allocateWorker(robotFQN);
    }

    @Test
    public void testPing() throws Exception {
        this.mockMvc.perform(
                get(basePath + "/ping")
        ).andExpect(status().isOk());

    }

    /**
     * Posting to /workers should allocate a new available worker.
     */
    @Test
    public void testCreateWorker() throws Exception {
        // When creating a worker
        this.mockMvc.perform(
                post(basePath + "/workers")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("robotFullyQualifiedName", "test.worker")
        )
                // The response must be 201-CREATED
                .andExpect(status().isCreated())
                // And contain a Location URL as a header
                .andExpect(header().string("Location", isValidUrl()))
                .andDo(document("create-worker"));
    }

    /**
     * Workers MUST be created by fully qualified name. No paths.
     */
    @Test
    public void testCreateWorkerWithInvalidRobot() throws Exception {
        // When creating a worker
        this.mockMvc.perform(
                post(basePath + "/workers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        // With an invalid robot name
                        .param("robotFullyQualifiedName", "Path/To/My Robot.xill")
        )
                // The response must be 404-NOT_FOUND
                .andExpect(status().isNotFound());
    }

    /**
     * A robot field must be present when allocating a worker.
     */
    @Test
    public void testCreateWorkerWithoutRobot() throws Exception {
        // When creating a worker
        this.mockMvc.perform(
                post(basePath + "/workers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        // With an invalid robot name
                        .param("wrongParameterName", "value")
        )
                // The response must be 400-Bad Request
                .andExpect(status().isBadRequest());
    }

    /**
     * A robot must compile when allocating a worker.
     */
    @Test
    public void testCreateWorkerRobotNotCompile() throws Exception {
        // When creating a worker
        this.mockMvc.perform(
                post(basePath + "/workers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        // With an invalid robot name
                        .param("robotFullyQualifiedName", "test.invalidWorker")
        )
                // The response must be 409-Conflict
                .andExpect(status().isConflict());
    }

    /**
     * When no more workers can be allocated, the response should be not acceptable.
     */
    @Test
    public void testCreateWorkerWithFullQueue() throws Exception {
        // Fill the worker pool with 3
        for (int i = 0; i < 3; i++) {
            this.mockMvc.perform(
                    post(basePath + "/workers")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            // With a valid robot name
                            .param("robotFullyQualifiedName", "test.worker")

            )
                    // The response must be 201-CREATED
                    .andExpect(status().isCreated());
        }

        // Adding the fourth worker should error
        this.mockMvc.perform(
                post(basePath + "/workers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        // With a valid robot name
                        .param("robotFullyQualifiedName", "test.worker")
        )
                .andExpect(status().isNotAcceptable());
    }

    /**
     * Releasing a worker can be done by calling DELETE /worker/{id}.
     */
    @Test
    public void testDeleteWorker() throws Exception {
        WorkerID id = allocateWorker("test.worker");

        // Deleting a worker by id
        this.mockMvc.perform(
                delete(basePath + "/worker/" + id.getId())
        )
                // Should return 204-No Content
                .andExpect(status().isNoContent())
                .andDo(document("delete-worker"));
    }

    /**
     * If a non-valid or non-allocated worker id is used a 404 response will occur.
     */
    @Test
    public void testDeleteNonExistingWorker() throws Exception {
        // Deleting a non-existing worker
        this.mockMvc.perform(
                delete(basePath + "/workers/000400")
        )
                // Should return 404-Not Found
                .andExpect(status().isNotFound())
                .andDo(document("delete-worker-not-exist"));
    }

    /**
     * If a running worker is released, it should return an error.
     */
    @Test
    public void testDeleteRunningWorker() throws Exception {
        // Deleting a running worker should return error
        WorkerID id = allocateWorker("test.TerminateTest");

        // Start running
        Thread running = new Thread(() -> {
            try {
                webService.runWorker(id, null);
            } catch (BaseException e) {
                System.err.println(e.getMessage());
            }
        });
        running.start();

        await().pollDelay(1, SECONDS).atMost(10, SECONDS).until(() -> {
            // Terminate a running worker
            MvcResult result = this.mockMvc.perform(
                    delete(basePath + "/worker/" + id.getId())
            ).andReturn();

            return result.getResponse().getStatus();
        }, equalTo(HttpStatus.CONFLICT.value()));

        // Wait until the robot finishes
        webService.stopWorker(id);
        await().atMost(10, SECONDS).until(() -> !running.isAlive());
    }


    /**
     * Running a worker that is attached to a robot with a non-stream, non-null result should result in an
     * application/json response.
     */
    @Test
    public void testRunLoadedRobotWithJsonReturnValue() throws Exception {
        WorkerID id = allocateWorker("test.JsonReturnTest");

        // Run a robot
        this.mockMvc.perform(
                post(basePath + "/worker/" + id.getId() + "/run")
        )
                // Should return 200 - OK
                .andExpect(status().isOk())
                // And contain a json body
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(not(isEmptyString())));
    }

    /**
     * Running a worker that is attached to a robot with a stream result should result in an application/octet-stream
     * response.
     */
    @Test
    public void testRunLoadedRobotWithStreamReturnValue() throws Exception {
        WorkerID id = allocateWorker("test.StreamReturnTest");

        // Run a robot
        this.mockMvc.perform(
                post(basePath + "/worker/" + id.getId() + "/run")
        )
                // Should return 200 - OK
                .andExpect(status().isOk())
                // And contain a steam body
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().string(not(isEmptyString())));
    }

    /**
     * Running a worker that is attached to a robot with a null result should result in a 204 - NO CONTENT response.
     */
    @Test
    public void testRunLoadedRobotWithoutReturnValue() throws Exception {
        WorkerID id = allocateWorker("test.NullReturnTest");

        // Run a robot
        this.mockMvc.perform(
                post(basePath + "/worker/" + id.getId()+ "/run")
        )
                // Should return 204 - NO CONTENT
                .andExpect(status().isNoContent());
    }

    /**
     * Running a worker that is not allocated or does not exist should result in a 404 - NOT FOUNT response.
     */
    @Test
    public void testRunNotLoadedRobot() throws Exception {
        // Run a non existing robot/worker
        this.mockMvc.perform(
                post(basePath + "/workers/000404/activate")
        )
                // Should return 404 - NOT FOUND
                .andExpect(status().isNotFound());
    }

    /**
     * A running worker can be terminated by calling POST /worker/{id}/stop.
     */
    @Test
    public void testTerminateRunningWorker() throws Exception {
        WorkerID id = allocateWorker("test.TerminateTest");

        // Start running
        Thread running = new Thread(() -> {
            try {
                webService.runWorker(id, null);
            } catch (BaseException e) {
                System.err.println(e.getMessage());
            }
        });
        running.start();

        await().atMost(10, SECONDS).until(() -> {
            // Terminate a running worker
            MvcResult result = this.mockMvc.perform(
                    post(basePath + "/worker/" + id.getId() + "/stop")
            ).andReturn();

            return result.getResponse().getStatus();
        }, equalTo(HttpStatus.NO_CONTENT.value()));

        // Wait until the robot finishes
        await().atMost(10, SECONDS).until(() -> !running.isAlive());
    }

    /**
     * If a worker is not running the result of the terminate call should be 400 - BAD REQUEST.
     */
    @Test
    public void testTerminateNotRunningWorker() throws Exception {
        WorkerID id = allocateWorker("test.TerminateTest");

        // Terminate a non-running worker
        this.mockMvc.perform(
                post(basePath + "/worker/" + id.getId() + "/stop")
        )
                // Should return 409 - CONFLICT
                .andExpect(status().isConflict());
    }

    /**
     * If the id is either invalid or does not exist the response should be 404 - NOT FOUND.
     */
    @Test
    public void testTerminateNonExistingWorker() throws Exception {
        // Terminate a non-existing worker
        this.mockMvc.perform(
                post(basePath + "/worker/000404/stop")
        )
                // Should return 404 - NOT FOUND
                .andExpect(status().isNotFound());
    }
}
