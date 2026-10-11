# PayFriend API (v1)

Base URL (local): `http://localhost:8080`. All bodies are JSON. Amounts are integer cents in SGD.

## Authentication

Protected routes require `Authorization: Bearer <JWT>`. Tokens expire after 1 hour.

### `POST /api/v1/auth/demo-login`

**DEMO ONLY.** Issues a token for a fixed demo user and creates a demo wallet with SGD 1,000.00 of play money.

```json
{ "token": "eyJ...", "userId": "demo-customer-001", "walletId": "wallet:demo-customer-001" }
```

## Wallet

### `GET /api/v1/wallets/me`

```json
{ "walletId": "wallet:demo-customer-001", "balanceCents": 100000, "currency": "SGD" }
```

## Transactions

### `POST /api/v1/transactions`

Request:

```json
{
  "idempotencyKey": "pf-3f6c5c0e-1c4a-4a8e-9a6f-0d1e2f3a4b5c",
  "merchantId": "MERCH-001",
  "merchantName": "PayFriend Demo Cafe",
  "amountCents": 1250,
  "description": "optional"
}
```

Validation: `idempotencyKey` 8-100 chars of `A-Za-z0-9_-`; `merchantId` 1-64 chars of `A-Za-z0-9_-`; `merchantName` 1-120 chars; `description` up to 255 chars; `amountCents` between 1 and 1,000,000.

| Status | Meaning |
| --- | --- |
| `201` | Payment created. Body is the transaction |
| `200` | Idempotent replay: same key and same payment. Body is the original transaction |
| `400` | Validation error or malformed JSON (`INVALID_REQUEST`, `INVALID_AMOUNT`, `BAD_REQUEST`) |
| `401` | Missing or invalid token |
| `409` | `IDEMPOTENCY_CONFLICT`: key already used for a different payment |
| `422` | `INSUFFICIENT_FUNDS` |

Transaction object:

```json
{
  "id": "uuid",
  "idempotencyKey": "pf-...",
  "payerAccountId": "wallet:demo-customer-001",
  "merchantId": "MERCH-001",
  "merchantName": "PayFriend Demo Cafe",
  "amountCents": 1250,
  "currency": "SGD",
  "description": null,
  "status": "SETTLED",
  "createdAt": "2026-01-01T12:00:00Z"
}
```

### `GET /api/v1/transactions`

Up to 50 transactions touching the caller's wallet, newest first. Includes the opening demo top-up.

### `GET /api/v1/transactions/{id}`

A payment made by the caller. `404` otherwise.

## Health

### `GET /health`

```json
{ "status": "ok", "auditChainValid": true }
```

## Error format

```json
{ "error": "INSUFFICIENT_FUNDS", "message": "Insufficient wallet balance" }
```

## Merchant QR payload

`PAYFRIEND|<merchantId>|<merchantName, "_" for spaces>|<UEN>|<amount or empty>`

Example: `PAYFRIEND|MERCH-001|PayFriend_Demo_Cafe|201912345A|12.50`
