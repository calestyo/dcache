package org.dcache.webadmin.controller.impl;

import diskCacheV111.util.CacheException;
import java.security.cert.X509Certificate;
import org.dcache.auth.Subjects;
import javax.security.auth.Subject;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.dcache.auth.LoginReply;
import org.dcache.auth.LoginStrategy;
import org.dcache.auth.Password;
import org.dcache.auth.UserNamePrincipal;
import org.dcache.webadmin.controller.LogInService;
import org.dcache.webadmin.controller.exceptions.LogInServiceException;
import org.dcache.webadmin.view.beans.UserBean;
import org.dcache.webadmin.view.util.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jans
 */
public class LoginStrategyLogInService implements LogInService {

    private static final Logger _log = LoggerFactory.getLogger(LogInService.class);
    private LoginStrategy _loginStrategy;
    private int _adminGid;

    @Override
    public UserBean authenticate(String username, char[] password) throws LogInServiceException {
        Subject subject = new Subject();
        Password pass = new Password(String.valueOf(password));
        subject.getPrivateCredentials().add(pass);
        UserNamePrincipal userPrincipal = new UserNamePrincipal(username);
        subject.getPrincipals().add(userPrincipal);
        return authenticate(subject);
    }

    @Override
    public UserBean authenticate(X509Certificate[] certChain) throws LogInServiceException {
        Subject subject = new Subject();
        subject.getPublicCredentials().add(certChain);
        return authenticate(subject);
    }

    public UserBean authenticate(Subject subject) throws LogInServiceException {
        LoginReply login;
        try {
            login = _loginStrategy.login(subject);
            if (login == null) {
                throw new NullPointerException();
            }
        } catch (CacheException ex) {
            throw new LogInServiceException("failed to authenticate", ex);
        }
        UserBean user = mapLoginToUser(login);
        return user;
    }

    private UserBean mapLoginToUser(LoginReply login) {
        UserBean user = new UserBean();
        Subject subject = login.getSubject();
        user.setUsername(Subjects.getUserName(subject));
        Roles roles = mapGidsToRoles(Subjects.getGids(subject));
        user.setRoles(roles);
        return user;
    }

    private Roles mapGidsToRoles(long[] gids) {
        Roles roles = new Roles();
        boolean isAdmin = false;
        for (long gid : gids) {
            _log.debug("GID : {}", gid);
            if (gid == _adminGid) {
                roles.add(Role.ADMIN);
                isAdmin = true;
            }
        }
        if (!isAdmin) {
            roles.add(Role.USER);
        }
        return roles;
    }

    public void setLoginStrategy(LoginStrategy loginStrategy) {
        if (loginStrategy == null) {
            throw new IllegalArgumentException();
        }
        _loginStrategy = loginStrategy;
    }

    public void setAdminGid(int adminGid) {
        _log.debug("admin GID set to {}", adminGid);
        _adminGid = adminGid;
    }
}
