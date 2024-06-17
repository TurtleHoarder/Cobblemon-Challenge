package com.turtlehoarder.cobblemonchallenge.api.storage

import com.turtlehoarder.cobblemonchallenge.api.storage.party.FakePlayerPartyStore

import com.cobblemon.mod.common.api.reactive.Observable
import com.cobblemon.mod.common.api.storage.PokemonStore
import com.cobblemon.mod.common.api.storage.StoreCoordinates
import com.cobblemon.mod.common.api.storage.StorePosition
import com.cobblemon.mod.common.api.storage.factory.PokemonStoreFactory
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.JsonObject
import net.minecraft.server.level.ServerPlayer
import net.minecraft.nbt.CompoundTag
import java.util.*

class FakePokemonStore<FakePartyPosition>(
    private val playerStore : FakePlayerPartyStore,
    /** The UUID of the store. The exact uniqueness requirements depend on the method used for saving. */
    override val uuid: UUID = playerStore.playerUUID
): PokemonStore<StorePosition>() {

    /** Gets the [Pokemon] at the given position. */
    override operator fun get(position: StorePosition): Pokemon?{
        return null
    }

    /** Gets the first empty position that a [Pokemon] might be put. */
    override fun getFirstAvailablePosition(): StorePosition? {
        return null
    }

    /** Gets an iterable of all [ServerPlayer]s that should be notified of any changes to the Pokémon in this store. */
    override fun getObservingPlayers(): Iterable<ServerPlayer> {
        return emptyList()
    }

    /** Sends the contents of this store to a player as if they've never seen it before. This initializes the store then sends each contained Pokémon. */
    override fun sendTo(player: ServerPlayer) { }

    /**
     * Runs initialization logic for this store, knowing that it has just been constructed in a [PokemonStoreFactory].
     *
     * The minimum of what this function should do is iterate over all the Pokémon in this store and set their store
     * coordinates.
     *
     * If this does not get called, or it does not do its job properly, serious de-sync issues may follow.
     */
    override fun initialize() { }

    override fun iterator(): Iterator<Pokemon> {
        TODO("Not yet implemented")
    }

    /**
     * Sets the given position with the given [Pokemon], which can be null. This is for internal use only because
     * other, more public methods will additionally send updates to the client, and for logical reasons this means
     * there must be an internal and external set method.
     */
    override fun setAtPosition(position: StorePosition, pokemon: Pokemon?) {

    }

    /** Returns true if the given position is pointing to a legitimate location in this store. */
    override fun isValidPosition(position: StorePosition): Boolean {
        return true
    }

    override fun saveToNBT(nbt: CompoundTag): CompoundTag {
        return CompoundTag()
    }

    override fun loadFromNBT(nbt: CompoundTag): PokemonStore<StorePosition> {
        return this
    }

    override fun saveToJSON(json: JsonObject): JsonObject {
        return JsonObject()
    }

    override fun loadFromJSON(json: JsonObject): PokemonStore<StorePosition> {
        return this
    }

    override fun savePositionToNBT(position: StorePosition, nbt: CompoundTag) {

    }

    override fun loadPositionFromNBT(nbt: CompoundTag): StoreCoordinates<StorePosition> {
        TODO("Not yet implemented")
    }

    /**
     * Returns an [Observable] that emits Unit whenever there is a change to this store. This includes any save-worthy
     * change to a [Pokemon] contained in the store. You can access an [Observable] in each [Pokemon] that emits Unit for
     * each change, accessed by [Pokemon.getChangeObservable].
     */
    override fun getAnyChangeObservable(): Observable<Unit> {
        return Observable.just(Unit) // @Eric: I think this should work but... I'll defer to you lol ;) - David
    }
}