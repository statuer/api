package fr.lefuturiste.statuer.discord;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.ArrayList;

public class Context {
    private MessageReceivedEvent event;
    private ArrayList<String> parts;

    public final Color ERROR_COLOR = Color.decode("#E74C3C");
    public final Color INFO_COLOR = Color.decode("#2980B9");
    public final Color SUCCESS_COLOR = Color.decode("#27ae60");

    Context(MessageReceivedEvent event) {
        this.event = event;
    }

    void startLoading() {
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

    void setParts(ArrayList<String> parts) {
        this.parts = parts;
    }
}
