# Memory Leaks and Optimizations Analysis

## Memory Leaks Found:

1. **LaserListener - BukkitRunnable not cancelled**: Particle task continues after plugin disable
2. **LaserListener - Maps grow indefinitely**: `lastLocation` and `lastAlertTime` never cleaned up
3. **PasscodeGUIListener - BukkitRunnable tasks not stored**: Can't cancel if plugin disables
4. **PasscodeGUIListener - Expired door access entries**: Only cleaned on access check, not periodically

## Optimizations Needed:

1. **LaserListener - Particle.DustOptions**: Created new on every spawn, should reuse
2. **LaserListener - getAllLasers()**: Creates new HashMap every 5 ticks
3. **LaserListener - Vector cloning**: Excessive cloning in distance calculations
4. **PasscodeGUIListener - Location key creation**: String concatenation inefficient
5. **Player quit cleanup**: No cleanup handlers for player data

