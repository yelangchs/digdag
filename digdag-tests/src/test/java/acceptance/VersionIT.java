package acceptance;

import io.digdag.client.DigdagClient;
import io.digdag.core.Version;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static acceptance.TestUtils.main;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class VersionIT
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ExecutorService executor;
    private Path config;

    private String host;
    private int port;
    private String endpoint;

    @Before
    public void setUp()
            throws Exception
    {
        config = Files.createFile(folder.getRoot().toPath().resolve("config"));
        executor = Executors.newCachedThreadPool();
        executor.execute(() -> main("server", "-m", "-c", config.toString()));

        host = "localhost";
        port = 65432;
        endpoint = "http://" + host + ":" + port;

        // Poll and wait for server to come up
        for (int i = 0; i < 30; i++) {
            DigdagClient client = DigdagClient.builder()
                    .host(host)
                    .port(port)
                    .build();
            try {
                client.getProjects();
                break;
            }
            catch (Exception e) {
                System.out.println(".");
            }
            Thread.sleep(1000);
        }
    }

    @After
    public void tearDown()
            throws Exception
    {
        executor.shutdownNow();
    }

    @Test
    public void testVersion()
            throws Exception
    {
        CommandStatus status = main("version", "-e", endpoint);
        assertThat(status.outUtf8(), containsString("Client version: " + Version.version()));
        assertThat(status.outUtf8(), containsString("Server version: " + Version.version()));
    }
}