# ECHO PlayerCore Smoke Test

1. Build workspace.
2. Launch dedicated server or dev server.
3. Join as player.
4. Run /sethome.
5. Move away.
6. Run /home.
7. Confirm teleport.
8. Run /homes.
9. Run /delhome.
10. Confirm /home fails after deletion.
11. Run /spawn.
12. Confirm /back returns to previous location.
13. Die, respawn, run /back if enabled.
14. Run /rtp.
15. Confirm safe landing.
16. Try /rtp again and confirm cooldown.
17. Restart server and confirm homes persist.
18. Confirm no client-only classloading issues.
