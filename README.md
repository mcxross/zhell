# zhell: Sui Sponsored Gas Service

**zhell** is a lightweight, high-performance gas station service for the **Sui blockchain**, built with **Kotlin** and **Ktor**. It allows authorized clients (via API Keys) to request gas sponsorship for their transactions, enabling "gasless" user experiences.

It is designed to be stateless and serverless-ready, using **Google Cloud Firestore** for API key management and **Google Cloud Run** for scalable deployment.

## Features

* **Gas Sponsorship**: Signs Sui transaction bytes with a sponsor key to pay for gas fees.
* **API Key Management**: Admin API to generate, validate, and revoke API keys.
* **Secure Authentication**:
    * **Admin**: Bearer Token authentication (constant-time comparison).
    * **Client**: High-entropy API keys (`zk_...`) stored in Firestore.
* **Serverless Architecture**:
    * **Compute**: Dockerized Ktor application (Cloud Run).
    * **Database**: Google Cloud Firestore (NoSQL) for low-latency key lookups.
* **Network Agnostic**: Configurable for Sui Mainnet, Testnet, or Devnet.

## Tech Stack

* **Language**: Kotlin
* **Framework**: Ktor (Netty)
* **Database**: Google Cloud Firestore (Native Mode)
* **Blockchain**: Sui (via `ksui` SDK)
* **Build Tool**: Gradle (Kotlin DSL)
* **Container**: Docker (Multi-stage build)

---

## Configuration

The application is configured entirely via Environment Variables.

| Variable | Description | Required | Default |
| :--- | :--- | :--- | :--- |
| `ADMIN_TOKEN` | A secure string used to authenticate Admin requests. | **Yes** | - |
| `SPONSOR_PRIVATE_KEY` | The Sui private key (bech32 `suiprivkey...`) used to sign transactions. | **Yes** | - |
| `SUI_NETWORK` | The target Sui network: `MAINNET`, `TESTNET`, or `DEVNET`. | No | `TESTNET` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to service account JSON (only required for local dev). | No | - |

---

## Local Development

### Prerequisites
1.  **Java 21** installed.
2.  **Google Cloud Project** with Firestore enabled (Native Mode).
3.  **Service Account JSON** with "Cloud Datastore User" role (for local testing).

### Running Locally
1.  **Set Environment Variables**:
    ```bash
    export ADMIN_TOKEN="my-super-secret-admin-token"
    export SPONSOR_PRIVATE_KEY="suiprivkey1..."
    export SUI_NETWORK="TESTNET"
    export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
    ```

2.  **Run the Server**:
    ```bash
    ./gradlew run
    ```

3.  **Test the Admin Endpoint**:
    ```bash
    curl -X POST [http://0.0.0.0:8080/admin/api-keys](http://0.0.0.0:8080/admin/api-keys) \
         -H "Authorization: Bearer my-super-secret-admin-token" \
         -H "Content-Type: application/json" \
         -d '{"name": "Local Test", "owner": "Developer"}'
    ```

---

## Deployment (Google Cloud Run)

This project is optimized for Cloud Run. It does not require a SQL database; it connects directly to Firestore using the container's identity.

### 1. Prerequisites
* Google Cloud CLI (`gcloud`) installed and authenticated.
* Artifact Registry repo created (or use GCR).
* Firestore Database created in your project.

### 2. Build and Push Container
Use Google Cloud Build to create the image without needing local Docker:

```bash
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/zhell-gas-service
```

### 3. Deploy Service
Deploy the container, setting the production secrets as environment variables.
Security Tip: In a real production setup, use Google Secret Manager for the private key and admin token instead of plain text env vars.

```bash
gcloud run deploy zhell-gas-service \
--image gcr.io/YOUR_PROJECT_ID/zhell-gas-service \
--platform managed \
--region us-central1 \
--allow-unauthenticated \
--set-env-vars ADMIN_TOKEN="prod-secure-admin-token" \
--set-env-vars SPONSOR_PRIVATE_KEY="suiprivkey1..." \
--set-env-vars SUI_NETWORK="MAINNET"
```
### 4. Permissions
Ensure the Cloud Run Service Account has the Cloud Datastore User role to access Firestore.
API Reference
### 1. Generate API Key (Admin Only)
   POST /admin/api-keys

Headers: Authorization: Bearer <ADMIN_TOKEN>

Body:

```JSON

{
"name": "Mobile App v1",
"owner": "Frontend Team"
}
```

Response:

```JSON
{
"apiKey": "zk_abc123..."
}
```

### 2. Sponsor Transaction
POST /gas

Headers: X-API-Key: <YOUR_API_KEY>

Body:

```JSON
{
  "txBytes": "<BASE64_ENCODED_TX_BYTES>",
  "sender": "0xUserAddress"
}
```

Response:

```JSON
{
"txBytes": "<BYTES>",
"sponsorSignature": "<SUI_SIGNATURE>",
"status": "SPONSORED"
}
```