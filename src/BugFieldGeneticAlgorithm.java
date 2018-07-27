import java.io.*;
import java.util.*;

public class BugFieldGeneticAlgorithm {

    private static final Random rnd = new Random();

    private static String getStartFieldName(String bestFieldName) {
        if (null != bestFieldName) return bestFieldName;

        int bestFieldScore = -1;

        File directory = new File("");
        directory = new File(directory.getAbsolutePath());
        for (File fieldFile : directory.listFiles()) {
            String fileName = fieldFile.getName();

            if (!fileName.endsWith(".zip")) {
                continue;
            }

            int spaceIndex = fileName.indexOf(' ');
            if (spaceIndex < 0) {
                continue;
            }

            int fieldScore = Integer.parseInt(fileName.substring(0, spaceIndex));
            if (bestFieldScore < fieldScore) {
                bestFieldScore = fieldScore;
                bestFieldName = fileName;
            }
        }

        return bestFieldName;
    }

    private static final int POPULATION_SIZE = 20;
    private static final int HARD_MUTATION_SIZE = 12;
    private static final int FILTER_STOP_ITERATIONS = 300, FILTER_STOP_PERCENTAGE = 10, FILTER_STOP_DELTA = 200;
    private static final int NON_CHANGE_STOP_ITERATIONS = 2000;

    public static void main(String[] args) throws IOException {
        String startFieldName = getStartFieldName(null);
        BugField startField = BugField.readFromZip(startFieldName);

        final int populationSize = POPULATION_SIZE;
        BugFieldGeneticAlgorithm algo = new BugFieldGeneticAlgorithm(populationSize);

        for (int globalIteration = 0; ; ++globalIteration) {
            BugField[] fields = new BugField[populationSize];

            for (int i = 0; i < populationSize; ++i) {
                fields[i] = BugField.create();
            }

            fields[0] = startField;

            BugField[] hardMutants = BugFieldUtils.hardMutations(startField, populationSize - 1, HARD_MUTATION_SIZE);
            for (int i = 1; i < populationSize && i - 1 < hardMutants.length; ++i) {
                fields[i] = hardMutants[i - 1];
            }

            BugField[] bestFields = algo.process(
                    fields, startField.steps,
                    NON_CHANGE_STOP_ITERATIONS,
                    FILTER_STOP_ITERATIONS, FILTER_STOP_PERCENTAGE, FILTER_STOP_DELTA
            );

            startField = BugField.create(bestFields[0]);

            String populationDirectoryName = BugField.POPULATION_DIRECTORY_NAME + "_" + globalIteration;
            new File(populationDirectoryName).mkdir();

            for (BugField field : bestFields) {
                field.printToDirectory(populationDirectoryName);
                BugField.delete(field);
            }
        }
    }

    private final int populationSize;
    private final int[] bestSelected;
    private final BugField[] fields;

    private BugFieldGeneticAlgorithm(int populationSize) {
        this.populationSize = populationSize;
        this.bestSelected = new int[populationSize];

        this.fields = new BugField[populationSize + (populationSize + 1) * populationSize / 2];
    }

    private BugField[] process(BugField[] fieldsInput, int startFieldSteps,
                         int nonChangeStopIterations,
                         int filterStopIterations, int filterStopPercentage, int filterStopDelta) {
        final int filterStopSize = (populationSize * filterStopPercentage - 1) / 100 + 1;
        final int filterStopIndex = populationSize - filterStopSize - 1;

        System.arraycopy(fieldsInput, 0, fields, 0, populationSize);
        Arrays.sort(fields, 0, populationSize);

        boolean changed = startFieldSteps < fields[0].steps;

        try {
            for (int iteration = 0, lastUpdateIterationDelta = 1; ; ++iteration, lastUpdateIterationDelta++) {
                int bestSteps = fields[0].steps;

                childGenerating();
                selection();

                BugField nextBestField = fields[0];
                int nextBestSteps = nextBestField.steps;

                boolean updatedBest = bestSteps < nextBestSteps;
                changed |= updatedBest;

                if (updatedBest || iteration % 10 == 0) {
                    System.out.println(iteration + " " + nextBestSteps);

                    if (updatedBest) {
                        BugField bestField = BugField.create(nextBestField);
                        bestField.printToTmp();
                        BugField.delete(bestField);
                    }

                    if (updatedBest) {
                        lastUpdateIterationDelta = 0;
                    }
                }

                for (int i = 0; i < populationSize; ++i) {
                    fields[i].printToPopulation();
                }

                if (changed) {
                    if (lastUpdateIterationDelta > nonChangeStopIterations) break;
                    else if (lastUpdateIterationDelta > filterStopIterations) {
                        int filterDelta = nextBestSteps - fields[filterStopIndex].steps;
                        if (filterDelta <= filterStopDelta) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < populationSize; ++i) {
            fieldsInput[i] = BugField.create(fields[i]);
            fields[i].removeZip();
            BugField.delete(fields[i]);
        }

        return fieldsInput;
    }

    private static BugField crossover(BugField first, BugField second) {
        int n = first.n, m = first.m;

        boolean[][] wall = new boolean[n][m];
        for (int i = 0; i < n; ++i) {
            wall[i][0] = wall[i][m - 1] = true;
        }
        for (int j = 0; j < m; ++j) {
            wall[0][j] = wall[n - 1][j] = true;
        }

//        int sizeN = first.n / 4, sizeM = first.m / 4;
        int sizeN = 3, sizeM = 5;

        boolean[][] firstWall = first.wall, secondWall = second.wall;

        for (int i = 1; i < n - 1; i += sizeN) {
            for (int j = 1; j < m - 1; j += sizeM) {
//                boolean firstPart = rnd.nextDouble() < firstProb;
                boolean firstPart = rnd.nextBoolean();
                boolean[][] fromWall = (firstPart ? firstWall : secondWall);

                for (int dx = 0; dx < sizeN && i + dx < n - 1; ++dx) {
                    for (int dy = 0; dy < sizeM && j + dy < m - 1; ++dy) {
                        wall[i + dx][j + dy] = fromWall[i + dx][j + dy];
                    }
                }
            }
        }

        return BugField.create(wall);
    }

    private static BugField mutation(BugField field, boolean mutateSelf) {
        int sizeN = rnd.nextInt(field.n / 3) + 1;
        int sizeM = rnd.nextInt(field.m / 3) + 1;

        int startN = rnd.nextInt(field.n - sizeN - 1) + 1;
        int startM = rnd.nextInt(field.m - sizeM - 1) + 1;

        boolean[][] wall = (mutateSelf ? field.wall : new boolean[field.n][field.m]);
        if (!mutateSelf) {
            for (int i = 0; i < field.n; ++i) {
                wall[i] = field.wall[i].clone();
            }
        }

        for (int i = 0; i < sizeN; ++i) {
            for (int j = 0; j < sizeM; ++j) {
                wall[i + startN][j + startM] = rnd.nextBoolean();
            }
        }

        if (mutateSelf) {
            field.recalculateSteps();
        }

        return (mutateSelf ? field : BugField.create(wall));
    }

    private BugField[] childGenerating() throws IOException {
        for (int i = 0, index = populationSize; i < populationSize; ++i) {
            BugField firstParent = fields[i];
            fields[index++] = mutation(firstParent, false);

            for (int j = i + 1; j < populationSize; ++j, ++index) {
                BugField child = crossover(firstParent, fields[j]);
                child = mutation(child, true);

                fields[index] = child;
            }
        }

        return fields;
    }

    private BugField[] selection() {
        Arrays.sort(fields);

        BugField field;

        int selectedCount = 0;
        for (int i = 0; i < fields.length && selectedCount < populationSize; ++i) {
            if (i == 0 || fields[i].steps != fields[i - 1].steps) {
                bestSelected[selectedCount++] = i;
            }
        }

        for (int i = 0; i < populationSize; ++i) {
            if (bestSelected[i] != i) {
                field = fields[i];
                if (field != null) {
                    field.removeZip();
                    BugField.delete(field);
                }

                fields[i] = fields[bestSelected[i]];
                fields[bestSelected[i]] = null;
            }
        }

        for (int i = populationSize; i < fields.length; ++i) {
            field = fields[i];
            if (field != null) {
                field.removeZip();
                BugField.delete(field);
            }
        }

        return fields;
    }
}
