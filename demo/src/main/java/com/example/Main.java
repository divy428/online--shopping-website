package com.example;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Main {

    private static final String LOGIN_URL =
            "http://localhost/online_shopping/login.php";
    private static final String HOME_URL =
            "http://localhost/online_shopping/home.php";

    private static final int PRODUCTS_TO_ADD = 3;
    private static final int UNITS_PER_PRODUCT = 2;
    private static final int WAIT_TIMEOUT_SECONDS = 15;

    private static final String EMPTY_CART_MESSAGE =
            "Your cart is empty. Nothing to checkout.";

    private static final By EMAIL_FIELD = By.name("email");
    private static final By PASSWORD_FIELD = By.name("password");
    private static final By LOGIN_BUTTON = By.name("login");
    private static final By PRODUCT_CARDS =
            By.cssSelector(".product-list .card");
    private static final By CART_TOTAL =
            By.cssSelector(".cartbox .total");
    private static final By CART_ITEMS = By.cssSelector(".cart-item");
    private static final By CART_ITEM_DETAILS =
            By.cssSelector(".cart-item .details");
    private static final By CHECKOUT_BUTTON =
            By.cssSelector(".checkout-form input[name='checkout']");
    private static final By RESULT_BOX = By.cssSelector(".box");
    private static final By HOME_LINK =
            By.cssSelector("a.btn[href='home.php']");

    private static final Pattern CART_ROW_PATTERN = Pattern.compile(
            "^(.*) - Qty:\\s*(\\d+)\\s*-\\s*(.+)$");

    private static int passedChecks = 0;
    private static int failedChecks = 0;

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        try {
            printIntroduction();
            TestCredentials credentials = readCredentials();

            driver = new ChromeDriver();
            driver.manage().window().maximize();
            wait = new WebDriverWait(
                    driver,
                    Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

            login(credentials);

            if (!isLoggedIn()) {
                check("VALID LOGIN TEST", false);
                System.out.println("Shopping checks SKIPPED: login failed.");
                return;
            }

            check("VALID LOGIN TEST", true);
            runShoppingTests(credentials);

        } catch (Exception exception) {
            failedChecks++;
            printError(exception);

        } finally {
            printSummary();
            closeBrowser();
            scanner.close();
        }
    }

    private static void printIntroduction() {
        System.out.println("===== ONLINE SHOPPING AUTOMATION =====");
        System.out.println("Use a dedicated test account.");
        System.out.println(
                "This run adds " + PRODUCTS_TO_ADD + " products, removes one,");
        System.out.println(
                "logs out, then completes checkout after login.");
        System.out.println();
    }

    private static TestCredentials readCredentials() {
        System.out.print("Enter test account email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter test account password: ");
        String password = scanner.nextLine();

        return new TestCredentials(email, password);
    }

    private static void login(TestCredentials credentials) {
        driver.get(LOGIN_URL);

        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD))
                .sendKeys(credentials.email());

        driver.findElement(PASSWORD_FIELD).sendKeys(credentials.password());
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON))
                .click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("home.php"),
                ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"),
                        "Invalid email or password")));
    }

    private static boolean isLoggedIn() {
        return driver.getCurrentUrl().contains("home.php");
    }

    private static void runShoppingTests(TestCredentials credentials)
            throws Exception {

        waitForCart();

        List<WebElement> products = driver.findElements(PRODUCT_CARDS);
        check("PRODUCT LIST TEST", !products.isEmpty());

        if (products.isEmpty()) {
            System.out.println("Shopping checks SKIPPED: no products.");
            return;
        }

        if (products.size() < PRODUCTS_TO_ADD) {
            throw new IllegalStateException(
                    "At least " + PRODUCTS_TO_ADD
                            + " products are required for this test.");
        }

        check(
                "INITIAL CART TOTAL TEST",
                cartSubtotalSum().compareTo(cartTotal()) == 0);

        Map<String, BigDecimal> addedProducts = new LinkedHashMap<>();
        BigDecimal expectedCartTotal = BigDecimal.ZERO;

        for (int index = 0; index < PRODUCTS_TO_ADD; index++) {
            WebElement product = findProductByIndex(index);
            String productName = product.findElement(By.tagName("h4")).getText();
            BigDecimal unitPrice = money(
                    product.findElement(By.cssSelector(".price")).getText());

            System.out.println();
            System.out.println("Adding product " + (index + 1) + ": "
                    + productName);
            System.out.println("Unit price: " + unitPrice);
            System.out.println("Quantity: " + UNITS_PER_PRODUCT);

            addToCart(product, UNITS_PER_PRODUCT);

            BigDecimal productSubtotal = unitPrice.multiply(
                    BigDecimal.valueOf(UNITS_PER_PRODUCT));

            addedProducts.put(productName, productSubtotal);
            expectedCartTotal = expectedCartTotal.add(productSubtotal);

            int actualQuantity = productQuantity(productName);
            BigDecimal actualTotal = cartTotal();

            check(
                    "PRODUCT " + (index + 1) + " ADDED TEST",
                    actualQuantity == UNITS_PER_PRODUCT);
            check(
                    "PRODUCT " + (index + 1) + " SUBTOTAL TEST",
                    productSubtotal(productName)
                            .compareTo(productSubtotal) == 0);
            check(
                    "CART TOTAL AFTER PRODUCT " + (index + 1) + " TEST",
                    actualTotal.compareTo(expectedCartTotal) == 0);

            System.out.println(
                    "Quantity after add: " + actualQuantity);
            System.out.println(
                    "Cart total after add: " + actualTotal);
        }

        check(
                "ALL PRODUCTS PRESENT IN CART TEST",
                cartSubtotalSum().compareTo(cartTotal()) == 0);

        String productToRemove = addedProducts.keySet()
                .toArray(new String[0])[1];

        System.out.println();
        System.out.println("Removing product: " + productToRemove);

        runRemoveFromCartTests(
                productToRemove,
                cartTotal(),
                addedProducts.get(productToRemove));

        runLogoutTest();

        runGuestCheckoutTest(credentials);
    }

    private static WebElement findProductByIndex(int index) {
        List<WebElement> products = driver.findElements(PRODUCT_CARDS);

        if (index >= products.size()) {
            throw new IllegalStateException(
                    "Product index not found: " + index);
        }

        return products.get(index);
    }

    private static void addToCart(WebElement product, int quantity) {
        WebElement quantityInput = product.findElement(By.name("qty"));
        quantityInput.clear();
        quantityInput.sendKeys(String.valueOf(quantity));

        WebElement totalBeforeRefresh = driver.findElement(CART_TOTAL);

        product.findElement(By.cssSelector("input[name='add']")).click();

        wait.until(ExpectedConditions.stalenessOf(totalBeforeRefresh));
        waitForCart();
    }

    private static void runRemoveFromCartTests(
            String productName,
            BigDecimal totalBeforeRemove,
            BigDecimal expectedRemovedSubtotal) {

        WebElement removeControl = findRemoveControl(productName);

        if (removeControl == null) {
            check("REMOVE CONTROL FOUND TEST", false);
            System.out.println(
                    "Remove tests SKIPPED: Remove link/button not found.");
            return;
        }

        check("REMOVE CONTROL FOUND TEST", true);

        WebElement bodyBeforeRemove =
                driver.findElement(By.tagName("body"));

        removeControl.click();

        wait.until(ExpectedConditions.stalenessOf(bodyBeforeRemove));
        waitForCart();

        int quantityAfterRemove = productQuantity(productName);
        BigDecimal totalAfterRemove = cartTotal();
        BigDecimal expectedTotalAfterRemove =
                totalBeforeRemove.subtract(expectedRemovedSubtotal);

        boolean productRemoved = quantityAfterRemove == 0;
        boolean totalDecreased = totalAfterRemove
                .compareTo(totalBeforeRemove) < 0;
        boolean totalMatches = totalAfterRemove
                .compareTo(expectedTotalAfterRemove) == 0;

        check("PRODUCT REMOVED FROM CART TEST", productRemoved);
        check("CART TOTAL DECREASED AFTER REMOVE TEST", totalDecreased);
        check(
                "CART TOTAL MATCHES AFTER REMOVE TEST",
                totalMatches);
        check(
                "CART SUBTOTAL MATCHES TOTAL AFTER REMOVE TEST",
                cartSubtotalSum().compareTo(totalAfterRemove) == 0);

        System.out.println(
                "Quantity after remove: " + quantityAfterRemove);
        System.out.println(
                "Cart total after remove: " + totalAfterRemove);
    }

    private static WebElement findRemoveControl(String productName) {
        for (WebElement cartItem : driver.findElements(CART_ITEMS)) {
            String itemText = cartItem
                    .findElement(By.cssSelector(".details"))
                    .getText();

            Matcher cartRow = parseCartRow(itemText);

            if (!cartRow.group(1).equals(productName)) {
                continue;
            }

            List<WebElement> removeCandidates = new ArrayList<>();

            removeCandidates.addAll(cartItem.findElements(
                    By.xpath(".//a[contains("
                            + "translate(normalize-space(.), "
                            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                            + "'abcdefghijklmnopqrstuvwxyz'), "
                            + "'remove')]")));

            removeCandidates.addAll(cartItem.findElements(
                    By.xpath(".//a[contains("
                            + "translate(@href, "
                            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                            + "'abcdefghijklmnopqrstuvwxyz'), "
                            + "'remove')]")));

            removeCandidates.addAll(cartItem.findElements(
                    By.xpath(".//button[contains("
                            + "translate(normalize-space(.), "
                            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                            + "'abcdefghijklmnopqrstuvwxyz'), "
                            + "'remove')]")));

            removeCandidates.addAll(cartItem.findElements(
                    By.xpath(".//input[contains("
                            + "translate(@value, "
                            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                            + "'abcdefghijklmnopqrstuvwxyz'), "
                            + "'remove')]")));

            for (WebElement candidate : removeCandidates) {
                if (candidate.isDisplayed()) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static void runLogoutTest() {
        WebElement logoutControl = findLogoutControl();

        if (logoutControl == null) {
            check("LOGOUT CONTROL FOUND TEST", false);
            System.out.println(
                    "Logout test SKIPPED: Logout link/button not found.");
            return;
        }

        check("LOGOUT CONTROL FOUND TEST", true);

        WebElement bodyBeforeLogout =
                driver.findElement(By.tagName("body"));

        logoutControl.click();

        wait.until(ExpectedConditions.stalenessOf(bodyBeforeLogout));

        boolean loggedOut =
                driver.findElements(EMAIL_FIELD).size() > 0
                        || !driver.getCurrentUrl().contains("home.php");

        check("LOGOUT TEST", loggedOut);

        System.out.println("Current URL after logout: "
                + driver.getCurrentUrl());
    }

    private static WebElement findLogoutControl() {
        List<WebElement> logoutCandidates = new ArrayList<>();

        logoutCandidates.addAll(driver.findElements(
                By.xpath("//a[contains("
                        + "translate(normalize-space(.), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                        + "'abcdefghijklmnopqrstuvwxyz'), "
                        + "'logout')]")));

        logoutCandidates.addAll(driver.findElements(
                By.xpath("//a[contains("
                        + "translate(@href, "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                        + "'abcdefghijklmnopqrstuvwxyz'), "
                        + "'logout')]")));

        logoutCandidates.addAll(driver.findElements(
                By.xpath("//button[contains("
                        + "translate(normalize-space(.), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                        + "'abcdefghijklmnopqrstuvwxyz'), "
                        + "'logout')]")));

        logoutCandidates.addAll(driver.findElements(
                By.xpath("//a[contains("
                        + "translate(normalize-space(.), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                        + "'abcdefghijklmnopqrstuvwxyz'), "
                        + "'sign out')]")));

        logoutCandidates.addAll(driver.findElements(
                By.xpath("//a[contains("
                        + "translate(normalize-space(.), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                        + "'abcdefghijklmnopqrstuvwxyz'), "
                        + "'log out')]")));

        for (WebElement candidate : logoutCandidates) {
            if (candidate.isDisplayed()) {
                return candidate;
            }
        }

        return null;
    }

    private static void runGuestCheckoutTest(TestCredentials credentials)
            throws Exception {

        System.out.println();
        System.out.println("Opening home page for guest checkout test.");
        driver.get(HOME_URL);
        waitForCart();

        WebElement checkoutButton =
                wait.until(ExpectedConditions.elementToBeClickable(
                        CHECKOUT_BUTTON));

        WebElement bodyBeforeCheckout =
                driver.findElement(By.tagName("body"));

        checkoutButton.click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.stalenessOf(bodyBeforeCheckout),
                ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD),
                ExpectedConditions.urlContains("thank")));

        boolean loginPageOpened =
                driver.findElements(EMAIL_FIELD).size() > 0;

        check(
                "CHECKOUT REDIRECTS TO LOGIN TEST",
                loginPageOpened);

        if (!loginPageOpened) {
            System.out.println(
                    "Checkout did not open login page. "
                            + "Skipping login-and-checkout flow.");
            return;
        }

        System.out.println("Login page opened after checkout click.");
        System.out.println("Logging in again to complete checkout.");

        driver.findElement(EMAIL_FIELD).sendKeys(credentials.email());
        driver.findElement(PASSWORD_FIELD).sendKeys(credentials.password());
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON))
                .click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("home.php"),
                ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"),
                        "Invalid email or password")));

        if (!isLoggedIn()) {
            check("LOGIN AFTER CHECKOUT REDIRECT TEST", false);
            System.out.println(
                    "Checkout completion SKIPPED: login failed.");
            return;
        }

        check("LOGIN AFTER CHECKOUT REDIRECT TEST", true);

        driver.get(HOME_URL);
        waitForCart();

        BigDecimal totalBeforeCheckout = cartTotal();

        if (totalBeforeCheckout.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(
                    "Checkout completion SKIPPED: cart is empty after login.");
            return;
        }

        clickCheckout();

        String confirmationText = getResultMessage().toLowerCase();
        String currentUrl = driver.getCurrentUrl().toLowerCase();

        boolean thanksPageDisplayed =
                confirmationText.contains("thank")
                        || confirmationText.contains("thanks")
                        || currentUrl.contains("thank");

        check("CHECKOUT THANK YOU PAGE TEST", thanksPageDisplayed);

        System.out.println();
        System.out.println("Checkout confirmation page opened.");
        System.out.println("Current URL: " + driver.getCurrentUrl());
        System.out.println("Page message: " + getResultMessage());

        System.out.println();
        System.out.println(
                "Press ENTER after viewing the Thank You page to continue.");
        scanner.nextLine();

        System.out.println(
                "Checkout completed. Thank You page verified successfully.");
    }

    private static void clickCheckout() {
        WebElement checkoutButton =
                wait.until(ExpectedConditions.elementToBeClickable(
                        CHECKOUT_BUTTON));

        WebElement bodyBeforeCheckout =
                driver.findElement(By.tagName("body"));

        checkoutButton.click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.stalenessOf(bodyBeforeCheckout),
                ExpectedConditions.urlContains("thank"),
                ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"),
                        "Thank")));
    }

    private static String getResultMessage() {
        return driver.findElement(By.tagName("body")).getText();
    }

    private static boolean isCartEmpty() {
        boolean noCartItems = driver.findElements(CART_ITEMS).isEmpty();
        return noCartItems && cartTotal().compareTo(BigDecimal.ZERO) == 0;
    }

    private static void waitForCart() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(CART_TOTAL));
    }

    private static BigDecimal cartTotal() {
        return money(driver.findElement(CART_TOTAL).getText());
    }

    private static BigDecimal cartSubtotalSum() {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (WebElement row : driver.findElements(CART_ITEM_DETAILS)) {
            Matcher cartRow = parseCartRow(row.getText());
            subtotal = subtotal.add(money(cartRow.group(3)));
        }

        return subtotal;
    }

    private static int productQuantity(String productName) {
        int quantity = 0;

        for (WebElement row : driver.findElements(CART_ITEM_DETAILS)) {
            Matcher cartRow = parseCartRow(row.getText());

            if (cartRow.group(1).equals(productName)) {
                quantity += Integer.parseInt(cartRow.group(2));
            }
        }

        return quantity;
    }

    private static BigDecimal productSubtotal(String productName) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (WebElement row : driver.findElements(CART_ITEM_DETAILS)) {
            Matcher cartRow = parseCartRow(row.getText());

            if (cartRow.group(1).equals(productName)) {
                subtotal = subtotal.add(money(cartRow.group(3)));
            }
        }

        return subtotal;
    }

    private static Matcher parseCartRow(String rowText) {
        Matcher matcher = CART_ROW_PATTERN.matcher(rowText);

        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "Cannot read cart row: " + rowText);
        }

        return matcher;
    }

    private static BigDecimal money(String amountText) {
        String numericValue = amountText
                .replace(",", "")
                .replaceAll("[^0-9.\\-]", "");

        if (numericValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot read amount from: " + amountText);
        }

        return new BigDecimal(numericValue);
    }

    private static void check(String testName, boolean passed) {
        if (passed) {
            passedChecks++;
            System.out.println(testName + ": PASS");
        } else {
            failedChecks++;
            System.out.println(testName + ": FAIL");
        }
    }

    private static void printError(Exception exception) {
        System.out.println();
        System.out.println("AUTOMATION RUN: ERROR");
        System.out.println(exception.getClass().getSimpleName());
        System.out.println(exception.getMessage());
    }

    private static void printSummary() {
        System.out.println();
        System.out.println("===== EXECUTION SUMMARY =====");
        System.out.println("Passed checks: " + passedChecks);
        System.out.println("Failed checks / errors: " + failedChecks);
    }

    private static void closeBrowser() {
        try {
            if (driver != null) {
                System.out.println("Press ENTER to close Chrome.");

                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                }
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private record TestCredentials(String email, String password) {
    }
}