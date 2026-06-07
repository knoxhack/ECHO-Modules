# ECHO RuntimeGuard Smoke Test

1. Run `./gradlew :echoruntimeguard:compileJava --warning-mode all`.
2. Run `./gradlew :echoruntimeguard:runGameTestServer --warning-mode all`.
3. Run `./gradlew :echoruntimeguard:build --warning-mode all`.
4. Launch a dev client.
5. Run `/echo_perf status`.
6. Run `/echo_perf mode potato`.
7. Run `/echo_perf mode balanced`.
8. Run `/echo_perf emergency on`, then `/echo_perf emergency off`.
9. Run `/echo_perf dump`.
10. Confirm matching `.txt` and `.json` reports appear under `run/echo-runtimeguard/reports/`.
11. Launch or compile the dedicated server run and confirm no client-only class crash occurs.
12. Run `/echo_perf particles` and confirm budget counters are present.
13. Run `/echo_perf multiblocks` and confirm validation queue data is present.
14. Confirm Core `EchoOptionalServices.runtimeGuard()` reports RuntimeGuard available in GameTests.
15. Confirm `SmartTickService` returns different rates for nearby, far, and emergency work in GameTests.
16. Confirm RuntimeGuard config files load.
17. Confirm server-impacting commands require gamemaster permissions.
18. If MultiblockCore is present, form a controller and confirm scheduled revalidation appears in `/echo_perf multiblocks`.
19. If HoloMap is present, run a manual sync and confirm `/echo_perf network` records HoloMap sync traffic.
20. If Lens is present, trigger server Deep Scan repeatedly and confirm RuntimeGuard throttles only when the Lens guard budget is exceeded.
21. If NetCore is present, confirm background duplicate packet accounting appears in RuntimeGuard network diagnostics without blocking gameplay/action packets.
