package org.to.telegramfinalproject.Server;

import java.util.UUID;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.User;
import org.to.telegramfinalproject.Security.PasswordHashing;

public class AuthService {
    private final userDatabase userDb = new userDatabase();

    public AuthService() {
    }

    public boolean register(String user_id, String username, String password, String profile_name) {
        if (!this.userDb.existsByUsername(username) && !this.userDb.existsByUserId(user_id)) {
            String passwordRegex = "\\b(?=[^\\s]*[A-Z])(?=[^\\s]*[a-z])(?=[^\\s]*\\d)(?=[^\\s]*[!@#$%^&*])[^\\s]{8,}\\b";
            if (!password.matches(passwordRegex)) {
                System.out.println("Password doesn't Valid(At list one capital and one special char(!@#$%^&*), minimum 8 char ");
                return false;
            } else {
                UUID uuid = UUID.randomUUID();
                password = PasswordHashing.hash(password);
                User user = new User(user_id, uuid, username, password, profile_name);
                return this.userDb.save(user);
            }
        } else {
            System.out.println("Username/ user id is already taken");
            return false;
        }
    }

    public User login(String username, String password) {
        User user = this.userDb.findByUsername(username);
        if (user == null) {
            System.out.println("User not found.");
            return null;
        } else if (!PasswordHashing.verify(password, user.getPassword())) {
            System.out.println("Incorrect password");
            return null;
        } else {
            return user;
        }
    }

    public boolean loginCheck(String username , String password){
        User user = userDb.findByUsername(username);
        String pass = user.getPassword();
        password = PasswordHashing.hash(password);
        String Username = user.getUsername();
        boolean login = false;
        if(user!= null && Username.equals(username)&& pass.equals(password)){
            login = true;
        }
        return login;

    }
}


