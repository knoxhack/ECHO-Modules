package com.knoxhack.echo.migrationcore;

import java.util.List;

public final class EchoMigrationCore {
    public static final String MODID = "echomigrationcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echodatacore",
            "echoschemacore",
            "echovalidationcore"
        );
    public static final List<String> PROVIDES = List.of(
            "migration.manifest",
            "migration.dry_run",
            "migration.rollback_report",
            "migration.id_aliases"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "migration_manifest",
            "dry_run_report",
            "rollback_compatibility_report",
            "renamed_id_map",
            "removed_module_notes"
        );

    public EchoMigrationCore() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
