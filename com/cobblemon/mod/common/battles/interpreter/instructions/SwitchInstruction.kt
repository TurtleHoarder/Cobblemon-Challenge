/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.interpreter.instructions

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.PokemonSeenEvent
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.battles.ActiveBattlePokemon
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor
import com.cobblemon.mod.common.battles.dispatch.*
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.pokemon.effects.IllusionEffect
import com.cobblemon.mod.common.net.messages.client.battle.BattleSwitchPokemonPacket
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler.SEND_OUT_DURATION
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler.SEND_OUT_STAGGER_BASE_DURATION
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler.SEND_OUT_STAGGER_RANDOM_MAX_DURATION
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.party
import com.cobblemon.mod.common.util.swap
import java.util.concurrent.CompletableFuture
import kotlin.random.Random
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity

/**
 * Format: |switch|POKEMON|DETAILS|HP STATUS
 *
 * POKEMON has switched in (if there was an old Pokémon at that position, it is switched out).
 * POKEMON|DETAILS represents all the information that can be used to tell Pokémon apart.
 * The switched Pokémon has HP health points and STATUS status.
 * @author Deltric
 * @since January 22nd, 2022
 */
class SwitchInstruction(val instructionSet: InstructionSet, val battleActor: BattleActor, val publicMessage: BattleMessage, val privateMessage: BattleMessage): InterpreterInstruction {

    override fun invoke(battle: PokemonBattle) {

        val (pnx, pokemonID) = publicMessage.pnxAndUuid(0) ?: return
        val (actor, activePokemon) = battle.getActorAndActiveSlotFromPNX(pnx)
        val entity = if (actor is EntityBackedBattleActor<*>) actor.entity else null

        val imposter = instructionSet.getNextInstruction<TransformInstruction>(this)?.expectedTarget != null
        val illusion = publicMessage.battlePokemonFromOptional(battle, "is")
        val pokemon = publicMessage.battlePokemon(0, battle) ?: return

        if (!battle.started) {  // battle 'starts' at beginning of dispatches; see InitializeInstruction

            battle.dispatchToFront {    // this needs to happen before InitializeInstruction dispatches

                val pokemonEntity = pokemon.entity
                if (pokemonEntity != null && actor !is PokemonBattleActor) {    // pokemon entities starting on the field should already have battlePokemon init; see InitializeInstruction

                    illusion?.let { IllusionEffect(it.effectedPokemon).start(pokemonEntity) }
                    broadcastSwitch(battle, actor, pokemon, illusion)
                    WaitDispatch(0.5F)
                }
                else if (pokemonEntity == null && entity != null) {
                    activePokemon.battlePokemon = pokemon
                    activePokemon.illusion = illusion
                    val targetPos = activePokemon.getSendOutPosition()
                    if (targetPos != null) {
                        val battleSendoutCount = activePokemon.getActorShowdownId()[1].digitToInt() - 1 + actor.stillSendingOutCount
                        actor.stillSendingOutCount++
                        battle.sendSidedUpdate(actor, BattleSwitchPokemonPacket(pnx, pokemon, true, illusion), BattleSwitchPokemonPacket(pnx, pokemon, false, illusion))
                        broadcastSwitch(battle, actor, pokemon, illusion)
                        afterOnServer(seconds = battleSendoutCount * SEND_OUT_STAGGER_BASE_DURATION + if (battleSendoutCount > 0) Random.nextFloat() * SEND_OUT_STAGGER_RANDOM_MAX_DURATION else 0F ) {
                            pokemon.effectedPokemon.sendOutWithAnimation(
                                    source = entity,
                                    battleId = battle.battleId,
                                    level = entity.level() as ServerLevel,
                                    doCry = false,
                                    position = targetPos,
                                    illusion = illusion?.let { IllusionEffect(it.effectedPokemon) }
                            ).thenApply {
                                actor.stillSendingOutCount--
                            }
                            WaitDispatch(0.5F)  // we're already waiting 1.5 seconds. this prevents flooding from consecutive SwitchInstructions
                        }
                    }
                }
                GO
            }

            val futureSwitches = instructionSet.getSubsequentInstructions(this).filterIsInstance<SwitchInstruction>()
            if (futureSwitches.isEmpty()) {
                if (battle.format.adjustLevel > 0) {
                    // means battle is using clone teams, recall the "real" pokemon before the sendouts occur
                    var waitOnRecall = false
                    battle.actors.forEach { it ->
                        val playerUUIDS = it.getPlayerUUIDs()
                        playerUUIDS.forEach { uuid ->
                            uuid.getPlayer()?.party()?.forEach { pokemon ->
                                if (pokemon.entity != null) {
                                    waitOnRecall = true
                                    pokemon.entity!!.recallWithAnimation()
                                }
                            }
                        }
                    }
                    if (waitOnRecall) {
                        battle.dispatchWaitingToFront(SEND_OUT_DURATION) {  }
                    }
                }
            }
        }
        else {
            battle.dispatchInsert {
                val newHealth = privateMessage.argumentAt(2)!!.split(" ")[0]
                val remainingHealth = newHealth.split("/")[0].toInt()
                pokemon.effectedPokemon.currentHealth = remainingHealth
                pokemon.sendUpdate()

                if (activePokemon.battlePokemon == pokemon) {
                    return@dispatchInsert emptySet() // Already switched in, Showdown does this if the pokemon is going to die before it can switch
                }

                activePokemon.battlePokemon?.let { oldPokemon ->
                    if (publicMessage.effect()?.id == "batonpass") oldPokemon.contextManager.swap(pokemon.contextManager, BattleContext.Type.BOOST, BattleContext.Type.UNBOOST)
                    oldPokemon.contextManager.clear(BattleContext.Type.VOLATILE, BattleContext.Type.BOOST, BattleContext.Type.UNBOOST)
                    battle.majorBattleActions[oldPokemon.uuid] = publicMessage

                    val publicName = (activePokemon.illusion ?: oldPokemon).effectedPokemon.getDisplayName()
                    actor.sendMessage(battleLang("withdraw.self", publicName))
                    battle.actors.filter { it != actor }.forEach { it.sendMessage(battleLang("withdraw.other", actor.getName(), publicName)) }
                }
                battle.majorBattleActions[pokemon.uuid] = publicMessage

                setOf(
                    BattleDispatch {
                        if (entity != null) {
                            createEntitySwitch(battle, actor, entity, pnx, activePokemon, pokemon, illusion, imposter)
                        } else {
                            createNonEntitySwitch(battle, actor, pnx, activePokemon, pokemon, illusion)
                        }
                    }
                )
            }
        }
    }

    companion object{
        fun createEntitySwitch(
            battle: PokemonBattle,
            actor: BattleActor,
            entity: LivingEntity,
            pnx: String,
            activePokemon: ActiveBattlePokemon,
            newPokemon: BattlePokemon,
            illusion: BattlePokemon? = null,
            imposter: Boolean = false
        ): DispatchResult {
            val pokemonEntity = activePokemon.battlePokemon?.entity
            // If we can't find the entity for some reason then we're going to skip the recall animation
            val sendOutFuture = CompletableFuture<Unit>()
            (pokemonEntity?.recallWithAnimation() ?: CompletableFuture.completedFuture(Unit)).thenApply {
                // Queue actual swap and send-in after the animation has ended
                actor.pokemonList.swap(actor.activePokemon.indexOf(activePokemon), actor.pokemonList.indexOf(newPokemon))
                activePokemon.battlePokemon = newPokemon
                activePokemon.illusion = illusion
                battle.sendSidedUpdate(actor, BattleSwitchPokemonPacket(pnx, newPokemon, true, illusion), BattleSwitchPokemonPacket(pnx, newPokemon, false, illusion))
                val newEntity = newPokemon.entity
                if (newEntity != null) {
                    illusion?.let { IllusionEffect(it.effectedPokemon).start(newEntity) }
                    afterOnServer(seconds = SEND_OUT_DURATION) {
                        if (!imposter) newPokemon.entity?.cry()
                        sendOutFuture.complete(Unit)
                    }
                } else {
                    // For Singles, we modify the sendout position based on the pokemon's hitbox size
                    val pos = (if (battle.format.battleType.pokemonPerSide == 1) activePokemon.getSendOutPosition()
                        else  activePokemon.position?.second) ?: entity.position()
                    // Send out at previous Pokémon's location if it is known, otherwise actor location
                    val world = entity.level() as ServerLevel
                    newPokemon.effectedPokemon.sendOutWithAnimation(
                        source = entity,
                        battleId = battle.battleId,
                        level = world,
                        position = pos,
                        doCry = !imposter,
                        illusion = illusion?.let { IllusionEffect(it.effectedPokemon) }
                    ).thenAccept { sendOutFuture.complete(Unit) }
                }

                broadcastSwitch(battle, actor, newPokemon, illusion)
            }

            return UntilDispatch { sendOutFuture.isDone }
        }

        fun createNonEntitySwitch(battle: PokemonBattle, actor: BattleActor, pnx: String, activePokemon: ActiveBattlePokemon, newPokemon: BattlePokemon, illusion: BattlePokemon? = null): DispatchResult {
            actor.pokemonList.swap(actor.activePokemon.indexOf(activePokemon), actor.pokemonList.indexOf(newPokemon))
            activePokemon.battlePokemon = newPokemon
            activePokemon.illusion = illusion
            battle.sendSidedUpdate(actor, BattleSwitchPokemonPacket(pnx, newPokemon, true, illusion), BattleSwitchPokemonPacket(pnx, newPokemon, false, illusion))
            broadcastSwitch(battle, actor, newPokemon, illusion)
            return WaitDispatch(SEND_OUT_DURATION)
        }

        private fun broadcastSwitch(battle: PokemonBattle, actor: BattleActor, newPokemon: BattlePokemon, illusion: BattlePokemon?) {
            val publicPokemon = (illusion ?: newPokemon).effectedPokemon
            val publicLang = publicPokemon.nickname?.let { nickname ->
                battleLang("switch.other.nickname", actor.getName(), nickname, publicPokemon.species.translatedName)
            } ?: battleLang("switch.other", actor.getName(), publicPokemon.getDisplayName(true))
            actor.sendMessage(battleLang("switch.self", publicPokemon.getDisplayName(true)))
            battle.actors.filter { it != actor }.forEach {
                it.sendMessage(publicLang)
            }
            battle.playerUUIDs.forEach { uuid ->
                CobblemonEvents.POKEMON_SEEN.post(PokemonSeenEvent(uuid, publicPokemon))
            }
        }
    }

}