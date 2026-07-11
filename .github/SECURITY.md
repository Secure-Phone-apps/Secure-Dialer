# Security Policy

## Supported Versions

We take the security and privacy of **Secure Dialer** extremely seriously. Because we handle critical user data (contacts, call logs) entirely **on-device** with **zero internet permission**, our attack surface is significantly minimized. 

The following versions of Secure Dialer are currently supported with security updates:

| Version | Supported |
| ------- | --------- |
| 1.x     | ✅ Yes    |
| < 1.0   | ❌ No     |

## Reporting a Vulnerability

If you discover a security vulnerability or privacy leak within this repository, please **do not** open a public GitHub issue. Publicly disclosing a vulnerability can expose users to unnecessary risks.

Instead, please report the vulnerability through one of the following methods:

1. **GitHub Security Advisories:** Navigate to the "Security" tab of this repository and click "Report a vulnerability" to submit a private draft.
2. **Direct Email:** Contact the maintainers at `movstore.online@gmail.com`.

### What to Include in Your Report

To help us investigate and patch the issue quickly, please include:
* A detailed description of the vulnerability.
* Clear, step-by-step instructions to reproduce the issue (and a proof-of-concept script/app if available).
* The potential impact (e.g., local privilege escalation, unauthorized permission bypass).
* The Android OS version and device model on which you reproduced the issue.

### Our Response Process

* **Acknowledgement:** We will acknowledge receipt of your report within **48 hours**.
* **Triage & Fix:** We will keep you updated as we investigate, verify, and develop a patch for the issue.
* **Disclosure:** Once a patch is released, we will publicly disclose the vulnerability and credit you for your responsible disclosure (if desired).

---

## Zero-Network Policy Assurance

As a fundamental security architecture of **Secure Dialer**, this application **does not declare the `android.permission.INTERNET` permission**. 
* This means the app is physically incapable of transmitting any data (contacts, logs, numbers dialed) over the web.
* This is our core cryptographic and OS-level guarantee of your privacy.
