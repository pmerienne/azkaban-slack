# How to enable slack alerting with Azkaban
- [Create a slack bot](https://my.slack.com/services/new/bot) and note the authentication token
- Download [latest release](https://github.com/pmerienne/azkaban-slack/releases/download/0.0.1/azkaban-slack-0.0.1-dist.tar.gz)
- Extract archive content in plugins/alerter directory
- Edit plugins/alerter/azkaban-slack-X.X.X/conf/plugin.properties and set "slack.channel" and "slack.authentication.token" properties
- Voilà !

Note that alerts will be sent on slack if the flow property "alert.type" is set to "slack". Currently, flow property can only be set in the UI and scheduling options.

All properties (slack.channel, slack.alert.on.success, slack.alert.on.first.error, slack.alert.on.error) can be overwritten in the flow property.


