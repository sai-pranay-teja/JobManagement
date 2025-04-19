
# 📦 Job Management Portal — Pattern-Driven CI/CD

A web-based Job Portal implemented using **Java Servlets, JSP, and JDBC**, integrated with a **pattern-driven CI/CD pipeline** using **Jenkins** and optionally **GitHub Actions**. The pipeline supports **benchmarking**, **A/B testing modes**, and **performance metrics logging**.

---

## 🗂️ Project Structure

```
JobManagement/
├── .github/                 # GitHub Actions workflow
│   └── workflows/
│       └── ci.yml           # GitHub Actions CI workflow
├── backup code/             # Backup code snapshots
├── src/
│   └── main/
│       ├── java/            # Java source files
│       ├── resources/       # config.properties etc.
│       ├── test/            # JUnit test classes
│       └── webapp/          # JSP and static assets
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

## 🚀 GitHub Actions (CI/CD)

A GitHub Actions workflow is available in:

```
.github/workflows/ci.yml
```

This workflow can replicate core parts of the Jenkins pipeline like:

- Java compilation
- Test execution (JUnit)
- Artifact packaging

### ⚠️ Mutual Exclusivity

To prevent conflicts between Jenkins and GitHub Actions:

- ✅ **To use GitHub Actions:**  
  Disable or stop Jenkins execution (e.g., pause or take Jenkins offline).

- ✅ **To use Jenkins:**  
  Comment out or remove GitHub Actions triggers (like `on: push`) in `.github/workflows/ci.yml` or Just disable the workflow in GitHub

Only one CI system should be active at a time to avoid race conditions and duplicate deployments.

---

## 🔐 SSH Configuration

The pipeline uses the following SSH variables:

```groovy
SSH_KEY  = '/var/lib/jenkins/.ssh/id_rsa' // Update as per your key
SSH_USER = 'root'
SSH_HOST = '40.192.66.15'                 // Replace with your server's IP
```

💡 For production use, store these as Jenkins credentials instead of hardcoded values.

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



## 🧑‍💻 Developer


Developed as part of a **research project for a Master's thesis**, aimed at publication in a **Scopus-indexed journal** on empirical software engineering.


Maintained by [Sai Pranay Teja](https://github.com/sai-pranay-teja)
