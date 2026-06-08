import java.util.*;

public class MonorepoBuildSystem {
    private final Map<String, List<String>> adjList = new HashMap<>();
    private final Map<String, Integer> visitedState = new HashMap<>(); // 0: White, 1: Gray, 2: Black
    private final List<String> cyclePath = new ArrayList<>();

    private static final int UNVISITED_WHITE = 0;
    private static final int VISITING_GRAY = 1;
    private static final int FULLY_PROCESSED_BLACK = 2;

    public void addDependency(String serviceA, String serviceB) {
        adjList.putIfAbsent(serviceA, new ArrayList<>());
        adjList.putIfAbsent(serviceB, new ArrayList<>());
        adjList.get(serviceA).add(serviceB);
        
        visitedState.put(serviceA, UNVISITED_WHITE);
        visitedState.put(serviceB, UNVISITED_WHITE);
    }

    public boolean detectAndReportCycle() {
        System.out.println("Initializing Razorpay CI Build-Graph Verification Pipeline...");
        System.out.println("Total Build Targets Registered: " + adjList.size());
        System.out.println("----------------------------------------------------------------");

        for (String node : adjList.keySet()) {
            if (visitedState.get(node) == UNVISITED_WHITE) {
                if (dfsCheck(node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsCheck(String currentNode) {
        visitedState.put(currentNode, VISITING_GRAY);
        cyclePath.add(currentNode);

        List<String> dependencies = adjList.get(currentNode);
        if (dependencies != null) {
            for (String neighbor : dependencies) {
                int state = visitedState.get(neighbor);

                if (state == VISITING_GRAY) {
                    System.out.println("❌ CRITICAL ERROR: BUILD FAILURE DETECTED [CYCLE SUSPECTED]");
                    System.out.println("================================================================");
                    System.out.print("Dependency Loop Traced: ");
                    int startIndex = cyclePath.indexOf(neighbor);
                    for (int i = startIndex; i < cyclePath.size(); i++) {
                        System.out.print(cyclePath.get(i) + " ➔ ");
                    }
                    System.out.println(neighbor);
                    System.out.println("================================================================");
                    return true;
                }

                if (state == UNVISITED_WHITE) {
                    if (dfsCheck(neighbor)) {
                        return true;
                    }
                }
            }
        }

        cyclePath.remove(cyclePath.size() - 1);
        visitedState.put(currentNode, FULLY_PROCESSED_BLACK);
        return false;
    }

    public static void main(String[] args) {
        MonorepoBuildSystem ciPipeline = new MonorepoBuildSystem();

        // Razorpay Case Study Dependency Mapping
        ciPipeline.addDependency("auth", "ledger");
        ciPipeline.addDependency("payments", "auth");
        ciPipeline.addDependency("payments", "fraud");
        ciPipeline.addDependency("kyc", "auth");
        ciPipeline.addDependency("ledger", "fraud");
        ciPipeline.addDependency("fraud", "notify");
        ciPipeline.addDependency("admin-ui", "payments");
        ciPipeline.addDependency("admin-ui", "kyc");
        ciPipeline.addDependency("customer-ui", "payments");
        ciPipeline.addDependency("gateway", "admin-ui");
        ciPipeline.addDependency("gateway", "customer-ui");
        
        // The Hidden Cycle Edge
        ciPipeline.addDependency("notify", "gateway");

        boolean cycleFound = ciPipeline.detectAndReportCycle();
        
        if (!cycleFound) {
            System.out.println("✅ SUCCESS: Build Graph is a valid DAG.");
        }
    }
}