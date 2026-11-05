<div align="center">
  <img src="assets/homeease-cover.png" alt="HomeEase - trusted home help, on demand" width="100%">
</div>

<div align="center">

# HomeEase

### Trusted home help, on demand

Find and book everyday home services - from plumbers and maids to other local workers - in one place.

</div>

---

## Contents

- [About](#about)
- [Technology](#technology)
- [Getting started](#getting-started)
- [Run the application](#run-the-application)
- [Run tests](#run-tests)
- [Project structure](#project-structure)
- [Troubleshooting](#troubleshooting)

## About

HomeEase is a home-services booking application. Customers can explore services and connect with workers for household jobs; workers can manage their service activity through the application.

The web pages are rendered by Spring Boot with Thymeleaf. The HTML, CSS, and JavaScript frontend is served by the same application, so a separate frontend development server is not required.

## Technology

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.2.2 |
| Web pages | HTML, CSS, JavaScript, Thymeleaf |
| Database | MySQL |
| Build | Maven Wrapper |

## Getting started

### Requirements

- [JDK 21](https://adoptium.net/) (ensure `java` is available on your `PATH`)
- [MySQL Server](https://dev.mysql.com/downloads/mysql/)
- Git

Maven is not required separately; the project includes the Maven Wrapper.

### 1. Get the project

```powershell
git clone https://github.com/Nishi701/HomeEase_New.git
Set-Location HomeEase_New
```

### 2. Create the database

Start MySQL and create the database used by the application:

```sql
CREATE DATABASE homeease
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. Configure local settings

The application settings are in `src/main/resources/application.properties`. Set the MySQL connection URL, username, and password to match your local MySQL installation. The default application URL is `http://localhost:8039`.

For example, set connection details in the PowerShell window you will use to start the application:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/homeease"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "your-local-mysql-password"
```

Features that send email, sign in with Google, or process payments also need valid provider credentials. Configure those for your own development environment before trying the corresponding flows. Never commit passwords, API keys, or other credentials to source control.

## Run the application

From the project root, run:

```powershell
.\mvnw.cmd spring-boot:run
```

When startup completes, open **[http://localhost:8039](http://localhost:8039)** in your browser.

<details>
  <summary>Running on macOS or Linux</summary>

  From the project root, run:

  ```bash
  ./mvnw spring-boot:run
  ```

</details>

## Run tests

Run the test suite from the project root:

```powershell
.\mvnw.cmd test
```

On macOS or Linux, use `./mvnw test`.

## Project structure

```text
src/
|-- main/
|   |-- java/com/homeease/homeease/    # Spring Boot application, controllers, services, models
|   `-- resources/
|       |-- templates/                 # Thymeleaf HTML pages
|       |-- static/css/                # Stylesheets
|       |-- static/js/                 # Browser JavaScript
|       `-- application.properties     # Application and database configuration
`-- test/                              # Automated tests
assets/
`-- homeease-cover.png                 # README cover image
```

## Troubleshooting

<details>
  <summary>MySQL connection or access denied error</summary>

  Make sure MySQL is running, the `homeease` database exists, and the username and password supplied to the application are valid.

</details>

<details>
  <summary>Port 8039 is already in use</summary>

  Stop the other process using port `8039`, or change `server.port` in `src/main/resources/application.properties` and open the application at the updated port.

</details>

<details>
  <summary>Java version or Maven Wrapper error</summary>

  Confirm that JDK 21 is installed and that `java -version` reports Java 21. Run the wrapper command from the project root.

</details>
