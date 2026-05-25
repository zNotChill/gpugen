package me.znotchill

import BaseGenerator
import me.znotchill.blossom.server.BlossomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerLoadedEvent

object Server : BlossomServer() {
    val mainInstance = BaseGenerator.new()

    override fun preLoad() {
        mainInstance.timeRate = 0
        listener<AsyncPlayerConfigurationEvent> { event ->
            event.spawningInstance = mainInstance
            event.player.respawnPoint = Pos(0.0, 80.0, 0.0)
            event.player.permissionLevel = 4
        }
        listener<PlayerLoadedEvent> { event ->
            event.player.gameMode = GameMode.SPECTATOR
        }
    }
    override fun postLoad() {
        logger.info("Server started!")
    }
}

fun main() {
    Server.start()
}