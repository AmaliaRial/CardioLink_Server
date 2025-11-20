package thread;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class tryHash {
    private static int workload = 12;

    public static void main(String[] args) {
        String password = "hola";
        System.out.println(password);
        String hashedP = hashPassword(password);
        System.out.println(hashedP);
        boolean check = checkPassword(password, hashedP);
        System.out.println(check);

        String password2 = "hola";
        System.out.println(password2);
        String hashedP2 = hashPassword(password2);
        System.out.println(hashedP2);
        boolean check2 = checkPassword(password, hashedP2);
        System.out.println(check2);
    }

    public static String hashPassword(String password_plaintext) {
        String salt = BCrypt.gensalt(workload);
        String hashed_password = BCrypt.hashpw(password_plaintext, salt);

        return(hashed_password);
    }

    public static boolean checkPassword(String password_plaintext, String stored_hash) {
        boolean password_verified = false;

        if (null == stored_hash || !stored_hash.startsWith("$2a$"))
            throw new IllegalArgumentException("Invalid hash provided for comparison");

        password_verified = BCrypt.checkpw(password_plaintext, stored_hash);

        return (password_verified);
    }
}
