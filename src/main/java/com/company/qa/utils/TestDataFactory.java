package com.company.qa.utils;

import com.company.qa.models.UserRecord;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;

public final class TestDataFactory {

    private static final Faker FAKER = new Faker();

    private TestDataFactory() {}

    public static UserRecord createRandomUser() {
        return Instancio.of(UserRecord.class)
                .set(Select.field(UserRecord.class, "name"), FAKER.name().fullName())
                .set(Select.field(UserRecord.class, "job"), FAKER.job().title())
                .set(Select.field(UserRecord.class, "email"), FAKER.internet().emailAddress())
                .ignore(Select.field(UserRecord.class, "id"))
                .ignore(Select.field(UserRecord.class, "createdAt"))
                .create();
    }
}
