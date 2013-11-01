package nl.vpro.api.rs.client;

import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.rs.MediaRestService;
import nl.vpro.api.transfer.ProgramList;

import java.io.IOException;

// MGNL-8683
public class ApiClientThreadSafeTest {

    public static void main(String[] args) throws IOException {
        ApiClient apiClient = new ApiClient("http://rs.test.vpro.nl", 10000);

        final MediaRestService mediaRestService = apiClient.getMediaRestService();

        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    ProgramList programList = mediaRestService.getRecentReplayablePrograms(100, 0, AvType.VIDEO.toString());
                    System.out.println(programList);
                }
            }).start();
            System.out.println("Thread " + (i+1) + " started");
        }

        System.out.println("All done");
    }
}
