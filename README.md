# NY Waterway Fare Configuration Tool

**Course:** CMPT 276 — Introduction to Software Engineering, Simon Fraser University

**Project Repository:** [https://github.com/harpoonwithaz/nyw-fare-config](https://github.com/harpoonwithaz/nyw-fare-config)

**Authors:** [Athan L](https://github.com/wishadele), [Harendeep](https://github.com/harendeep29-eng), [Lakshya J](https://github.com/lakshyajoshipersonal-bit),
[Oliver T](https://github.com/harpoonwithaz/), [Tejas D](https://github.com/tejas6433)

**Application Link:** [nyw-fare-config](https://nyw-fare-config.onrender.com/)

## Abstract

### High Level Purpose

An administrative web application designed to replace the fragile, manual configuration workflow currently used by Caracal Technologies and NY Waterway (NYW) to configure physical Ticket Vending Machines (TVMs) across ferry terminals.

### Core Functionality

Provides an interactive interface for client services staff and operators to update origin-destination fares, manage ticket availability and sales windows, author pre-purchase route alert banners, and publish vetted catalog configurations.

### Architectural Overview

A Java Spring Boot web application using ThymeLeaf as the templating engine with a PostgreSQL database for storing session based credentials and persistent storage.
The application communicates via REST HTTP calls with Caracal Technologies’ Docker-packaged multi-tenant platform API and server.

## Core Problem and How We Solve It

### Background

Caracal Technologies manufactures and operates TVMs for parking facilities, parks and recreation, and municipal/mass transit clients, including Edmonton and New York Waterway.
New York Waterway operates commuter ferry services across multiple terminals with distinct route-based pricing and operating schedules.

### Current workflow

NYW operational staff compile route updates, schedule changes, and rate adjustments into complex, two-dimensional Microsoft Excel spreadsheets.
These spreadsheets are emailed to Caracal Technologies, where engineers manually translate spreadsheet rows into raw, deeply nested JSON configuration files.

### What Problem We Solve

The reliance on spreadsheets and manual process of updating the ferry ticket prices, availability, and customer warning messages is extremely time consuming.
Our application aims to solve this problem by providing a centralized, user friendly interface where the employees can manage ticket configurations, review changes, and publish updates more efficiently and accurately.

### Target Audience

The primary audience is NY waterway employees and operators who are the ones responsible for managing ferry ticket vending machines and their configurations.
These users need a simple and reliable way to update ticket prices, manage ticket availability, configure customer warning messages, and review changes before publishing them.

## Project Scope

### Features and Sub-problems

### Epic 1 User Authentication thriugh a login page featuring both Local and Google sign in options

The main focus of this epic is to provide NY Waterway employees with a secure and convenient way to access the system and to prevent unauthorized access. In this epic, we will:

Develop a Local Login page which would allow employees to sign in using their registered email address or username and password, through a custom/local database.
Then we will advance to develop a Google Login page which would allow employees to sign in using their authorized Google accounts to achieve a faster and easier login process.

In both of these iterations, we will carefully and strictly verify user credentials and ensure that only authorized employees can access the system. In  case of unauthorized accounts, invalid credentials, missing fields, and authentication failures, etc we will also display appropriate error messages.

We will also implement a logout button which logs the user out for improved security and also auto-logout after a set time.
The main goal is to provide NY Waterway employees with a secure, user-friendly authentication experience through both local login and Google login, ensuring that only authorized users can access the system.

### Epic 2

### Epic 3

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


