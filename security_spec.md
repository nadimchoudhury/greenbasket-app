# GreenBasket Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every collection (`/users/{userId}/products`, `/orders`, `/suppliers`, `/restock_orders`, `/expenses`) is isolated under `/users/{userId}` and requires `request.auth != null && request.auth.uid == userId` and `incoming().userId == request.auth.uid`.
2. **Schema & Bounds Enforcement**: Every `create` and `update` operation validates all field types, string lengths, numeric non-negativity, and allowed key sets via `isValid[Entity](incoming())` and `hasOnly()`.
3. **Immutable Metadata**: `userId` and `createdAt` cannot be modified after creation (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
4. **Terminal State Locking**: Cancelled customer orders (`status == 'CANCELLED'`) and completed/cancelled supplier restock orders (`status in ['RECEIVED', 'CANCELLED']`) cannot be mutated further.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/products").get()` -> Rejected.
2. **Cross-Tenant Read**: `bobDb.collection("users/alice/products").doc("p1").get()` -> Rejected.
3. **Identity Spoofing on Create**: `aliceDb` writing `userId: "bob"` inside `/users/alice/products/p1` -> Rejected.
4. **Shadow/Ghost Field Injection**: Adding `isAdmin: true` to a product create/update -> Rejected by `hasOnly()`.
5. **Negative Stock/Price Injection**: Setting `sellingPrice: -5` or `stockQuantity: -10` -> Rejected by `isValidProduct()`.
6. **Discount Out of Bounds**: Setting `discountPercent: 150` -> Rejected (`discountPercent <= 100`).
7. **Oversized String Poisoning**: Setting `name` with 500 characters -> Rejected (`name.size() <= 120`).
8. **Malformed Document ID**: Creating document with spaces/special chars in ID -> Rejected by `isValidId()`.
9. **Immutable `createdAt` Tampering**: Updating `createdAt` on an existing product -> Rejected.
10. **Future Timestamp Spoofing**: Writing `createdAt` in the future (`> request.time`) -> Rejected.
11. **Terminal Order Mutation**: Updating a `CANCELLED` customer order -> Rejected by terminal state lock.
12. **Terminal Restock Mutation**: Updating a `RECEIVED` restock order -> Rejected by terminal state lock.
