package com.jike.hotrank.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "jike-hotrank")
public class HotRankProperties {

    private Heat heat = new Heat();

    private AntiSpam antiSpam = new AntiSpam();

    @Data
    public static class Heat {
        private Weights weights = new Weights();

        @Data
        public static class Weights {
            private int like = 1;
            private int bookmark = 2;
            private int share = 3;
            private int comment = 5;

            public int of(Integer interactionType) {
                if (interactionType == null) {
                    return 0;
                }
                return switch (interactionType) {
                    case 1 -> like;
                    case 2 -> bookmark;
                    case 3 -> share;
                    case 5 -> comment;
                    default -> 0;
                };
            }
        }
    }

    @Data
    public static class AntiSpam {
        private Frequency frequency = new Frequency();
        private Device device = new Device();
        private Surge surge = new Surge();

        @Data
        public static class Frequency {
            private boolean enabled = true;
            private int windowHours = 24;
            private int maxInteractions = 50;
        }

        @Data
        public static class Device {
            private boolean enabled = true;
            private int windowHours = 24;
            private int firstPenaltyUserThreshold = 3;
            private int secondPenaltyUserThreshold = 5;
            private BigDecimal firstPenaltyMultiplier = new BigDecimal("0.5");
            private BigDecimal secondPenaltyMultiplier = new BigDecimal("0.3");
        }

        @Data
        public static class Surge {
            private boolean enabled = true;
            private int currentWindowHours = 1;
            private int historyWindowHours = 24;
            private int multiplierThreshold = 10;
            private int minimumCurrentCount = 10;
        }
    }
}
