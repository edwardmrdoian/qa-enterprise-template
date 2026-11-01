package com.company.qa.clients;

import com.company.qa.core.api.ApiSpecificationFactory;
import com.company.qa.models.UserRecord;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** API client = one method per endpoint, returns Response. Assertions belong in the tests, not here. */
public class UserApiClient {

    private static final String USERS = "/api/users";

    private final String baseUri;

    /** Uses api.base.url from config. */
    public UserApiClient() {
        this(null);
    }

    /** Uses a custom base URI, e.g. a WireMock server. */
    public UserApiClient(String baseUri) {
        this.baseUri = baseUri;
    }

    private RequestSpecification spec() {
        return baseUri == null ? ApiSpecificationFactory.baseSpec() : ApiSpecificationFactory.baseSpec(baseUri);
    }

    @Step("Create user")
    public Response createUser(UserRecord user) {
        return given().spec(spec()).body(user).when().post(USERS);
    }

    @Step("Get user by id: {id}")
    public Response getUser(String id) {
        return given().spec(spec()).when().get(USERS + "/" + id);
    }

    @Step("List users, page {page}")
    public Response listUsers(int page) {
        return given().spec(spec()).queryParam("page", page).when().get(USERS);
    }

    @Step("Update user {id}")
    public Response updateUser(String id, UserRecord user) {
        return given().spec(spec()).body(user).when().put(USERS + "/" + id);
    }

    @Step("Delete user {id}")
    public Response deleteUser(String id) {
        return given().spec(spec()).when().delete(USERS + "/" + id);
    }
}
