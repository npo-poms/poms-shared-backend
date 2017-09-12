package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.MediaClassificationService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.elasticsearch.TransportClientFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/broadcasterService.xml"})
@Slf4j
public abstract class AbstractESRepositoryTest  {

    protected static Client client;

    @Inject
    protected TransportClientFactory clientFactory;


    @Autowired
    BroadcasterService broadcasterService;


    @Before
    public void abstractSetup() {

        if (client == null) {
            client = clientFactory.client("test");
        }
        log.info("Built {}", client);
        ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());

        when(broadcasterService.find(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return new Broadcaster(id, id + "display");

        });
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
