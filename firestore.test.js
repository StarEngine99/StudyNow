const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_101";
const BOB_UID = "bob_student_202";

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

test("Unauthenticated user cannot read or write student profiles or habits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
});

test("Owner can create and partially update StudentProfile; rejects ghost fields & future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Nguyen Minh Anh",
      email: "minhanh@student.edu.vn",
      schoolName: "Dai hoc Dong Thap",
      className: "CNTT K23",
      studentCode: "0023410499",
      avatarPreset: "INDIGO_OWL",
      pomodoroMinutesTotal: 50,
      pomodoroSessionsCount: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      schoolName: "Dai hoc Khoa hoc Tu nhien",
      pomodoroMinutesTotal: 75,
      pomodoroSessionsCount: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field (shadow update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future Edit",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner can create, list, update, and delete Habits in subcollection; @DocumentId 'id' field rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_reading").set({
      userId: ALICE_UID,
      title: "Doc sach 20 phut",
      category: "STUDY",
      targetMinutes: 20,
      completedDates: ["2026-11-22", "2026-11-23"],
      currentStreak: 2,
      bestStreak: 5,
      reminderHour: 20,
      reminderMinute: 30,
      reminderEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unfiltered list on user-scoped subcollection succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").get()
  );

  // Partial update on check-in
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_reading").update({
      completedDates: ["2026-11-22", "2026-11-23"],
      currentStreak: 3,
      bestStreak: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject unexpected @DocumentId "id" inside document body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_extra").set({
      id: "habit_extra",
      userId: ALICE_UID,
      title: "Tap the duc",
      category: "HEALTH",
      targetMinutes: 30,
      completedDates: [],
      currentStreak: 0,
      bestStreak: 0,
      reminderHour: 7,
      reminderMinute: 0,
      reminderEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid category enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_bad_cat").set({
      userId: ALICE_UID,
      title: "Invalid",
      category: "GAMING",
      targetMinutes: 15,
      completedDates: [],
      currentStreak: 0,
      bestStreak: 0,
      reminderHour: 19,
      reminderMinute: 0,
      reminderEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner can delete habit
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_reading").delete()
  );
});

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's profile or habits", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const past = new Date(Date.now() - 10000);
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("habits").doc("bob_habit").set({
      userId: BOB_UID,
      title: "Hoc Tieng Anh",
      category: "LANGUAGE",
      targetMinutes: 30,
      completedDates: [],
      currentStreak: 0,
      bestStreak: 0,
      reminderHour: 21,
      reminderMinute: 0,
      reminderEnabled: true,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ displayName: "Hijacked", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_habit").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_habit").delete());
});
