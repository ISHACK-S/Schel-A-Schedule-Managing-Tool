import java.util.Map;
import com.schel.config.DatabaseConfig;
import com.schel.database.SupabaseClient;

public class ConfigProbe {
    public static void main(String[] args) throws Exception {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        System.out.println("CONFIGURED: " + cfg.isConfigured());
        try {
            String response = SupabaseClient.getInstance().get("categories", Map.of("select", "*", "limit", "1"));
            System.out.println("REQUEST: successful");
            System.out.println("RESPONSE_LENGTH: " + response.length());
        } catch (Exception e) {
            System.out.println("REQUEST: failed");
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
