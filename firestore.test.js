const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_rakesh";
const ADMIN_EMAIL = "therakeshattri@gmail.com";

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

// --- TESTS ---

test("Resident: submits maintenance record with pending status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("maintenance_records").add({
      userId: ALICE_UID,
      userName: "Alice Sharma",
      flatNumber: "Plot 114, Flat 101",
      monthYear: "October 2026",
      amount: 1000,
      utrNumber: "123456789012",
      status: "pending",
      createdAt: new Date(),
    })
  );
});

test("Admin: queries pending maintenance and approves payment", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("maintenance_records").doc("bob_record").set({
      userId: BOB_UID,
      userName: "Bob Singh",
      flatNumber: "Flat 202",
      monthYear: "October 2026",
      amount: 1000,
      utrNumber: "987654321098",
      status: "pending",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
  }).firestore();

  await assertSucceeds(adminDb.collection("maintenance_records").doc("bob_record").get());
  await assertSucceeds(
    adminDb.collection("maintenance_records").doc("bob_record").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );
});

test("Admin: posts society notice and resident reads it", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(
    adminDb.collection("notices").add({
      title: "Water Tank Cleaning on Sunday",
      description: "Water supply will remain interrupted between 9 AM to 1 PM.",
      date: "2026-10-05",
      priority: "urgent",
      postedBy: ADMIN_UID,
      createdAt: new Date(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertSucceeds(aliceDb.collection("notices").get());
});

test("Admin: responds to resident complaint and resolves it", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("complaints").doc("c1").set({
      userId: ALICE_UID,
      userName: "Alice",
      flatNumber: "101",
      title: "Flickering Light",
      description: "Pole 3 light is dim",
      status: "open",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(
    adminDb.collection("complaints").doc("c1").update({
      status: "resolved",
      adminResponse: "Electrician replaced the LED fixture at 3:00 PM.",
      updatedAt: new Date(),
    })
  );
});

test("Admin: can promote resident to admin role and approve membership", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob",
      email: "bob@test.com",
      flatNumber: "B-201",
      role: "resident",
      approvalStatus: "pending",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      role: "admin",
      approvalStatus: "approved",
      updatedAt: new Date(),
    })
  );
});
