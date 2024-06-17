package com.cobblemontournament.util;

import java.util.ArrayList;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SeedUtil
{
    /**
     * IndexedSeeds( int index, int seed )
     *  index: order index of seed in tournament structure
     *  seed: value of seed
     */
    public static IndexedSeedArray getIndexedSeedArray(int seedCount, IndexedSeedSortType sortStatus)
    {
        var seeds = getSeedArray(seedCount);
        var size = seeds.size();
        var placeHolder = new IndexedSeed(-1,-1);
        var indexedSeeds = new Vector<>(Stream.generate(() -> placeHolder)
                .limit(size)
                .collect(Collectors.toList())
        );
        for (int i = 0; i < size; i++) {
            indexedSeeds.set(i, new IndexedSeed(i,seeds.get(i)));
        }
        return new IndexedSeedArray(indexedSeeds,sortStatus);
    }

    /** @return Vector with sorted seeds for tournament bracket<br>
     *  -> the array size will be the nearest ceil to power of 2 int for (param)seedCount <br>
     *      ex 1: (param)seedCount = 5 -> nearest power of 2 -> 8 <br>
     *      ex 2: (param)seedCount = 33 -> nearest power of 2 -> 64 (ceil not round b/c return val needs to include ALL seeds)
     */
    public static Vector<Integer> getSeedArray(int seedCount)
    {
        var testVector = new Vector<Integer>() { { add(0); add(0); } };
        var testValue = testVector.get(0);

        int finalSize = ceilToPowerOfTwo(seedCount);
        //System.out.println("finalSize " + finalSize);
        ArrayList<Integer> seeds = new ArrayList<>(2) { { add(1); add(2); } };
        //System.out.println("seeds " + seeds);

        // minSeed = the lowest seed (highest int values) 'processed' into the ArrayList<> seeds
        int previousSeed = 2;
        //  empty ~ Pair(x,-1) -> x is always > 0 & represents a 'processed' seed
        int emptySeeds = 0;
        while (previousSeed < finalSize) {
            // step 1 -> add filler entry (-1) to collection if all current seed values in seeds are processed
            if (emptySeeds < 1) {
                emptySeeds = seeds.size();
                int newSize = seeds.size() * 2;
                // .collect & new ArrayList<> necessary b/c ArrayList is mutable & List is not
                var newSeeds = new ArrayList<>(Stream.generate(() -> -1)
                        .limit(newSize)
                        .collect(Collectors.toList())
                );
                for (int i = 0; i * 2 < newSize; i++) {
                    newSeeds.add(i * 2,seeds.get(i)); // insert last processed value
                    newSeeds.remove(newSize); // trim offset filler entry from tail of collection
                }
                seeds = newSeeds;
                //System.out.println("seeds " + seeds);
            }

            // step 2 -> find next
            // Pair<Integer,Integer>
            //     value 1 -> the 'processed' seed value
            //     value 2 -> value 1's pair index (value 1 index + 1)
            var seedIndex1 = new IndexedSeed(-1,-1); // empty ~ Pair(x,-1)
            var seedIndex2 = new IndexedSeed(-1,-1); // empty ~ Pair(x,-1)
            // +2 b/c always checking index that has a guaranteed processed seed value
            //      > basically -> value != -1
            for (int i = 0; i < seeds.size(); i = i + 2) {
                if (seeds.get(i + 1) != -1) {
                    continue;
                }
                int seed = seeds.get(i);
                if (seedIndex1.seed() >= seed && seedIndex2.seed() >= seed){
                    continue;
                }
                // replace lower value seed (aka higher seed)
                if (seedIndex1.seed() < seedIndex2.seed()){
                    seedIndex1 = new IndexedSeed(i + 1,seed); // + 1, b/c looking for empty (-1) seed pairing
                    continue;
                }
                seedIndex2 = new IndexedSeed(i + 1,seed);
            }

            // step 3 -> apply next seeds to array
            var seed1 = ++previousSeed;
            var seed2 = ++previousSeed;
            emptySeeds = emptySeeds - 2;

            // !! 'Higher' -> 1 is 'Higher' seed than 2, but lower value
            var isSeed1Higher = seedIndex1.seed() < seedIndex2.seed();
            int index1 = isSeed1Higher ? seedIndex2.index() : seedIndex1.index();
            int index2 = isSeed1Higher ? seedIndex1.index() : seedIndex2.index();
            seeds.add(index1,seed1);
            seeds.remove(index1 + 1);
            seeds.add(index2,seed2);
            seeds.remove(index2 + 1);
        }

        return doFinalSeedSort(seeds);
    }

    private static Vector<Integer> doFinalSeedSort(ArrayList<Integer> seeds)
    {
        // all seed collections will be power of 2 -> safe to bit shift for half
        int halfSize = seeds.size() >> 1;
        var filler = new ArrayList<Integer>(0);
        // size = half size of seeds, b/c we want all arrays to have 2 seeds each
        var seedArrays = new ArrayList<>(Stream.generate(() -> filler)
                .limit(halfSize)
                .collect(Collectors.toList())
        );
        seedArrays.add(0,seeds);
        seedArrays.remove(1);

        // int used to make bit shifting safe
        //      -> should be 0 on even iterations & 1 on odd iterations
        int iterationOffset = 0;
        // iterations = (seeds.size/4)
        var iterations = seeds.size() >> 2;
        for (int i = 0; i < iterations; i++) {

            int size = firstFillerIndex(seedArrays);
            for (int ii = 0; ii < size; ii++) {

                var index = ii * 2;
                var array = seedArrays.get(index);
                var arrays = split(array);
                var front = arrays.get(0);
                var back = arrays.get(1);

                //var frontMaxSeed = front.stream().min(Integer::compareTo).orElseThrow();
                //var backMaxSeed = back.stream().min(Integer::compareTo).orElseThrow();
                var frontMaxSeed = front.stream()
                        .min(Integer::compareTo)
                        .orElse(seeds.size());
                var backMaxSeed = back.stream()
                        .min(Integer::compareTo)
                        .orElse(seeds.size());

                if (frontMaxSeed < backMaxSeed) {
                    // front has high seed -> reverse back order
                    reverse(back);
                } else {
                    // back has high seed -> reverse front order
                    reverse(front);
                }

                seedArrays.remove(index); // original array
                seedArrays.add(index,front);
                seedArrays.remove(seedArrays.size() - 1); // trim off tail
                seedArrays.add(index + 1,back);
            }
        }


        var finalSeeds = new Vector<Integer>(Stream.generate(() -> -1)
                .limit(seeds.size())
                .collect(Collectors.toList())
        );
        var index = 0;
        var lastIndex = finalSeeds.size() - 1;
        for (int i = 0; i < seedArrays.size(); i++) {
            var nestedSeeds = seedArrays.get(i);
            for (int ii = 0; ii < seedArrays.size(); ii++) {
                if (nestedSeeds.size() <= ii) {
                    break;
                }
                finalSeeds.remove(lastIndex);
                finalSeeds.add(index++,nestedSeeds.get(ii));
            }
        }
        return finalSeeds;
    }

    public static Integer ceilToPowerOfTwo(int value)
    {
        int maxBitInt = Integer.highestOneBit(value);
        return (!(value == 0) && (value ^ maxBitInt) == 0) ? value : maxBitInt << 1;
    }

    private static int firstFillerIndex(ArrayList<ArrayList<Integer>> arrayLists)
    {
        var firstEmptyIndex = -1;
        for (int i = 0; i < arrayLists.size(); i++) {
            if (arrayLists.get(i).isEmpty()){
                firstEmptyIndex = i;
                break;
            }
        }
        return firstEmptyIndex;
    }

    // !! size must be pow of 2 b/c bit shift to middle used !!
    private static ArrayList<ArrayList<Integer>> split(ArrayList<Integer> array)
    {
        int middle = array.size() >> 1;
        var front = getNewFilled(middle);
        var back = getNewFilled(middle);
        for (int i = 0; i < middle; i++) {
            front.add(i,array.get(i));
            front.remove(middle);
        }
        int index = middle;
        for (int i = 0; i < middle; i++) {
            back.add(i,array.get(index++));
            back.remove(middle);
        }
        return new ArrayList<>() { { add(front); add(back); } };
    }

    private static ArrayList<Integer> getNewFilled(int length)
    {
        return new ArrayList<>(Stream.generate(() -> -1)
                .limit(length)
                .collect(Collectors.toList())
        );
    }

    private static void reverse(ArrayList<Integer> arrayList)
    {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            arrayList.add(i,arrayList.remove(size - 1));
        }
    }

    public static boolean isPowerOfTwo(int value)
    {
        return !(value == 0) && (value ^ Integer.highestOneBit(value)) == 0;
    }

}
