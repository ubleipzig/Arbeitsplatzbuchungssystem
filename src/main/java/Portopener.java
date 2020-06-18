import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class Portopener {

    public Portopener() {
        init();
    }

    private void init() {

        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        vertx.createHttpServer().requestHandler(router).listen(12106, result -> {
            if (result.succeeded()) {
                Promise.promise().complete();
            } else {
                Promise.promise().fail(result.cause());
            }
        });

        router.route().handler(CorsHandler.create(".*."));
        router.route("/booking/test*").handler(BodyHandler.create());
        router.get("/booking/test").handler(this::test);

    }

    private void test(RoutingContext rc) {
        rc.response().end("OK");
    }


    public static void main(String args[]) {
        new Portopener();
    }
}
