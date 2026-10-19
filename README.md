# NY Waterway Fare Configuration Tool

**Course:** CMPT 276 — Introduction to Software Engineering, Simon Fraser University

**Project Repository:** [https://github.com/harpoonwithaz/nyw-fare-config](https://github.com/harpoonwithaz/nyw-fare-config)

**Authors:** [Athan L](https://github.com/wishadele), [Harendeep](https://github.com/harendeep29-eng), [Lakshya J](https://github.com/lakshyajoshipersonal-bit),
[Oliver T](https://github.com/harpoonwithaz/), [Tejas D](https://github.com/tejas6433)

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

### Epic 1

### Epic 2

### Epic 3

### Epic 4

### Epic 5 : Configuration Review, Validation, and Publishing
The main focus here is to allow NY Waterway employees to review and verify configuration changes before publishing them to the vending machines.

The main features would include:

Configuration Comparison: Develop an interface which would display the proposed changes alongside the currently published configurations, allowing employees to fully identify the differences before publishing.

Discrepency Detection: Validate the proposed configurations against the baseline dataset in order to identify any incorrect prices, ticket availability issues, and other inconsistencis.

Configuration Publishing: Allow operators to publish verified configurations to the existing backend through the provided API.

Change History: Provide operators with access to previous configuration changes and publication records to improve transparency and accountability.

The main goal here is to reduce human error and ensure that only reviewed and validated configurations are being published to the vending machines.


