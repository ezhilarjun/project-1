# Smart City Waste Optimization System

A web-based waste collection management system for a smart city. It tracks waste levels at collection points, lets citizens request pickups, and gives administrators tools to plan truck routes on a map, follow truck locations, and re-plan automatically when a truck breaks down.

**Tech stack:** Java 17+ · Spring Boot 3 · Spring Data JPA (Hibernate) · MySQL (XAMPP) · HTML / CSS / JavaScript · Leaflet + OpenStreetMap

---

## Table of contents

1. [Features](#1-features)
2. [Architecture](#2-architecture)
3. [Project structure](#3-project-structure)
4. [Prerequisites](#4-prerequisites)
5. [Setup and run](#5-setup-and-run)
6. [Sites and pages](#6-sites-and-pages)
7. [How the optimization works](#7-how-the-optimization-works)
8. [Network simulation engine](#8-network-simulation-engine)
9. [REST API reference](#9-rest-api-reference)
10. [Database](#10-database)
11. [Access control (admin vs user)](#11-access-control-admin-vs-user)
12. [Business rules and validation](#12-business-rules-and-validation)
13. [Demo walkthrough](#13-demo-walkthrough)
14. [Troubleshooting](#14-troubleshooting)
15. [Known limitations](#15-known-limitations)
16. [Possible next steps](#16-possible-next-steps)

---

## 1. Features

**Citizen (user) site**
- Request a waste pickup for a collection point with a priority (High / Medium / Low).
- See the status of requests (Pending / Completed).

**Admin site**
- **Dashboard:** live counts of collection points, total waste (kg), pending and completed requests, high-priority requests, and a request queue with **Mark complete**.
- **Collection Points:** view waste levels with a fill bar, edit the waste level, add new points.
- **Optimizer:** map with depot, collection points and trucks; assigns pending requests to trucks within capacity (High priority first); orders stops by shortest distance; draws road routes with distance and time; **auto re-optimizes when a truck breaks down**.
- **Network Simulation:** a graph-based city simulation with Dijkstra shortest paths, road blocking, vehicle breakdown and reassignment, and an event history.

**Driver console**
- Pick a truck, share its location with live GPS (or simulate driving), report a breakdown, and mark it back in service.

---

## 2. Architecture

```
 Chrome ── admin.html / user.html / driver.html / simulation.html
              │  fetch('/api/...')
              ▼
 Spring Boot (one app, two ports)
   ├─ Port 8080 = ADMIN site      ┐  PortAccessFilter decides what
   └─ Port 8081 = USER site       ┘  each port is allowed to do
              │
              ├─ ApiController     points, requests, dashboard   ──► MySQL (XAMPP)
              ├─ TruckController   trucks, locations, breakdown  ──► MySQL (XAMPP)
              └─ SimController     in-memory simulation engine   (package "marudham", no database)
```

- The front end is plain HTML/CSS/JavaScript served by Spring Boot from `static/`.
- If the backend cannot be reached, `admin.html` and `user.html` fall back to **demo mode**, which stores data in the browser. The sidebar status says which mode is active.

---

## 3. Project structure

```
MarudhamWasteWeb/
├── pom.xml
├── database.sql
├── README.md
└── src/main/
    ├── java/
    │   ├── com/marudham/web/
    │   │   ├── MarudhamWasteWebApplication.java   app entry point + seed data
    │   │   ├── config/
    │   │   │   ├── DualPortConfig.java            opens the second port (8081)
    │   │   │   └── PortAccessFilter.java          admin/user access rules
    │   │   ├── controller/
    │   │   │   ├── ApiController.java             /api/points, /api/requests, /api/dashboard
    │   │   │   ├── TruckController.java           /api/trucks, point location
    │   │   │   └── SimController.java             /api/sim/*
    │   │   ├── model/
    │   │   │   ├── CollectionPoint.java           entity (with latitude/longitude)
    │   │   │   ├── CollectionRequest.java         entity
    │   │   │   └── Truck.java                     entity
    │   │   └── repository/
    │   │       ├── CollectionPointRepository.java
    │   │       ├── CollectionRequestRepository.java
    │   │       └── TruckRepository.java
    │   └── marudham/                              simulation engine (plain Java)
    │       ├── CollectionService.java             main simulation logic
    │       ├── RoadNetwork.java                   weighted graph (adjacency list)
    │       ├── RoutePlanner.java                  Dijkstra + nearest-neighbour
    │       ├── MinHeap.java                       hand-written binary min-heap
    │       ├── PathResult.java
    │       ├── CollectionPoint.java, CollectionRequest.java, CollectionRecord.java
    │       ├── CollectionVehicle.java, SmallTruck.java, LargeTruck.java
    │       ├── Assignment.java, Priority.java, RequestStatus.java
    │       └── SampleData.java                    sample city, points, fleet, request waves
    └── resources/
        ├── application.properties
        └── static/
            ├── admin.html        admin console
            ├── user.html         citizen portal
            ├── driver.html       truck driver console
            └── simulation.html   network simulation page
```

> `Main.java` (the console menu for the simulation engine) is **not** part of the web project. Keep it outside `src/` to avoid two `main` methods.
> The older `index.html`, `app.js` and `style.css` are replaced by the pages above and can be deleted.

---

## 4. Prerequisites

| Requirement | Notes |
|---|---|
| JDK 17 or newer | Check with `java -version` |
| Maven | Or use the Maven wrapper (`mvnw`) if the project has one |
| XAMPP | Only the **MySQL** module is needed (not Apache) |
| Google Chrome | Any modern browser works |
| Internet connection | Needed for map tiles (OpenStreetMap), Leaflet (CDN) and road routing (OSRM) |
| VS Code (optional) | With *Extension Pack for Java* and *Spring Boot Extension Pack* |

`pom.xml` must contain the Spring Boot parent and plugin plus these dependencies:

```xml
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
<dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
```

---

## 5. Setup and run

### 5.1 Start MySQL
1. Open the **XAMPP Control Panel** and click **Start** next to **MySQL**. It should turn green and show port **3306**.
2. If it fails with *"Port 3306 in use"*, another MySQL is running. Stop that service (`services.msc` → MySQL → Stop, set Startup type to Manual), then start XAMPP's MySQL again.

### 5.2 Create the database
From a terminal (VS Code terminal, **Ctrl + `**):

```
C:\xampp\mysql\bin\mysql.exe -u root -e "CREATE DATABASE IF NOT EXISTS marudham_waste;"
C:\xampp\mysql\bin\mysql.exe -u root -e "SHOW DATABASES;"
```

You can also run `database.sql`. Spring Boot creates the tables itself.

### 5.3 Configure `src/main/resources/application.properties`

```properties
server.port=8080
app.user.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/marudham_waste?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.error.include-message=always
```

XAMPP's default `root` user has **no password**. If you set one, put it in `spring.datasource.password`.

### 5.4 Run the application
Open a terminal **in the project folder** (the one containing `pom.xml`):

```
cd /d D:\Users\ADMIN\Downloads\MarudhamWasteWeb\MarudhamWasteWeb
mvn spring-boot:run
```

Wait for these log lines:

```
HikariPool-1 - Start completed.                      <- connected to MySQL
Tomcat started on ports 8080 (http), 8081 (http)     <- both sites are up
Started MarudhamWasteWebApplication ...              <- ready
```

Keep this window open while using the sites. Press **Ctrl + C** to stop. Keep XAMPP MySQL running.

### 5.5 Open the sites

| Site | URL |
|---|---|
| Admin console | http://localhost:8080 |
| Citizen portal | http://localhost:8081 |
| Driver console | http://localhost:8080/driver.html |
| Network simulation | http://localhost:8080/simulation.html |

The sidebar should say **"Connected to Spring Boot + MySQL"**. Press **Ctrl + F5** after replacing any HTML file.

### 5.6 Demo mode (no backend, no MySQL)
Opening `admin.html` or `user.html` directly in Chrome (or with the VS Code *Live Server* extension) runs in demo mode with browser storage and the four sample points. The simulation page and the truck/driver features need the Spring Boot backend.

---

## 6. Sites and pages

### Admin site (port 8080)

| Page | What it does |
|---|---|
| **Dashboard** | Stats (points, total kg, pending, completed), high-priority count, shortcuts, and the **request queue** where requests are marked complete. |
| **Collection Points** | Table with zone, waste level and fill bar; **Edit kg**; **Add collection point** form. |
| **Optimizer** | Map and route planner (see section 7). Drag a grey point marker to correct its location. |
| **Network Simulation** | Opens `simulation.html` (see section 8). |

An alert bar at the top of the admin pages announces truck breakdowns and recoveries.

### Citizen site (port 8081)
Single page, **Request Pickup**: choose a point and priority, submit, and watch the request status table.

### Driver console (`/driver.html`, admin port only)
- **Start live GPS:** sends the browser's location to the server every time it changes.
- **Simulate driving (demo):** moves the selected truck a little every 3 seconds.
- **Report breakdown / Back in service:** changes the truck status; the admin site detects it within about 5 seconds.

---

## 7. How the optimization works

The Optimizer page recalculates when it opens, when you press **Optimize route**, after a point is dragged, and automatically when a truck's status changes.

1. **Inputs:** pending requests, collection point coordinates, and **active** trucks (status `ACTIVE`) with their capacity and current position.
2. **Assignment:** requests are sorted by priority (High, Medium, Low) and then by weight (heaviest first). Each request goes into the first trip that still has room. If none has room, a new trip is created for the active truck with the fewest trips.
3. **Stop order (shortest path):** for each trip, stops are ordered by a **nearest-neighbour** pass starting at the truck's position, then improved with **2-opt**. The distance includes the return leg to the depot. Distances use the haversine (great-circle) formula.
4. **Road route:** the ordered stops are sent to the public **OSRM** demo server to get real road geometry, distance and duration. If OSRM cannot be reached, a straight-line route is drawn and the estimate uses 30 km/h. The trip card shows `(road)` or `(straight-line)`.
5. **Breakdown handling:** the admin page polls `/api/trucks` every 5 seconds. When a truck switches to `BREAKDOWN` (or back to `ACTIVE`), an alert is shown and the plan is recomputed, so the broken truck's requests move to the remaining trucks.

**Defaults:** depot at about 8.79, 78.138 (Thoothukudi area; set by `DEPOT` in `admin.html`). Seeded trucks: **T1** (800 kg) and **T2** (600 kg). Seeded points CP01 to CP04 get approximate coordinates; any point without coordinates gets a position near the depot until you drag it.

Priority decides which trip a request is assigned to; within a trip the order is purely by distance.

---

## 8. Network simulation engine

A separate, in-memory model of a fictional municipality (package `marudham`). It runs entirely in Java and is exposed through `SimController` and `simulation.html`.

| Part | Details |
|---|---|
| **City** | 13 locations: depot `D0`, junctions `J1` to `J3`, and collection points `CP1` to `CP9`, joined by 20 weighted roads |
| **Fleet** | `ST-1` Small Truck (500 kg, cost 6/km, 30 km/h), `LT-1` Large Truck (1200 kg, 14/km, 25 km/h), `LT-2` Large Truck (1000 kg, 14/km, 25 km/h) |
| **Request queue** | `PriorityQueue`: higher priority first, then earlier arrival |
| **Shortest path** | Dijkstra with a hand-written binary `MinHeap`; ignores blocked roads |
| **Multi-stop order** | Nearest-neighbour heuristic (not guaranteed optimal, since the full tour is a travelling-salesman problem) |
| **Dynamic events** | Blocking a road re-plans affected vehicles; a breakdown returns the request to the queue for reassignment; repair puts the vehicle back |
| **Clock** | Starts Day 1 at 08:00; collection takes 5 min, unloading 10 min |
| **History** | Every assignment, collection, unload, reroute, road block, breakdown, reassignment and repair is logged |

**Simulation page controls:** Load wave 1 / wave 2, Assign next, Process all, End shift, Reset, add a request, find the shortest path between two locations, click a road to block or reopen it, and per-vehicle buttons (▶ 1 hop, ⏭ To arrival, ✔ Confirm, 🏭 Depot, ⚠ Breakdown / 🔧 Repair).

Map legend: red dashed road = blocked; coloured lines = each vehicle's planned route; yellow line = shortest-path query; collection point colour = open request priority (red High, yellow Medium, green Low).

> The simulation is independent of the MySQL data. A restart resets it.

---

## 9. REST API reference

### Points, requests, dashboard (`ApiController`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| GET | `/api/points` | | All collection points |
| POST | `/api/points` | `{id, name, zone, wasteKg}` | Add a point |
| PUT | `/api/points/{id}/waste` | `{wasteKg}` | Update waste level |
| GET | `/api/requests` | | All requests, newest first |
| POST | `/api/requests` | `{pointId, priority}` | Create a request |
| POST | `/api/requests/{id}/complete` | | Complete a request and remove its waste from the point |
| GET | `/api/dashboard` | | Counts and total waste |

### Trucks and locations (`TruckController`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| GET | `/api/trucks` | | All trucks with status and position |
| PUT | `/api/trucks/{id}/location` | `{lat, lng}` | Update a truck's position |
| POST | `/api/trucks/{id}/breakdown` | | Set status to `BREAKDOWN` |
| POST | `/api/trucks/{id}/repair` | | Set status to `ACTIVE` |
| PUT | `/api/points/{id}/location` | `{lat, lng}` | Save a point's coordinates |

### Simulation (`SimController`, base `/api/sim`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| GET | `/state` | | Full simulation state (nodes, roads, points, vehicles, requests, history, counters) |
| POST | `/reset` | | Reset to the sample city |
| POST | `/wave/{1\|2}` | | Load a wave of sample requests |
| POST | `/request` | `{pointId, priority}` | Submit a request (`priority` = H / M / L) |
| POST | `/assign` | | Assign the next request to a vehicle |
| POST | `/advance` | `{vehicleId, hops}` | Move a vehicle along its route |
| POST | `/arrive` | `{vehicleId}` | Move a vehicle to its destination |
| POST | `/confirm` | `{vehicleId, kg?}` | Confirm collection (optional actual kg) |
| POST | `/process` | | Process all remaining requests automatically |
| POST | `/block`, `/unblock` | `{a, b}` | Block or reopen the road between two locations |
| POST | `/breakdown`, `/repair` | `{vehicleId}` | Vehicle breakdown or repair |
| POST | `/return` | `{vehicleId}` | Send a vehicle to the depot and unload |
| POST | `/endshift` | | Recall all vehicles |
| GET | `/path?from=A&to=B` | | Dijkstra shortest path |

Errors are returned as HTTP 400 (invalid input), 404 (not found) or 409 (conflict) with a `message`.

---

## 10. Database

Database name: **`marudham_waste`**. Hibernate creates and updates the tables automatically (`spring.jpa.hibernate.ddl-auto=update`).

**`collection_points`**

| Column | Type | Notes |
|---|---|---|
| id | VARCHAR(20) | primary key, e.g. `CP01` |
| name, zone | VARCHAR | required |
| waste_kg | DOUBLE | 0 to 5000 |
| latitude, longitude | DOUBLE | nullable; set by seed data or by dragging markers |
| updated_at | DATETIME | |

**`collection_requests`**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | auto-increment primary key |
| point_id, point_name | VARCHAR | |
| priority | VARCHAR | HIGH / MEDIUM / LOW |
| status | VARCHAR | PENDING / COMPLETED |
| waste_kg | DOUBLE | waste recorded when the request was made |
| created_at | DATETIME | |

**`trucks`**

| Column | Type | Notes |
|---|---|---|
| id | VARCHAR(20) | primary key, e.g. `T1` |
| name | VARCHAR | |
| capacity_kg | DOUBLE | |
| latitude, longitude | DOUBLE | last known position |
| status | VARCHAR | `ACTIVE` / `BREAKDOWN` |
| updated_at | DATETIME | |

**Seed data (first start only):** points CP01 Market Street (180 kg), CP02 Bus Stand (320 kg), CP03 College Road (95 kg), CP04 Lake View (410 kg); trucks T1 (800 kg) and T2 (600 kg). On every start, points without coordinates are given locations.

Useful checks:

```
C:\xampp\mysql\bin\mysql.exe -u root -D marudham_waste -e "SELECT * FROM collection_points; SELECT * FROM collection_requests; SELECT * FROM trucks;"
```

---

## 11. Access control (admin vs user)

One Spring Boot application listens on two ports, and `PortAccessFilter` limits each one.

| Port | Role | Allowed |
|---|---|---|
| **8080** | Admin | Everything (`/admin.html`, `/driver.html`, `/simulation.html`, the whole API). `/` redirects to `/admin.html`. `/user.html` redirects to `/admin.html`. |
| **8081** | User | Only `/user.html`, `GET /api/points`, `GET /api/requests`, `POST /api/requests`. `/` redirects to `/user.html`. Everything else returns **403**. |

The port is set by `app.user.port` (default 8081).

> There is **no login**. Anyone who can reach port 8080 can use the admin site.

---

## 12. Business rules and validation

- Waste level must be a number from **0 to 5000 kg**.
- A point ID must be unique (conflict error otherwise).
- A request needs a valid point, and the point must have **more than 0 kg** of waste.
- Priority must be High, Medium or Low.
- A point can have only **one open request** (Pending) at a time.
- Only **Pending** requests can be completed.
- Completing a request subtracts the recorded waste from the point (never below 0).
- Truck location updates need valid latitude (-90 to 90) and longitude (-180 to 180).

---

## 13. Demo walkthrough

1. Start MySQL, run the app, and open http://localhost:8080 and http://localhost:8081.
2. **User site:** pick a point and a priority, then click **Submit request**.
3. **Admin Dashboard:** see the request in the queue and the Pending count go up.
4. **Admin → Optimizer:** the map shows the depot, points and trucks, with a planned route per truck.
5. Open http://localhost:8080/driver.html in another tab, pick **Truck 1**, click **Simulate driving**, and watch it move on the map.
6. Click **Report breakdown**. Within about 5 seconds the admin site shows a red alert and the routes are recomputed without Truck 1.
7. Click **Back in service** to restore it.
8. On the Dashboard, click **Mark complete** to finish a request and watch the point's waste drop.
9. Open **Network Simulation**: **Load wave 1** → **Assign next** a few times → **▶ 1 hop** → click a road on a route to block it → **⚠ Breakdown** on a truck → **Process all**.

---

## 14. Troubleshooting

| Problem | Cause and fix |
|---|---|
| `No plugin found for prefix 'spring-boot'` | Maven was run outside the project folder. `cd` to the folder containing `pom.xml` first (use `cd /d` for another drive). |
| XAMPP: *Port 3306 in use* | Another MySQL service is running. Stop it in `services.msc`, or move XAMPP to port 3307 and update the JDBC URL. |
| `Communications link failure` | MySQL is not running in XAMPP. |
| `Access denied for user 'root'` | You set a root password. Put it in `spring.datasource.password`. |
| `Unknown database 'marudham_waste'` | Create the database (section 5.2). |
| `Port 8080 was already in use` | The old app is still running. Press Ctrl + C in its window or end the process in Task Manager. |
| Only port 8080 appears in the Tomcat log | `DualPortConfig.java` is missing or in the wrong folder (`src/main/java/com/marudham/web/config/`). |
| Sidebar says **Demo mode** | The backend is not running, or the page was opened as a file instead of `http://localhost:8080`. |
| Sidebar stuck on **Connecting…** | Press Ctrl + F5. If it persists, press F12 and check the Console for errors. |
| Generic error messages in the UI | Add `server.error.include-message=always` to `application.properties`. |
| Map is blank or routes are straight lines | No internet access, or the OSRM demo server is unreachable. |
| `package jakarta.servlet does not exist` | The project is on Spring Boot 2.x. Use `javax.servlet` in `PortAccessFilter`. |
| Hibernate warning about the MySQL dialect (5.5.5) | Harmless with XAMPP's MariaDB. |
| Two `main` methods / main class error | Do not put `Main.java` from the simulation engine inside `src/`. |

---

## 15. Known limitations

- **No authentication or user accounts.** The admin site is open to anyone who can reach port 8080, and all citizens see all requests.
- **The network simulation is separate from MySQL**, keeps state in memory, and resets on restart. Its map positions are fixed diagram coordinates, not real streets.
- **Point and truck coordinates are approximate** demo values around Thoothukudi. Correct them by dragging markers on the Optimizer map.
- **The optimizer is heuristic.** Nearest-neighbour with 2-opt gives good routes but not a guaranteed optimum, and assignment is greedy.
- **Road routing depends on the public OSRM demo server**, which is for light use only and may be slow or unavailable.
- **Live GPS** from a phone needs HTTPS (browsers allow GPS over plain HTTP only on `localhost`). Use *Simulate driving* for demos.
- **Truck status changes are detected by polling** every 5 seconds, not pushed instantly.
- The completed-request flow is manual: an admin marks requests complete.

---

## 16. Possible next steps

- Admin login with roles (Spring Security) and citizen accounts, so users see only their own requests.
- Connect the MySQL points and trucks to the simulation's road graph.
- WebSocket or Server-Sent Events for instant breakdown alerts.
- Capacity-aware truck assignment with time windows, or a proper vehicle-routing solver.
- Reports and history pages (collections per day, distance and fuel cost per truck).
- Deploy with HTTPS so drivers can use live GPS from phones.
