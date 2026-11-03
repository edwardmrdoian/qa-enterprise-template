package com.company.qa.core.api;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.net.URI;

/**
 * Reports metadata only. Headers, query values and payloads can contain credentials or personal
 * data.
 */
public final class SafeApiReporting implements Filter {
    @Override
    public Response filter(
            FilterableRequestSpecification request,
            FilterableResponseSpecification response,
            FilterContext context) {
        URI uri = URI.create(request.getURI());
        Allure.addAttachment(
                "HTTP request",
                "text/plain",
                request.getMethod()
                        + " "
                        + uri.getScheme()
                        + "://"
                        + uri.getHost()
                        + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                        + uri.getPath(),
                ".txt");
        Response result = context.next(request, response);
        Allure.addAttachment(
                "HTTP response",
                "text/plain",
                "Status: "
                        + result.statusCode()
                        + "\nContent-Type: "
                        + result.contentType()
                        + "\nDuration (ms): "
                        + result.time(),
                ".txt");
        return result;
    }
}
