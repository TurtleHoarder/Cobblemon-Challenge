package com.cobblemontournament.util;

import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.Vector;

public class IndexedSeedArray
{
    public IndexedSeedArray(
            Vector<IndexedSeed> collection,
            @Nullable IndexedSeedSortType sortingType
    ) {
        this.collection = collection;
        if (sortingType == null) {
            return;
        }
        switch (sortingType) {
            case SEED_ASCENDING -> sortBySeedAscending();
            case SEED_DESCENDING -> sortBySeedDescending();
            case INDEX_ASCENDING -> sortByIndexAscending();
            case INDEX_DESCENDING -> sortByIndexDescending();
        }
    }

    public Vector<IndexedSeed> collection;
    private IndexedSeedSortType indexedSeedStatus = IndexedSeedSortType.UNKNOWN;
    public IndexedSeedSortType sortStatus()
    {
        return indexedSeedStatus;
    }

    public int size()
    {
        return collection.size();
    }
    public IndexedSeed get(int index)
    {
        return collection.get(index);
    }

    public void sortBySeedAscending()
    {
        collection.sort(Comparator.comparing(IndexedSeed::seed));
        indexedSeedStatus = IndexedSeedSortType.SEED_ASCENDING;
    }
    public void sortBySeedDescending()
    {
        collection.sort(Comparator.comparing(IndexedSeed::seed).reversed());
        indexedSeedStatus = IndexedSeedSortType.SEED_DESCENDING;
    }
    public void sortByIndexAscending()
    {
        collection.sort(Comparator.comparing(IndexedSeed::index));
        indexedSeedStatus = IndexedSeedSortType.INDEX_ASCENDING;
    }
    public void sortByIndexDescending()
    {
        collection.sort(Comparator.comparing(IndexedSeed::index).reversed());
        indexedSeedStatus = IndexedSeedSortType.INDEX_DESCENDING;
    }

}
