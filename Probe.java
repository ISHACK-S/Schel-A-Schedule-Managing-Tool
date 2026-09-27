import com.schel.api.AuthApiServer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Probe {
  public static void main(String[] args) throws Exception {
    AuthApiServer.start(8081);

    HttpURLConnection health = (HttpURLConnection) new URL("http://localhost:8081/api/health").openConnection();
    health.setRequestMethod("GET");
    health.setConnectTimeout(5000);
    health.setReadTimeout(5000);
    System.out.println("HEALTH=" + health.getResponseCode());
    System.out.println(new String(health.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

    HttpURLConnection schedules = (HttpURLConnection) new URL("http://localhost:8081/api/schedules").openConnection();
    schedules.setRequestMethod("GET");
    schedules.setConnectTimeout(5000);
    schedules.setReadTimeout(5000);
    System.out.println("SCHEDULES=" + schedules.getResponseCode());
    System.out.println(new String(schedules.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
  }
}
