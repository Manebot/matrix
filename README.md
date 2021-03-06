<p align="center">
  <br/>
  <img height="28" src="https://img.shields.io/github/issues/manebot/discord.svg?style=for-the-badge">
</p>

<p align="center">
<img src="https://raw.githubusercontent.com/Manebot/matrix/master/image.png">
</p>

This is the reference implementation of the **Matrix** platform for **Manebot**, my multi-platform (where platform means chat platform) chatbot framework. You can use this plugin to get Manebot to interact with your Matrix server(s).  The integration is completely seamless; simply install the Matrix plugin to Manebot, add some Matrix homeservers, and watch your existing plugins/features auto-magically work on the Matrix platform!

The support for Matrix is provided through **jmdsk** by ma1ula: https://github.com/ma1uta/jmsdk.

## Manebot

Manebot is a really neat plugin-based Java chatbot framework that allows you to write one "bot", and host it across several platforms. This plugin provides the **Matrix** "*Platform*" to Manebot, which allows Manebot to seamlessly interact with Matrix and provide all of the features your Manebot instance is set up to provide on Matrix.

#### How do I make a bot with this?

You don't have to do anything specifically to make a bot for Matrix with Manebot; the objective of Manebot is to act as middleware, abstracting the Matrix platform away from you as a developer and to provide you a platform-agnostic API to seamlessly port (or simoultaneously host) your bot in other platforms, such as Slack or Discord.

In summary, simply follow the guides on making a bot with Manebot, and just install Matrix when you're ready to test that platform!

https://github.com/Manebot/manebot/wiki/Docker-setup

## Installation

Manebot uses the **Maven** repository system to coordinate plugin and dependency installation. Because of this, you can easily install the Matrix platform plugin without interacting with your filesystem at all.

```
plugin install matrix
```

After you've installed Matrix, you should enable it:

```
plugin enable matrix
```

... and that's it! Matrix will automatically start with Manebot, and even re-install itself if you "accidentally" the associated JAR files. It's got your back.

## Connecting to a homeserver

When you've installed and enabled the Matrix plugin, you can then add it to your homeserver(s), like Synapse.  As Matrix is a distributed architecture, the Matrix plugin supports connecting to any number of homeservers.

To connect to a homeserver, use:

```
matrix homeserver add id url username password
```

Where,
 - "id" is the identifier to use for the homeserver (also used as the community ID)
 - "url" is the HTTP(S) URL of the homeserver (like https://matrix.yoursite.org:8448/)
 - "username" is the username of the bot user
 - "password" is the password of the bot user
 
Finally, you can enable your homeserver (and therefore connect to it) with,

```
matrix homeserver enable id
```

This plugin will cache the access token and device ID of a user login session in the database.  If the access token becomes invalid, a new access token will be acquired.

## Using the bot on Matrix

To use the bot on matrix, simply invite it to any room; it will join any room it is invited to immediately.  Once the bot is in a room with you, you can use `.register` to recognize yourself on the bot.

From there, you can use `.help` to look at available commands, such as `.plugin` to install additional features.

## Uninstall

```
plugin uninstall matrix
```

You should restart Manebot too to make sure it's totally unplugged. You can clean up any no longer needed plugins it required with:

```
plugin autoremove
```
