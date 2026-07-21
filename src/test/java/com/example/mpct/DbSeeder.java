package com.example.mpct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DbSeeder {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-holy-cake-ajkmfnl5-pooler.c-3.us-east-2.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_slJFmM9P3ocI";

        try {
            System.out.println("Connecting to Neon DB...");
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected!");

            String sql = new String(Files.readAllBytes(Paths.get("C:\\\\Users\\\\Blae\\\\.gemini\\\\antigravity-cli\\\\brain\\\\52b12342-8b88-4ad3-9e13-13ef9e1aa020\\\\test_data_more.sql")), java.nio.charset.StandardCharsets.UTF_8).replace("\uFEFF", "");
            
            // Split by empty lines or semicolons might be dangerous if there's data containing them,
            // but the file should have blocks separated by semicolons.
            String[] statements = sql.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String s : statements) {
                    if (s.trim().isEmpty()) continue;
                    System.out.println("Executing:\n" + s.trim().substring(0, Math.min(s.trim().length(), 100)) + "...");
                    try {
                        stmt.execute(s.trim());
                        System.out.println("Success!");
                    } catch (Exception e) {
                        System.err.println("Error executing statement:");
                        e.printStackTrace();
                        // break early so we don't mess up state
                        return;
                    }
                }
            }
            
            conn.close();
            System.out.println("All Done!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


