# Online Shopping Automation

Selenium WebDriver-based automation for an online shopping website.

It tests:
- Valid login
- Product listing
- Add to cart
- Cart quantity, subtotal, and total
- Checkout success
- Cart empty after checkout
- Empty-cart checkout handling

## Tech Stack

- Java
- Selenium WebDriver
- Maven
- ChromeDriver
- JUnit-style assertion checks in a console-based test runner

## Project Structure

```text
demo/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── Main.java
```

## Setup

1. Install Java 17 or later.
2. Install Google Chrome.
3. Update ChromeDriver path in `Main.java` if required.
4. Open Terminal in the `demo` folder.

## Run the Project

```powershell
mvn compile
mvn exec:java -Dexec.mainClass=com.example.Main
```

If `mvn` is not in PATH, use the full Maven path:

```powershell
& "C:\path\to\maven\bin\mvn.cmd" compile
& "C:\path\to\maven\bin\mvn.cmd" "-Dexec.mainClass=com.example.Main" exec:java
```

## Test Account

Use a dedicated test account only.  
The automation adds products to the cart and checkout creates a real order on the test account.

## Current Result

text
Passed checks: 11
Failed checks / errors: 0
