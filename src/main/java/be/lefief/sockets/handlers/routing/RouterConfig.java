package be.lefief.sockets.handlers.routing;

import be.lefief.util.CommandTopicHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
public class RouterConfig {

    @Bean
    public Object initRouting(
            Collection<CommandTopicHandler> topicHandlerCollection,
            CommandRouter router
    ){
        topicHandlerCollection.forEach(router::register);
        return new Object();
    }

}
