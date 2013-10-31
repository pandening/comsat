/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.LocalActorUtil;
import static co.paralleluniverse.comsat.webactors.servlet.ServletWebActors.ACTOR_KEY;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebActorServlet extends HttpServlet {
    public String redirectPath = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        setRedirectNoSessionPath(config.getInitParameter("redirectNoSessionPath"));
    }

    public WebActorServlet setRedirectNoSessionPath(String path) {
        this.redirectPath = path;
        return this;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        sendToActor(req, resp);
    }

    private void sendToActor(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        ActorRef<Object> actor = (ActorRef<Object>) req.getSession().getAttribute(ACTOR_KEY);
        if (actor == null) {
            resp.sendRedirect(redirectPath);
            return;
        }
        if (LocalActorUtil.isDone(actor)) {
            req.getSession().removeAttribute(ACTOR_KEY);
            resp.sendError(500, "Actor is dead, please login again");
            return;
        }
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        req.startAsync();
        try {
            actor.send(new ServletHttpMessage(req, resp));
        } catch (SuspendExecution ex) {
            req.getServletContext().log("Exception: ", ex);
        }
    }

}
