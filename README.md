# AI Code Review System

CodeReviewer is an advanced, AI-powered static analysis and code review platform designed to automatically detect security vulnerabilities, code quality issues, and performance bottlenecks in any codebase.

## 🚀 Features

* **Dual Analysis Engine:** Combines the deterministic precision of an advanced local static analysis engine with the contextual understanding of a Large Language Model (Gemini 3.8 Flash).
* **Comprehensive Vulnerability Detection:** Automatically detects critical vulnerabilities including SQL Injection, Stored/Reflected XSS, Hardcoded Credentials, and Client-Side Authentication Bypasses.
* **Performance Insights:** Identifies deep nested loops ($O(n^2)$ complexity) and architectural bottlenecks.
* **Resilient Architecture:** Fully graceful fallback to the local static engine if the AI API is rate-limited or unavailable.
* **Professional Dashboard:** A sleek, GitHub-style developer UI with dark/light modes, floating actionable issue cards, and distinct metrics for overall code health.
* **Language Agnostic:** Natively supports Java, JavaScript, Python, TypeScript, and HTML/CSS with intelligent framework detection.

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot 3.3.4, Spring AI
* **Frontend:** Vanilla JavaScript, HTML5, CSS3, Feather Icons
* **Database:** MySQL, Hibernate/JPA
* **AI Provider:** Google Gemini API

## 📋 Quick Start

### 1. Prerequisites
* Java 21 or higher
* Maven 3.6+
* MySQL Server (running on port 3306)

### 2. Configuration
Export the following environment variables in your terminal before running the application:

```bash
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export GEMINI_API_KEY="your_gemini_api_key"
export GITHUB_TOKEN="your_github_token"
```

### 3. Run the Application
Start the Spring Boot backend:

```bash
./mvnw spring-boot:run
```

Access the application at `http://localhost:8080`.

## 🛡️ Architecture & Security
The system features robust AST-like pattern matching for the static engine, ensuring that even if AI quotas are exceeded, critical CI/CD pipelines and vulnerability scans are never interrupted. Code is sanitized and strictly parsed via JSON schemas to prevent LLM hallucinations.

## 📄 License
This project is licensed under the MIT License.
