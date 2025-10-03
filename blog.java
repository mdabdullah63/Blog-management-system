package org.example;

import java.sql.*;
import java.util.Scanner;

class User {
    String uid, password, role;
    User(String uid, String password, String role) {
        this.uid = uid;
        this.password = password;
        this.role = role;
    }
}

class Post {
    int id;
    String title, content, author;
    Post(int id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }
}

public class blog {
    static Scanner sc = new Scanner(System.in);
    static Connection conn;
    static User loggedInUser = null;

    public static void main(String[] args) {
        try {
            // JDBC connection details - replace user and password with your credentials
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BlogManagement", "root", "root");
            while (true) {
                showUserPanel();
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed:");
            e.printStackTrace();
            return;
        }
    }

    static void showUserPanel() {
        System.out.println("***********************************************");
        System.out.println("               BLOG MANAGEMENT                 ");
        System.out.println("***********************************************");
        System.out.println("USER Panel :");
        System.out.println("1) Sign Up        2) Sign In        *) Admin");
        System.out.println("-----------------------------------------------");
        System.out.print("Select your account first : ");
        String choice = sc.nextLine();
        if (choice.equals("1")) signUp();
        else if (choice.equals("2")) signIn();
        else if (choice.equals("*")) adminPanel();
        else System.out.println("Invalid Choice!");
    }

    static void signUp() {
        try {
            System.out.print("Enter your UiD: ");
            String uid = sc.nextLine();
            System.out.print("Enter your password: ");
            String password = sc.nextLine();
            System.out.print("Enter your role (user/admin): ");
            String role = sc.nextLine();

            String sql = "INSERT INTO users (uid, password, role) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, uid);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();

            System.out.println("User registered successfully!");
        } catch (SQLException e) {
            System.out.println("Error during sign up:");
            e.printStackTrace();
        }
    }

    static void signIn() {
        try {
            System.out.print("Enter your UiD: ");
            String uid = sc.nextLine();
            System.out.print("Enter your password: ");
            String password = sc.nextLine();

            String sql = "SELECT * FROM users WHERE uid = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, uid);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                loggedInUser = new User(rs.getString("uid"), rs.getString("password"), rs.getString("role"));
                System.out.println("Login successful. " + uid);
                if (loggedInUser.role.equals("user")) {
                    registeredUserPanel();
                } else if (loggedInUser.role.equals("admin")) {
                    adminPanel();
                }
            } else {
                System.out.println("Invalid credentials.");
            }
        } catch (SQLException e) {
            System.out.println("Error during sign in:");
            e.printStackTrace();
        }
    }

    static void adminPanel() {
        while (true) {
            System.out.println("Admin Panel:");
            System.out.println("1) Create Post       2) Read All Posts       3) View Post       4) Delete Post       5) Logout");
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    createPost();
                    break;
                case "2":
                    readAllPosts();
                    break;
                case "3":
                    viewPost();
                    break;
                case "4":
                    deletePost();
                    break;
                case "5":
                    loggedInUser = null;
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    static void registeredUserPanel() {
        while (true) {
            System.out.println("Registered User Panel:");
            System.out.println("1) Create Post   2) Read All Posts   3) View Post   4) Delete Post   5) Logout");
            System.out.print("Enter your choice: ");
            String ch = sc.nextLine();
            switch (ch) {
                case "1":
                    createPost();
                    break;
                case "2":
                    readAllPosts();
                    break;
                case "3":
                    viewPost();
                    break;
                case "4":
                    deletePost();
                    break;
                case "5":
                    loggedInUser = null;
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    static void createPost() {
        try {
            System.out.print("Title: ");
            String title = sc.nextLine();
            System.out.print("Content: ");
            String content = sc.nextLine();

            String sql = "INSERT INTO posts (title, content, author) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, loggedInUser.uid);
            ps.executeUpdate();

            System.out.println("Post created Successfully");
        } catch (SQLException e) {
            System.out.println("Error creating post:");
            e.printStackTrace();
        }
    }

    static void readAllPosts() {
        try {
            String sql = "SELECT * FROM posts ORDER BY id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next()) {
                System.out.println(i++ + ". " + rs.getString("title") + " by " + rs.getString("author"));
            }
        } catch (SQLException e) {
            System.out.println("Error reading posts:");
            e.printStackTrace();
        }
    }

    static void viewPost() {
        try {
            System.out.print("Enter post number to view: ");
            int num = Integer.parseInt(sc.nextLine());

            String sql = "SELECT * FROM posts ORDER BY id LIMIT 1 OFFSET ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, num - 1);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Title: " + rs.getString("title"));
                System.out.println("By: " + rs.getString("author"));
                System.out.println("Content: " + rs.getString("content"));
            } else {
                System.out.println("Invalid post number.");
            }
        } catch (SQLException e) {
            System.out.println("Error viewing post:");
            e.printStackTrace();
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input.");
        }
    }

    static void deletePost() {
        try {
            System.out.print("Enter post number to delete: ");
            int num = Integer.parseInt(sc.nextLine());

            // First retrieve post id and author by offset
            String selectSql = "SELECT id, author FROM posts ORDER BY id LIMIT 1 OFFSET ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setInt(1, num - 1);
            ResultSet rs = selectPs.executeQuery();

            if (rs.next()) {
                int postId = rs.getInt("id");
                String author = rs.getString("author");

                if (author.equals(loggedInUser.uid) || loggedInUser.role.equals("admin")) {
                    String deleteSql = "DELETE FROM posts WHERE id = ?";
                    PreparedStatement deletePs = conn.prepareStatement(deleteSql);
                    deletePs.setInt(1, postId);
                    int affected = deletePs.executeUpdate();
                    if (affected > 0) {
                        System.out.println("Post deleted.");
                    } else {
                        System.out.println("Failed to delete post.");
                    }
                } else {
                    System.out.println("Cannot delete others' posts.");
                }
            } else {
                System.out.println("Invalid post number.");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting post:");
            e.printStackTrace();
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input.");
        }
    }
}
