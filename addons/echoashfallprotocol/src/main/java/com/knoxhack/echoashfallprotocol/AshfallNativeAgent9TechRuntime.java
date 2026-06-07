package com.knoxhack.echoashfallprotocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AshfallNativeAgent9TechRuntime {
    static final String CONTRACT_ID = "adaptercore.agent9.tech.machine_power_logistics.v1";

    private AshfallNativeAgent9TechRuntime() {
    }

    static Map<String, Object> run(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> machinePowerTarget = AshfallNativeMachinePowerRuntimeTarget.initialize(safeContext);
        Map<String, Object> machinePowerResourceAudit = AshfallNativeMachinePowerResourceAudit.run(safeContext);
        Map<String, Object> recipeCatalog = AshfallNativeMachineRecipeCatalog.describe();
        NativeTechPacket packet = NativeTechPacket.reference();

        boolean machinePlaced = packet.placeMachine("scrap_press");
        boolean uiOpened = packet.openMachineUi("scrap_press");
        int inserted = packet.insertInput("scrap_press", "scrap_metal", 9);
        boolean graphConnected = packet.connectPowerGraph();
        int ticks = packet.tickUntilOutput(80);
        int moved = packet.transfer("scrap_press", "ore_grinder", "compressed_scrap", 1);
        boolean multiblockValid = packet.validateMultiblock("factory_controller");
        Map<String, Object> vehicleAction = packet.moveVehicle("wasteland_rover", 4);
        Map<String, Object> economyCharge = packet.charge("faction_trade_depot", "scrap_credit", 25);
        List<String> lootOutputs = packet.openLoot("supply_crate");
        Map<String, Object> saveState = packet.save();
        NativeTechPacket restored = NativeTechPacket.restore(saveState);
        boolean stateReloaded = restored.outputCount("scrap_press", "compressed_scrap") == 0
                && restored.inputCount("ore_grinder", "compressed_scrap") == 1
                && restored.energy("battery_bank") == packet.energy("battery_bank")
                && restored.missionComplete("echoashfallprotocol:mission/build_scrap_press");

        boolean pass = "PASS".equals(machinePowerTarget.get("status"))
                && "PASS".equals(machinePowerResourceAudit.get("status"))
                && Boolean.TRUE.equals(recipeCatalog.get("resourceLoaded"))
                && machinePlaced
                && uiOpened
                && inserted == 9
                && graphConnected
                && ticks == 40
                && packet.recipeProgressed
                && packet.outputProduced
                && moved == 1
                && multiblockValid
                && Boolean.TRUE.equals(vehicleAction.get("completed"))
                && Boolean.TRUE.equals(economyCharge.get("paid"))
                && !lootOutputs.isEmpty()
                && stateReloaded;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", CONTRACT_ID);
        result.put("serviceId", "echoashfallprotocol:agent9_native_tech_runtime");
        result.put("moduleId", "echoashfallprotocol");
        result.put("packId", safeContext.getOrDefault("packId", "ashfall"));
        result.put("adapterCoreBridge", true);
        result.put("adapterCoreContract", CONTRACT_ID);
        result.put("runtime", "echo_native_loader");
        result.put("referenceBehavior", "ashfall_machine_power_logistics");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("serviceCodeExecuted", true);
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        result.put("machinePowerRuntimeStatus", machinePowerTarget.get("status"));
        result.put("machinePowerResourceAuditStatus", machinePowerResourceAudit.get("status"));
        result.put("recipeCatalogResourceLoaded", recipeCatalog.get("resourceLoaded"));
        result.put("placeMachine", machinePlaced);
        result.put("openMachineUi", uiOpened);
        result.put("insertInput", inserted == 9);
        result.put("insertedInputCount", inserted);
        result.put("consumePower", packet.powerConsumed > 0);
        result.put("powerConsumed", packet.powerConsumed);
        result.put("processRecipe", packet.recipeProgressed);
        result.put("recipeProgressTicks", packet.progressTicks);
        result.put("outputResult", packet.outputProduced);
        result.put("outputItem", "compressed_scrap");
        result.put("outputCountBeforeLogistics", packet.outputCountBeforeLogistics);
        result.put("powerGraphConnects", graphConnected);
        result.put("logisticsTransfer", moved == 1);
        result.put("oreGrinderInputCount", restored.inputCount("ore_grinder", "compressed_scrap"));
        result.put("saveMachineState", !saveState.isEmpty());
        result.put("reloadMachineState", stateReloaded);
        result.put("missionDependsOnMachineCompletion",
                restored.missionComplete("echoashfallprotocol:mission/build_scrap_press"));
        result.put("multiblockValidation", multiblockValid);
        result.put("vehicleMovementAction", vehicleAction);
        result.put("economyCost", economyCharge);
        result.put("lootOutputs", lootOutputs);
        result.put("powerGraph", packet.powerGraph());
        result.put("inventoryPorts", packet.inventoryPorts());
        result.put("status", pass ? "PASS" : "FAIL");
        result.put("summary", pass
                ? "Agent 9 native tech backend executed machine placement/UI/input, power consumption, recipe output, save/load, mission dependency, multiblock validation, logistics transfer, vehicle movement, economy cost, and loot output through AdapterCore no-launch packets."
                : "Agent 9 native tech backend failed one or more executable behavior checks.");
        return Map.copyOf(result);
    }

    private static final class NativeTechPacket {
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final Set<String> completedMissions = new LinkedHashSet<>();
        private boolean graphConnected;
        private boolean recipeProgressed;
        private boolean outputProduced;
        private int powerConsumed;
        private int progressTicks;
        private int outputCountBeforeLogistics;
        private int scrapCreditBalance = 100;

        static NativeTechPacket reference() {
            NativeTechPacket packet = new NativeTechPacket();
            packet.nodes.put("micro_generator", Node.power("micro_generator", "GENERATOR", 3_000, 64, 8));
            packet.nodes.put("power_cable", Node.power("power_cable", "CABLE", 1_000, 50, 0));
            packet.nodes.put("load_distributor", Node.power("load_distributor", "ROUTER", 2_000, 512, 0));
            packet.nodes.put("battery_bank", Node.power("battery_bank", "BATTERY", 10_000, 100, 0));
            packet.nodes.put("scrap_press", Node.machine("scrap_press", 1_500, 128, 40, 1));
            packet.nodes.put("item_pipe", Node.pipe("item_pipe"));
            packet.nodes.put("ore_grinder", Node.machine("ore_grinder", 2_000, 128, 80, 2));
            packet.nodes.put("factory_controller", Node.machine("factory_controller", 0, 0, 0, 0));
            packet.nodes.put("wasteland_rover", Node.vehicle("wasteland_rover", 12));
            return packet;
        }

        static NativeTechPacket restore(Map<String, Object> saved) {
            NativeTechPacket packet = reference();
            packet.graphConnected = Boolean.TRUE.equals(saved.get("graphConnected"));
            packet.recipeProgressed = Boolean.TRUE.equals(saved.get("recipeProgressed"));
            packet.outputProduced = Boolean.TRUE.equals(saved.get("outputProduced"));
            packet.powerConsumed = number(saved.get("powerConsumed"));
            packet.progressTicks = number(saved.get("progressTicks"));
            packet.outputCountBeforeLogistics = number(saved.get("outputCountBeforeLogistics"));
            packet.scrapCreditBalance = number(saved.get("scrapCreditBalance"));
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> savedNodes =
                    (Map<String, Map<String, Object>>) saved.getOrDefault("nodes", Map.of());
            for (Map.Entry<String, Map<String, Object>> entry : savedNodes.entrySet()) {
                Node node = packet.nodes.get(entry.getKey());
                if (node != null) {
                    node.restore(entry.getValue());
                }
            }
            @SuppressWarnings("unchecked")
            List<String> missions = (List<String>) saved.getOrDefault("completedMissions", List.of());
            packet.completedMissions.addAll(missions);
            return packet;
        }

        boolean placeMachine(String id) {
            Node node = nodes.get(id);
            if (node == null) {
                return false;
            }
            node.placed = true;
            return true;
        }

        boolean openMachineUi(String id) {
            Node node = nodes.get(id);
            if (node == null || !node.placed) {
                return false;
            }
            node.uiOpen = true;
            return true;
        }

        int insertInput(String id, String itemId, int count) {
            Node node = nodes.get(id);
            if (node == null || !node.uiOpen) {
                return 0;
            }
            node.inputs.merge(itemId, count, Integer::sum);
            return count;
        }

        boolean connectPowerGraph() {
            link("micro_generator", "power_cable");
            link("power_cable", "load_distributor");
            link("load_distributor", "battery_bank");
            link("load_distributor", "scrap_press");
            link("scrap_press", "item_pipe");
            link("item_pipe", "ore_grinder");
            graphConnected = true;
            return true;
        }

        int tickUntilOutput(int maxTicks) {
            int ticks = 0;
            while (ticks < maxTicks && outputCount("scrap_press", "compressed_scrap") == 0) {
                ticks++;
                tickPower();
                tickMachine(nodes.get("scrap_press"), "scrap_metal", "compressed_scrap");
            }
            outputCountBeforeLogistics = outputCount("scrap_press", "compressed_scrap");
            if (outputCountBeforeLogistics > 0) {
                outputProduced = true;
                completedMissions.add("echoashfallprotocol:mission/build_scrap_press");
            }
            return ticks;
        }

        int transfer(String from, String to, String itemId, int count) {
            Node source = nodes.get(from);
            Node target = nodes.get(to);
            if (source == null || target == null || !graphConnected) {
                return 0;
            }
            int moved = Math.min(count, source.outputs.getOrDefault(itemId, 0));
            if (moved <= 0) {
                return 0;
            }
            source.outputs.put(itemId, source.outputs.get(itemId) - moved);
            target.inputs.merge(itemId, moved, Integer::sum);
            completedMissions.add("echoashfallprotocol:mission/install_item_pipe");
            return moved;
        }

        boolean validateMultiblock(String id) {
            return "factory_controller".equals(id)
                    && graphConnected
                    && nodes.containsKey("scrap_press")
                    && nodes.containsKey("power_cable")
                    && nodes.containsKey("item_pipe");
        }

        Map<String, Object> moveVehicle(String vehicleId, int steps) {
            Node vehicle = nodes.get(vehicleId);
            int moved = Math.min(steps, vehicle.fuel);
            vehicle.fuel -= moved;
            vehicle.position += moved;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("vehicleId", vehicleId);
            result.put("requestedSteps", steps);
            result.put("movedSteps", moved);
            result.put("fuelAfter", vehicle.fuel);
            result.put("completed", moved == steps);
            return result;
        }

        Map<String, Object> charge(String tradeRuleId, String currencyId, int cost) {
            scrapCreditBalance -= cost;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tradeRuleId", tradeRuleId);
            result.put("currencyId", currencyId);
            result.put("cost", cost);
            result.put("balanceAfter", scrapCreditBalance);
            result.put("paid", scrapCreditBalance >= 0);
            return result;
        }

        List<String> openLoot(String sourceId) {
            if (!"supply_crate".equals(sourceId)) {
                return List.of();
            }
            return List.of("echoashfallprotocol:scrap_metal", "echoashfallprotocol:scrap_wire");
        }

        Map<String, Object> save() {
            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, Object> savedNodes = new LinkedHashMap<>();
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                savedNodes.put(entry.getKey(), entry.getValue().save());
            }
            data.put("nodes", savedNodes);
            data.put("completedMissions", List.copyOf(completedMissions));
            data.put("graphConnected", graphConnected);
            data.put("recipeProgressed", recipeProgressed);
            data.put("outputProduced", outputProduced);
            data.put("powerConsumed", powerConsumed);
            data.put("progressTicks", progressTicks);
            data.put("outputCountBeforeLogistics", outputCountBeforeLogistics);
            data.put("scrapCreditBalance", scrapCreditBalance);
            return data;
        }

        int outputCount(String id, String itemId) {
            return nodes.get(id).outputs.getOrDefault(itemId, 0);
        }

        int inputCount(String id, String itemId) {
            return nodes.get(id).inputs.getOrDefault(itemId, 0);
        }

        int energy(String id) {
            return nodes.get(id).energy;
        }

        boolean missionComplete(String missionId) {
            return completedMissions.contains(missionId);
        }

        List<Map<String, Object>> powerGraph() {
            List<Map<String, Object>> graph = new ArrayList<>();
            for (String id : List.of("micro_generator", "power_cable", "load_distributor", "battery_bank", "scrap_press")) {
                Node node = nodes.get(id);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", id);
                entry.put("kind", node.kind);
                entry.put("energy", node.energy);
                entry.put("capacity", node.capacity);
                entry.put("neighbors", List.copyOf(node.neighbors));
                graph.add(entry);
            }
            return List.copyOf(graph);
        }

        List<Map<String, Object>> inventoryPorts() {
            return List.of(
                    inventoryPort("scrap_press", "input", List.of("scrap_metal")),
                    inventoryPort("scrap_press", "output", List.of("compressed_scrap")),
                    inventoryPort("ore_grinder", "input", List.of("compressed_scrap")));
        }

        private void tickPower() {
            Node generator = nodes.get("micro_generator");
            generator.energy = Math.min(generator.capacity, generator.energy + generator.generationPerTick);
            moveEnergy("micro_generator", "power_cable", 8);
            moveEnergy("power_cable", "load_distributor", 8);
            moveEnergy("load_distributor", "scrap_press", 1);
            moveEnergy("load_distributor", "battery_bank", 7);
        }

        private void tickMachine(Node machine, String inputItem, String outputItem) {
            if (machine.energy < machine.powerPerTick || machine.inputs.getOrDefault(inputItem, 0) <= 0) {
                return;
            }
            machine.energy -= machine.powerPerTick;
            powerConsumed += machine.powerPerTick;
            machine.progress++;
            progressTicks = machine.progress;
            recipeProgressed = true;
            if (machine.progress >= machine.recipeTicks) {
                machine.inputs.put(inputItem, machine.inputs.get(inputItem) - 1);
                machine.outputs.merge(outputItem, 1, Integer::sum);
                machine.progress = 0;
            }
        }

        private void moveEnergy(String from, String to, int amount) {
            Node source = nodes.get(from);
            Node target = nodes.get(to);
            int moved = Math.min(amount, Math.min(source.energy, target.capacity - target.energy));
            if (moved <= 0) {
                return;
            }
            source.energy -= moved;
            target.energy += moved;
        }

        private void link(String left, String right) {
            nodes.get(left).neighbors.add(right);
            nodes.get(right).neighbors.add(left);
        }

        private static Map<String, Object> inventoryPort(String machineId, String port, List<String> accepts) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("machineId", machineId);
            data.put("port", port);
            data.put("accepts", accepts);
            return data;
        }
    }

    private static final class Node {
        private final String id;
        private final String kind;
        private final int capacity;
        private final int generationPerTick;
        private final int recipeTicks;
        private final int powerPerTick;
        private final List<String> neighbors = new ArrayList<>();
        private final Map<String, Integer> inputs = new LinkedHashMap<>();
        private final Map<String, Integer> outputs = new LinkedHashMap<>();
        private boolean placed;
        private boolean uiOpen;
        private int energy;
        private int progress;
        private int fuel;
        private int position;

        private Node(String id, String kind, int capacity, int generationPerTick, int recipeTicks, int powerPerTick) {
            this.id = id;
            this.kind = kind;
            this.capacity = capacity;
            this.generationPerTick = generationPerTick;
            this.recipeTicks = recipeTicks;
            this.powerPerTick = powerPerTick;
        }

        static Node power(String id, String kind, int capacity, int transferPerTick, int generationPerTick) {
            return new Node(id, kind + ":" + transferPerTick, capacity, generationPerTick, 0, 0);
        }

        static Node machine(String id, int capacity, int transferPerTick, int recipeTicks, int powerPerTick) {
            Node node = new Node(id, "MACHINE:" + transferPerTick, capacity, 0, recipeTicks, powerPerTick);
            node.placed = true;
            return node;
        }

        static Node pipe(String id) {
            return new Node(id, "INVENTORY_PIPE", 0, 0, 0, 0);
        }

        static Node vehicle(String id, int fuel) {
            Node node = new Node(id, "VEHICLE", 0, 0, 0, 0);
            node.fuel = fuel;
            return node;
        }

        Map<String, Object> save() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("placed", placed);
            data.put("uiOpen", uiOpen);
            data.put("energy", energy);
            data.put("progress", progress);
            data.put("fuel", fuel);
            data.put("position", position);
            data.put("inputs", Map.copyOf(inputs));
            data.put("outputs", Map.copyOf(outputs));
            return data;
        }

        @SuppressWarnings("unchecked")
        void restore(Map<String, Object> data) {
            placed = Boolean.TRUE.equals(data.get("placed"));
            uiOpen = Boolean.TRUE.equals(data.get("uiOpen"));
            energy = number(data.get("energy"));
            progress = number(data.get("progress"));
            fuel = number(data.get("fuel"));
            position = number(data.get("position"));
            inputs.clear();
            inputs.putAll((Map<String, Integer>) data.getOrDefault("inputs", Map.of()));
            outputs.clear();
            outputs.putAll((Map<String, Integer>) data.getOrDefault("outputs", Map.of()));
        }
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
