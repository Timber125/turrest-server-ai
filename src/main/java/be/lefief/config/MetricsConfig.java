package be.lefief.config;

import be.lefief.game.GameService;
import be.lefief.service.matchmaking.MatchmakingService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics.
 * Exposes key metrics via Micrometer for Prometheus scraping.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder gameMetrics(GameService gameService) {
        return registry -> {
            Gauge.builder("turrest.games.active", gameService, GameService::getActiveGameCount)
                    .description("Number of active games")
                    .register(registry);

            Gauge.builder("turrest.players.online", gameService, GameService::getOnlinePlayerCount)
                    .description("Number of online players")
                    .register(registry);
        };
    }

    @Bean
    public MeterBinder matchmakingMetrics(MatchmakingService matchmakingService) {
        return registry -> {
            Gauge.builder("turrest.matchmaking.queue_size", matchmakingService, MatchmakingService::getTotalQueueSize)
                    .description("Total players in matchmaking queues")
                    .register(registry);
        };
    }
}
