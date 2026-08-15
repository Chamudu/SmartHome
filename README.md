# Smart Home

A real-time home monitoring and control platform for Android. Smart Home brings floor planning,
device control, safety automation, camera monitoring, and usage insights into a single system.

## Platform components

- `mobile/` — native Android client built with Kotlin and Jetpack Compose
- `simulator/` — browser-based hardware simulator built with React and TypeScript
- `backend/` — trusted automation and notification services
- `firebase/` — database rules, indexes, and local emulator configuration
- `docs/` — product and engineering documentation

## Product capabilities

- Create simple multi-floor home layouts using rooms and a configurable grid
- Place and control outlets, switch gangs, lights, safety-critical appliances, and cameras
- Observe real-time device state across the Android client and hardware simulator
- Automatically switch off safety-critical appliances after a configured duration
- Schedule lighting and receive operational alerts
- Review device activity, usage history, and estimated energy and cost

The product is currently in the architecture and specification stage. See the
[documentation index](docs/README.md) and [delivery roadmap](docs/02-roadmap.md).

## Energy estimation

The Usage tab estimates each device's power usage from its confirmed ON/OFF history. Devices do not
report a live meter reading, so consumption is estimated from active time and an assumed wattage.

```
estimatedEnergy (kWh) = watts × activeDuration (ms) / 3_600_000_000
estimatedCost     ($) = estimatedEnergy (kWh) × pricePerKwh
```

- `activeDuration` is the accumulated time a device spent confirmed `ON` during the selected period
  (Today / 7 days / 30 days), computed by pairing ON/OFF state-transition events in
  `app/src/main/java/com/smarthome/app/domain/usage/UsageCalculator.kt`.
- `watts` is a per-profile default in `EnergyEstimator.defaultWatts`
  (`app/src/main/java/com/smarthome/app/domain/usage/EnergyEstimator.kt`):

  | Profile | Watts |
  | --- | --- |
  | OUTLET | 100 |
  | MULTI_SWITCH | 60 per channel |
  | SAFETY_OUTLET | 1500 |
  | LIGHT | 9 |
  | CAMERA | 5 |

- `pricePerKwh` is a fixed tariff (`DEFAULT_PRICE_PER_KWH = 0.20`). Multi-switch channels accumulate
  energy independently (each channel uses the per-channel wattage). Results are labeled "Est." because
  the wattage and tariff are assumptions, not meter readings.

The calculation is mirrored in `functions/src/energyEstimator.ts` and `simulator/src/energy.ts` with
JUnit and Vitest coverage in `EnergyEstimatorTest.kt` and `energyEstimator.test.ts`.
