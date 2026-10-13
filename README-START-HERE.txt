MARUDHAM SMART CITY WASTE COLLECTION OPTIMIZATION - WEB VERSION

REQUIREMENTS
- JDK 17 or newer
- MySQL Server (XAMPP MySQL or standalone MySQL)
- Maven 3.8+ (or use an IDE with Maven support)
- Internet for the first Maven build to download Spring Boot dependencies

STEP 1 - DATABASE
1. Start MySQL in XAMPP, or start your MySQL service.
2. Open phpMyAdmin (usually http://localhost/phpmyadmin) or MySQL Workbench.
3. Run database.sql. This creates the marudham_waste database.
4. Open src/main/resources/application.properties.
5. Set spring.datasource.username and spring.datasource.password to your MySQL credentials.
   XAMPP defaults are often username=root and an empty password; change them if yours differ.

STEP 2 - RUN THE WEBSITE
1. Extract the ZIP to a folder, for example C:\MarudhamWasteWeb.
2. Open a terminal/Command Prompt in the extracted MarudhamWasteWeb folder (the folder containing pom.xml).
3. Run: mvn spring-boot:run
4. Wait until the console says the application has started.
5. Open http://localhost:8080 in Chrome.

ALTERNATIVE IN INTELLIJ / ECLIPSE
Import the folder as an existing Maven project, ensure JDK 17 is selected, then run
com.marudham.web.MarudhamWasteWebApplication. Open http://localhost:8080.

FEATURES IN THIS FIRST WEB VERSION
- Dashboard totals, live collection-point table and waste level editor
- Add collection points
- Submit High / Medium / Low priority collection requests
- Prevent duplicate open requests for the same point
- Mark a request completed and subtract its recorded waste from the point
- MySQL persistence through Spring Data JPA
- Sample points are seeded on first startup only when the points table is empty

IMPORTANT
- Existing console Java source is included in original-console-src/ for reference. The web app is a separate Spring Boot web layer and does not yet invoke every route-planning/vehicle simulation class from the original console program.
- Do not open index.html directly from File Explorer; use http://localhost:8080 so the API and database work.
- If Maven is not installed, install Apache Maven or run the project from IntelliJ IDEA with Maven support.
- If you see Access denied for user 'root', correct the username/password in application.properties.
- If port 8080 is busy, change server.port=8081 and open http://localhost:8081.
