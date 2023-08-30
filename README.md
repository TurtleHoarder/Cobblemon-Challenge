## Cobblemon Challenge

### Summary
Cobblemon Challenge is an extremely simple plugin for Cobblemon that makes challenging your friends and rivals to flat-level Pokemon battles
easier! This is a server-side plugin that only needs to be installed on the Server.

#### Commands

```/challenge <username>``` - Challenges specified player to a lvl 50 pokemon battle. They may accept or deny this challenge.

```/challenge <username> level <level>``` - Challenges specified player to a lvl X battle where X can be 1-100.

Example: ```/challenge TurtleHoarder level 100``` challenges TurtleHoarder to a level 100 battle.
#### Configurations
There are numerous options that will allow you to customize the Challenge experience to your server's needs. These
settings can be found in the Cobblemon Challenge config file:

```challengeDistanceRestriction``` - The value that determines if challenges are restricted by distance. Set to **false** if you would
want no restrictions on distance. This value is set to **true** by default.

```maxChallengeDistance``` - If challengeDistanceRestriction is set to **true**, then this value defines the max distance 
that a challenge can be sent. This is set to 50 blocks by default.

```defaultChallengeLevel``` - The value that determines the level of a challenge if there is not level specified by the challenger.
This is set to 50 by default for lvl 50 battles.

```challengeExpirationTime``` - The value that determines how long a challenge should be pending before it expires. This is
set to 60000 milliseconds / 1 minute by default.

```challengeCooldownTime``` - The value that determines how long a player must wait before sending a consecutive request. This value is 
set to 5000 milliseconds / 5 seconds by default, though players will need to wait until their existing challenge expires before sending another one.
