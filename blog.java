package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// User model with encapsulation
class User {
    private String uid;
    private String password;
    private String role;

    public User(String uid, String password, String role) {
        this.uid = uid;
        this.password = password;
        this.role = role;
    }
    public String getUid() { return uid; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}

// Post model with encapsulation
class Post {
    private int id;
    private String title;
    private String content;
    private String author;

    public Post(int id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
}

// Data Access Object for Users
class UserDAO {
    private Connection conn;
    public UserDAO(Connection conn) { this.conn = conn; }

    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (uid, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUid());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
        }
    }

    public User getUserByCredentials(String uid, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE uid = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("uid"), rs.getString("password"), rs.getString("role"));
                }
            }
        }
        return null;
    }
}

// Data Access Object for Posts
class PostDAO {
    private Connection conn;
    public PostDAO(Connection conn) { this.conn = conn; }

    public void addPost(Post post) throws SQLException {
        String sql = "INSERT INTO posts (title, content, author) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            ps.setString(3, post.getAuthor());
            ps.executeUpdate();
        }
    }

    public List<Post> getAllPosts() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(new Post(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("author")
                ));
            }
        }
        return posts;
    }

    public Post getPostByOffset(int offset) throws SQLException {
        String sql = "SELECT * FROM posts ORDER BY id LIMIT 1 OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Post(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("author")
                    );
                }
            }
        }
        return null;
    }

    public boolean deletePostById(int id) throws SQLException {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }
}

// Service layer for business logic
class BlogService {
    private final UserDAO userDAO;
    private final PostDAO postDAO;
    private User loggedInUser;

    public BlogService(Connection conn) {
        userDAO = new UserDAO(conn);
        postDAO = new PostDAO(conn);
    }

    public void signUp(String uid, String password, String role) throws SQLException {
        userDAO.addUser(new User(uid, password, role));
    }

    public boolean signIn(String uid, String password) throws SQLException {
        User user = userDAO.getUserByCredentials(uid, password);
        if (user != null) {
            loggedInUser = user;
            return true;
        }
        return false;
    }

    public User getLoggedInUser() { return loggedInUser; }

    public void logout() { loggedInUser = null; }

    public void createPost(String title, String content) throws SQLException {
        if (loggedInUser != null) {
            postDAO.addPost(new Post(0, title, content, loggedInUser.getUid()));
        }
    }

    public List<Post> getAllPosts() throws SQLException {
        return postDAO.getAllPosts();
    }

    public Post getPostByNumber(int postNumber) throws SQLException {
        return postDAO.getPostByOffset(postNumber - 1);
    }

    public boolean deletePost(int postNumber) throws SQLException {
        Post post = postDAO.getPostByOffset(postNumber - 1);
        if (post == null) return false;

        if (post.getAuthor().equals(loggedInUser.getUid()) || loggedInUser.getRole().equals("admin")) {
            return postDAO.deletePostById(post.getId());
        }
        return false;
    }
}

// UI and application entrypoint, interacts with service layer only
public class blog {
    private static Scanner sc = new Scanner(System.in);
    private static Connection conn;
    private static BlogService service;

    public static void main(String[] args) {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BlogManagement", "root", "root");
            service = new BlogService(conn);

            while(true) {
                showUserPanel();
            }
        } catch(SQLException e) {
            System.out.println("Database connection failed:");
            e.printStackTrace();
        }
    }

    private static void showUserPanel() throws SQLException {
        System.out.println("***********************************************");
        System.out.println("               BLOG MANAGEMENT                 ");
        System.out.println("***********************************************");
        System.out.println("USER Panel :");
        System.out.println("1) Sign Up        2) Sign In        *) Admin");
        System.out.println("-----------------------------------------------");
        System.out.print("Select your account first : ");
        String choice = sc.nextLine();
        switch(choice) {
            case "1": signUp(); break;
            case "2": signIn(); break;
            case "*": adminPanel(); break;
            default: System.out.println("Invalid Choice!"); break;
        }
    }

    private static void signUp() throws SQLException {
        System.out.print("Enter your UiD: ");
        String uid = sc.nextLine();
        System.out.print("Enter your password: ");
        String password = sc.nextLine();
        System.out.print("Enter your role (user/admin): ");
        String role = sc.nextLine();

        service.signUp(uid, password, role);
        System.out.println("User registered successfully!");
    }

    private static void signIn() throws SQLException {
        System.out.print("Enter your UiD: ");
        String uid = sc.nextLine();
        System.out.print("Enter your password: ");
        String password = sc.nextLine();

        if (service.signIn(uid, password)) {
            System.out.println("Login successful. " + uid);
            User user = service.getLoggedInUser();
            if ("user".equals(user.getRole())) {
                registeredUserPanel();
            } else if ("admin".equals(user.getRole())) {
                adminPanel();
            }
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private static void adminPanel() throws SQLException {
        while(true) {
            System.out.println("Admin Panel:");
            System.out.println("1) Create Post   2) Read All Posts   3) View Post   4) Delete Post   5) Logout");
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();
            switch(choice) {
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
                    service.logout();
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void registeredUserPanel() throws SQLException {
        while(true) {
            System.out.println("Registered User Panel:");
            System.out.println("1) Create Post   2) Read All Posts   3) View Post   4) Delete Post   5) Logout");
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();
            switch(choice) {
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
                    service.logout();
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void createPost() throws SQLException {
        System.out.print("Title: ");
        String title = sc.nextLine();
        System.out.print("Content: ");
        String content = sc.nextLine();

        service.createPost(title, content);
        System.out.println("Post created Successfully");
    }

    private static void readAllPosts() throws SQLException {
        var posts = service.getAllPosts();
        int i = 1;
        for(Post p : posts) {
            System.out.println(i++ + ". " + p.getTitle() + " by " + p.getAuthor());
        }
    }

    private static void viewPost() throws SQLException {
        System.out.print("Enter post number to view: ");
        int num;
        try {
            num = Integer.parseInt(sc.nextLine());
        } catch(NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }
        Post p = service.getPostByNumber(num);
        if (p != null) {
            System.out.println("Title: " + p.getTitle());
            System.out.println("By: " + p.getAuthor());
            System.out.println("Content: " + p.getContent());
        } else {
            System.out.println("Invalid post number.");
        }
    }

    private static void deletePost() throws SQLException {
        System.out.print("Enter post number to delete: ");
        int num;
        try {
            num = Integer.parseInt(sc.nextLine());
        } catch(NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }
        boolean deleted = service.deletePost(num);
        System.out.println(deleted ? "Post deleted." : "Cannot delete post (may not exist or insufficient permissions).");
    }
}
