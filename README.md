jMonkeyEngine
=============

[![Build Status](https://github.com/jMonkeyEngine/jmonkeyengine/workflows/Build%20jMonkeyEngine/badge.svg)](https://github.com/jMonkeyEngine/jmonkeyengine/actions)

jMonkeyEngine is a 3-D game engine for adventurous Java developers. It’s open-source, cross-platform, and cutting-edge.
v3.8.0 is the latest stable version of the engine.

The engine is used by several commercial game studios and computer-science courses. Here's a taste:

![jME3 Games Mashup](https://i.imgur.com/nF8WOW6.jpg)

 - [jME powered games on IndieDB](https://www.indiedb.com/engines/jmonkeyengine/games)
 - [Boardtastic 2](https://boardtastic-2.fileplanet.com/apk)
 - [Attack of the Gelatinous Blob](https://attack-gelatinous-blob.softwareandgames.com/)
 - [Mythruna](https://mythruna.com/)
 - [PirateHell (on Steam)](https://store.steampowered.com/app/321080/Pirate_Hell/)
 - [3089 (on Steam)](https://store.steampowered.com/app/263360/3089__Futuristic_Action_RPG/)
 - [3079 (on Steam)](https://store.steampowered.com/app/259620/3079__Block_Action_RPG/)
 - [Lightspeed Frontier (on Steam)](https://store.steampowered.com/app/548650/Lightspeed_Frontier/)
 - [Skullstone](http://www.skullstonegame.com/)
 - [Spoxel (on Steam)](https://store.steampowered.com/app/746880/Spoxel/)
 - [Nine Circles of Hell (on Steam)](https://store.steampowered.com/app/1200600/Nine_Circles_of_Hell/)
 - [Leap](https://gamejolt.com/games/leap/313308)
 - [Jumping Jack Flag](http://timealias.bplaced.net/jack/)
 - [PapaSpace Flight Simulation](https://www.papaspace.at/)
 - [Cubic Nightmare (on Itch)](https://jaredbgreat.itch.io/cubic-nightmare)
 - [Chatter Games](https://chatter-games.com)
 - [Exotic Matter](https://exoticmatter.io)
 - [Demon Lord (on Google Play)](https://play.google.com/store/apps/details?id=com.dreiInitiative.demonLord&pli=1)
 - [Marvelous Marbles (on Steam)](https://store.steampowered.com/app/2244540/Marvelous_Marbles/)
 - [Boxer (on Google Play)](https://play.google.com/store/apps/details?id=com.tharg.boxer)
 - [Depthris (on Itch)](https://codewalker.itch.io/depthris)
 - [Stranded (on Itch)](https://tgiant.itch.io/stranded)
 - [The Afflicted Forests (Coming Soon to Steam)](https://www.indiedb.com/games/the-afflicted-forests)
 - [Star Colony: Beyond Horizons (on Google Play)](https://play.google.com/store/apps/details?id=game.colony.ColonyBuilder)
 - [High Impact (on Steam)](https://store.steampowered.com/app/3059050/High_Impact/)

## Getting Started

Go to https://github.com/jMonkeyEngine/sdk/releases to download the jMonkeyEngine SDK.
Read [the wiki](https://jmonkeyengine.github.io/wiki) for the installation guide and tutorials.
Join [the discussion forum](https://hub.jmonkeyengine.org/) to participate in our community,
get your questions answered, and share your projects.

Note: The master branch on GitHub is a development version of the engine and is NOT MEANT TO BE USED IN PRODUCTION.

### Technology Stack

 - windowed, multi-platform IDE derived from NetBeans
 - libraries for GUI, networking, physics, SFX, terrain, importing assets, etc.
 - platform-neutral core library for scene graph, animation, rendering, math, etc.
 - LWJGL v2/v3 (to access GLFW, OpenAL, OpenGL, and OpenVR) or Android or iOS
 - Java Virtual Machine (v8 or higher)

### Documentation

Did you miss it? Don't sweat it, [here it is again](https://jmonkeyengine.github.io/wiki).

### Contributing

Read our [contribution guide](https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/CONTRIBUTING.md).

### License

[New BSD (3-clause) License](https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/LICENSE.md)

### How to Build the Engine from Source

1. Install a Java Development Kit (JDK),
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation:
   (In other words, set it to the path of a directory/folder
   containing a "bin" that contains a Java executable.
   That path might look something like
   "C:\Program Files\Eclipse Adoptium\jdk-17.0.3.7-hotspot"
   or "/usr/lib/jvm/java-17-openjdk-amd64/" or
   "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using Fish: `set -g JAVA_HOME "` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the engine source code from GitHub:
  + using Git:
    + `git clone https://github.com/jMonkeyEngine/jmonkeyengine.git`
    + `cd jmonkeyengine`
    + `git checkout -b latest v3.7.0-stable` (unless you plan to do development)
  + using a web browser:
    + browse to [the latest release](https://github.com/jMonkeyEngine/jmonkeyengine/releases/latest)
    + follow the "Source code (zip)" link at the bottom of the page
    + save the ZIP file
    + extract the contents of the saved ZIP file
    + `cd` to the extracted directory/folder
4. Run the Gradle wrapper:
  + using Bash or Fish or PowerShell or Zsh: `./gradlew build`
  + using Windows Command Prompt: `.\gradlew build`

After a successful build,
fresh JARs will be found in "*/build/libs".

You can install the JARs to your local Maven repository:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew install`
+ using Windows Command Prompt: `.\gradlew install`

You can run the "jme3-examples" app:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew run`
+ using Windows Command Prompt: `.\gradlew run`

You can restore the project to a pristine state:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew clean`
+ using Windows Command Prompt: `.\gradlew clean`
