package com.company.qa.utils;

import com.company.qa.models.UserRecord;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestDataFactory {

    private static final Logger log = LoggerFactory.getLogger(TestDataFactory.class);

    private TestDataFactory() {}

    public static UserRecord createRandomUser() {
        return user().build();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static <T> T create(Class<T> type, long seed) {
        log.info("Creating {} with seed {}", type.getSimpleName(), seed);
        return Instancio.of(type).withSeed(seed).create();
    }

    public static final class UserBuilder {
        private long seed = initialSeed();
        private String id;
        private String name;
        private String job;
        private String email;
        private String createdAt;
        private boolean nameSet;
        private boolean jobSet;
        private boolean emailSet;

        public UserBuilder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public UserBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserBuilder name(String name) {
            this.nameSet = true;
            this.name = name;
            return this;
        }

        public UserBuilder job(String job) {
            this.jobSet = true;
            this.job = job;
            return this;
        }

        public UserBuilder email(String email) {
            this.emailSet = true;
            this.email = email;
            return this;
        }

        public UserBuilder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserRecord build() {
            log.info("Creating UserRecord with seed {}", seed);
            Faker faker = new Faker(new Random(seed));
            String generatedName = faker.name().fullName();
            String generatedJob = faker.job().title();
            String generatedEmail = faker.internet().emailAddress();
            return new UserRecord(
                    id,
                    nameSet ? name : generatedName,
                    jobSet ? job : generatedJob,
                    emailSet ? email : generatedEmail,
                    createdAt);
        }

        private static long initialSeed() {
            String configured = System.getProperty("test.seed", System.getenv("TEST_SEED"));
            if (configured == null) return ThreadLocalRandom.current().nextLong();
            try {
                return Long.parseLong(configured);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("test.seed / TEST_SEED must be an integer", e);
            }
        }
    }
}
