<div align="left" style="position: relative;">
<h1>NETMONITOR_NMS</h1>
<p align="left">
    <em><code>❯ Enterprise-Grade Network Management & Monitoring System</code></em>
</p>
<p align="left">
    </p>
<p align="left">Built with the tools and technologies:</p>
<p align="left">
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=java,spring,react,mysql,maven,nodejs,css,html">
    </a>
</p>
</div>
<br clear="right">

##  Table of Contents

- [ Overview](#-overview)
- [ Features](#-features)
- [ Project Structure](#-project-structure)
- [ Getting Started](#-getting-started)
  - [ Prerequisites](#-prerequisites)
  - [ Installation](#-installation)
  - [ Usage](#-usage)
- [ Project Roadmap](#-project-roadmap)
- [ Contributing](#-contributing)
- [ License](#-license)
- [ Acknowledgments](#-acknowledgments)

---

##  Overview

**Netmonitor_NMS** is a comprehensive full-stack application designed for real-time monitoring of network infrastructure. It replaces manual CLI checks with a modern, interactive web dashboard that provides visibility into device health, traffic utilization, and topology.

The system follows a **Poller-Collector-Presenter** architecture:
1.  **Backend (Java/Spring Boot):** Handles SNMP polling, data aggregation, and alert generation.
2.  **Frontend (React):** Visualizes data through dynamic dashboards, topology maps, and interactive charts.

---

##  Features

* **⚡ Real-Time Dashboard:** Overview of all managed devices with "Status Circles" providing instant counts of Up, Down, Critical, and Trouble states.
* **🗺️ Network Topology Map:** Interactive, drag-and-drop node-link diagram visualizing the network structure, built with **React Flow**. It supports "Demo Mode" for testing layout visualization without hardware.
* **📊 Traffic Analytics:** Detailed **Recharts** area graphs for Interface RX/TX rates with selectable time ranges (1 Hour to 30 Days).
* **🔍 Auto-Discovery:** Scans subnets (CIDR) to automatically identify and add SNMP-enabled devices to the inventory.
* **🔔 Alerting & Logs:** Centralized event log for tracking system errors, startup events, and threshold breaches (e.g., High CPU, Link Down).
* **📈 Historical Data:** Stores raw metrics for immediate analysis and aggregated hourly/daily data for long-term reporting.

---

##  Project Structure

```sh
└── Netmonitor_NMS/
    ├── backend
    │   ├── .gitignore
    │   ├── pom.xml
    │   └── src
    │       └── main
    │           ├── java/com/network/snmp
    │           │   ├── controller
    │           │   ├── service
    │           │   ├── model
    │           │   └── repository
    │           └── resources
    └── frontend
        ├── .gitignore
        ├── package.json
        ├── public
        └── src
            ├── components
            │   ├── Dashboard.js
            │   ├── InterfaceGraph.js
            │   ├── NetworkMap.js
            │   ├── Dashboard.css
            │   └── InterfaceGraph.css
            ├── App.js
            ├── App.css
            └── index.js

```

---

## Getting Started

### Prerequisites

Before getting started with Netmonitor_NMS, ensure your runtime environment meets the following requirements:

* **Java JDK:** Version 17 or higher.
* **Node.js:** Version 16 or higher.
* **Database:** MySQL (Ensure a database instance is running).
* **Maven:** For building the backend.

### Installation

**1. Clone the Repository**

```sh
git clone https://github.com/theiyappan/Netmonitor_NMS
cd Netmonitor_NMS

```

**2. Backend Setup**
Navigate to the backend directory and install dependencies:

```sh
cd backend
mvn clean install

```

*Note: Ensure your `application.properties` or `application.yml` is configured with your MySQL credentials.*

**3. Frontend Setup**
Navigate to the frontend directory and install dependencies:

```sh
cd ../frontend
npm install

```

### Usage

**Start the Backend Server**

```sh
cd backend
mvn spring-boot:run

```

The API server will typically start on `http://localhost:8080`.

**Start the Frontend Client**

```sh
cd frontend
npm start

```

The dashboard will launch in your browser at `http://localhost:3000`.

---

## Project Roadmap

* [X] **Core Monitoring**: SNMP Polling, Interface Metrics, Up/Down Status.
* [X] **Visualization**: Dashboard, Interactive Topology Map, Historical Graphs.
* [X] **Alerting**: Event logging for Critical/Warning states.
* [ ] **WMI Integration**: Add support for Windows Server monitoring (CPU/RAM/Disk).
* [ ] **RBAC**: Implement Role-Based Access Control for Admin vs. Viewer users.
* [ ] **Distributed Polling**: Separate the poller into a microservice for scalability.

---

## Contributing

* **💬 [Join the Discussions](https://github.com/theiyappan/Netmonitor_NMS/discussions)**: Share your insights, provide feedback, or ask questions.
* **🐛 [Report Issues](https://github.com/theiyappan/Netmonitor_NMS/issues)**: Submit bugs found or log feature requests for the `Netmonitor_NMS` project.
* **💡 [Submit Pull Requests](https://github.com/theiyappan/Netmonitor_NMS/blob/main/CONTRIBUTING.md)**: Review open PRs, and submit your own PRs.

---

## License

This project is protected under the [MIT](https://choosealicense.com/licenses/mit/) License. For more details, refer to the [LICENSE](https://choosealicense.com/licenses/) file.

---

## Acknowledgments

* **Charts:** Powered by [Recharts](https://recharts.org/).
* **Icons:** Provided by [Lucide React](https://lucide.dev/).
* **Topology:** Built with [React Flow](https://reactflow.dev/).
