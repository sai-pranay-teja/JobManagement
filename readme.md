# 📦 Job Management Portal — Pattern-Driven CI/CD

A web-based Job Portal implemented using **Java Servlets, JSP, and JDBC**, integrated with a **pattern-driven CI/CD pipeline** using **Jenkins**. The pipeline supports **benchmarking**, **A/B testing modes**, and **performance metrics logging**.

---

## 🗂️ Project Structure

```
JobManagement/
├── .github/                 # GitHub Actions workflow (future use)
├── backup code/             # Backup code snapshots
├── src/
│   └── main/
│       ├── java/            # Java source files
│       ├── resources/       # config.properties etc.
│       ├── test/            # JUnit test classes
│       └── webapp/          # JSP and static assets
├── buildspec.yml            # AWS CodeBuild spec (future use)
└── Jenkinsfile              # Main CI/CD pipeline
```

---

## 🚀 Features

- Dynamic job and candidate management
- Admin, company, and student panels
- Pattern-driven Jenkins pipeline with:
  - A/B testing modes
  - Unit & integration testing
  - WAR packaging
  - SSH deployment to remote Tomcat server
  - Benchmark tracking (build/test/deploy times)

---

## ⚙️ Jenkins Pipeline (CI/CD)

The Jenkins pipeline (defined in `Jenkinsfile`) automates:

- Git checkout
- Pattern-driven or full compilation
- Parallel or sequential testing
- WAR packaging
- Remote Tomcat deployment via SSH
- Metrics logging in CSV

### 🎛️ Switch Execution Mode

In the Jenkinsfile:

```groovy
def mode = 'A' // 'A' = optimized pattern execution, 'B' = full execution
```

---

## 🔐 SSH Configuration

The pipeline uses the following SSH variables:

```groovy
SSH_KEY  = '/var/lib/jenkins/.ssh/id_rsa' // Update as per your key
SSH_USER = 'root'
SSH_HOST = '40.192.66.15'                 // Replace with your server's IP
```

> 💡 You can modify the `SSH_KEY` and `SSH_HOST` directly in the Jenkinsfile. For production use, it’s better to use Jenkins Credentials instead of hardcoded paths.

---

## 🧪 Test Logs

JUnit test results are written to:

- `test_results.log-unit`
- `test_results.log-integration` or `test_results.log-combined`

---

## 📈 Benchmark Metrics

A CSV log is maintained for each pipeline run:

```text
stage_metrics.csv
```

Contains:

- Build Time
- Test Time
- Deploy Time
- Lead Time for Change
- Total Pipeline Time

---

## ⚒️ WAR Deployment

WAR file generated:

```text
JobManagement_JENKINS.war
```

Deployment directory:

```text
/opt/tomcat10/webapps/
```

---

## 🧪 Additional CI Options

Although not currently active, the repository includes:

- `.github/workflows/` — GitHub Actions
- `buildspec.yml` — AWS CodeBuild

These are placeholders for future CI/CD integrations.

---

## 🧑‍💻 Developer

Developed as part of **Samyak National Level Techno-Management Fest**.  
Maintained by [Sai Pranay Teja](https://github.com/sai-pranay-teja)
