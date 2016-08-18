package com.github.pmerienne.azkaban.slack;

import java.util.Map;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import azkaban.alert.Alerter;
import azkaban.executor.ExecutableFlow;
import azkaban.sla.SlaOption;
import azkaban.utils.Props;
import azkaban.utils.Utils;

public class SlackAlerter implements Alerter {

    private final static Logger LOGGER = Logger.getLogger(SlackAlerter.class);

    private final SlackSession session;
    private Props props;

    public SlackAlerter(Props props) {
        LOGGER.info("Initializing slack alerter");
        try {
            this.props = props;
            String authenticationToken = props.getString("slack.authentication.token");
            session = SlackSessionFactory.createWebSocketSlackSession(authenticationToken);
            session.connect();
        } catch (Exception e) {
            String message = "Unable to init slack alerter: " + e.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public void alertOnSuccess(ExecutableFlow flow) throws Exception {
        if (isActivated(flow, "success")) {
            StringBuilder sb = new StringBuilder("Flow '" + flow.getFlowId() + "' has succeeded:\n");
            sb.append("\t- Start: " + formatTime(flow.getStartTime()) + "\n");
            sb.append("\t- End: " + formatTime(flow.getEndTime()) + "\n");
            sb.append("\t- Duration: " + Utils.formatDuration(flow.getStartTime(), flow.getEndTime()));

            SlackChannel channel = getChannel(flow);
            send(channel, sb.toString());
        }
    }

    public void alertOnError(ExecutableFlow flow, String... extraReasons) throws Exception {
        if (isActivated(flow, "error")) {
            StringBuilder sb = new StringBuilder("Flow '" + flow.getFlowId() + "' has failed:\n");
            sb.append("\t- Start: " + formatTime(flow.getStartTime()) + "\n");
            sb.append("\t- End: " + formatTime(flow.getEndTime()) + "\n");
            sb.append("\t- Duration: " + Utils.formatDuration(flow.getStartTime(), flow.getEndTime()));

            SlackChannel channel = getChannel(flow);
            send(channel, sb.toString());
        }
    }

    public void alertOnFirstError(ExecutableFlow flow) throws Exception {
        if (isActivated(flow, "first.error")) {
            StringBuilder sb = new StringBuilder("Flow '" + flow.getFlowId() + "' has encountered a first failure on:\n");
            sb.append("\t- Start: " + formatTime(flow.getStartTime()) + "\n");
            sb.append("\t- End: " + formatTime(flow.getEndTime()) + "\n");
            sb.append("\t- Duration: " + Utils.formatDuration(flow.getStartTime(), flow.getEndTime()));

            SlackChannel channel = getChannel(flow);
            send(channel, sb.toString());
        }
    }

    public void alertOnSla(SlaOption slaOption, String slaMessage) throws Exception {
        SlackChannel channel = getChannel(props.getString("slack.channel"));
        send(channel, "Sla Violation Alert:\n" + slaMessage);
    }

    private void send(SlackChannel channel, String message) {
        session.sendMessage(channel, message, null);
    }

    private String formatTime(long timestamp) {
        return new DateTime(timestamp).toString("yyyy-MM-dd HH:mm:ss");
    }

    private boolean isActivated(ExecutableFlow flow, String type) {
        Map<String, String> parameters = flow.getExecutionOptions().getFlowParameters();
        String property = "slack.alert.on." + type;
        String defaultActivation = props.getString(property, "false");
        String activated = parameters.getOrDefault(property, defaultActivation);

        return Boolean.parseBoolean(activated);
    }

    private SlackChannel getChannel(ExecutableFlow flow) {
        Map<String, String> parameters = flow.getExecutionOptions().getFlowParameters();
        String defaultChannel = props.getString("slack.channel");
        String name = parameters.getOrDefault("slack.channel", defaultChannel);

        return getChannel(name);
    }

    private SlackChannel getChannel(String name) {
        SlackChannel channel = session.findChannelByName(name);
        if (channel == null) {
            throw new IllegalArgumentException("Could not find slack channel " + name);
        }
        return channel;
    }
}
