# ECHO Recovery API

## RecoveryService

ECHO Core exposes `EchoCoreServices` recovery hooks. ECHO Recovery registers itself as the provider.

### For Mod Developers

Use `EchoCoreServices.registerRecoveryService(...)` to register custom recovery behavior.

### Grave Placement Providers

Implement `GravePlacementProvider` to override where graves are placed:

```java
public interface GravePlacementProvider {
    Optional<BlockPos> getPlacement(ServerLevel level, ServerPlayer player, BlockPos deathPos);
}
```

Register via reflection or service loading.

### Recovery Rule Providers

Implement `RecoveryRuleProvider` to define custom item rules:

```java
public interface RecoveryRuleProvider {
    GraveRule getRule(ItemStack stack);
}
```

Rules:
- `SOULBOUND` - stays with player
- `ALWAYS_GRAVE` - always goes to grave
- `DROP_ON_DEATH` - drops normally
- `DESTROY_ON_DEATH` - destroyed
- `PROTECTED` - cannot be stolen
- `NO_GRAVE` - bypasses grave

## Item Tags

- `echorecovery:soulbound`
- `echorecovery:always_grave`
- `echorecovery:drop_on_death`
- `echorecovery:destroy_on_death`
- `echorecovery:protected`
- `echorecovery:no_grave`
