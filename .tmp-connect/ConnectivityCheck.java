import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.config.DatabaseConfig;
import com.schel.database.SupabaseClient;

public class ConnectivityCheck {
    public static void main(String[] args) throws Exception {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        System.out.println("SUPABASE_URL configured: " + (cfg.getSupabaseUrl() != null && !cfg.getSupabaseUrl().isBlank() ? "YES" : "NO"));
        System.out.println("SUPABASE_API_KEY configured: " + (cfg.getSupabaseApiKey() != null && !cfg.getSupabaseApiKey().isBlank() ? "YES" : "NO"));
        try {
            String response = SupabaseClient.getInstance().get("categories", Map.of("select", "*", "limit", "1"));
            ObjectMapper mapper = new ObjectMapper();
            var node = mapper.readTree(response);
            System.out.println("REQUEST: successful");
            System.out.println("TABLE: categories");
            System.out.println("RECORDS_RETURNED: " + (node.isArray() ? node.size() : 0));
        } catch (Exception e) {
            System.out.println("REQUEST: failed");
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
