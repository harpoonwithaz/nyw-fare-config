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

### Target Audience

## Project Scope

### Features and Sub-problems

### Epic 1

### Epic 2

### Epic 3

### Epic 4

### Epic 5
