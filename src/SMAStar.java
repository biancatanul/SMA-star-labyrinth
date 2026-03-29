import java.util.*;

public class SMAStar {

    static final String RESET   = "\033[0m";
    static final String BOLD    = "\033[1m";
    static final String BG_WALL = "\033[48;5;234m";
    static final String BG_OPEN = "\033[48;5;255m";
    static final String BG_CUR  = "\033[48;5;39m";   //current
    static final String BG_FRON = "\033[48;5;20m";   //frontier
    static final String BG_VIS  = "\033[48;5;220m";   //visited
    static final String BG_FORG = "\033[48;5;88m";   //forgotten
    static final String BG_PATH = "\033[48;5;28m";   //path
    static final String BG_SRC  = "\033[48;5;22m";   //start
    static final String BG_DST  = "\033[48;5;130m";  //goal
    static final String FG_W    = "\033[97m";         
    static final String FG_DIM  = "\033[90m";         
    static final String FG_CYAN = "\033[96m";
    static final String FG_YEL  = "\033[93m";
    static final String FG_GRN  = "\033[92m";
    static final String FG_RED  = "\033[91m";
    static final String FG_MAG  = "\033[95m";

    //node
    static class Node {
        int r, c;
        double g, f;
        Node parent;
        int depth;
        List<int[]> successors = new ArrayList<>();
        Map<String, Double> forgotten = new HashMap<>();

        Node(int r, int c, double g, Node parent, int[] dst) {
            this.r = r; this.c = c; this.g = g; this.parent = parent;
            this.depth = parent != null ? parent.depth + 1 : 0;
            double h = Math.abs(r - dst[0]) + Math.abs(c - dst[1]);
            this.f = Math.max(parent != null ? parent.f : 0, g + h);
        }
    }

    static String key(int r, int c) { return r + "," + c; }

    static int[] nextSuccessor(Node node, int[][] grid) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        int rows = grid.length, cols = grid[0].length;
        for (int[] d : dirs) {
            int nr = node.r + d[0], nc = node.c + d[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] == 0) continue;
            int[] cand = {nr, nc};
            boolean seen = node.successors.stream().anyMatch(s -> s[0]==cand[0] && s[1]==cand[1]);
            if (!seen) { node.successors.add(cand); return cand; }
        }
        return null;
    }

    //render grid in-place 
    static int lastLines = 0;

    static void render(int[][] grid, int[] src, int[] dst,
                       String currentKey, Set<String> frontier,
                       Set<String> visited, Set<String> forgotten,
                       Set<String> path, Map<String,String> labels,
                       String status, String logLine,
                       int expanded, int generated, int pruned, int memUsed, int maxNodes) {

        StringBuilder sb = new StringBuilder();

        if (lastLines > 0) sb.append("\033[").append(lastLines).append("A\033[J");

        int rows = grid.length, cols = grid[0].length;

        // Header
        sb.append(BOLD).append(FG_CYAN).append("  SMA* Pathfinder").append(RESET).append("\n");
        sb.append(FG_DIM).append("  mem: ").append(memUsed).append("/").append(maxNodes)
          .append("  expanded: ").append(expanded)
          .append("  generated: ").append(generated)
          .append("  pruned: ").append(pruned).append(RESET).append("\n");
        sb.append("\n");

        // Grid
        for (int r = 0; r < rows; r++) {
            sb.append("  ");
            for (int c = 0; c < cols; c++) {
                String k = key(r, c);
                boolean isSrc  = r == src[0] && c == src[1];
                boolean isDst  = r == dst[0] && c == dst[1];
                boolean isWall = grid[r][c] == 0;
                boolean isCur  = k.equals(currentKey);
                boolean isFron = frontier.contains(k);
                boolean isVis  = visited.contains(k);
                boolean isForg = forgotten.contains(k);
                boolean isPath = path.contains(k);

                String label = labels.getOrDefault(k, null);

                String bg, text;
                if (isWall)       { bg = BG_WALL; text = "   "; }
                else if (isSrc)   { bg = BG_SRC;  text = " S "; }
                else if (isDst)   { bg = BG_DST;  text = " D "; }
                else if (isPath)  { bg = BG_PATH; text = " O "; }
                else if (isCur)   { bg = BG_CUR;  text = label != null ? centerLabel(label) : " * "; }
                else if (isForg)  { bg = BG_FORG; text = label != null ? centerLabel(label) : " X "; }
                else if (isVis)   { bg = BG_VIS;  text = " V "; }
                else if (isFron)  { bg = BG_FRON; text = label != null ? centerLabel(label) : " . "; }
                else              { bg = BG_OPEN; text = "   "; }

                sb.append(bg).append(FG_W).append(BOLD).append(text).append(RESET);
                if (c < cols - 1) sb.append(" ");
            }
            sb.append("\n");
            if (r < rows - 1) sb.append("\n");
        }

        sb.append("\n");

        // Legend
        sb.append("  ")
          .append(BG_CUR).append("   ").append(RESET).append(FG_DIM).append(" current  ").append(RESET)
          .append(BG_FRON).append("   ").append(RESET).append(FG_DIM).append(" frontier  ").append(RESET)
          .append(BG_VIS).append("   ").append(RESET).append(FG_DIM).append(" visited  ").append(RESET)
          .append(BG_FORG).append("   ").append(RESET).append(FG_DIM).append(" forgotten  ").append(RESET)
          .append(BG_PATH).append("   ").append(RESET).append(FG_DIM).append(" path").append(RESET)
          .append("\n\n");

        // Memory bar
        int barWidth = 30;
        int filled = maxNodes > 0 ? (int)((double)memUsed / maxNodes * barWidth) : 0;
        filled = Math.min(filled, barWidth);
        sb.append("  mem [");
        for (int i = 0; i < barWidth; i++)
            sb.append(i < filled ? (memUsed >= maxNodes ? FG_RED+"#"+RESET : FG_CYAN+"#"+RESET) : FG_DIM+"-"+RESET);
        sb.append("] ").append(memUsed).append("/").append(maxNodes).append("\n\n");

        // Status + log
        String statusColor = status.startsWith("V") ? FG_GRN : status.startsWith("X") ? FG_RED : FG_YEL;
        sb.append("  ").append(statusColor).append(BOLD).append(status).append(RESET).append("\n");
        if (logLine != null && !logLine.isEmpty()) {
            sb.append("  ").append(FG_DIM).append(logLine).append(RESET).append("\n");
        }

        String out = sb.toString();
        System.out.print(out);

        // Count lines printed so we can overwrite next frame
        lastLines = (int) out.chars().filter(ch -> ch == '\n').count();
    }

    static String centerLabel(String s) {
        if (s.length() >= 3) return s.substring(0, 3);
        if (s.length() == 2) return " " + s;
        return " " + s + " ";
    }

    //SMA* with live rendering 
    static void smaStar(int[][] grid, int[] src, int[] dst, int maxNodes, int delayMs) throws InterruptedException {
        int rows = grid.length;
        Set<String> frontier  = new LinkedHashSet<>();
        Set<String> visited   = new LinkedHashSet<>();
        Set<String> forgotten = new LinkedHashSet<>();
        Set<String> path      = new LinkedHashSet<>();
        Map<String,String> labels = new HashMap<>();

        PriorityQueue<Node> pq = new PriorityQueue<>(
            Comparator.comparingDouble((Node n) -> n.f).thenComparingInt(n -> -n.depth));

        Node root = new Node(src[0], src[1], 0, null, dst);
        pq.add(root);
        frontier.add(key(src[0], src[1]));

        int expanded = 0, generated = 1, pruned = 0;

        render(grid, src, dst, key(src[0],src[1]), frontier, visited, forgotten, path,
               labels, "Starting...", "", expanded, generated, pruned, pq.size(), maxNodes);
        Thread.sleep(delayMs);

        int iter = 0;
        while (!pq.isEmpty() && iter++ < 3000) {
            Node current = pq.peek();
            String curKey = key(current.r, current.c);
            frontier.remove(curKey);
            labels.put(curKey, "f" + fmtShort(current.f));

            // Goal check
            if (current.r == dst[0] && current.c == dst[1]) {
                Node n = current; while (n != null) { path.add(key(n.r,n.c)); n = n.parent; }
                render(grid, src, dst, "", new HashSet<>(), visited, forgotten, path,
                       labels, "V Path found!  length=" + path.size() + "  f=" + fmt(current.f),
                       "", expanded, generated, pruned, pq.size(), maxNodes);
                return;
            }

            int[] next = nextSuccessor(current, grid);

            if (next == null) {
                pq.poll(); expanded++;
                visited.add(curKey);
                String logLine;
                if (current.parent != null) {
                    String pk = key(current.parent.r, current.parent.c);
                    current.parent.forgotten.put(curKey, current.f);
                    double minF = current.parent.forgotten.values().stream().mapToDouble(v->v).min().orElse(current.f);
                    current.parent.f = minF;
                    if (!pq.contains(current.parent)) { pq.add(current.parent); frontier.add(pk); visited.remove(pk);}
                    logLine = "backed up (" + curKey + ") -> parent (" + pk + ")  new f=" + fmt(minF);
                    render(grid, src, dst, pk, frontier, visited, forgotten, path,
                           labels, "<- Backing up...", logLine, expanded, generated, pruned, pq.size(), maxNodes);
                } else {
                    logLine = "root exhausted";
                    render(grid, src, dst, "", frontier, visited, forgotten, path,
                           labels, "Root exhausted", logLine, expanded, generated, pruned, pq.size(), maxNodes);
                }
                Thread.sleep(delayMs);
                continue;
            }

            String childKey = key(next[0], next[1]);
            double newG = current.g + 1;
            Node child;
            String logLine;

            if (current.forgotten.containsKey(childKey)) {
                double backed = current.forgotten.remove(childKey);
                current.forgotten.remove(childKey);
                forgotten.remove(childKey);
                child = new Node(next[0], next[1], newG, current, dst);
                child.f = Math.max(backed, child.f);
                logLine = "restored forgotten child (" + childKey + ")  f=" + fmt(child.f);
            } else {
                child = new Node(next[0], next[1], newG, current, dst);
                logLine = "expand -> (" + childKey + ")  g=" + (int)child.g + "  h=" + fmt(child.f - child.g) + "  f=" + fmt(child.f);
            }
            generated++;
            labels.put(childKey, "f" + fmtShort(child.f));

            if (child.r == dst[0] && child.c == dst[1]) {
                Node n = child; while (n != null) { path.add(key(n.r,n.c)); n = n.parent; }
                render(grid, src, dst, "", new HashSet<>(), visited, forgotten, path,
                       labels, "V Path found!  length=" + path.size() + "  f=" + fmt(child.f),
                       logLine, expanded, generated, pruned, pq.size(), maxNodes);
                return;
            }

            // Memory cap
            if (pq.size() >= maxNodes) {
                Node worst = null;
                for (Node n : pq) {
                    if (worst == null) { worst = n; continue; }
                    if (n.f > worst.f || (n.f == worst.f && n.depth < worst.depth)) worst = n;
                }
                if (worst != null && worst != current) {
                    pq.remove(worst);
                    pruned++;
                    String wk = key(worst.r, worst.c);
                    frontier.remove(wk);
                    forgotten.add(wk);
                    if (worst.parent != null) worst.parent.forgotten.put(wk, worst.f);
                    render(grid, src, dst, curKey, frontier, visited, forgotten, path,
                           labels, "Pruning (" + wk + ")  f=" + fmt(worst.f),
                           "memory full - worst node removed", expanded, generated, pruned, pq.size(), maxNodes);
                    Thread.sleep(delayMs);
                }
            }

            frontier.add(childKey);
            pq.add(child);

            render(grid, src, dst, curKey, frontier, visited, forgotten, path,
                   labels, "-> Expanding...", logLine, expanded, generated, pruned, pq.size(), maxNodes);
            Thread.sleep(delayMs);
        }

        render(grid, src, dst, "", frontier, visited, forgotten, path,
               labels, "X No path found within memory limit.", "", expanded, generated, pruned, pq.size(), maxNodes);
    }

    static String fmt(double v)      { return v == Math.floor(v) ? String.valueOf((int)v) : String.format("%.1f", v); }
    static String fmtShort(double v) { return v == Math.floor(v) ? String.valueOf((int)v) : String.format("%.0f", v); }

    //Main 
    public static void main(String[] args) throws InterruptedException {
        int delayMs = 500; // delay between steps for visualization

        int[][] grid1 = {
            {1,1,1,1,1},
            {1,0,0,0,1},
            {1,1,1,0,1},
            {0,0,1,0,1},
            {1,1,1,1,1}
        };

        int[][] grid2 = {
            {1,1,1,1,1,1,1,1,1},
            {0,1,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,0,1},
            {0,0,0,0,0,0,1,0,1},
            {1,1,1,1,1,0,1,0,1},
            {1,0,0,0,0,0,1,0,1},
            {1,1,1,1,1,1,1,1,1}
        };

        int[][] grid3 = {
            {1,1,1,1,1},
            {1,0,0,0,1},
            {1,0,1,0,1},
            {1,0,0,0,1},
            {1,1,1,1,1}
        };

        System.out.println("\033[2J\033[H"); // clear screen

        System.out.println("\033[96m\033[1m --- Grid 1: 5x5  (memory=20) ---\033[0m\n");
        smaStar(grid1, new int[]{0,0}, new int[]{4,4}, 20, delayMs);
        Thread.sleep(1500);

        lastLines = 0;
        System.out.println("\n\033[96m\033[1m --- Grid 2: 9x9  (memory=20) ---\033[0m\n");
        smaStar(grid2, new int[]{0,0}, new int[]{8,8}, 20, delayMs);
        Thread.sleep(1500);

        lastLines = 0;
        System.out.println("\n\033[96m\033[1m --- Grid 3: 5x5 blocked dest  (memory=20) ---\033[0m\n");
        smaStar(grid3, new int[]{0,0}, new int[]{2,2}, 20, delayMs);

        System.out.println();
    }
}