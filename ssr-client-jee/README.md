## Pre requisites
Must run on any Java EE 7 application server, and JDK 7.
Server can also run on web servers (Tomcat) with CDI, JAXRS and JSR-356 Websockets client
not tested though (just use an application server !)

## Configuration

### Maven dependency
- Download the source and build by yourself =).
- Add as maven dependency

        <dependency>
           <groupId>com.josue.micro</groupId>
           <artifactId>registry-client-jee7</artifactId>
           <version>1.3</version>
           </dependency>
        <dependency>
        <dependency>
            <groupId>javax</groupId>
            <artifactId>javaee-api</artifactId>
            <version>7.0</version>
            <scope>provided</scope>
        </dependency>

### JAXRS application

    @EnableDiscovery(name = "myApp")
    @ApplicationPath("rest")
    public class JaxrsApp extends Application {
    }

### Configuration
Setting the following environment variables are necessary, otherwise the application won't start.

- `APP_NAME.url`: The full address of you application, where `APP_NAME` is the name registered on `@EnableDiscovery(name = "myApp")`
- `registry.url`: The full address of the registry server


**Setting environment variables**

Export the configuration as system environment containing the appropriate key=value, it could be as system file, export, etc.


#### OR

**System property**

    -DmyApp.url=http://192.168.0.7:1234/myApp -DregistryUrl=http://192.168.0.9:8080

#### OR

**Properties file** (`registry.properties`)

    registry.url=http://192.168.0.7:8080
    myApp.url=http://192.168.0.9:8888/myApp

#### OR

**With Docker**

    docker run -it -d -p 1234:8080  -e registry.url=http://192.168.0.7:8080 -e myApp.url=http://192.168.0.9:1234/myApp myApp


Note that if deployed manually, check the application root context, which is `/registry`

### Expected behaviour
- If `APP_NAME.url` and `registry.url` are not provided the app won't start
- If registry is not running, connection retry will happen every 10s for 999 times (to be configurable)

## Acessing services
To access the service URL simply use:

    @Inject
    ServiceStore serviceStore;
      
    ServiceInstance any = serviceStore.get("serviceName"); //default roundRobin
    ServiceInstance random = serviceStore.get("serviceName", Strategy.random());
    ServiceInstance roundRobin = serviceStore.get("serviceName", Strategy.roundRobin());
    
The `Strategy` class provides a way of customising how the services are accessed. To implement your own simply extends this class.