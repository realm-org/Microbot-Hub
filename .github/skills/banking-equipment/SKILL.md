---
name: banking-equipment
description: "Banking, equipment management, and inventory setup patterns for Microbot Hub plugin scripts. USE FOR: handling bank deposits/withdrawals, managing charged items (rings, staffs), preserving equipment during banking, using Rs2InventorySetup safely, handling gear swaps. DO NOT USE FOR: creating new plugins from scratch, build issues, publishing."
---

# Banking & Equipment Management Skill

Guidelines for safe banking, equipment preservation, and charged item handling in Microbot Hub scripts.

## Key Problems & Solutions

### 1. Charged Items (Ring of Dueling, Amulet of Glory, etc.)

Each charge level is a **different ItemID** in OSRS. `Ring of dueling(8)` ≠ `Ring of dueling(5)`.

**NEVER** hardcode a single charge variant (e.g. `ItemID.RING_OF_DUELING8`). Use name-based matching:

```java
// BAD - only finds fully charged rings
if (Rs2Bank.count(ItemID.RING_OF_DUELING8) > 0) { ... }

// GOOD - finds any charge level
if (Rs2Bank.hasItem("Ring of dueling")) { ... }
```

**Withdrawing charged items:**
```java
// BAD
Rs2Bank.withdrawOne(ItemID.RING_OF_DUELING8);

// GOOD - partial name match gets highest charge first (bank ordering)
Rs2Bank.withdrawOne("Ring of dueling");
```

**Checking equipped charged items:**
```java
// BAD - misses other charge levels
if (Rs2Equipment.isWearing(ItemID.RING_OF_DUELING8)) { ... }

// GOOD
Rs2ItemModel ring = Rs2Equipment.get(EquipmentInventorySlot.RING);
if (ring != null && ring.getName().contains("dueling")) { ... }
```

**Equipping from inventory:**
```java
Rs2ItemModel ring = Rs2Inventory.get(it -> it != null && it.getName().contains("Ring of dueling"));
if (ring != null) {
    Rs2Inventory.interact(ring, "Wear");
    sleepUntil(() -> {
        Rs2ItemModel equipped = Rs2Equipment.get(EquipmentInventorySlot.RING);
        return equipped != null && equipped.getName().contains("dueling");
    }, Rs2Random.between(3000, 6000));
}
```

### 2. Rs2InventorySetup and doesEquipmentMatch()

`doesEquipmentMatch()` does exact ID comparison by default. A ring with fewer charges than the setup expects will cause it to return `false`, triggering `loadEquipment()` which calls `depositEquipment()` — stripping ALL worn gear.

**Always add a retry limit and charged-item tolerance:**

```java
if (!inventorySetup.doesEquipmentMatch()) {
    // Check if the "mismatch" is just a charged item with different charges
    boolean chargedItemOk = Rs2Equipment.get(EquipmentInventorySlot.RING) != null
            && Rs2Equipment.get(EquipmentInventorySlot.RING).getName().contains("dueling");

    int retries = 0;
    while (!inventorySetup.doesEquipmentMatch() && retries < 3) {
        if (!super.isRunning()) break;
        inventorySetup.loadEquipment();
        retries++;
    }
    // Accept near-match if only charged items differ
    if (!inventorySetup.doesEquipmentMatch() && chargedItemOk) {
        Microbot.log("Equipment close enough (charged item charges differ) - continuing.");
    }
}
```

### 3. Depositing Safely

**`Rs2Bank.depositAll()`** deposits inventory only, NOT equipment. Equipment is deposited by `depositEquipment()` or indirectly by `loadEquipment()`.

**Prefer `depositAllExcept()` when you have items to preserve:**

```java
// Available overloads:
Rs2Bank.depositAllExcept(String... names);           // partial name match
Rs2Bank.depositAllExcept(Integer... ids);             // exact item IDs
Rs2Bank.depositAllExcept(Collection<String> names);   // collection of names
Rs2Bank.depositAllExcept(Predicate<Rs2ItemModel> p);  // custom logic
```

**Build a keep-list for items that should stay in inventory:**
```java
List<String> keepItems = new ArrayList<>(Arrays.asList(
    "Spade", "Prayer potion(4)", "Prayer potion(3)",
    "Ring of dueling", foodName
));
// Add conditional items
if (config.useBrews()) {
    keepItems.add("Forgotten brew(4)");
    keepItems.add("Forgotten brew(3)");
}
Rs2Bank.depositAllExcept(keepItems);
```

**MKE Wintertodt's per-slot approach (most precise but more complex):**
```java
// Deposit slot-by-slot, preserving specific items
for (int slot = 0; slot < 28; slot++) {
    Rs2ItemModel item = Rs2Inventory.get(slot);
    if (item != null && !shouldKeep(item)) {
        Rs2Bank.depositOne(item.getId());
    }
}
```

### 4. Equipment Replacement During Banking

**Check by name, not null, for charged equipment:**
```java
// BAD - only triggers when slot is completely empty
if (Rs2Equipment.get(EquipmentInventorySlot.RING) == null) { getNewRing(); }

// GOOD - also catches wrong item type in slot
Rs2ItemModel ring = Rs2Equipment.get(EquipmentInventorySlot.RING);
if (ring == null || !ring.getName().contains("dueling")) {
    getNewRing();
}
```

**Pattern for items that degrade/break (e.g. barrows equipment, charged items):**
```java
// Check if equipped item still has charges/is functional
Rs2ItemModel staff = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
if (staff != null && staff.getName().contains("uncharged")) {
    // Need to replace
}
```

### 5. Banking Flow Template

```java
if (shouldBank) {
    if (!Rs2Bank.isOpen()) {
        Rs2Bank.walkToBankAndUseBank(bankLocation);
    } else {
        // 1. Save notable loot before depositing
        saveLootToTracking();

        // 2. Deposit inventory (keeping essentials)
        Rs2Bank.depositAllExcept(keepItems);

        // 3. Withdraw supplies (check current count first)
        if (Rs2Inventory.count(foodId) < targetFood) {
            int needed = targetFood - Rs2Inventory.count(foodId);
            Rs2Bank.withdrawX(foodId, needed);
        }

        // 4. Check/replace equipment (name-based for charged items)
        Rs2ItemModel ring = Rs2Equipment.get(EquipmentInventorySlot.RING);
        if (ring == null || !ring.getName().contains("dueling")) {
            Rs2Bank.withdrawOne("Ring of dueling");
            Rs2ItemModel newRing = Rs2Inventory.get(
                it -> it != null && it.getName().contains("Ring of dueling"));
            if (newRing != null) Rs2Inventory.interact(newRing, "Wear");
        }

        // 5. Verify supplies before leaving
        suppliesCheck(config, true);
    }
}
```

## Common Pitfalls

| Pitfall | Fix |
|---------|-----|
| `loadEquipment()` infinite loop on charged items | Add retry limit + charged-item tolerance |
| Hardcoded `ItemID.RING_OF_DUELING8` | Use `"Ring of dueling"` name matching |
| `depositAll()` then re-withdraw everything | Use `depositAllExcept(keepItems)` |
| Building a keepItems list but never using it | Pass it to `depositAllExcept()` |
| Only checking `== null` for equipment slot | Also check item name matches expected type |
