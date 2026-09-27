import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.User;
import com.schel.repository.UserRepository;

public class AuthProbe {
    public static void main(String[] args) throws Exception {
        String email = "authrealtest." + System.currentTimeMillis() + "@example.com";
        String username = "realauthuser" + System.currentTimeMillis();
        AuthController controller = AuthController.getInstance();

        User created = controller.register(username, email, "Password123");
        System.out.println("REGISTRATION_OK: " + created.getEmail());

        User byEmail = UserRepository.getInstance().findByEmail(email);
        System.out.println("USER_FOUND_BY_EMAIL: " + (byEmail != null && email.equalsIgnoreCase(byEmail.getEmail())));

        try {
            controller.login(email, "WrongPassword123");
            System.out.println("INVALID_LOGIN: unexpected success");
        } catch (AuthenticationException e) {
            System.out.println("INVALID_LOGIN: rejected");
        }

        try {
            controller.register(username, email, "Password456");
            System.out.println("DUPLICATE: unexpected success");
        } catch (AuthenticationException e) {
            System.out.println("DUPLICATE: rejected -> " + e.getMessage());
        }

        User loggedIn = controller.login(email, "Password123");
        System.out.println("LOGIN_OK: " + loggedIn.getEmail());
        System.out.println("SESSION_AUTHENTICATED: " + controller.isAuthenticated());
    }
}
