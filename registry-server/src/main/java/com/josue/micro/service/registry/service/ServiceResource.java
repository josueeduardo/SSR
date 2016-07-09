package com.josue.micro.service.registry.service;


import com.josue.micro.service.registry.link.LinkControl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Josue on 09/06/2016.
 */
@Path("services")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceResource implements Serializable {

    @Inject
    private ServiceControl control;

    @Inject
    private LinkControl linkControl;

    @GET
    public Response getServices(@QueryParam("name") String serviceName) {
        Map<String, List<ServiceConfig>> services = control.getServices(serviceName);
        List<ServiceConfigResponse> response = services.entrySet().stream()
                .map(e -> new ServiceConfigResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

}