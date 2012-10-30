import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void testImpacts() {
        Response response = GET("/repo/jquery/jquery/commits/stats");
        assertIsOk(response);
        assertEquals("text/json", response.contentType);
    }

}
