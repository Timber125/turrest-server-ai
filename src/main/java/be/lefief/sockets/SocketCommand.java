package be.lefief.sockets;

import java.util.Map;
import java.util.UUID;


public abstract class SocketCommand {
    protected String subject;
    protected String topic;
    protected Map<String, Object> data;

    public SocketCommand(){}
    public SocketCommand(String subject, String topic, Map<String, Object> data) {
        this.subject = subject;
        this.topic = topic;
        this.data = data;
    }

    public SocketCommand setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public SocketCommand setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public SocketCommand setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public String getTopic() {
        return topic;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
