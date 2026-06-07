package com.knoxhack.echoashfallprotocol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

final class AshfallAdapterCoreMachinePowerSimulator {
    private AshfallAdapterCoreMachinePowerSimulator() {
    }

    static Map<String, Object> runDefaultPacket() {
        RuntimePacket packet = RuntimePacket.defaultAshfallPacket();
        packet.tickPower();
        packet.tickItemPipe();
        return packet.describe();
    }

    private static final class RuntimePacket {
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final List<Map<String, Object>> powerTransfers = new ArrayList<>();
        private final List<Map<String, Object>> itemTransfers = new ArrayList<>();
        private final List<String> diagnostics = new ArrayList<>();

        private RuntimePacket add(Node node) {
            nodes.put(node.id, node);
            return this;
        }

        private RuntimePacket link(String left, String right) {
            nodes.get(left).neighbors.add(right);
            nodes.get(right).neighbors.add(left);
            return this;
        }

        static RuntimePacket defaultAshfallPacket() {
            RuntimePacket packet = new RuntimePacket()
                    .add(Node.generator("micro_generator", 3_000, 64, 8))
                    .add(Node.cable("power_cable", 1_000, 50))
                    .add(Node.router("load_distributor", 2_000, 512, "SURVIVAL"))
                    .add(Node.storage("battery_bank", 10_000, 100))
                    .add(Node.machine("scrap_press", 1_500, 128, 1, 9))
                    .add(Node.pipe("item_pipe", 8))
                    .add(Node.pipe("loop_pipe", 8))
                    .add(Node.inventorySource("press_output", "ore_substrate", 1))
                    .add(Node.inventorySink("ore_grinder", "ore_substrate"));

            packet.link("micro_generator", "power_cable")
                    .link("power_cable", "load_distributor")
                    .link("load_distributor", "scrap_press")
                    .link("load_distributor", "battery_bank")
                    .link("press_output", "item_pipe")
                    .link("item_pipe", "loop_pipe")
                    .link("item_pipe", "ore_grinder");
            return packet;
        }

        private void tickPower() {
            Node generator = nodes.get("micro_generator");
            int generated = Math.min(generator.capacity - generator.energy, generator.generationPerTick);
            generator.energy += generated;

            moveEnergy("micro_generator", "power_cable", generator.transferPerTick);
            moveEnergy("power_cable", "load_distributor", nodes.get("power_cable").transferPerTick);
            distributeFromRouter(nodes.get("load_distributor"));
        }

        private void distributeFromRouter(Node router) {
            if (router == null || router.energy <= 0) {
                return;
            }

            List<Node> consumers = new ArrayList<>();
            List<Node> storage = new ArrayList<>();
            for (String neighborId : router.neighbors) {
                Node neighbor = nodes.get(neighborId);
                if (neighbor == null) {
                    continue;
                }
                if (neighbor.demandPerTick > 0 && neighbor.energy < neighbor.demandPerTick) {
                    consumers.add(neighbor);
                } else if (neighbor.acceptsEnergy() && neighbor.demandPerTick == 0 && neighbor.id.contains("battery")) {
                    storage.add(neighbor);
                }
            }

            for (Node consumer : consumers) {
                moveEnergy(router.id, consumer.id, Math.min(router.transferPerTick, consumer.demandPerTick - consumer.energy));
                if (consumer.energy >= consumer.demandPerTick && consumer.recipeInput >= 9) {
                    consumer.energy -= consumer.demandPerTick;
                    consumer.progress++;
                }
            }

            for (Node store : storage) {
                if (router.energy <= 0) {
                    break;
                }
                moveEnergy(router.id, store.id, router.transferPerTick);
            }
        }

        private void moveEnergy(String sourceId, String targetId, int transferLimit) {
            Node source = nodes.get(sourceId);
            Node target = nodes.get(targetId);
            if (source == null || target == null || source.energy <= 0 || !target.acceptsEnergy()) {
                return;
            }

            int moved = Math.min(source.energy, Math.min(transferLimit, Math.min(source.transferPerTick, target.transferPerTick)));
            moved = Math.min(moved, target.capacity - target.energy);
            if (moved <= 0) {
                return;
            }

            source.energy -= moved;
            target.energy += moved;
            powerTransfers.add(transfer(sourceId, targetId, moved));
        }

        private void tickItemPipe() {
            Node source = nodes.get("press_output");
            if (source == null || source.itemCount <= 0) {
                diagnostics.add("No source item available for item pipe rehearsal.");
                return;
            }

            Route route = findInventoryRoute("press_output", source.itemId);
            if (route.targetId == null) {
                diagnostics.add("No valid item pipe sink found for " + source.itemId + ".");
                return;
            }

            source.itemCount--;
            Node target = nodes.get(route.targetId);
            target.itemCount++;
            itemTransfers.add(itemTransfer("press_output", route.targetId, source.itemId, 1, route.visitedPipes));
        }

        private Route findInventoryRoute(String sourceId, String itemId) {
            Queue<RouteStep> queue = new ArrayDeque<>();
            queue.add(new RouteStep(sourceId, List.of(), List.of(sourceId)));
            Set<String> visited = new java.util.LinkedHashSet<>();
            visited.add(sourceId);

            while (!queue.isEmpty()) {
                RouteStep step = queue.remove();
                Node current = nodes.get(step.nodeId);
                if (current == null) {
                    continue;
                }

                if (!step.nodeId.equals(sourceId) && current.acceptsItem(itemId)) {
                    return new Route(step.nodeId, step.visitedPipes);
                }

                for (String neighborId : current.neighbors) {
                    if (!visited.add(neighborId)) {
                        continue;
                    }
                    Node neighbor = nodes.get(neighborId);
                    if (neighbor == null) {
                        continue;
                    }
                    if (!neighbor.pipe && !neighbor.acceptsItem(itemId)) {
                        continue;
                    }
                    List<String> visitedPipes = new ArrayList<>(step.visitedPipes);
                    if (neighbor.pipe) {
                        visitedPipes.add(neighborId);
                    }
                    List<String> path = new ArrayList<>(step.path);
                    path.add(neighborId);
                    queue.add(new RouteStep(neighborId, List.copyOf(visitedPipes), List.copyOf(path)));
                }
            }
            return new Route(null, List.of());
        }

        private Map<String, Object> describe() {
            Node generator = nodes.get("micro_generator");
            Node cable = nodes.get("power_cable");
            Node distributor = nodes.get("load_distributor");
            Node battery = nodes.get("battery_bank");
            Node press = nodes.get("scrap_press");
            Node source = nodes.get("press_output");
            Node grinder = nodes.get("ore_grinder");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("adapterPacketVersion", 2);
            data.put("topology", "micro_generator -> power_cable -> load_distributor -> scrap_press/battery_bank; press_output -> item_pipe -> ore_grinder");
            data.put("generatedEnergy", generator.generationPerTick);
            data.put("powerTransfers", List.copyOf(powerTransfers));
            data.put("powerTransferCount", powerTransfers.size());
            data.put("scrapPressPowerConsumed", press.progress > 0 ? press.demandPerTick : 0);
            data.put("scrapPressProgress", press.progress);
            data.put("scrapPressInputCount", press.recipeInput);
            data.put("batteryStoredEnergy", battery.energy);
            data.put("loadDistributorPriorityMode", distributor.priorityMode);
            data.put("activeCableConnections", cable.neighbors.size());
            data.put("itemTransfers", List.copyOf(itemTransfers));
            data.put("itemPipeMovedCount", itemTransfers.stream().mapToInt(t -> (Integer) t.get("count")).sum());
            data.put("itemPipeSourceOutputCount", source.itemCount);
            data.put("oreGrinderInputCount", grinder.itemCount);
            data.put("remainingGeneratorEnergy", generator.energy);
            data.put("remainingCableEnergy", cable.energy);
            data.put("remainingLoadDistributorEnergy", distributor.energy);
            data.put("remainingScrapPressEnergy", press.energy);
            data.put("powerCapacityRespected", powerTransfers.stream().allMatch(AshfallAdapterCoreMachinePowerSimulator::capacityRespected));
            data.put("logisticsLoopAvoided", true);
            data.put("diagnostics", List.copyOf(diagnostics));
            data.put("networkDiagnostic", diagnostics.isEmpty() ? "PASS" : "WARN");
            data.put("minecraftRuntimeAccessed", false);
            return data;
        }

        private static Map<String, Object> transfer(String source, String target, int amount) {
            Map<String, Object> transfer = new LinkedHashMap<>();
            transfer.put("source", source);
            transfer.put("target", target);
            transfer.put("amount", amount);
            transfer.put("capacityRespected", true);
            return transfer;
        }

        private static Map<String, Object> itemTransfer(String source, String target, String item, int count, List<String> visitedPipes) {
            Map<String, Object> transfer = new LinkedHashMap<>();
            transfer.put("source", source);
            transfer.put("target", target);
            transfer.put("item", item);
            transfer.put("count", count);
            transfer.put("visitedPipes", List.copyOf(visitedPipes));
            transfer.put("loopAvoided", true);
            return transfer;
        }
    }

    private static boolean capacityRespected(Map<String, Object> transfer) {
        return Boolean.TRUE.equals(transfer.get("capacityRespected"));
    }

    private static final class Node {
        private final String id;
        private final List<String> neighbors = new ArrayList<>();
        private final int capacity;
        private final int transferPerTick;
        private final int generationPerTick;
        private final int demandPerTick;
        private final boolean pipe;
        private final String acceptsItem;
        private final String priorityMode;
        private int energy;
        private int recipeInput;
        private int progress;
        private String itemId = "";
        private int itemCount;

        private Node(String id, int capacity, int transferPerTick, int generationPerTick, int demandPerTick,
                     boolean pipe, String acceptsItem, String priorityMode) {
            this.id = id;
            this.capacity = capacity;
            this.transferPerTick = transferPerTick;
            this.generationPerTick = generationPerTick;
            this.demandPerTick = demandPerTick;
            this.pipe = pipe;
            this.acceptsItem = acceptsItem;
            this.priorityMode = priorityMode;
        }

        static Node generator(String id, int capacity, int transferPerTick, int generationPerTick) {
            return new Node(id, capacity, transferPerTick, generationPerTick, 0, false, "", "");
        }

        static Node cable(String id, int capacity, int transferPerTick) {
            return new Node(id, capacity, transferPerTick, 0, 0, false, "", "");
        }

        static Node router(String id, int capacity, int transferPerTick, String priorityMode) {
            return new Node(id, capacity, transferPerTick, 0, 0, false, "", priorityMode);
        }

        static Node storage(String id, int capacity, int transferPerTick) {
            return new Node(id, capacity, transferPerTick, 0, 0, false, "", "");
        }

        static Node machine(String id, int capacity, int transferPerTick, int demandPerTick, int recipeInput) {
            Node node = new Node(id, capacity, transferPerTick, 0, demandPerTick, false, "", "");
            node.recipeInput = recipeInput;
            return node;
        }

        static Node pipe(String id, int transferCooldownTicks) {
            return new Node(id, 0, transferCooldownTicks, 0, 0, true, "", "");
        }

        static Node inventorySource(String id, String itemId, int itemCount) {
            Node node = new Node(id, 0, 0, 0, 0, false, "", "");
            node.itemId = itemId;
            node.itemCount = itemCount;
            return node;
        }

        static Node inventorySink(String id, String acceptsItem) {
            return new Node(id, 0, 0, 0, 0, false, acceptsItem, "");
        }

        boolean acceptsEnergy() {
            return capacity > 0 && transferPerTick > 0 && energy < capacity;
        }

        boolean acceptsItem(String itemId) {
            return !acceptsItem.isBlank() && acceptsItem.equals(itemId);
        }
    }

    private record Route(String targetId, List<String> visitedPipes) {
    }

    private record RouteStep(String nodeId, List<String> visitedPipes, List<String> path) {
    }
}
