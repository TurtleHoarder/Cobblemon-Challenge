## Cobblemon Challenge

### Summary
Cobblemon Challenge is an extremely simple plugin for Cobblemon that makes challenging your friends and rivals to flat-level Pokemon battles
easier! This is a server-side plugin that only needs to be installed on the Server.

#### Commands
There are four possible challenge properties:
  1. Username (required)
  2. Level or min/maxLevel (optional & mutually exclusive)
  3. Handicap (optional)
  4. NoPreview (optional, true if excluded & false if specified)

```/challenge <username>``` - Challenges specified player to a lvl 50 pokemon battle. They may accept or deny this challenge.

```/challenge <username> level <level>``` - Challenges specified player to a lvl X battle where X can be 1-100.

```/challenge <username> minLevel <minLevel> maxLevel <maxLevel>``` - Challenges specified player to a lvl <minLevel> - <maxLevel> battle where <minLevel> & <maxLevel> can be 1-100.

```/challenge <username> handicapP1 <levelOffset> handicapP1 <levelOffset>``` - Challenges specified player to a battle where player1 (challenger) & player2 (challenged) are offset by the specified levels. <levelOffset> can be from (-99)-(99).

```/challenge <username> noPreview``` - Challenges specified player to a default battle with no preview of the rivals pokemon during starter selection.

##### Command Input Order:
  There are 12 possible input combinations for challenge properties. The order of input must be username -> level or min/maxLevel -> handicap -> noPreview. If the challenge property is optional you may skip it when inputting the command. The default values will be used.

##### All Possible Commands:

```/challenge <username>```

```/challenge <username> handicapP1 <levelOffset> handicapP1 <levelOffset>```

```/challenge <username> noPreview```

```/challenge <username> handicapP1 <levelOffset> handicapP1 <levelOffset> noPreview```


```/challenge <username> level <level>```

```/challenge <username> level <level> handicapP1 <levelOffset> handicapP1 <levelOffset>```

```/challenge <username> level <level> noPreview```

```/challenge <username> level <level> handicapP1 <levelOffset> handicapP1 <levelOffset> noPreview```


```/challenge <username> minLevel <minLevel> maxLevel <maxLevel>```

```/challenge <username> minLevel <minLevel> maxLevel <maxLevel> handicapP1 <levelOffset> handicapP1 <levelOffset>```

```/challenge <username> minLevel <minLevel> maxLevel <maxLevel> noPreview```

```/challenge <username> minLevel <minLevel> maxLevel <maxLevel> handicapP1 <levelOffset> handicapP1 <levelOffset> noPreview```


#### Configurations
There are numerous options that will allow you to customize the Challenge experience to your server's needs. These
settings can be found in the Cobblemon Challenge config file:

```challengeDistanceRestriction``` - The value that determines if challenges are restricted by distance. Set to **false** if you would
want no restrictions on distance. This value is set to **true** by default.

```maxChallengeDistance``` - If challengeDistanceRestriction is set to **true**, then this value defines the max distance 
that a challenge can be sent. This is set to 50 blocks by default.

```defaultChallengeLevel``` - The value that determines the level of a challenge if there is not level specified by the challenger.
This is set to 50 by default for lvl 50 battles.

```defaultHandicap``` - The value that determines each players final level of each Pokemon if a handicap is not specified by the challenger.
This is set to 0 by default.

```challengeExpirationTime``` - The value that determines how long a challenge should be pending before it expires. This is
set to 60000 milliseconds / 1 minute by default.

```challengeCooldownTime``` - The value that determines how long a player must wait before sending a consecutive request. This value is 
set to 5000 milliseconds / 5 seconds by default, though players will need to wait until their existing challenge expires before sending another one.
