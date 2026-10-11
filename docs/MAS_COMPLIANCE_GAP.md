# MAS Compliance Gap Analysis (Indicative)

> **Not legal advice.** This is an engineering-level mapping of what the scaffold does and does not cover, prepared without legal review. Verify every reference against current MAS publications and take advice from qualified Singapore counsel before relying on it.

PayFriend holds no MAS licence and moves no real money. Operating a payment service in Singapore requires a licence under the Payment Services Act 2019 (major or standard payment institution, depending on scope and volume).

## Reference points to review

- Payment Services Act 2019 and its regulations (licensing, safeguarding of customer money, e-money rules)
- MAS Technology Risk Management (TRM) Guidelines
- MAS AML/CFT notices for payment services licensees (the PSN series)
- MAS guidance on cyber hygiene and outsourcing
- PDPA obligations for personal data

## Control mapping

| Control area | In the scaffold | Gap before any real operation |
| --- | --- | --- |
| Governance and key officers | None | Fit-and-proper key personnel, compliance and AML roles, board oversight |
| Safeguarding of customer funds | None. Play money only | Segregated safeguarding arrangements with an approved institution |
| Authentication | Crypto-bound biometric confirmation, JWT sessions | Real KYC / identity proofing, MFA policy, step-up rules, device binding with attestation, session management review |
| Authorisation and access control | Per-user wallet scoping on payment routes | Role-based admin access, privileged access management, maker-checker |
| Cryptography and key management | Android Keystore (StrongBox where present), HMAC JWT | HSM-backed server keys, rotation, key ceremony, TLS pinning review |
| Audit logging | Hash-chained audit events, append-only ledger design | Persistent tamper-evident store, external anchoring, retention, log monitoring |
| Transaction integrity | Double entry, idempotency, integer money | Multi-instance concurrency control, reconciliation, settlement and dispute workflows |
| AML/CFT | None (a mock rules engine is only planned) | Customer due diligence, sanctions screening, transaction monitoring, suspicious transaction reporting |
| Secure development | CI builds and tests, validation of untrusted input | Threat modelling, SAST/DAST, dependency scanning, independent penetration testing |
| Resilience and incident management | Docker-based local stack | HA deployment, backups and recovery tests, incident response and MAS notification procedures |
| Data protection | No personal data stored beyond demo ids | Data classification, consent, retention, breach handling under the PDPA |
| Third-party and cloud risk | None | Vendor due diligence, outsourcing arrangements, exit plans |

## Suggested path for a licensed partner

1. Treat this repository as an architecture and code accelerator, not a compliant system.
2. A licensed institution maps its own control framework onto these components and closes the gaps above.
3. Commission an independent security assessment before handling real customer data or funds.
