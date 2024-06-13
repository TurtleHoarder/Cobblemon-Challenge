## Cobblemon Challenge

### Summary
Cobblemon Challenge is an extremely simple plugin for Cobblemon that makes challenging your friends and rivals to flat-level Pokemon battles
easier! This is a server-side plugin that only needs to be installed on the Server.

#### Command Parameters
```/challenge <username>``` - Challenges specified player to a lvl 50 pokemon battle. They may accept or deny this challenge.

```/challenge <username> level <level>``` - Challenges specified player to a lvl ```<level>``` battle where ```<level>``` can be **1 min** & **100 max**.

```/challenge <username> levelRange <minLevel> <maxLevel>``` - Challenges specified player to a lvl where the Pokemon's levels are clamped between the ```<minLevel>```&```<maxLevel>``` where ```<minLevel>```&```<maxLevel>``` can be **1 min** & **100 max**.

```/challenge <username> handicap <self> <rival>``` - Challenges specified player to a battle where ```<self>``` (the challenger) & ```<rival>``` (the challenged) Pokemon level is offset by the input level. ```<self>```&```<rival>``` can be from **-99 min** & **99 max**.

```/challenge <username> showPreview <true/false>``` - Challenges specified player to a default battle with no preview of the rivals pokemon during starter selection.

##### Notes About Command Parameters:
1. The order of parameter input must be ```username``` -> ```level/levelRange``` -> ```handicap``` -> ```showPreview```. 
2. If the challenge property is optional & not specified in the command. The default values from the config file will be used.

#### Configurations
There are numerous options that will allow you to customize the Challenge experience to your server's needs. These
settings can be found in the Cobblemon Challenge config file:

```challengeDistanceRestriction``` - The value that determines if challenges are restricted by distance. Set to **false** if you would
want no restrictions on distance. This value is set to **true** by default.

```maxChallengeDistance``` - If challengeDistanceRestriction is set to **true**, then this value defines the max distance
that a challenge can be sent. This is set to 50 blocks by default.

```defaultChallengeLevel``` - The value that determines the level of a challenge if there is no level specified by the challenger.
This is set to 50 by default for lvl 50 battles.

```defaultHandicap``` - The value that determines each player's final level of each Pokemon if a handicap is not specified by the challenger.
This is set to 0 by default.

```defaultShowPreview``` - If defaultShowPreview is set to **true** both players will see which pokemon the opponent is bringing to battle. If defaultShowPreview is set to **false** both players parties will be hidden from their opponent.
(Note: Even when defaultShowPreview is true the player will never see the opponents chosen lead pokemon)
This is set to true by default.

```challengeExpirationTime``` - The value that determines how long a challenge should be pending before it expires. This is
set to 60000 milliseconds / 1 minute by default.

```challengeCooldownTime``` - The value that determines how long a player must wait before sending a consecutive request. This value is
set to 5000 milliseconds / 5 seconds by default, though players will need to wait until their existing challenge expires before sending another one.

### All Possible Commands:

```/challenge <username>```

```/challenge <username> handicap <self> <rival>```

```/challenge <username> showPreview <true/false>```

```/challenge <username> handicap <self> <rival> showPreview <true/false>```


```/challenge <username> level <level>```

```/challenge <username> level <level> handicap <self> <rival>```

```/challenge <username> level <level> showPreview <true/false>```

```/challenge <username> level <level> handicap <self> <rival> showPreview <true/false>```


```/challenge <username> levelRange <minLevel> <maxLevel>```

```/challenge <username> levelRange <minLevel> <maxLevel> handicap <self> <rival>```

```/challenge <username> levelRange <minLevel> <maxLevel> showPreview <true/false>```

```/challenge <username> levelRange <minLevel> <maxLevel> handicap <self> <rival> showPreview <true/false>```