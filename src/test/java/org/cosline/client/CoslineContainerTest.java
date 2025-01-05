package org.cosline.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

//@SpringBootTest
@Testcontainers
public class CoslineContainerTest {

    private static final int CONTAINER_PORT = 6767;

    @Container
    static GenericContainer<?> coslineContainer = new GenericContainer<>(
            DockerImageName.parse("tinumistry/cosline"))
            .withExposedPorts(CONTAINER_PORT)
            ;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.cosline.url",
            () -> String.format("http://localhost:%d",
                    coslineContainer.getMappedPort(CONTAINER_PORT)));
    }

    @Test
    void containerShouldStart() {
        assert coslineContainer.isRunning();
        
        // Get the mapped port
        Integer mappedPort = coslineContainer.getMappedPort(CONTAINER_PORT);
        System.out.println("Container is running on port: " + mappedPort);
        
        // Add your test logic here
    }
}