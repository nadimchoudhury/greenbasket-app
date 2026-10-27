const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_store_1";
const BOB_UID = "bob_store_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProductPayload(userId) {
  const now = new Date();
  return {
    userId,
    name: "Organic Hass Avocado",
    barcode: "890123456001",
    category: "Produce",
    imageUrl: "produce_avocado",
    unit: "pc",
    costPrice: 1.2,
    sellingPrice: 2.5,
    discountPercent: 10,
    stockQuantity: 45,
    reorderThreshold: 12,
    reorderQuantity: 50,
    supplierId: "sup_1",
    supplierName: "GreenValley Farms",
    totalSoldUnits: 0,
    totalRevenue: 0.0,
    totalProfit: 0.0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/products`).doc("prod_1").set(validProductPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, read, list, and update their own product", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/products`).doc("prod_1");

  await assertSucceeds(docRef.set(validProductPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/products`).get());
  await assertSucceeds(
    docRef.update({
      stockQuantity: 30,
      discountPercent: 15,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access is strictly rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/products`).doc("prod_1").set(validProductPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/products`).doc("prod_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/products`).get());
});

test("Shadow field and invalid discount or negative stock are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/products`).doc("prod_bad");

  await assertFails(
    docRef.set({
      ...validProductPayload(ALICE_UID),
      ghostField: "hacked",
    })
  );

  await assertFails(
    docRef.set({
      ...validProductPayload(ALICE_UID),
      discountPercent: 150,
    })
  );

  await assertFails(
    docRef.set({
      ...validProductPayload(ALICE_UID),
      stockQuantity: -5,
    })
  );
});

test("Customer orders and terminal state locking work as expected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const orderRef = aliceDb.collection(`users/${ALICE_UID}/orders`).doc("ord_101");

  const orderPayload = {
    userId: ALICE_UID,
    orderNumber: "ORD-1001",
    customerName: "Walk-In Customer",
    customerPhone: "555-0100",
    deliveryAddress: "",
    orderType: "WALK_IN",
    status: "PENDING",
    paymentMethod: "CASH",
    itemsSummary: "2x Organic Hass Avocado",
    items: [
      {
        productId: "prod_1",
        productName: "Organic Hass Avocado",
        barcode: "890123456001",
        quantity: 2,
        unitCost: 1.2,
        unitPrice: 2.25,
        discountPercent: 10,
        lineTotal: 4.5,
        lineProfit: 2.1,
      },
    ],
    totalItemsCount: 2,
    subtotal: 5.0,
    discountTotal: 0.5,
    totalAmount: 4.5,
    totalCost: 2.4,
    netProfit: 2.1,
    orderDateEpochDay: 20752,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(orderRef.set(orderPayload));
  await assertSucceeds(orderRef.update({ status: "CANCELLED", updatedAt: new Date() }));
  // Terminal state lock: once CANCELLED, cannot mutate back to COMPLETED
  await assertFails(orderRef.update({ status: "COMPLETED", updatedAt: new Date() }));
});
