# Native Release Packaging Guide

Native addon releases use `.echo-addon` packages generated from module descriptors.

## Required Outputs

- `<module>-<version>.echo-addon`
- `<module>-<version>-sources.jar`
- `META-INF/echo.mod.json`
- `echo-addon-package.json`

Publish module artifacts from `knoxhack/ECHO-Modules` and consume them through Native Edition `moduleRequirements`.
