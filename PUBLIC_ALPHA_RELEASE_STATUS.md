# Module Release Status

ECHO Modules is the first-party module source and artifact contract repo. It now tracks the ECHO Native `1.0.0-RC1` release candidate lane, not a generic Native Platform public alpha.

Only modules with compiled `.echo-addon` artifacts, descriptors, checksums, release metadata, and release-mode loader proof may be marked `ready-native`. Modules that rely on `local_build_output_classpath_fallback`, `source-packaged` output, or `--allow-missing-runtime` remain `blocked-with-reason` or non-player-facing.

Public module assets are staged through GitHub releases and imported into the Release Index after artifact and provenance checks pass.
