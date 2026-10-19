# NY Waterway Fare Configuration Tool

**Course:** CMPT 276 — Introduction to Software Engineering, Simon Fraser University

**Project Repository:** [https://github.com/harpoonwithaz/nyw-fare-config](https://github.com/harpoonwithaz/nyw-fare-config)

**Authors:** [Athan L](https://github.com/wishadele), [Harendeep](https://github.com/harendeep29-eng), [Lakshya J](https://github.com/lakshyajoshipersonal-bit),
[Oliver T](https://github.com/harpoonwithaz/), [Tejas D](https://github.com/tejas6433)

**Application Link:** [nyw-fare-config](https://nyw-fare-config.onrender.com/)

## Abstract

NY Waterway Fare Configuration Tool is an administrative web application designed to replace the fragile, manual configuration workflow currently used by Caracal Technologies and NY Waterway (NYW) to configure physical Ticket Vending Machines (TVMs) across ferry terminals.
The application provides an interactive interface for client services staff and operators to update origin-destination fares, manage ticket availability and sales windows, author pre-purchase route alert banners, and publish vetted catalog configurations.
The technical implementation will include a Java Spring Boot web application using ThymeLeaf as the templating engine with a PostgreSQL database for storing session based credentials and persistent storage.
The application communicates via REST HTTP calls with Caracal Technologies’ Docker-packaged multi-tenant platform API and server.

## Core Problem & How We Solve It

### Background

Caracal Technologies manufactures and operates TVMs for parking facilities, parks and recreation, and municipal/mass transit clients, including Edmonton and New York Waterway.
New York Waterway operates commuter ferry services across multiple terminals with distinct route-based pricing and operating schedules.

### Current workflow & Existing Systems

NYW operational staff compile route updates, schedule changes, and rate adjustments into complex, two-dimensional Microsoft Excel spreadsheets.
These spreadsheets are emailed to Caracal Technologies, where engineers manually translate spreadsheet rows into raw, deeply nested JSON configuration files.
These spreadsheets provide no transactional integrity, no live synchronization with TVM fleet endpoints, no constraint checking, and require technical developers to execute updates.

Existing commercial fleet configuration suites (e.g., SAP, custom automated fare collection platforms) are costly, proprietary, and incompatible with Caracal’s multi-tenant dimensional pricing architecture.

### What Problem We Solve

The reliance on spreadsheets and manual process of updating the ferry ticket prices, availability, and customer warning messages is extremely time consuming.
Our application aims to solve this problem by providing a centralized, user friendly interface where the employees can manage ticket configurations, review changes, and publish updates more efficiently and accurately.
It will introduce localized persistence for pre-purchase passenger notices, and enforce strict front-end and server-side schema constraints (e.g., preventing negative dollar amounts or malformed schedules) at the point of entry.

### Target Audience

The primary audience is NY waterway employees and operators who are the ones responsible for managing ferry ticket vending machines and their configurations.
These users need a simple and reliable way to update ticket prices, manage ticket availability, configure customer warning messages, and review changes before publishing them.

## Project Scope & 5 Core Epics

### Features and Sub-problems

### Epic 1 User Authentication thriugh a login page featuring both Local and Google sign in options

The main focus of this epic is to provide NY Waterway employees with a secure and convenient way to access the system and to prevent unauthorized access. In this epic, we will:

Develop a Local Login page which would allow employees to sign in using their registered email address or username and password, through a custom/local database.
Then we will advance to develop a Google Login page which would allow employees to sign in using their authorized Google accounts to achieve a faster and easier login process.

In both of these iterations, we will carefully and strictly verify user credentials and ensure that only authorized employees can access the system. In  case of unauthorized accounts, invalid credentials, missing fields, and authentication failures, etc we will also display appropriate error messages.

We will also implement a logout button which logs the user out for improved security and also auto-logout after a set time.
The main goal is to provide NY Waterway employees with a secure, user-friendly authentication experience through both local login and Google login, ensuring that only authorized users can access the system.

### Epic 2 : Interactive Origin-Destination Rate Grid & Fare Management

This epic addresses the core daily task of NY Waterway fare managers by delivering an interactive rate table that mirrors the operator's mental model.
It replaces the error-prone manual translation of spreadsheets into raw JSON configuration files by providing an intuitive, web-based rate management matrix for Caracal client services personnel and New York Waterway administrators.

When an operator selects an origin terminal, the interface dynamically constructs a comprehensive interactive pricing grid displaying every valid route destination alongside all ticket products and their prevailing prices.
Each row will represent an outbound destination, and columns represent the price catalog (e.g., ADULT_ONEWAY, CHILD_ONEWAY, FULL_10TRIP, FULL_MONTHLY, BIKE_ONEWAY).
Each cell will clearly display its active pricing status in the standard U.S. currency format, and routes that are not currently offered or active at that station display distinctive visual badges (e.g., NOT_SOLD or N/A) rather than ambiguous blank fields, preventing confusion between un-configured fares and free fares.

The operator can click directly into any price cell to enter edit mode. As values are adjusted, the interface visually highlights modified cells with a "pending draft" state (e.g., subtle yellow border or badge), giving the operator an immediate visual log of all uncommitted modifications on the active page.
The operator can discard pending edits on a cell-by-cell basis or reset the entire grid back to the current platform state before executing a save.
Once satisfied with the adjustments, the operator clicks "Save Rate Changes". A confirmation dialog presents a concise summary of all pending rate changes (specifying Origin, Destination, Product, Old Price, and New Price) before final submission.
Upon confirmation, the application commits the changes through the backend and returns a real-time status alert confirming how many price rules were updated on the platform.

Because the physical ticket vending machines rely on exact decimal pricing rules, this epic enforces multi-tiered client side (Browser) and server side (Spring Boot) data validation, handling Caracal platform architectural constraints.
The module implements real-time client-side validation to reject blank entries, negative numbers, and currency values with more than two decimal places.
On submission, the backend converts grid modifications into transactional payload rows and executes bulk price adjustments against the platform API, updating the live origin-destination rule sets immediately.

### Epic 3

Epic 3 : Ticket Availability and Product Lifecycle Management

This part of the project is about controlling which tickets riders can buy, where, and when. From what we understand so far, availability is currently buried in spreadsheets and nested JSON files, which makes simple changes slow and error-prone. Our goal is to let staff manage it directly in the app.

The main features would include:

Scoped Ticket Toggles: Staff can activate or deactivate ticket types (such as Adult, Child, or 10-Trip) at an individual terminal, route, or rate table, so a change in one place doesn’t affect the rest of the network.

Temporal Availability Rules: We would model time-based conditions using the platform’s when-condition-create mechanism, including the monthly ticket purchase window (the 20th of the previous month to the 10th of the current month), weekday vs. weekend availability, and holiday schedules.

Permanent Product Phase-Outs: Staff can mark a discontinued ticket type, such as Senior tickets, as NOT_SOLD across all rate tables in one action.

Validation and Interface: A simple screen would show which ticket types are active for each terminal, route, and rate table. Conflicting rules, such as overlapping date windows, would be flagged with clear error messages before saving.

The main goal is to give NY Waterway employees a reliable way to control what is sold, where, and when, while reducing manual work and errors. Changes made here would feed into the review and publishing flow in Epic 5. We expect the details of this epic to change as we learn more about the platform and talk with the client.

### Epic 4 : Pre-Purchase Service Alerts and TVM Preview

This part of the project is about what riders see on the ticket machine before they buy a ticket. From what we understand so far, Caracal's platform doesn't have a good way to store things like route alerts, out-of-service hours, or Spanish translations of messages. Because of that, it's hard for NY Waterway to let riders know about service changes right at the machine.

Our plan is to store this information in our own application's database instead of relying on the platform. Staff would be able to write a notice for a route, mark when that route isn't running, and add both an English and a Spanish version of the message. When a configuration gets published, these notices would be sent along with the rest of the settings so they show up on the machine.

We also want to handle routes that only run on weekdays. Riders may still want to buy a ticket on the weekend for their Monday trip, so we'd like to allow that while showing a short message explaining that the route isn't running today and the ticket is for the next weekday.

Another issue is that the current prompts on the machine use generic "Dismiss" and "Confirm" buttons, which can be confusing. We'd like staff to be able to write clearer button labels that say what the button actually does.

To tie all of this together, we're planning a simple preview of the ticket machine screen inside the app. As staff edit a message or a button label, or switch between English and Spanish, they'd be able to see roughly how it will look on the machine before anything is published. We expect the details of this epic to change as we learn more about the platform and talk with the client.

### Epic 5 : Configuration Review, Validation, and Publishing
The main focus here is to allow NY Waterway employees to review and verify configuration changes before publishing them to the vending machines.

The main features would include:

Configuration Comparison: Develop an interface which would display the proposed changes alongside the currently published configurations, allowing employees to fully identify the differences before publishing.

Discrepency Detection: Validate the proposed configurations against the baseline dataset in order to identify any incorrect prices, ticket availability issues, and other inconsistencis.

Configuration Publishing: Allow operators to publish verified configurations to the existing backend through the provided API.

Change History: Provide operators with access to previous configuration changes and publication records to improve transparency and accountability.

The main goal here is to reduce human error and ensure that only reviewed and validated configurations are being published to the vending machines.

## Technical Architecture & REST API Integration

### Technology Stack

**Backend:** Java 25 with Spring Boot (MVC, Spring Data JPA, Spring Security).

**Frontend:** Thymeleaf server-side templates styled with Bootstrap / CSS, with vanilla JavaScript for dynamic matrix interactions.

**Application Persistence:** PostgreSQL database hosted on Render (storing local user credentials, draft edits, alert banners, and operating hours).

**Deployment Environment:** Containerized Maven Docker Image web service running on Render.

### Rest API Integration

The application will interact with Caracal’s Go-based microservice architecture via HTTP REST endpoints.
The application will query both key GET and POST endpoints for fare fetching and updating.
The development and testing will be executed against an isolated sandbox tenant deployed locally via Docker.
