package com.turtlehoarder.cobblemonchallenge.api.storage.party

import  com.cobblemon.mod.common.api.storage.StorePosition

class FakePartyPosition(private var slot: Int = 0) : StorePosition {
    fun set(newSlot: Int) { slot = newSlot }
    fun get(): Int { return slot }
}