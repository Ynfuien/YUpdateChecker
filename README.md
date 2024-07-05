[![Discord](https://img.shields.io/discord/1236019317208776786?style=flat&logo=discord&label=Discord&color=%235d6af2
)](https://discord.gg/kZJhKZ48j8)
# YUpdateChecker
Plugin and datapack update checker that works out of the box. No need to configure anything. Too good to be true? Well, yes. The catch is that it works only with projects downloaded from Modrinth. Thanks to their awesome [API](https://docs.modrinth.com) and a Java [wrapper](https://github.com/masecla22/Modrinth4J) by masecla22 that saved me a lot of work.

## How it works?
It's really simple. Plugin hashes a file (.jar or .zip), checks the hash using Modrinth API and logs the results. Yep, that's it. Modrinth is doing the heavy lifting by hashing every single file that is uploaded and then making it available via their API. Only caveat is - you can't change the contents of a downloaded jar or datapack. So no editing `plugin.yml` or MC functions. That's how hashing works, different file - different hash. I mean, you can change it of course, it just won't be picked by the plugin ¯\_(ツ)_/¯ 

### Additional info
- Messages are fully customizable  with a lang file, supporting [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) and [MiniMessage](https://docs.advntr.dev/minimessage/index.html)
- Commands have tab completions
- Modrinth API is currently [rate limited](https://docs.modrinth.com/#section/Ratelimits) at 300 requests per minute per IP. Which means that an update check may take a little bit longer, if you have too many plugins/datapacks. (Plugin makes at least 1 request for each plugin/datapack, and then 2 more if it was found on Modrinth)

# Download
You can download the plugin only on [Modrinth](https://modrinth.com/plugin/yupdatechecker) or compile it yourself.

# Permissions
| Permission                     | Access to                            |
|--------------------------------|--------------------------------------|
| `yupdatechecker.admin`         | /yuc \<reload \| version\>           |
| `yupdatechecker.updates`       | /updates                             |
| `yupdatechecker.updates.check` | /updates check                       |
| `yupdatechecker.updates.show`  | /updates show [plugins \| datapacks] |

# Media
Checking updates<br/>
![Gif presents an usage of the in-game commands. "/updates check" command is entered. Plugin proceeds to check updates while showing an action bar with current progress status. Upon completion a message is sent with a suggestion of running "/updates show" next. After running it, general data about the check is shown: how many plugins and datapacks were found on Modrinth, how many is up to date, and how long it took to complete the check.](https://i.imgur.com/M6m5OHq.gif)

Looking through results<br/>
![Presented are results of "/updates show plugins" and "show datapacks". Those are split into pages of 10 items, each listed under one another, with a name of a plugin/datapack and an information whether it's up to date or whether it's X versions behind. Names of the projects can be hovered for additional info like description and current/new version numbers. When clicked it will open an url with the latest plugin/datapack version that meets the config and Minecraft version requirements.](https://i.imgur.com/llPpHuL.gif)

# License
This project uses [GNU GPLv3](https://github.com/Ynfuien/YUpdateChecker/main/blob/LICENSE) license.