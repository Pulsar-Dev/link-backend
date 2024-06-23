# Pulsar Link Backend
Pulsar Link Backend is the API for the whole Pulsar Link project. It is responsible for handling users, statistics, tickets, and a few other things.
It is designed to be used with the rest of the Pulsar Link project. However, it can be used on its own - There is not really much point of doing this though.

## Features
- User verification - Linking users to their Gmodstore, Discord and Steam accounts and links them to a single Pulsar Link ID. - Used with [Pulsar Link Verify]()
- Tickets - Allows users to create tickets within discord. Used with [Pulsar Link Tickets](https://github.com/Pulsar-Dev/link-tickets)
- Addon usage statistics - Provides developers with statistics on how many servers are currently using their addons - Frontend is not yet available

## Installation
This has only been tested to work on Linux. It may work on other operating systems, but it is not guaranteed - No support will be provided for other operating systems.
1. Download Java 21 or higher. 
2. Download the latest release from the [releases page](https://github.com/Pulsar-Dev/link-backend/releases/latest).
3. Create a `.env` file based on [`.env.example`](https://github.com/Pulsar-Dev/link-backend/blob/master/.env.example)
4. Run the bot with `java -jar pulsar-link-backend.jar`

## Building
1. Install Java 21 or higher.
2. Clone the repository
3. Create a `.env` file based on [`.env.example`](https://github.com/Pulsar-Dev/link-tickets/blob/master/.env.example)
4. Run the project with the provided IntelliJ run configurations. No other run configurations are provided.

### Building a release
1. Run `./gradlew buildFatJar`
2. The jar will be located in `./build/libs/`