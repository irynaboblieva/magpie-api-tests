# Magpie API Tests

This project contains automated tests for the Magpie API endpoints: `/quote` and `/quote-in`.

## Access
- The code is hosted on GitHub.
- Access is granted to: `jobs@magpiefi.xyz`.

## Requirements
- Java 21+
- Maven 3.6+
- Allure

## How to Run Tests

1. Clone the repository:
   ```bash
   git clone <repository_url>
   ```

2. Navigate to the project directory:
   ```bash
   cd <project_directory>
   ```

3. Install dependencies and run the tests:
   ```bash
   mvn clean test
   ```

4. To generate and view the Allure report:
   ```bash
   mvn allure:serve
   ```

## Test Plan
For details on the test plan, see
[Test Plan](https://docs.google.com/document/d/1BQtV7LNXNk-WGPzDxG6PxmNvxawosMlCrVgNojhpxyo/edit?usp=sharing).

## Reporting
- **Format:** Allure reports
- Reports are stored in the `target/allure-results` directory.

## Logs and API Requests
- Test logs are saved to track request and response details.

## Repository
- [GitHub Repository:] (https://github.com/irynaboblieva/magpie-api-tests.git)
