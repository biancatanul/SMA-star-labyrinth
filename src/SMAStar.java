import java.util.*;

public class SMAStar {
    static final int ROW = 5;
    static final int COL = 5;
    static final int MAX_NODES = 6; // memory limit

    static class Node {
        int row, col;
        double f, g, h;
        Node parent;
        int depth;
        List<int[]> successors;
        Map<String, Double> forgottenChildren;

        Node(int row, int col, double g, double h, Node parent) {
            this.row = row;
            this.col = col;
            this.g = g;
            this.h = h;
            this.f = Math.max(parent != null ? parent.f : 0, g + h);
            this.parent = parent;
            this.depth = parent != null ? parent.depth + 1 : 0;
            this.successors = new ArrayList<>();
            this.forgottenChildren = new HashMap<>();
        }
    }
    static boolean isValid(int row, int col) {
        return row >= 0 && row < ROW && col >= 0 && col < COL;
    }

    static boolean isUnBlocked(int[][] grid, int row, int col) {
        return grid[row][col] == 1;
    }

    static boolean isDestination(int row, int col, int[] dest) {
        return row == dest[0] && col == dest[1];
    }

    static double calcH(int row, int col, int[] dest) {
        return Math.abs(row - dest[0]) + Math.abs(col - dest[1]);
    }
    static void tracePath(Node node) {
        List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(new int[]{node.row, node.col});
            node = node.parent;
        }
        Collections.reverse(path);
        System.out.println("Path found:");
        for (int[] p : path) {
            System.out.print("(" + p[0] + "," + p[1] + ") ");
        }
        System.out.println();
    }
    static int[] nextSuccessor(Node node, int[][] grid, int[] dest) {
        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] dir : directions) {
            int newRow = node.row + dir[0];
            int newCol = node.col + dir[1];
            if (!isValid(newRow, newCol) || !isUnBlocked(grid, newRow, newCol))
                continue;
            int[] candidate = {newRow, newCol};
            boolean alreadyGenerated = false;
            for (int[] s : node.successors) {
                if (s[0] == candidate[0] && s[1] == candidate[1]) {
                    alreadyGenerated = true;
                    break;
                }
            }
            if (!alreadyGenerated) {
                node.successors.add(candidate);
                return candidate;
            }
        }
        return null; // all successors generated
    }
    static void smaStar(int[][] grid, int[] src, int[] dest) {
        PriorityQueue<Node> queue = new PriorityQueue<>(
                Comparator.comparingDouble((Node n) -> n.f)
                        .thenComparingInt(n -> -n.depth)
        );

        double startH = calcH(src[0], src[1], dest);
        Node root = new Node(src[0], src[1], 0, startH, null);
        queue.add(root);

        while (!queue.isEmpty()) {
            Node current = queue.peek();

            if (isDestination(current.row, current.col, dest)) {
                tracePath(current);
                return;
            }

            int[] nextPos = nextSuccessor(current, grid, dest);

            if (nextPos == null) {
                // all successors generated, back up f to parent
                queue.poll();
                if (current.parent != null) {
                    current.parent.forgottenChildren.put(
                            current.row + "," + current.col, current.f);
                    double minF = Double.MAX_VALUE;
                    for (double val : current.parent.forgottenChildren.values())
                        minF = Math.min(minF, val);
                    current.parent.f = minF;
                    if (!queue.contains(current.parent))
                        queue.add(current.parent);
                }
                continue;
            }

            double newG = current.g + 1;
            double newH = calcH(nextPos[0], nextPos[1], dest);
            Node child;

            // check if this was a forgotten child
            String key = nextPos[0] + "," + nextPos[1];
            if (current.forgottenChildren.containsKey(key)) {
                double backedUpF = current.forgottenChildren.remove(key);
                child = new Node(nextPos[0], nextPos[1], newG, newH, current);
                child.f = Math.max(backedUpF, child.f);
            } else {
                child = new Node(nextPos[0], nextPos[1], newG, newH, current);
            }

            if (isDestination(child.row, child.col, dest)) {
                tracePath(child);
                return;
            }

            // if memory full, drop worst node
            if (queue.size() >= MAX_NODES) {
                // find shallowest node with highest f
                Node worst = null;
                for (Node n : queue) {
                    if (worst == null) { worst = n; continue; }
                    if (n.f > worst.f || (n.f == worst.f && n.depth < worst.depth))
                        worst = n;
                }
                if (worst != null && worst != current) {
                    queue.remove(worst);
                    if (worst.parent != null) {
                        worst.parent.forgottenChildren.put(
                                worst.row + "," + worst.col, worst.f);
                    }
                }
            }

            queue.add(child);
        }

        System.out.println("No solution found within memory limit.");
    }

    public static void main(String[] args) {
        int[][] grid = {
                {1, 1, 1, 1, 1},
                {1, 0, 0, 0, 1},
                {1, 1, 1, 0, 1},
                {0, 0, 1, 0, 1},
                {1, 1, 1, 1, 1}
        };

        int[] src  = {0, 0};
        int[] dest = {4, 4};

        smaStar(grid, src, dest);
    }
}
