import com.schel.config.DatabaseConfig;
public class ConfigProbe {
    public static void main(String[] args) {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        System.out.println("CONFIGURED: " + cfg.isConfigured());
    }
}
