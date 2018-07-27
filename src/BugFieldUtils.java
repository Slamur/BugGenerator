import java.io.IOException;
import java.util.*;

public class BugFieldUtils {

    static class BugFieldMinimumHeap extends PriorityQueue<BugField> {

        BugFieldMinimumHeap() {
            super((a, b) -> Integer.compare(a.steps, b.steps));
        }

        void fill(BugField[] fields, int size) {
            for (int i = size - 1; i >= 0; --i) {
                fields[i] = poll();
            }
        }
    }

    static int getBit(int mask, int bit) { return ((mask >> bit) & 1); }

    static int getBit(long mask, int bit) { return (int)((mask >> bit) & 1); }

    static boolean checkBit(int mask, int bit){
        return getBit(mask, bit) != 0;
    }

    @SuppressWarnings("unused")
    static boolean checkBit(long mask, int bit){
        return getBit(mask, bit) != 0;
    }

    static BugField[] hardCrossover(BugField firstField, BugField secondField, int populationSize, int maxCount) throws IOException {
        BugFieldMinimumHeap bestFields = new BugFieldMinimumHeap();
        bestFields.add(firstField);
        bestFields.add(secondField);

        BugField tmpChildField = BugField.create();
        boolean[][] childWall = tmpChildField.wall;

        Set<String> unique = new HashSet<>();

        BugField totalBestField = (firstField.steps > secondField.steps ? firstField : secondField);
        hardCrossover(firstField, secondField, populationSize, maxCount, bestFields, unique, totalBestField, childWall, true);

        BugField[] resultFields = new BugField[populationSize];
        bestFields.fill(resultFields, populationSize);
        return resultFields;
    }


    static BugField hardCrossover(BugField firstField, BugField secondField, int populationSize, int maxCount, BugFieldMinimumHeap bestFields, Set<String> unique, BugField totalBestField, boolean[][] childWall, boolean verbose) throws IOException {
        int n = firstField.n, m = firstField.m;
        int innerN = n - 2, innerM = m - 2;

        boolean[][] firstWall = firstField.wall, secondWall = secondField.wall;

        int lastNCount = -1;
        for (int nCountIterator = 1; nCountIterator <= maxCount; ++nCountIterator) {
            int mCount = maxCount / nCountIterator;
            int nCount = maxCount / mCount;

            if (lastNCount == nCount) continue;
            lastNCount = nCount;

            int nSize = (innerN - 1) / nCount + 1;
            int mSize = (innerM - 1) / mCount + 1;

            int size = nCount * mCount;

            int[][] blockStarts = new int[size][2];

            int startIndex = 0;
            for (int i = 1; i <= innerN; i += nSize) {
                for (int j = 1; j <= innerM; j += mSize, ++startIndex) {
                    blockStarts[startIndex][0] = i;
                    blockStarts[startIndex][1] = j;
                }
            }

            size = startIndex;
            int maskSize = (1 << size), maskLimit = maskSize - 1; // we don't need all '0' and all '1'

            int verbosePart = maskSize / 10;

            for (int mask = 1; mask < maskLimit; ++mask) {
                for (int bit = 0; bit < size; ++bit) {
                    int nStart = blockStarts[bit][0], mStart = blockStarts[bit][1];
                    boolean[][] parentWall = (checkBit(mask, bit) ? secondWall : firstWall);

                    for (int i = 0, x = nStart; i < nSize && x <= innerN; ++i, ++x) {
                        for (int j = 0, y = mStart; j < mSize && y <= innerM; ++j, ++y) {
                            childWall[x][y] = parentWall[x][y];
                        }
                    }
                }

                BugField childField = BugField.create(childWall);
                int childSteps = childField.steps;

                if (verbose && mask % verbosePart == 0) {
                    System.out.println(
                            String.format("Verbose nCount(nSize) %d(%d), mCount(mSize) %d(%d), mask %s, steps %d",
                                    nCount, nSize, mCount, mSize,
                                    Integer.toBinaryString(maskSize + mask).substring(1),
                                    totalBestField.steps
                            )
                    );
                }

                if (bestFields.size() < populationSize || childSteps > bestFields.peek().steps) {
                    boolean[][] clonedChildWall = new boolean[n][m];
                    for (int row = 0; row < n; ++row) {
                        System.arraycopy(childWall[row], 0, clonedChildWall[row], 0, m);
                    }

                    childField = new BugField(clonedChildWall);
                    childField.steps = childSteps;

                    if (unique.add(childField.toString())) {
                        bestFields.add(childField);
                        if (bestFields.size() > populationSize) {
                            BugField minField = bestFields.poll();
                            unique.remove(minField.toString());
                        }

                        if (childSteps > totalBestField.steps) {
                            totalBestField = childField;
                            totalBestField.printToTmp();

                            if (verbose) {
                                System.out.println(
                                        String.format("Update nCount(nSize) %d(%d), mCount(mSize) %d(%d), mask %s, steps %d",
                                                nCount, nSize, mCount, mSize,
                                                Integer.toBinaryString(maskSize + mask).substring(1),
                                                totalBestField.steps
                                        )
                                );
                            }
                        }
                    }
                }
            }

            if (verbose) {
                System.out.println(
                        String.format("nCount(nSize) %d(%d), mCount(mSize) %d(%d), steps %d",
                                nCount, nSize, mCount, mSize,
                                totalBestField.steps
                        )
                );
            }
        }

        return totalBestField;
    }

    static BugField[] hardMutations(BugField baseField, int populationSize, int maxSize) throws IOException {
        BugFieldMinimumHeap bestFields = new BugFieldMinimumHeap();

        bestFields.add(baseField);
        baseField.printToPopulation();

        BugField totalBestField = BugField.create(baseField);

        int n = baseField.n, m = baseField.m;
        boolean[][] baseWall = baseField.wall;

        boolean[][] mutantWall = new boolean[n][m];
        for (int i = 0; i < n; ++i) {
            System.arraycopy(baseWall[i], 0, mutantWall[i], 0, m);
        }

        int lastMSize = -1;
        for (int nSizeIterator = 1; nSizeIterator <= maxSize; ++nSizeIterator) {
            int mSize = maxSize / nSizeIterator;
            if (mSize == lastMSize) continue;

            int nSize = maxSize / mSize;
            lastMSize = mSize;

            int size = nSize * mSize;
            int maskSize = (1 << size);

//            int verbosePart = maskSize / 10;

            for (int i = 1; i + nSize < n; ++i) {
                for (int j = 1; j + mSize < m; ++j) {
                    for (int mask = 0; mask < maskSize; ++mask) {
                        for (int x = 0, bit = 0; x < nSize; ++x) {
                            for (int y = 0; y < mSize; ++y, ++bit) {
                                mutantWall[i + x][j + y] = checkBit(mask, bit);
                            }
                        }

                        BugField mutantField = BugField.create(mutantWall);
                        int mutantSteps = mutantField.steps;

//                        if (mask % verbosePart == 0) {
//                            System.out.println(
//                                    String.format("Verbose nSize %d, mSize %d, i %d, j %d, mask %s, steps %d",
//                                            nSize, mSize, i, j,
//                                            Integer.toBinaryString(maskSize + mask).substring(1),
//                                            totalBestField.steps
//                                    )
//                            );
//                        }

                        boolean possibleAdd = bestFields.size() < populationSize || mutantSteps > bestFields.peek().steps;
                        if (!possibleAdd) {
                            mutantField.wall = null;
                            BugField.delete(mutantField);
                        } else {
//                            mutantField.initMasks();

                            boolean alreadyContained = false;
                            for (BugField field : bestFields) {
//                                alreadyContained |= field.equalsByMasks(mutantField);
                                alreadyContained |= field.steps == mutantSteps;
                                if (alreadyContained) break;
                            }

                            if (!alreadyContained) {
                                if (mutantField.wall == mutantWall) {
                                    boolean[][] clonedMutantWall = new boolean[n][m];
                                    for (int row = 0; row < n; ++row) {
                                        System.arraycopy(mutantWall[row], 0, clonedMutantWall[row], 0, m);
                                    }

                                    mutantField.wall = clonedMutantWall;
                                }

                                bestFields.add(mutantField);
                                mutantField.printToPopulation();

                                if (bestFields.size() > populationSize) {
                                    BugField removed = bestFields.poll();
                                    removed.removeZip();
                                    BugField.delete(removed);
                                }

                                if (mutantSteps > totalBestField.steps) {
                                    BugField.delete(totalBestField);
                                    totalBestField = BugField.create(mutantField);
                                    System.out.println(
                                            String.format("Update nSize %d, mSize %d, i %d, j %d, mask %s, steps %d",
                                                    nSize, mSize, i, j,
                                                    Integer.toBinaryString(maskSize + mask).substring(1),
                                                    totalBestField.steps
                                            )
                                    );
                                    totalBestField.printToTmp();
                                }
                            }
                        }
                    }

                    for (int x = 0, bit = 0; x < nSize; ++x) {
                        for (int y = 0; y < mSize; ++y, ++bit) {
                            mutantWall[i + x][j + y] = baseWall[i + x][j + y];
                        }
                    }

                    System.out.println(
                            String.format("nSize %d, mSize %d, i %d, j %d, steps %d",
                                    nSize, mSize, i, j,
                                    totalBestField.steps
                            )
                    );
                }
            }
        }

        BugField[] fields = new BugField[populationSize];
        bestFields.fill(fields, populationSize);
        return fields;
    }


}
