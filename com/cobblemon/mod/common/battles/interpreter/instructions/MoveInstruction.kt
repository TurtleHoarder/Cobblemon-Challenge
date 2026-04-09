/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.interpreter.instructions

import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.api.battles.interpreter.Effect
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addBattleMessageFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addStandardFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMostSpecificMoLangValue
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext
import com.cobblemon.mod.common.api.moves.animations.ActionEffectTimeline
import com.cobblemon.mod.common.api.moves.animations.ActionEffects
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider
import com.cobblemon.mod.common.api.moves.animations.UsersProvider
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.dispatch.CauserInstruction
import com.cobblemon.mod.common.battles.dispatch.GO
import com.cobblemon.mod.common.battles.dispatch.InstructionSet
import com.cobblemon.mod.common.battles.dispatch.InterpreterInstruction
import com.cobblemon.mod.common.battles.dispatch.UntilDispatch
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.evolution.progress.UseMoveEvolutionProgress
import com.cobblemon.mod.common.util.asArrayValue
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.cobblemonResource
import java.util.concurrent.CompletableFuture
import net.minecraft.world.entity.LivingEntity

/**
 * Format: |move|POKEMON|MOVE|TARGET
 *
 * POKEMON has used MOVE at TARGET.
 * @author Deltric
 * @since January 22nd, 2022
 */
class MoveInstruction(
    val instructionSet: InstructionSet,
    val message: BattleMessage
) : InterpreterInstruction, CauserInstruction {
    val effect = message.effectAt(1) ?: Effect.pure("", "")
    val move = Moves.getByNameOrDummy(effect.id)
    val actionEffect: ActionEffectTimeline? by lazy { ActionEffects.getEffectWithBattleContext(cobblemonResource(move.name), userPokemon) }
    val spreadTargets = message.optionalArgument("spread")?.split(",") ?: emptyList()

    var future = CompletableFuture.completedFuture(Unit)
    var holds = mutableSetOf<String>()

    lateinit var userPokemon: BattlePokemon
    var targetPokemon: BattlePokemon? = null

    override fun invoke(battle: PokemonBattle) {
        userPokemon = message.battlePokemon(0, battle)!!
        targetPokemon = message.battlePokemon(2, battle)

        val actionEffect = actionEffect ?: ActionEffects.actionEffects["generic_move".asIdentifierDefaultingNamespace()]
        val targetPokemon = targetPokemon // So smart non-null casts can happen

        val optionalEffect = message.effect()
        ShowdownInterpreter.broadcastOptionalAbility(battle, optionalEffect, userPokemon)

        battle.dispatch { UntilDispatch { instructionSet.getMostRecentInstruction<MoveInstruction>(this)?.future?.isDone != false } }

        battle.dispatch {
            val pokemonName = userPokemon.getName()
            ShowdownInterpreter.lastCauser[battle.battleId] = message
            // For Spread targets the message data only gives the pnx strings. So we can't know what pokemon are actually targeted until the previous messages have been interpreted
            val spreadTargetPokemon = spreadTargets.map { battle.activePokemon.firstOrNull() { poke -> poke.getPNX() == it }?.battlePokemon }

            val targetPokemonEntity = targetPokemon?.entity
            if (targetPokemonEntity != null) {
                userPokemon.entity?.brain?.setMemory(CobblemonMemories.TARGETED_BATTLE_POKEMON, targetPokemonEntity.uuid)
            }

            userPokemon.effectedPokemon.let { pokemon ->
                if (UseMoveEvolutionProgress.supports(pokemon, move)) {
                    val progress = pokemon.evolutionProxy.current().progressFirstOrCreate({ it is UseMoveEvolutionProgress && it.currentProgress().move == move }) { UseMoveEvolutionProgress() }
                    progress.updateProgress(UseMoveEvolutionProgress.Progress(move, progress.currentProgress().amount + 1))
                }
            }

            val lang = when {
                optionalEffect?.id == "magicbounce" ->
                    battleLang("ability.magicbounce", pokemonName, move.displayName)
                move.name != "struggle" && spreadTargetPokemon.isEmpty() && targetPokemon != null && targetPokemon != userPokemon && targetPokemon.health > 0 ->
                    battleLang("used_move_on", pokemonName, move.displayName, targetPokemon.getName())
                else ->
                    battleLang("used_move", pokemonName, move.displayName)
            }
            battle.broadcastChatMessage(lang)
            battle.majorBattleActions[userPokemon.uuid] = message

            val runtime = MoLangRuntime().also {
                battle.addQueryFunctions(it.environment.query).addStandardFunctions()
                it.environment.query.addBattleMessageFunctions(message)
            }

            val providers = mutableListOf<Any>(battle)
            userPokemon.effectedPokemon.entity?.let { entity ->
                runtime.environment.query.addFunction("user") { entity.asMostSpecificMoLangValue()}
                UsersProvider(entity)
            }?.let(providers::add)

            if (spreadTargetPokemon.isNotEmpty()) {
                val targetEntities = spreadTargetPokemon.mapNotNull { it?.effectedPokemon?.entity }
                runtime.environment.query.addFunction("targets") { targetEntities.asArrayValue { it.asMostSpecificMoLangValue() } }
                providers.add(TargetsProvider(targetEntities))
            } else {
                targetPokemon?.effectedPokemon?.entity?.let { entity ->
                    runtime.environment.query.addFunction("target") { entity.asMostSpecificMoLangValue() }
                    val provider = TargetsProvider(entity)
                    providers.add(provider)
                }
            }

            actionEffect ?: return@dispatch GO
            val context = ActionEffectContext(
                actionEffect = actionEffect,
                runtime = runtime,
                providers = providers,
                level = battle.players.firstOrNull()?.level()
            )

            val subsequentInstructions = instructionSet.findInstructionsCausedBy(this)
            val missedTargets = subsequentInstructions.filterIsInstance<MissInstruction>().mapNotNull { it.target }
            val hitCountInstruction = subsequentInstructions.filterIsInstance<HitCountInstruction>().firstOrNull()

            runtime.environment.query.addFunction("missed") { params ->
                if (params.params.size == 0) {
                    return@addFunction DoubleValue(missedTargets.isNotEmpty())
                } else {
                    val entityUUID = params.getString(0)
                    return@addFunction DoubleValue(missedTargets.any { it.entity?.stringUUID == entityUUID })
                }
            }

            val hurtTargets = subsequentInstructions.filterIsInstance<DamageInstruction>().mapNotNull { it.expectedTarget }
            runtime.environment.query.addFunction("hurt") { params ->
                if (params.params.size == 0) {
                    return@addFunction DoubleValue(hurtTargets.isNotEmpty())
                } else {
                    val entityUUID = params.getString(0)
                    return@addFunction DoubleValue(hurtTargets.any { it.entity?.stringUUID == entityUUID })
                }
            }

            if (hitCountInstruction != null && hitCountInstruction.hitCount != null) {
                runtime.environment.query.addFunction("hit_count") { DoubleValue(hitCountInstruction.hitCount.toDouble()) }
            }

            runtime.environment.query.addFunction("move") { move.struct }
            runtime.environment.query.addFunction("instruction_id") { StringValue(cobblemonResource("move").toString()) }

            this.future = actionEffect.run(context)
            holds = context.holds // Reference so future things can check on this action effect's holds
            future.thenApply { holds.clear() }
            return@dispatch UntilDispatch { "effects" !in holds }.andThen {
                val userPokemonId = userPokemon.entity?.uuid ?: return@andThen
                val targets = hurtTargets.mapNotNull { it.entity }
                userPokemonId.let { id -> targets.forEach { it.brain.setMemory(CobblemonMemories.TARGETED_BATTLE_POKEMON, id) } }
            }
        }
    }
}