# Indusface WAS Scan Plugin

### Overview
The Indusface WAS Scan Plugin enhances your Jenkins pipelines with automated security scanning, helping you identify and address vulnerabilities early in the development cycle. By integrating seamlessly into CI/CD workflows, this plugin ensures that security is part of your development process.

### Key Features
- **Automated Scanning**: Triggers security scans automatically during the build process.
- **Real-time Insights**: Delivers clear and concise scan results.
- **Proactive Security**: Option to fail builds based on configurable security thresholds.

---

## Quick Start

### 1. Install the Plugin
Download and install the plugin from the Jenkins Update Center.

### 2. Configure the Jenkins Job
Enable the plugin in your Jenkins job's **Post-build Actions**. You can either create a new job or modify an existing one.

### 3. Enable the Plugin
In the **Post-build Actions** section, select the **WAF Scan Plugin**.

### 4. Provide Application Details
Enter the web application name and access key required for scanning.

### 5. Run the Job
Once the application build succeeds, the scan will automatically start.

### 6. Monitor the Scan
Track the progress and results in the **WAS Scan report** section based on the pre-configured pass/fail criteria.

---

## Benefits
- **Faster Time to Market**: Identify and fix security issues early in the development cycle.
- **Enhanced Security Posture**: Strengthen your application's defenses.
- **Improved Code Quality**: Maintain high standards throughout the development lifecycle.

---

## Contributing
We welcome community contributions! To contribute:

1. Fork this repository.
2. Make your changes.
3. Submit a pull request to help us improve this plugin.

---

## Support
For issues, questions, or feedback, please submit an issue in the [GitHub repository](https://github.com/productteam-was/indusface-was-scan) or contact the plugin maintainers.

---

## License
This plugin is open-source and licensed under the MIT License.
