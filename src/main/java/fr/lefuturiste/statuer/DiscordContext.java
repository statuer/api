package fr.lefuturiste.statuer;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class DiscordContext {
    private MessageReceivedEvent event;
    private ArrayList<String> parts;

    public DiscordContext(MessageReceivedEvent event) {
        this.event = event;
    }

    public void startLoading() {
        event.getChannel().sendTyping().complete();
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    public ArrayList<String> getParts() {
        return parts;
    }

    public String[] getPartsAsArray() {
        return parts.toArray(new String[0]);
    }

    public void respond(String message) {
        event.getChannel().sendMessage(message).complete();
    }

    public void respondEmbed(EmbedBuilder builder) {
        event.getChannel().sendMessage(builder.build()).complete();
    }

    public void warn() {
        warn("An unknown error as occurred");
    }

    public void success() {
        success("Success!");
    }

    public void success(String message) {
        respond(":white_check_mark: " + message);
    }

    public void warn(String message) {
        respond(":warning: " + message);
    }

    public void usageString(String message) {
        respond(":interrobang: " + message);
    }

    public void setParts(ArrayList<String> parts) {
        this.parts = parts;
    }
}
