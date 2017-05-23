package nl.vpro.domain.api;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.LocalClientFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/broadcasterService.xml"})
public abstract class AbstractESRepositoryTest {

    protected static LocalClientFactory clientFactory = new LocalClientFactory();
    protected static Client client;

    @Autowired
    BroadcasterService broadcasterService;

    @BeforeClass
    public static void staticSetup() throws IOException, ExecutionException, InterruptedException {
        clientFactory.setPath(null);
        client = clientFactory.client("test");
        ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());
    }

    @Before
    public void abstractSetup() {
        when(broadcasterService.find(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return new Broadcaster(id, id + "display");

        });
    }



    @AfterClass
    public static void shutdownAbstract() {
        clientFactory.node().stop();
    }

    protected static String getTypeName(MediaObject media) {
        boolean deletedType = false;

        switch (media.getWorkflow()) {
            case DELETED:
            case REVOKED:
            case MERGED:
                deletedType = true;
                break;
            default:
                break;
        }
        return (deletedType ? "deleted" : "") + media.getClass().getSimpleName().toLowerCase();
    }

}
