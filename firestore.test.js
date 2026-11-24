const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPinPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    visibility: "public",
    title: "صيدلية النبض المناوبة",
    description: "صيدلية مناوبة 24 ساعة مع توفر كافة الأدوية",
    category: "PHARMACY",
    status: "OPEN",
    latitude: 24.7136,
    longitude: 46.6753,
    cityName: "الرياض",
    confirmationsCount: 1,
    createdBy: uid,
    createdByName: "أحمد",
    updatedBy: uid,
    updatedByName: "أحمد",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user cannot read, list, or create pins", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("city_pins").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("city_pins").doc("pin_1").set(validPinPayload(ALICE_UID))
  );
});

test("Signed-in user can create pin without id field, query with visibility filter, and reject unfiltered list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("city_pins").doc("pin_1").set(validPinPayload(ALICE_UID))
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    aliceDb.collection("city_pins").where("visibility", "==", "public").get()
  );

  // Unfiltered query must fail
  await assertFails(aliceDb.collection("city_pins").get());

  // Rejects extra `id` field (@DocumentId guard) and ghost fields
  await assertFails(
    aliceDb
      .collection("city_pins")
      .doc("pin_2")
      .set(validPinPayload(ALICE_UID, { id: "pin_2" }))
  );
  await assertFails(
    aliceDb
      .collection("city_pins")
      .doc("pin_3")
      .set(validPinPayload(ALICE_UID, { isVerified: true }))
  );
});

test("Community user can update pin status (+1 confirmation) and add status_update log, but cannot hijack title or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("city_pins").doc("pin_1").set(validPinPayload(ALICE_UID))
  );

  // Bob confirms/updates status
  await assertSucceeds(
    bobDb.collection("city_pins").doc("pin_1").update({
      status: "CROWDED",
      updatedBy: BOB_UID,
      updatedByName: "سارة",
      updatedAt: new Date(Date.now() - 500),
      confirmationsCount: 2,
    })
  );

  // Bob logs status update in subcollection
  await assertSucceeds(
    bobDb
      .collection("city_pins")
      .doc("pin_1")
      .collection("status_updates")
      .doc("upd_1")
      .set({
        pinId: "pin_1",
        visibility: "public",
        status: "CROWDED",
        note: "ازدحام متوسط حالياً",
        authorId: BOB_UID,
        authorName: "سارة",
        createdAt: new Date(Date.now() - 500),
      })
  );

  // Filtered subcollection query succeeds, unfiltered fails
  await assertSucceeds(
    aliceDb
      .collection("city_pins")
      .doc("pin_1")
      .collection("status_updates")
      .where("visibility", "==", "public")
      .get()
  );
  await assertFails(
    aliceDb.collection("city_pins").doc("pin_1").collection("status_updates").get()
  );

  // Bob cannot modify immutable createdBy/createdAt or edit title or delete Alice's pin
  await assertFails(
    bobDb.collection("city_pins").doc("pin_1").update({
      title: "تم التعديل",
      updatedBy: BOB_UID,
      updatedByName: "سارة",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    bobDb.collection("city_pins").doc("pin_1").update({
      createdAt: new Date(Date.now() - 400),
      updatedBy: BOB_UID,
      updatedByName: "سارة",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(bobDb.collection("city_pins").doc("pin_1").delete());

  // Alice can delete her own pin
  await assertSucceeds(aliceDb.collection("city_pins").doc("pin_1").delete());
});

test("Rejects tampered future timestamps, invalid category/status, and oversized photoBase64", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb
      .collection("city_pins")
      .doc("pin_future")
      .set(validPinPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  await assertFails(
    aliceDb
      .collection("city_pins")
      .doc("pin_bad_cat")
      .set(validPinPayload(ALICE_UID, { category: "UNKNOWN" }))
  );

  await assertFails(
    aliceDb
      .collection("city_pins")
      .doc("pin_big_photo")
      .set(validPinPayload(ALICE_UID, { photoBase64: "a".repeat(120001) }))
  );
});

test("User can save Home, Work, and Favorite places privately while cross-user and unauthenticated access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  const homePayload = {
    placeType: "HOME",
    title: "بيتي",
    subtitle: "حي العليا",
    latitude: 24.7136,
    longitude: 46.6753,
    cityName: "الرياض",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("saved_places").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").set(homePayload)
  );

  // Alice can create Home and Favorite and list her own empty/non-empty collection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").set(homePayload)
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("fav_pin_1").set({
      placeType: "FAVORITE",
      title: "مطعمي المفضل",
      subtitle: "شارع التحلية",
      latitude: 24.7001,
      longitude: 46.6801,
      cityName: "الرياض",
      sourcePinId: "pin_1",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").get()
  );

  // Alice can update her Home preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").update({
      placeType: "HOME",
      title: "بيتي الجديد",
      subtitle: "حي النرجس",
      latitude: 24.8201,
      longitude: 46.6401,
      cityName: "الرياض",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot mutate createdAt or write invalid placeType or extra id field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").update({
      createdAt: new Date(Date.now() - 300),
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("bad_1").set({
      ...homePayload,
      placeType: "INVALID",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("bad_2").set({
      ...homePayload,
      id: "bad_2",
    })
  );

  // Bob cannot read, write, update, or delete Alice's saved places
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saved_places").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").set(homePayload)
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("home").delete()
  );

  // Alice can delete her favorite
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_places").doc("fav_pin_1").delete()
  );
});
