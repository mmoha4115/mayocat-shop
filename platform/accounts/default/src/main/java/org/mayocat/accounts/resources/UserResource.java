/*
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.accounts.resources;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mayocat.accounts.AccountsService;
import org.mayocat.accounts.meta.UserEntity;
import org.mayocat.accounts.model.Role;
import org.mayocat.accounts.model.User;
import org.mayocat.authorization.annotation.Authorized;
import org.mayocat.context.WebContext;
import org.mayocat.rest.Resource;
import org.mayocat.rest.annotation.ExistingTenant;
import org.mayocat.store.EntityAlreadyExistsException;
import org.mayocat.store.InvalidEntityException;
import org.xwiki.component.annotation.Component;

import com.yammer.metrics.annotation.Timed;

@Component(UserResource.PATH)
@Path(UserResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authorized
public class UserResource implements Resource
{
    public static final String PATH = API_ROOT_PATH + UserEntity.PATH;

    @Inject
    private AccountsService accountsService;

    @Inject
    private WebContext context;

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Authorized(roles = { Role.ADMIN })
    @ExistingTenant
    public Response addUser(@Valid User user)
    {
        try {
            if (context.getUser() == null) {
                // This can only mean there is no user recorded in database,
                // and this is the request to create the initial user.
                this.accountsService.createInitialUser(user);
            } else {
                this.accountsService.createUser(user);
            }

            return Response.ok().build();
        } catch (InvalidEntityException e) {
            throw new com.yammer.dropwizard.validation.InvalidEntityException(e.getMessage(), e.getErrors());
        } catch (EntityAlreadyExistsException e) {
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                    .entity("A user with this usernane or email already exists").type(MediaType.TEXT_PLAIN_TYPE)
                    .build());
        }
    }

    @Path("{slug}")
    @GET
    @Timed
    @ExistingTenant
    public User getUser(@PathParam("slug") String slug)
    {
        return accountsService.findUserByEmailOrUserName(slug);
    }
}
