package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class EmptyLoginTest {
    public static void main(String[] args) {
        WebDriver driver = null;

        try {
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.get("http://localhost/online_shopping/login.php");

            WebElement email = driver.findElement(By.name("email"));
            WebElement password = driver.findElement(By.name("password"));
            WebElement loginButton = driver.findElement(By.name("login"));

            email.clear();
            password.clear();
            loginButton.click();

            boolean emailRequired = driver.findElement(By.name("email"))
                    .getAttribute("required") != null;
            boolean passwordRequired = driver.findElement(By.name("password"))
                    .getAttribute("required") != null;

            if (emailRequired && passwordRequired) {
                System.out.println("EMPTY LOGIN TEST: PASS");
                System.out.println("Email and password fields are required.");
            } else {
                System.out.println("EMPTY LOGIN TEST: CHECK REQUIRED FIELDS");
            }

        } catch (Exception e) {
            System.out.println("EMPTY LOGIN TEST: ERROR");
            System.out.println(e.getMessage());
        } finally {
            if (driver != null) {
                System.out.println("Press ENTER in the terminal to close Chrome.");
                try {
                    System.in.read();
                } catch (Exception ignored) {
                }
                driver.quit();
            }
        }
    }
}