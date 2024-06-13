package com.turtlehoarder.cobblemonchallenge.api.storage.party

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.*

class FakePlayerPartyStore(
    private val fakeUUID: UUID = UUID(0, 0)
) : PlayerPartyStore(fakeUUID, fakeUUID) {

    // override function to serve the same purpose as the previous FakeStore
    override fun add(pokemon: Pokemon): Boolean {
        return true
    }
}