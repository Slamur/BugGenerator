import java.io.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Queue;

public class BugField implements Comparable<BugField> {

    static final int SITE_N = 21, SITE_M = 31;
    static final int SITE_WALL = (char) (254), SITE_EMPTY = (char) 0;

    static final BugAcmp.BugStepsCalculator stepsCalculator;

    static final String TMP_DIRECTORY_NAME = "tmp", POPULATION_DIRECTORY_NAME = "population";

    static Queue<BugField> freeFields;

    static {
        stepsCalculator = new BugAcmp.BugStepsCalculator(SITE_N, SITE_M);

        new File(TMP_DIRECTORY_NAME).mkdir();
        new File(POPULATION_DIRECTORY_NAME).mkdir();

        freeFields = new ArrayDeque<>();
    }

    int n, m;
    boolean[][] wall;
    int[] masks;

    int steps;
    String printFileName;

    static BugField create() {
        int n = SITE_N, m = SITE_M;

        boolean[][] wall = new boolean[n][m];

        Arrays.fill(wall[0], true);
        Arrays.fill(wall[n - 1], true);

        for (int i = 0; i < n; ++i) {
            wall[i][0] = true;
            wall[i][m - 1] = true;
        }

        return create(wall);
    }

    static BugField create(boolean[][] wall) {
        BugField field = getInstance(wall);
        field.recalculateSteps();

        return field;
    }

    static BugField create(BugField other) {
        BugField field = getInstance(other.wall);
        field.steps = other.steps;

        return field;
    }

    public static BugField getInstance(boolean[][] wall) {
        if (freeFields.isEmpty()) {
            return new BugField(wall);
        } else {
            BugField freeField = freeFields.poll();
            freeField.constructor();

            if (freeField.wall == null) {
                freeField.wall = wall;
            } else {
                for (int i = 0; i < freeField.n; ++i) {
                    System.arraycopy(wall[i], 0, freeField.wall[i], 0, freeField.m);
                }
            }

            return freeField;
        }
    }

    public static void delete(BugField field) {
        freeFields.add(field);
    }

    BugField(boolean[][] wall) {
        constructor();
        this.wall = wall;
    }

    private void constructor() {
        this.n = SITE_N;
        this.m = SITE_M;

        this.steps = -1;
        this.printFileName = null;
    }

    public void initMasks() {
        if (masks == null) {
            this.masks = new int[n];
        }

        for (int i = 0; i < n; ++i) {
            masks[i] = 0;
            for (int j = 0; j < m; ++j) {
                masks[i] |= (wall[i][j] ? 1 : 0) << j;
            }
        }
    }

    public void setMasks(int[] masks) {
        if (this.masks == null) {
            this.masks = masks.clone();
        } else {
            System.arraycopy(masks, 0, this.masks, 0, n);
        }
    }

    public boolean equalsByMasks(BugField other) {
        for (int i = 0; i < n; ++i) {
            if (masks[i] != other.masks[i]) return false;
        }

        return true;
    }

    @Override
    public int compareTo(BugField other) {
        return -Integer.compare(steps, other.steps);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(n).append(' ').append(m).append('\n');

        for (boolean[] row : wall) {
            for (boolean cell : row) {
                stringBuilder.append(cell ? BugAcmp.WALL : BugAcmp.EMPTY);
            }

            stringBuilder.append('\n');
        }

        stringBuilder.append(steps).append('\n');

        return stringBuilder.toString();
    }

    public String getFileName() {
        return steps + " " + hashCode();
    }

    public void printToTmp() throws IOException {
        if (printFileName != null) return;
        printToDirectory(TMP_DIRECTORY_NAME);
    }

    public void printToPopulation() throws IOException {
        if (printFileName != null) return;
        printToDirectory(POPULATION_DIRECTORY_NAME);
    }

    void printToDirectory(String directoryName) throws IOException {
        printToZip(directoryName + "/" + getFileName());
    }

    private void printToZip(String name) throws IOException {
        printFileName = name + ".zip";
        OutputStream out = new FileOutputStream(printFileName);

        try {
            for (int j = 0; j < m; ++j) {
                for (int i = 0; i < n; ++i) {
                    out.write(wall[i][j] ? SITE_WALL : SITE_EMPTY);
                }
            }

            out.close();
        } catch (IOException e) {
            printFileName = null;
            throw e;
        }
    }

    public void removeZip() {
        if (printFileName== null) return;

        File file = new File(printFileName);
        file.delete();
    }

    public static BugField readFromZip(String name) throws IOException {
        InputStream in = new FileInputStream(name);

        int n = SITE_N, m = SITE_M;
        boolean[][] wall = new boolean[n][m];

        for (int j = 0; j < m; ++j) {
            for (int i = 0; i < n; ++i) {
                int cell = in.read();
                wall[i][j] = (cell == SITE_WALL);
            }
        }

        return create(wall);
    }

    public void recalculateSteps() {
        makeSoftConsistent();
        this.steps = stepsCalculator.getResult(wall);

//        if (!isConsistent()) {
//            System.out.println("Gotcha!");
//        } else {
//            this.steps = stepsCalculator.getResult(wall);
//        }
    }

    public void makeSoftConsistent() {
        wall[1][1] = false;
        wall[n - 2][m - 2] = false;
    }

    public boolean isConsistent() {
        for (int i = 0; i < n; ++i) {
            if (!wall[i][0] || !wall[i][m - 1]) return false;
        }

        for (int j = 0; j < m; ++j) {
            if (!wall[0][j] || !wall[n - 1][j]) return false;
        }

        return !wall[1][1] && !wall[n - 2][m - 2];
    }
}
