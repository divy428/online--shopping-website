
package com.example;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class InvalidLoginTest {

    public static void main(String[] args) {

        WebDriver driver = new ChromeDriver();

        try {
            driver.manage().window().maximize();

            WebDriverWait wait = new WebDriverWait(
                    driver, Duration.ofSeconds(10));

            driver.get("http://localhost/online_shopping/login.php");

            WebElement emailField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.name("email")));

            WebElement passwordField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.name("password")));

            emailField.sendKeys("a@gmail.com");
            passwordField.sendKeys("WrongPassword123");

            driver.findElement(By.name("login")).click();

            wait.until(ExpectedConditions.textToBePresentInElementLocated(
                    By.tagName("body"),
                    "Invalid email or password"));

            if (!driver.getCurrentUrl().contains("home.php")) {
                System.out.println("INVALID LOGIN TEST: PASS");
                System.out.println("Incorrect password was rejected.");
            } else {
                System.out.println("INVALID LOGIN TEST: FAIL");
            }

        } catch (Exception e) {
            System.out.println("INVALID LOGIN TEST: FAIL");
            System.out.println(e.getMessage());

        } finally {
            driver.quit();
        }
    }
}

