package com.knoxhack.echo.settlementcore.job;

import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import com.knoxhack.echo.settlementcore.api.JobType;
import com.knoxhack.echo.settlementcore.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Simple registry of settlement job definitions bound to job site blocks.
 */
public final class SettlementJobs {
    private static final List<JobDefinition> JOBS = new ArrayList<>();

    static {
        register("diver", "Diver", JobType.DIVER, ModBlocks.DIVERS_QUARTERS.id(),
            List.of("salvage", "underwater_repair", "exploration"));
        register("engineer", "Engineer", JobType.ENGINEER, ModBlocks.WORKSHOP.id(),
            List.of("crafting", "suit_upgrade", "machine_repair"));
        register("medic", "Medic", JobType.MEDIC, ModBlocks.MED_BAY.id(),
            List.of("healing", "decompression_treatment", "corruption_care"));
        register("cartographer", "Cartographer", JobType.CARTOGRAPHER, ModBlocks.SUBMERSIBLE_DOCK.id(),
            List.of("mapping", "route_planning", "survey"));
        register("deep_miner", "Deep Miner", JobType.DEEP_MINER, ModBlocks.DEEP_MINER_STATION.id(),
            List.of("abyssal_mining", "lattice_extraction", "pressure_drilling"));
        register("pressure_mechanic", "Pressure Mechanic", JobType.PRESSURE_MECHANIC, ModBlocks.PRESSURE_MECHANIC_STATION.id(),
            List.of("pump_repair", "suit_seal", "pressure_balancing"));
        register("xenobiologist", "Xenobiologist", JobType.XENO_BIOLOGIST, ModBlocks.XENO_BIOLOGIST_LAB.id(),
            List.of("sample_analysis", "hadal_study", "corruption_research"));
    }

    private SettlementJobs() {
    }

    public static List<JobDefinition> jobs() {
        return Collections.unmodifiableList(JOBS);
    }

    public static Optional<JobDefinition> byType(JobType type) {
        return JOBS.stream().filter(j -> j.type() == type).findFirst();
    }

    public static Optional<JobDefinition> byId(Identifier id) {
        return JOBS.stream().filter(j -> j.id().equals(id)).findFirst();
    }

    public static Map<String, String> poiBindings() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (JobDefinition job : JOBS) {
            result.put(job.type().name().toLowerCase(Locale.ROOT), job.poiBlock().toString());
        }
        return result;
    }

    private static void register(String path, String title, JobType type, Identifier poiBlock, List<String> duties) {
        JOBS.add(new JobDefinition(id(path), title, type, poiBlock, duties));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSettlementCore.MODID, path);
    }
}
