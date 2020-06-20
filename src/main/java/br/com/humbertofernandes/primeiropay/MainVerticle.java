package br.com.humbertofernandes.primeiropay;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  private WebClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    client = WebClient.create(vertx);
    Router router = Router.router(vertx);

    router.route("/api/primeiropay/*").handler(BodyHandler.create());
    router.post("/api/primeiropay/auth").handler(this::auth);
    router.post("/api/primeiropay/capture").handler(this::capture);
    router.post("/api/primeiropay/refund").handler(this::refund);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(
        config().getInteger("http.port", 8080), http -> {
          if (http.succeeded()) {
            startPromise.complete();
          } else {
            startPromise.fail(http.cause());
          }
        }
      );
  }

  private void auth(RoutingContext routingContext) {
    MultiMap params = routingContext.request().params();
    params.set("entityId", "8ac7a4ca6db97ef1016dbe9214e70aac");
    params.set("paymentType", "PA");
    params.set("currency", "BRL");

    client.post(443, "test.oppwa.com", "/v1/payments")
      .ssl(true)
      .bearerTokenAuthentication(getAuthorization(routingContext))
      .sendForm(params, ar -> response(routingContext, ar));
  }

  private void capture(RoutingContext routingContext) {
    MultiMap params = routingContext.request().params();
    params.set("entityId", "8ac7a4ca6db97ef1016dbe9214e70aac");
    params.set("paymentType", "CP");
    params.set("currency", "BRL");

    String id = removeParamId(params);
    String s = "/v1/payments/" + id;

    client.post(443, "test.oppwa.com", s)
      .ssl(true)
      .bearerTokenAuthentication(getAuthorization(routingContext))
      .sendForm(params, ar -> response(routingContext, ar));
  }

  private void refund(RoutingContext routingContext) {
    MultiMap params = routingContext.request().params();

    params.set("entityId", "8ac7a4ca6db97ef1016dbe9214e70aac");
    params.set("paymentType", "RF");
    params.set("currency", "BRL");

    String id = removeParamId(params);
    String s = "/v1/payments/" + id;

    HttpRequest<Buffer> bufferHttpRequest = client.post(443, "test.oppwa.com", s)
      .ssl(true)
      .bearerTokenAuthentication(getAuthorization(routingContext));
    bufferHttpRequest.sendForm(params, ar -> response(routingContext, ar));
  }

  private String removeParamId(MultiMap params) {
    String id = params.get("id");
    params.remove("id");
    return id;
  }

  private void response(RoutingContext routingContext, AsyncResult<HttpResponse<Buffer>> ar) {
    if (ar.succeeded()) {
      Object json = ar.result().body().toJson();
      JsonObject jsonBody = JsonObject.mapFrom(json);
      String returnJson;

      JsonObject jsonResult = jsonBody.getJsonObject("result");
      String code = jsonResult.getString("code");
      String description = jsonResult.getString("description");

      if (jsonBody.containsKey("id")) {
        String id = jsonBody.getString("id");
        Response response = new Response(id, code, description);
        returnJson = Json.encode(response);
      } else {
        Result result = new Result(code, description);
        returnJson = Json.encode(result);
      }

      routingContext.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(returnJson);
    } else {
      routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end("ERRO DE CONEXÃO!");
    }
  }

  private String getAuthorization(RoutingContext routingContext) {
    String authorization = routingContext.request().getHeader("Authorization");
    if (authorization != null && authorization.length() > 7) {
      authorization = authorization.substring(7);
    }
    return authorization;
  }
}
