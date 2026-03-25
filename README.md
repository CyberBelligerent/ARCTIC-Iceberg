# Iceberg

Iceberg is the range data model and build scheduler for ARCTIC. It holds all the hypervisor-neutral objects that describe a cyber range — networks, hosts, volumes, routers, security groups — and it's responsible for dispatching build tasks in the right order.

---

## Data Model

Everything hangs off a `RangeExercise`. That's the top-level container for a cyber range.

| Entity | What it is |
|--------|-----------|
| `RangeExercise` | The range itself. Has a name, description, provider name, graph layout, and collections of everything below. |
| `ArcticHost` | A VM/instance. Has a name, IP, OS type, networks, volumes, and an `extraVariables` map for hypervisor-specific fields. |
| `ArcticNetwork` | A network segment. Has CIDR, gateway, IP range start/end. |
| `ArcticRouter` | A router connecting networks together. |
| `ArcticVolume` | A storage volume. Can be bootable and tied to a specific image. |
| `ArcticSecurityGroup` | A security group with a name and description. |
| `ArcticSecurityGroupRule` | A firewall rule. Has direction, protocol, port range, and ethernet type. |

All entities have UUID-based IDs and a `rangeId` field that links them back to their parent exercise.

---

## Build Scheduler — `IcebergCreator`

`IcebergCreator` is a `Thread` component (prototype-scoped, so you get a fresh one per build). It bridges the hypervisor-neutral Iceberg objects to Shard's service objects, then runs the build.

**How it works:**
1. You call `attemptCreation()` — this tells ShardManager to create a session for the profile.
2. You call `createNetwork()`, `createHost()`, etc. for each resource. These translate Iceberg entities into Shard service objects (`ArcticNetworkSO`, `ArcticHostSO`, etc.) and queue tasks on the running context.
3. You call `ic.start()` — the thread starts and drains a `PriorityQueue` of all the queued tasks.

**Build order (by priority, lower = first):**
1. Networks
2. Address ranges / subnets
3. Volumes
4. Instances (hosts)
5. Security groups (last)

Same-priority tasks are treated as independent and can run in any order.

The thread pool running tasks is fixed at 5 threads.

---

## Repos

- `ExerciseRepo` — CRUD for `RangeExercise`, includes `findByName()`
- `ArcticHostRepo`, `ArcticNetworkRepo`, `ArcticRouterRepo`, `ArcticVolumeRepo`
- `ArcticSecurityGroupRepo`, `ArcticSecurityGroupRuleRepo`

---

## Notes

- Iceberg's role vs Shard's role is still being sorted out as part of the roadmap. Some of what Iceberg was meant to do has moved to Shard.
- Don't add new orchestration logic here without checking first — the boundary between Iceberg and Shard is actively being defined.
- Don't touch the `IcebergCreator` scheduler (the `PriorityQueue`, thread dispatch, `run()` method) without explicit instruction.
