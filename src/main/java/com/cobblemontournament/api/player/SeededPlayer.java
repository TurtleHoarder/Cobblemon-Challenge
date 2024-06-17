package com.cobblemontournament.api.player;

import java.util.UUID;

public record SeededPlayer (UUID id, Integer seed) { }
